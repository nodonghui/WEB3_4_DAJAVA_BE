package com.dajava.backend.domain.event.es.scheduler.vaildation;

import java.time.Duration;
import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dajava.backend.domain.event.es.entity.AbusingBaseLine;
import com.dajava.backend.domain.event.es.entity.SessionDataDocument;
import com.dajava.backend.domain.event.es.service.AbusingBaseLineService;
import com.dajava.backend.domain.event.es.service.PointerEventDocumentService;
import com.dajava.backend.domain.event.es.service.SessionDataDocumentService;

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
	private static final int EXTREME_SESSION_COUNT = 2000;

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
		// 초기 prior average는 경험적으로 설정하는 방향으로
		if (baseline.getSampleSize() < BASELINE_MIN_SAMPLE_SIZE) {
			if (eventCountPerHour > EXTREME_SESSION_COUNT) {
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
