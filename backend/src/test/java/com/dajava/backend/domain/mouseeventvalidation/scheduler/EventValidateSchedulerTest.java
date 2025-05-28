package com.dajava.backend.domain.mouseeventvalidation.scheduler;

import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import com.dajava.backend.domain.mouseeventsave.infra.memory.entity.PointerClickEventDocument;
import com.dajava.backend.domain.mouseeventsave.infra.memory.entity.PointerMoveEventDocument;
import com.dajava.backend.domain.mouseeventsave.infra.memory.entity.PointerScrollEventDocument;
import com.dajava.backend.domain.mouseeventsave.infra.memory.entity.SessionDataDocument;
import com.dajava.backend.domain.mouseeventvalidation.entity.SolutionEventDocument;
import com.dajava.backend.domain.mouseeventvalidation.scheduler.analyzer.ClickEventAnalyzer;
import com.dajava.backend.domain.mouseeventvalidation.scheduler.analyzer.MoveEventAnalyzer;
import com.dajava.backend.domain.mouseeventvalidation.scheduler.analyzer.ScrollEventAnalyzer;
import com.dajava.backend.domain.mouseeventvalidation.service.PointerEventDocumentService;
import com.dajava.backend.domain.mouseeventvalidation.service.SessionDataDocumentService;
import com.dajava.backend.domain.mouseeventvalidation.service.SolutionEventDocumentService;
import com.dajava.backend.domain.mouseeventsave.infra.redis.converter.EventConverter;
import com.dajava.backend.global.component.analyzer.BufferSchedulerProperties;

/*
 * es 리포지드에서 데이터를 꺼내 검증하는 스케줄러 통합테스트 입니다.
 *
 * @author NohDongHui
 * @since 2025-04-04
 */
@ExtendWith(MockitoExtension.class)
class EventValidateSchedulerTest {

	@Mock private SessionDataDocumentService sessionDataDocumentService;
	@Mock private SolutionEventDocumentService solutionEventDocumentService;
	@Mock private PointerEventDocumentService pointerEventDocumentService;

	@Mock private ClickEventAnalyzer esClickEventAnalyzer;
	@Mock private MoveEventAnalyzer esMoveEventAnalyzer;
	@Mock private ScrollEventAnalyzer esScrollEventAnalyzer;

	@Mock private BufferSchedulerProperties bufferSchedulerProperties;

	@InjectMocks
	private EventValidateScheduler scheduler;

	@BeforeEach
	void setup() {
		when(bufferSchedulerProperties.getBatchSize()).thenReturn(100);
	}

	@Test
	@DisplayName("processSession이 정상적으로 동작하는 경우")
	void testProcessSession_success() {
		// given
		String sessionId = "test-session";
		SessionDataDocument session = mock(SessionDataDocument.class);
		LocalDateTime now = LocalDateTime.now();
		long timestamp = now.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
		when(session.getSessionId()).thenReturn(sessionId);

		List<PointerClickEventDocument> clickEvents = List.of(
			PointerClickEventDocument.builder().isOutlier(false).timestamp(timestamp).build()
		);
		List<PointerMoveEventDocument> moveEvents = List.of(
			PointerMoveEventDocument.builder().isOutlier(false).timestamp(timestamp).build()
		);
		List<PointerScrollEventDocument> scrollEvents = List.of(
			PointerScrollEventDocument.builder().isOutlier(false).timestamp(timestamp).build()
		);

		when(pointerEventDocumentService.fetchAllClickEventDocumentsBySessionId(eq(sessionId), anyInt())).thenReturn(clickEvents);
		when(pointerEventDocumentService.fetchAllMoveEventDocumentsBySessionId(eq(sessionId), anyInt())).thenReturn(moveEvents);
		when(pointerEventDocumentService.fetchAllScrollEventDocumentsBySessionId(eq(sessionId), anyInt())).thenReturn(scrollEvents);

		List<SolutionEventDocument> solutionDocs = List.of(mock(SolutionEventDocument.class));

		try (MockedStatic<EventConverter> mocked = mockStatic(EventConverter.class)) {
			mocked.when(() -> EventConverter.toSolutionEventDocuments(clickEvents, moveEvents, scrollEvents))
				.thenReturn(solutionDocs);

			// when
			scheduler.processSession(session);

			// then
			verify(esClickEventAnalyzer).analyze(clickEvents);
			verify(esMoveEventAnalyzer).analyze(moveEvents);
			verify(esScrollEventAnalyzer).analyze(scrollEvents);
			verify(session).markAsVerified();
			verify(solutionEventDocumentService).saveAllSolutionEvents(solutionDocs);
		}
	}
}