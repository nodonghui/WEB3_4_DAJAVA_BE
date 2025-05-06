package com.dajava.backend.domain.event.es.scheduler.vaildation;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dajava.backend.domain.event.es.entity.AbusingBaseLine;
import com.dajava.backend.domain.event.es.entity.SessionDataDocument;
import com.dajava.backend.domain.event.es.service.AbusingBaseLineService;
import com.dajava.backend.domain.event.es.service.PointerEventDocumentService;
import com.dajava.backend.domain.event.es.service.SessionDataDocumentService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AbusingCheckProcessor {

	private final PointerEventDocumentService pointerEventDocumentService;
	private final AbusingBaseLineService abusingBaseLineService;
	private final SessionDataDocumentService sessionDataDocumentService;

	@Transactional
	public void processSession(SessionDataDocument session) {
		String sessionId = session.getSessionId();
		String pageUrl = session.getPageUrl();

		long eventCount = pointerEventDocumentService.countAllEvents(sessionId, pageUrl);

		AbusingBaseLine baseline = abusingBaseLineService.getBaselineByPageUrl(pageUrl);
		long avg = baseline.getAverageEventsPerHour();
		double stddev = baseline.getStandardDeviation();
		int sampleSize = baseline.getSampleSize();
		double m2 = baseline.getM2(); // ← 새로 추가된 필드

		// 이상치 검사
		double zScore = (eventCount - avg) / stddev;

		if (zScore >= 3.0) {
			session.markAsVerified(); // 이상치 세션 → 검증된 세션 처리
		} else if (Math.abs(zScore) <= 2.0) {
			// Welford 알고리즘 기반 평균/분산 업데이트
			int newSampleSize = sampleSize + 1;
			double delta = eventCount - avg;
			double newAvg = avg + delta / newSampleSize;
			double delta2 = eventCount - newAvg;
			double newM2 = m2 + delta * delta2;
			double newVariance = newM2 / (newSampleSize - 1);
			double newStddev = Math.sqrt(newVariance);

			baseline.updateStatistics(Math.round(newAvg), newStddev, newSampleSize, newM2);
		}

		sessionDataDocumentService.save(session);
	}
}
