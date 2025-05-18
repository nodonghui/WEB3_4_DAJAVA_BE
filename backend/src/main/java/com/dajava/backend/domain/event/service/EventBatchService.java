package com.dajava.backend.domain.event.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dajava.backend.domain.event.converter.PointerEventConverter;
import com.dajava.backend.domain.event.dto.PointerClickEventRequest;
import com.dajava.backend.domain.event.dto.PointerMoveEventRequest;
import com.dajava.backend.domain.event.dto.PointerScrollEventRequest;
import com.dajava.backend.domain.event.dto.SessionDataKey;
import com.dajava.backend.domain.event.es.entity.PointerClickEventDocument;
import com.dajava.backend.domain.event.es.entity.PointerMoveEventDocument;
import com.dajava.backend.domain.event.es.entity.PointerScrollEventDocument;
import com.dajava.backend.domain.event.es.entity.SessionDataDocument;
import com.dajava.backend.domain.event.es.repository.PointerClickEventDocumentRepository;
import com.dajava.backend.domain.event.es.repository.PointerMoveEventDocumentRepository;
import com.dajava.backend.domain.event.es.repository.PointerScrollEventDocumentRepository;
import com.dajava.backend.domain.event.es.repository.SessionDataDocumentRepository;
import com.dajava.backend.domain.event.exception.PointerEventException;
import com.dajava.backend.global.component.buffer.EventBuffer;
import com.dajava.backend.global.exception.ErrorCode;
import com.dajava.backend.global.utils.SessionDataKeyUtils;
import com.dajava.backend.global.utils.TimeUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * eventBuffer 에 존재하는 캐싱 리스트의 배치 처리를 담당하는 로직입니다.
 * 스케쥴러와 연계되어 비동기적으로 작동합니다.
 * @author Metronon
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EventBatchService {
	private final EventBuffer eventBuffer;
	private final SessionDataService sessionDataService;
	private final PointerClickEventDocumentRepository pointerClickEventDocumentRepository;
	private final PointerMoveEventDocumentRepository pointerMoveEventDocumentRepository;
	private final PointerScrollEventDocumentRepository pointerScrollEventDocumentRepository;
	private final SessionDataDocumentRepository sessionDataDocumentRepository;

	/**
	 * 각 이벤트 타입의 저장 로직을 배치화한 로직입니다.
	 * @param sessionDataKey sessionData 객체 생성 및 캐싱을 위해 주입합니다.
	 */
	public void processBatchForSession(SessionDataKey sessionDataKey, boolean isInactive) {

		int totalPendingEvents = countPendingEvents(sessionDataKey);

		//initData로 테스트시 throw 주석처리 후 사용
		if (totalPendingEvents == 0) {
			log.warn("배치처리할 데이터가 없습니다.");
			//throw new PointerEventException(ErrorCode.EVENT_DTO_NOT_FOUND);
		}

		// repository 에서 불린 값을 가져와서 체크
		SessionDataDocument sessionDataDocument = sessionDataService.createOrFindSessionDataDocument(sessionDataKey);

		processClickEvents(sessionDataKey);
		processMoveEvents(sessionDataKey);
		processScrollEvents(sessionDataKey);

		if (isInactive) {
			sessionDataService.removeFromEsCache(sessionDataKey);
			// 세션 종료 flag 값 true 로 변경
			sessionDataDocument.endSession();
		}


		String key = SessionDataKeyUtils.toKey(sessionDataKey);
		// 세션의 마지막 활동 시간 확인
		Long lastClickUpdate = eventBuffer.getClickBuffer().getLastUpdatedMap().get(key);
		Long lastMoveUpdate = eventBuffer.getMoveBuffer().getLastUpdatedMap().get(key);
		Long lastScrollUpdate = eventBuffer.getScrollBuffer().getLastUpdatedMap().get(key);

		// 가장 최근 업데이트 시간 계산
		Long latestUpdate = TimeUtils.getLatestUpdate(lastClickUpdate, lastMoveUpdate, lastScrollUpdate);

		// 최근 업데이트 시간 갱신 -> 어뷰징 필터링에 사용
		sessionDataDocument.updateLastEventTimeStamp(latestUpdate);

		//마지막에
		sessionDataDocumentRepository.save(sessionDataDocument);
	}

	/**
	 *
	 * @param sessionDataKey sessionDataKey 를 통해 버퍼의 이벤트 갯수를 가져옵니다
	 * @return 총 이벤트 갯수 (int)
	 */
	private int countPendingEvents(SessionDataKey sessionDataKey) {
		return eventBuffer.getClickEvents(sessionDataKey).size()
			+ eventBuffer.getMoveEvents(sessionDataKey).size()
			+ eventBuffer.getScrollEvents(sessionDataKey).size();
	}

	/**
	 * 클릭 이벤트의 버퍼에 접근 후, sessionData 에 데이터를 저장합니다.
	 * 현재 es에도 같이 저장합니다.
	 * @param sessionDataKey sessionDataKey 를 통해 eventBuffer 에 접근한 뒤, 관련 이벤트 리스트를 가져오고, 버퍼를 초기화합니다.
	 */
	private void processClickEvents(SessionDataKey sessionDataKey) {
		List<PointerClickEventRequest> clickEvents = eventBuffer.flushClickEvents(sessionDataKey);
		log.info("[배치 처리] 세션 {}: 클릭 이벤트 {} 개 처리", sessionDataKey, clickEvents.size());

		List<PointerClickEventDocument> documents = new ArrayList<>();
		for (PointerClickEventRequest request : clickEvents) {

			PointerClickEventDocument doc = PointerEventConverter.toClickEventDocument(request);
			documents.add(doc);
		}

		if (!documents.isEmpty()) {
			pointerClickEventDocumentRepository.saveAll(documents); // Elasticsearch 저장
		}
	}

	/**
	 * 무브 이벤트의 버퍼에 접근 후, sessionData 에 데이터를 저장합니다.
	 * @param sessionDataKey sessionDataKey 를 통해 eventBuffer 에 접근한 뒤, 관련 이벤트 리스트를 가져오고, 버퍼를 초기화합니다.
	 */
	private void processMoveEvents(SessionDataKey sessionDataKey) {
		List<PointerMoveEventRequest> moveEvents = eventBuffer.flushMoveEvents(sessionDataKey);
		log.info("[배치 처리] 세션 {}: 이동 이벤트 {} 개 처리", sessionDataKey, moveEvents.size());

		List<PointerMoveEventDocument> documents = new ArrayList<>();
		for (PointerMoveEventRequest request : moveEvents) {

			PointerMoveEventDocument doc = PointerEventConverter.toMoveEventDocument(request);
			documents.add(doc);
		}

		if (!documents.isEmpty()) {
			pointerMoveEventDocumentRepository.saveAll(documents);
		}
	}

	/**
	 * 스크롤 이벤트의 버퍼에 접근 후, sessionData 에 데이터를 저장합니다.
	 * @param sessionDataKey sessionDataKey 를 통해 eventBuffer 에 접근한 뒤, 관련 이벤트 리스트를 가져오고, 버퍼를 초기화합니다.
	 */
	private void processScrollEvents(SessionDataKey sessionDataKey) {
		List<PointerScrollEventRequest> scrollEvents = eventBuffer.flushScrollEvents(sessionDataKey);
		log.info("[배치 처리] 세션 {}: 스크롤 이벤트 {} 개 처리", sessionDataKey, scrollEvents.size());

		//es에 저장할 형태
		List<PointerScrollEventDocument> documents = new ArrayList<>();
		for (PointerScrollEventRequest request : scrollEvents) {

			PointerScrollEventDocument doc = PointerEventConverter.toScrollEventDocument(request);
			documents.add(doc);
		}

		if (!documents.isEmpty()) {
			pointerScrollEventDocumentRepository.saveAll(documents);
		}
	}
}

