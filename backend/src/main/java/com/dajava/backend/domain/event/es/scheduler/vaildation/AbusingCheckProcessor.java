package com.dajava.backend.domain.event.es.scheduler.vaildation;

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

	private static final int BASELINE_MIN_SAMPLE_SIZE = 30;

	@Transactional
	public void processSession(SessionDataDocument session) {
		String sessionId = session.getSessionId();
		String pageUrl = session.getPageUrl();

		log.debug("[AbusingCheck] 세션 처리 시작 - sessionId: {}, pageUrl: {}", sessionId, pageUrl);

		long eventCount = pointerEventDocumentService.countAllEvents(sessionId, pageUrl);
		log.debug("[AbusingCheck] 이벤트 개수 조회 완료 - sessionId: {}, eventCount: {}", sessionId, eventCount);

		AbusingBaseLine baseline = abusingBaseLineService.getBaselineByPageUrl(pageUrl);
		log.debug("[AbusingCheck] baseline 조회 완료 - avg: {}, stddev: {}, sampleSize: {}",
			baseline.getAverageEventsPerHour(), baseline.getStandardDeviation(), baseline.getSampleSize());

		// baseline 학습 단계
		if (baseline.getSampleSize() < BASELINE_MIN_SAMPLE_SIZE) {
			handleInitialLearning(baseline, eventCount);
			return;
		}

		double zScore = calculateZScore(eventCount, baseline);
		log.debug("[AbusingCheck] z-score 계산 - sessionId: {}, zScore: {}", sessionId, zScore);

		if (isAbusing(zScore)) {
			log.warn("[AbusingCheck] 이상치 세션 감지 - sessionId: {}, zScore: {}", sessionId, zScore);
			session.markAsVerified();
		} else if (Math.abs(zScore) <= 2.0) {
			handleNormalSession(baseline, eventCount);
		}

		sessionDataDocumentService.save(session);
		log.debug("[AbusingCheck] 세션 저장 완료 - sessionId: {}", sessionId);
	}

	private double calculateZScore(long eventCount, AbusingBaseLine baseline) {
		return (eventCount - baseline.getAverageEventsPerHour()) / baseline.getStandardDeviation();
	}

	private boolean isAbusing(double zScore) {
		return zScore >= 3.0;
	}

	private void handleInitialLearning(AbusingBaseLine baseline, long eventCount) {
		log.debug("[AbusingCheck] 초기 baseline 학습 중 - sampleSize: {}", baseline.getSampleSize());
		updateBaselineWithWelford(baseline, eventCount);
	}

	private void handleNormalSession(AbusingBaseLine baseline, long eventCount) {
		updateBaselineWithWelford(baseline, eventCount);
		log.debug("[AbusingCheck] baseline 업데이트 완료 - newAvg: {}, newStddev: {}, newSampleSize: {}",
			baseline.getAverageEventsPerHour(), baseline.getStandardDeviation(), baseline.getSampleSize());
	}

	private void updateBaselineWithWelford(AbusingBaseLine baseline, long eventCount) {
		long avg = baseline.getAverageEventsPerHour();
		double stddev = baseline.getStandardDeviation();
		int sampleSize = baseline.getSampleSize();
		double m2 = baseline.getM2();

		int newSampleSize = sampleSize + 1;
		double delta = eventCount - avg;
		double newAvg = avg + delta / newSampleSize;
		double delta2 = eventCount - newAvg;
		double newM2 = m2 + delta * delta2;
		double newVariance = newM2 / (newSampleSize - 1);
		double newStddev = Math.sqrt(newVariance);

		baseline.updateStatistics(Math.round(newAvg), newStddev, newSampleSize, newM2);
	}
}
