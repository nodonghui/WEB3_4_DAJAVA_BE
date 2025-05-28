package com.dajava.backend.domain.mouseeventvalidation.scheduler;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.elasticsearch.UncategorizedElasticsearchException;
import org.springframework.stereotype.Component;

import com.dajava.backend.domain.mouseeventsave.infra.memory.exception.PointerEventException;
import com.dajava.backend.domain.mouseeventsave.infra.memory.entity.PointerClickEventDocument;
import com.dajava.backend.domain.mouseeventsave.infra.memory.entity.PointerMoveEventDocument;
import com.dajava.backend.domain.mouseeventsave.infra.memory.entity.PointerScrollEventDocument;
import com.dajava.backend.domain.mouseeventsave.infra.memory.entity.SessionDataDocument;
import com.dajava.backend.domain.mouseeventvalidation.entity.SolutionEventDocument;
import com.dajava.backend.domain.mouseeventvalidation.service.PointerEventDocumentService;
import com.dajava.backend.domain.mouseeventvalidation.service.SessionDataDocumentService;
import com.dajava.backend.domain.mouseeventvalidation.service.SolutionEventDocumentService;
import com.dajava.backend.domain.mouseeventsave.infra.redis.converter.EventConverter;
import com.dajava.backend.domain.mouseeventvalidation.scheduler.analyzer.ClickEventAnalyzer;
import com.dajava.backend.domain.mouseeventvalidation.scheduler.analyzer.MoveEventAnalyzer;
import com.dajava.backend.domain.mouseeventvalidation.scheduler.analyzer.ScrollEventAnalyzer;
import com.dajava.backend.global.component.analyzer.BufferSchedulerProperties;
import com.dajava.backend.utils.EventsUtils;

import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 검증 로직을 주기적으로 실행하는 스케줄러 입니다.
 * es에서 데이터를 조회합니다.
 *
 * @author NohDongHui
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EventValidateScheduler {

	private final SessionDataDocumentService sessionDataDocumentService;
	private final SolutionEventDocumentService solutionEventDocumentService;
	private final PointerEventDocumentService pointerEventDocumentService;

	private final ClickEventAnalyzer ClickEventAnalyzer;
	private final MoveEventAnalyzer MoveEventAnalyzer;
	private final ScrollEventAnalyzer ScrollEventAnalyzer;

	private final BufferSchedulerProperties bufferSchedulerProperties;



	/**
	 * 주기적으로 es에서 세션 종료된 세션 데이터를 꺼내 검증합니다.
	 * 이상치 데이터는 isoutlier가 true로 저장되며
	 * click, move, scroll 이벤트 document는 soluitonEventDocument로 변환되어 es에 저장됩니다.
	 * 한번에 많은 데이터가 메모리에 들어오는 걸 대비해 배치 처리합니다.
	 * 배치 처리 구현에 페이징을 사용했습니다.
	 * 그래도 메모리 터지는 경우 최대 데이터 상한선을 설정해 스케줄러가 처리 가능한 데이터 제한
	 */
	//@Scheduled(fixedRateString = "#{@bufferSchedulerProperties.validateEndSessionMs}")
	//@SentryMonitored(level = SentryLevel.FATAL, operation = "validate_scheduler")
	public void runScheduledValidation() {

		log.info("[ValidateScheduler] 검증 스케줄러 시작");

		int batchSize = bufferSchedulerProperties.getBatchSize();
		int page = 0;

		Page<SessionDataDocument> resultPage;

		do {
			// private boolean isVerified; 도 조건에 추가해 애초에 조회되지 않게
			resultPage = sessionDataDocumentService.getEndedSessions(page, batchSize);

			log.info("[ValidateScheduler] Batch {}: SessionData size : {}", page, resultPage.getContent().size());

			for (SessionDataDocument sessionDataDocument : resultPage.getContent()) {
				try {
					processSession(sessionDataDocument);
				} catch (PointerEventException e) {
					log.warn("[ValidateScheduler] 세션 검증 실패 (이미 검증된 세션일 수 있음): {}, {}",
						sessionDataDocument.getSessionId(), e.getMessage());
				} catch (ElasticsearchException | UncategorizedElasticsearchException e) {
					log.warn("[ValidateScheduler] 세션 ID: {} - Elasticsearch 쿼리 실패", sessionDataDocument.getSessionId());
					log.warn("[ValidateScheduler] Exception Message: {}", e.getMessage());
					log.warn("[ValidateScheduler] Full Stack Trace", e);

				} catch (Exception e) {
					log.error("[ValidateScheduler] 예상치 못한 에러 발생 - 세션 ID: {}", sessionDataDocument.getSessionId(), e);
				}
			}

			page++;

			// 가비지 컬렉션 여유를 위해 짧은 sleep (optional)
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt(); // 인터럽트 상태 복구
				log.warn("Thread sleep interrupted", e);
			}

		} while (!resultPage.isLast());
	}

	/**
	 * 세션 검증을 진행하는 메소드로 sessionDataDocument의 sessionId로 각 클릭, 무브 ,스크롤
	 * 데이터를 조회합니다. 조회 시 timestamp를 기준으로 오름차순 합니다.
	 * 3종류 이벤트 데이터를 SolutionEventDocument로 통합시켜 저장합니다.
	 *
	 */
	public void processSession(SessionDataDocument sessionDataDocument) {

		//현재 조회도 isVerified false인 값을 조회하지만 혹시 몰라 조건 추가
		if (sessionDataDocument.isVerified()) {
			log.info("[ValidateScheduler] 이미 검증된 세션 데이터 입니다 sessionId : {}", sessionDataDocument.getSessionId());
			return;
		}

		String sessionId = sessionDataDocument.getSessionId();
		log.info("[ValidateScheduler] 검증 되는 세션 아이디 : {}", sessionId);

		List<PointerClickEventDocument> clickEvents = fetchValidClickEvents(sessionId);
		List<PointerMoveEventDocument> moveEvents = fetchValidMoveEvents(sessionId);
		List<PointerScrollEventDocument> scrollEvents = fetchValidScrollEvents(sessionId);

		analyzeEvents(clickEvents, moveEvents, scrollEvents);
		saveResults(sessionDataDocument, clickEvents, moveEvents, scrollEvents);
	}

	private List<PointerClickEventDocument> fetchValidClickEvents(String sessionId) {
		List<PointerClickEventDocument> list = pointerEventDocumentService.fetchAllClickEventDocumentsBySessionId(sessionId, bufferSchedulerProperties.getBatchSize());
		EventsUtils.filterValidClickEvents(list);
		log.info("[ValidateScheduler] 검증되는 clickEvents 개수: {}", list.size());
		return list;
	}

	private List<PointerMoveEventDocument> fetchValidMoveEvents(String sessionId) {
		List<PointerMoveEventDocument> list = pointerEventDocumentService.fetchAllMoveEventDocumentsBySessionId(sessionId, bufferSchedulerProperties.getBatchSize());
		EventsUtils.filterValidMoveEvents(list);
		log.info("[ValidateScheduler] 검증되는 moveEvents 개수: {}", list.size());
		return list;
	}

	private List<PointerScrollEventDocument> fetchValidScrollEvents(String sessionId) {
		List<PointerScrollEventDocument> list = pointerEventDocumentService.fetchAllScrollEventDocumentsBySessionId(sessionId, bufferSchedulerProperties.getBatchSize());
		EventsUtils.filterValidScrollEvents(list);
		log.info("[ValidateScheduler] 검증되는 scrollEvents 개수: {}", list.size());
		return list;
	}

	private void analyzeEvents(
		List<PointerClickEventDocument> clickEvents,
		List<PointerMoveEventDocument> moveEvents,
		List<PointerScrollEventDocument> scrollEvents
	) {
		ClickEventAnalyzer.analyze(clickEvents);
		MoveEventAnalyzer.analyze(moveEvents);
		ScrollEventAnalyzer.analyze(scrollEvents);
	}

	private void saveResults(
		SessionDataDocument sessionDataDocument,
		List<PointerClickEventDocument> clickEvents,
		List<PointerMoveEventDocument> moveEvents,
		List<PointerScrollEventDocument> scrollEvents
	) {
		sessionDataDocument.markAsVerified();

		log.info("[ValidateScheduler] 검증 완료");

		List<SolutionEventDocument> solutionEvents = EventConverter.toSolutionEventDocuments(
			clickEvents, moveEvents, scrollEvents
		);

		solutionEventDocumentService.saveAllSolutionEvents(solutionEvents);
		log.info("[ValidateScheduler] 저장된 SolutionEventDocument 개수 : {}", solutionEvents.size());

		sessionDataDocumentService.save(sessionDataDocument);
	}

}
