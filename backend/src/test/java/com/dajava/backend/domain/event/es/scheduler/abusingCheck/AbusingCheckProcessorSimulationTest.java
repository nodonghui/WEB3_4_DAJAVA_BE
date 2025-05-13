package com.dajava.backend.domain.event.es.scheduler.abusingCheck;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.dajava.backend.domain.event.es.entity.AbusingBaseLine;
import com.dajava.backend.domain.event.es.entity.SessionDataDocument;
import com.dajava.backend.domain.event.es.scheduler.vaildation.AbusingCheckProcessor;
import com.dajava.backend.domain.event.es.service.AbusingBaseLineService;
import com.dajava.backend.domain.event.es.service.PointerEventDocumentService;
import com.dajava.backend.domain.event.es.service.SessionDataDocumentService;

@ExtendWith(MockitoExtension.class)
public class AbusingCheckProcessorSimulationTest {

	@Mock
	private PointerEventDocumentService pointerEventDocumentService;

	@Mock
	private AbusingBaseLineService abusingBaseLineService;

	@Mock
	private SessionDataDocumentService sessionDataDocumentService;

	@InjectMocks
	private AbusingCheckProcessor abusingCheckProcessor;

	private final String pageUrl = "/test";

	@Test
	@DisplayName("학습 후 문제없이 z score이 계산되어 이상치 필터링이 진행됨")
	void testProcessSessionThroughLearningAndAbuseDetection() {
		AbusingBaseLine baseline = AbusingBaseLine.create(pageUrl);
		when(abusingBaseLineService.getBaselineByPageUrl(pageUrl)).thenReturn(baseline);

		// 10번 호출로 Welford 학습
		for (int i = 0; i < 11; i++) {
			long eventCount = 300 + (i % 2 == 0 ? 0 : 20); // 300, 320 반복
			String sessionId = "sess-" + i;
			SessionDataDocument session = SessionDataDocument.builder()
				.sessionId(sessionId)
				.pageUrl(pageUrl)
				.build();

			when(pointerEventDocumentService.countAllEvents(sessionId, pageUrl)).thenReturn(eventCount);

			abusingCheckProcessor.processSession(session);
		}

		// ✅ priorAverage 설정되었는지 확인
		assertEquals(11, baseline.getSampleSize());
		assertTrue(baseline.getPriorAverage() > 0);

		double bayesianAvg = baseline.calculateBayesianAverage();
		System.out.println("priorAverage: " + baseline.getPriorAverage());
		System.out.println("bayesianAverage: " + bayesianAvg);
		System.out.println("stddev: " + baseline.getStandardDeviation());

		// 11번째 세션 - 이상치 이벤트 수 (예: 900)
		String sessionId = "sess-10";
		SessionDataDocument session = SessionDataDocument.builder()
			.sessionId(sessionId)
			.pageUrl(pageUrl)
			.build();

		when(pointerEventDocumentService.countAllEvents(sessionId, pageUrl)).thenReturn(900L);
		abusingCheckProcessor.processSession(session);

		// 세션이 이상치로 판단되어 markAsVerified() 되었는지 확인
		assertTrue(session.isVerified());
	}

	@Test
	@DisplayName("샘플 10개 미만일 때는 Z-score 평가 없이 학습만 수행됨")
	void testNoZScoreEvaluationWhenSampleSizeIsLessThanThreshold() {
		AbusingBaseLine baseline = AbusingBaseLine.create(pageUrl);
		when(abusingBaseLineService.getBaselineByPageUrl(pageUrl)).thenReturn(baseline);

		// 샘플 수가 5개일 때까지 학습
		for (int i = 0; i < 5; i++) {
			String sessionId = "sess-init-" + i;
			SessionDataDocument session = SessionDataDocument.builder()
				.sessionId(sessionId)
				.pageUrl(pageUrl)
				.build();

			// 정상적인 eventCount (300)
			when(pointerEventDocumentService.countAllEvents(sessionId, pageUrl)).thenReturn(300L);
			abusingCheckProcessor.processSession(session);
		}

		assertEquals(5, baseline.getSampleSize());

		// 이후 극단적인 이벤트 수가 들어오더라도 평가되지 않음
		String sessionId = "sess-extreme";
		SessionDataDocument extremeSession = SessionDataDocument.builder()
			.sessionId(sessionId)
			.pageUrl(pageUrl)
			.build();

		// 아주 큰 eventCount
		when(pointerEventDocumentService.countAllEvents(sessionId, pageUrl)).thenReturn(5000L);
		abusingCheckProcessor.processSession(extremeSession);

		//샘플 수 < 10 이므로 Z-score 평가도 하지 않고, verified 되지 않아야 함
		assertFalse(extremeSession.isVerified(), "Z-score가 평가되어서는 안 됨 (sampleSize < 10)");
		assertEquals(5, baseline.getSampleSize(), "극단값은 무시되었어야 함");
	}
}
