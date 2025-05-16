package com.dajava.backend.domain.event.es.scheduler.abusingCheck;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

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
	@DisplayName("11개의 학습 이후 priorAverage 설정되고, 12번째 세션에서 이상치가 감지된다")
	void testZScoreDetectionAfterLearningPhase() {
		// given
		AbusingBaseLine baseline = AbusingBaseLine.create(pageUrl);
		when(abusingBaseLineService.getBaselineByPageUrl(pageUrl)).thenReturn(baseline);

		// 11개 학습용 세션
		for (int i = 0; i < 11; i++) {
			long eventCount = 300 + (i % 2 == 0 ? 0 : 20); // 300, 320 반복
			String sessionId = "sess-" + i;

			SessionDataDocument session = SessionDataDocument.builder()
				.sessionId(sessionId)
				.pageUrl(pageUrl)
				.timestamp(Instant.now().minus(30, ChronoUnit.MINUTES).toEpochMilli()) // 0.5시간 전
				.build();

			when(pointerEventDocumentService.countAllEvents(sessionId, pageUrl)).thenReturn(eventCount);

			abusingCheckProcessor.processSession(session);
		}

		// then: priorAverage가 설정되어야 함
		assertEquals(11, baseline.getSampleSize());
		assertTrue(baseline.getPriorAverage() > 0, "priorAverage가 설정되어야 함");

		// when: 이상치 세션 (900 이벤트)
		String outlierSessionId = "sess-outlier";
		SessionDataDocument outlierSession = SessionDataDocument.builder()
			.sessionId(outlierSessionId)
			.pageUrl(pageUrl)
			.timestamp(Instant.now().minus(1, ChronoUnit.HOURS).toEpochMilli()) // 1시간 전
			.build();

		when(pointerEventDocumentService.countAllEvents(outlierSessionId, pageUrl)).thenReturn(1500L);

		abusingCheckProcessor.processSession(outlierSession);

		// then: 이상치 세션으로 마킹되어야 함
		assertTrue(outlierSession.isVerified(), "Z-score가 3 이상이면 verified 되어야 함");
		verify(sessionDataDocumentService).save(outlierSession);
	}

	@Test
	@DisplayName("샘플 수 10 미만일 경우 이상치 세션도 학습에 포함되고 verified 되지 않음")
	void testNoZScoreWhenUnderMinimumSampleSize() {
		// given
		AbusingBaseLine baseline = AbusingBaseLine.create(pageUrl);
		when(abusingBaseLineService.getBaselineByPageUrl(pageUrl)).thenReturn(baseline);

		// 초기 학습 세션 5개만
		for (int i = 0; i < 5; i++) {
			String sessionId = "sess-init-" + i;
			SessionDataDocument session = SessionDataDocument.builder()
				.sessionId(sessionId)
				.pageUrl(pageUrl)
				.timestamp(Instant.now().minus(30, ChronoUnit.MINUTES).toEpochMilli())
				.build();

			when(pointerEventDocumentService.countAllEvents(sessionId, pageUrl)).thenReturn(300L);

			abusingCheckProcessor.processSession(session);
		}

		assertEquals(5, baseline.getSampleSize());

		// when: 극단값이 들어와도 학습에만 사용되고 이상치 판단은 하지 않음
		SessionDataDocument extremeSession = SessionDataDocument.builder()
			.sessionId("sess-extreme")
			.pageUrl(pageUrl)
			.timestamp(Instant.now().minus(30, ChronoUnit.MINUTES).toEpochMilli())
			.build();

		when(pointerEventDocumentService.countAllEvents("sess-extreme", pageUrl)).thenReturn(5000L);

		abusingCheckProcessor.processSession(extremeSession);

		// then: 이상치로 판단되면 안 되고, sampleSize도 그대로 유지됨
		assertFalse(extremeSession.isVerified(), "10개 미만일 경우 이상치로 판단되지 않아야 함");
		assertEquals(5, baseline.getSampleSize(), "극단값은 무시되어야 함");
	}
}