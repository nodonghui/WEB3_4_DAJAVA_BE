package com.dajava.backend.domain.sessionvalidation.scheduler;

import java.time.Duration;
import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dajava.backend.domain.sessionvalidation.entity.AbusingBaseLine;
import com.dajava.backend.domain.mouseeventsave.infra.memory.entity.SessionDataDocument;
import com.dajava.backend.domain.sessionvalidation.service.AbusingBaseLineService;
import com.dajava.backend.domain.mouseeventvalidation.service.PointerEventDocumentService;
import com.dajava.backend.domain.mouseeventvalidation.service.SessionDataDocumentService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class AbusingCheckProcessor {

	private final PointerEventDocumentService pointerEventDocumentService;
	private final AbusingBaseLineService abusingBaseLineService;
	private final SessionDataDocumentService sessionDataDocumentService;

	private static final int BASELINE_MIN_SAMPLE_SIZE = 11;


	@Transactional
	public void processSession(SessionDataDocument session) {
		String sessionId = session.getSessionId();
		String pageUrl = session.getPageUrl();

		log.debug("[AbusingCheck] 세션 처리 시작 - sessionId: {}, pageUrl: {}", sessionId, pageUrl);

		long eventCount = pointerEventDocumentService.countAllEvents(sessionId, pageUrl);
		long eventCountPerHour = calculateEventCountPerHour(session, eventCount);
		log.debug("[AbusingCheck] 이벤트 개수 조회 완료 - sessionId: {}, eventCount: {}", sessionId, eventCountPerHour);

		AbusingBaseLine baseline = abusingBaseLineService.getBaselineByPageUrl(pageUrl);
		log.debug("[AbusingCheck] baseline 조회 완료 - avg: {}, stddev: {}, sampleSize: {}",
			baseline.getAverageEventsPerHour(), baseline.getStandardDeviation(), baseline.getSampleSize());

		// baseline 학습 단계
		// 세션수가 충분히 많으면 학습 샘플수를 늘리고 초기 데이터는 isVerified를 true로 해 검증 로직에서 제외 시키는것 고려
		// 맨 처음 들어오는 50개 세션을 무시하고 분석해도 시스템에 문제 없는 경우 50개는 학습용으로 사용하고 검증 로직에서 제외
		// 현재는 10개만 받아 priorAverage과 초기 average, m2 생성
		if (baseline.getSampleSize() < BASELINE_MIN_SAMPLE_SIZE) {
			if (eventCountPerHour > baseline.getAverageEventsPerHour() * 10 ) {
				log.warn("[AbusingCheck] 극단값 무시 - sampleSize: {}, eventCount: {}",
					baseline.getSampleSize(), eventCountPerHour);
				return;
			}
			handleInitialLearning(baseline, eventCountPerHour);
			return;
		}

		double zScore = calculateZScore(eventCountPerHour, baseline);
		log.debug("[AbusingCheck] z-score 계산 - sessionId: {}, zScore: {}", sessionId, zScore);

		if (isAbusing(zScore)) {
			log.warn("[AbusingCheck] 이상치 세션 감지 - sessionId: {}, zScore: {}", sessionId, zScore);
			session.markAsVerified();
		} else if (Math.abs(zScore) <= 2.0) {
			handleNormalSession(baseline, eventCountPerHour);
		}

		sessionDataDocumentService.save(session);
		log.debug("[AbusingCheck] 세션 저장 완료 - sessionId: {}", sessionId);
	}

	private double calculateZScore(long eventCountPerHour, AbusingBaseLine baseline) {
		double bayesianAvg = baseline.calculateBayesianAverage();
		// 페이지 성격에 따라 최소 표준편차 다르게 적용하는 거 고려
		// 뉴스 페이지 경우 최소 표준편차를 더 낮추고 게임 사이트 경우 더 높힐 필요가 있다.
		return (eventCountPerHour - bayesianAvg) / Math.max(baseline.getStandardDeviation(), 30.0); // 최소 표준편차 보정
	}

	private boolean isAbusing(double zScore) {
		return zScore >= 3.0;
	}

	private void handleInitialLearning(AbusingBaseLine baseline, long eventCountPerHour) {
		log.debug("[AbusingCheck] 초기 baseline 학습 중 - sampleSize: {}", baseline.getSampleSize());

		if (baseline.getSampleSize() == BASELINE_MIN_SAMPLE_SIZE - 1) {
			baseline.setPriorAverage(baseline.getAverageEventsPerHour());
			log.debug("[AbusingCheck] prior 설정 완료 - priorAvg: {}, priorWeight: {}",
				baseline.getPriorAverage(), baseline.getPriorWeight());
		}

		updateBaselineWithWelford(baseline, eventCountPerHour);
	}

	private void handleNormalSession(AbusingBaseLine baseline, long eventCountPerHour) {
		updateBaselineWithWelford(baseline, eventCountPerHour);
		log.debug("[AbusingCheck] baseline 업데이트 완료 - newAvg: {}, newStddev: {}, newSampleSize: {}",
			baseline.getAverageEventsPerHour(), baseline.getStandardDeviation(), baseline.getSampleSize());
	}

	private void updateBaselineWithWelford(AbusingBaseLine baseline, long eventCountPerHour) {
		long avg = baseline.getAverageEventsPerHour();
		int sampleSize = baseline.getSampleSize();
		double m2 = baseline.getM2();

		int newSampleSize = sampleSize + 1;
		double delta = (double)eventCountPerHour - avg;
		double newAvg = avg + delta / newSampleSize;
		double delta2 = eventCountPerHour - newAvg;
		double newM2 = m2 + delta * delta2;
		double newVariance = newM2 / (newSampleSize - 1);
		double newStddev = Math.sqrt(newVariance);

		baseline.updateStatistics(Math.round(newAvg), newStddev, newSampleSize, newM2);
	}

	public long calculateEventCountPerHour(SessionDataDocument session, long eventCount) {
		LocalDateTime sessionStart = session.getTimestamp();
		LocalDateTime now = LocalDateTime.now();

		Duration duration = Duration.between(sessionStart, now);
		double durationHours = duration.toMillis() / (1000.0 * 60 * 60);

		// 최소 1시간으로 보정
		durationHours = Math.max(durationHours, 1.0);

		return Math.round(eventCount / durationHours);
	}

}
