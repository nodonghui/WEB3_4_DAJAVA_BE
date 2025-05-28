package com.dajava.backend.domain.mouseeventsave.infra.memory.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dajava.backend.domain.mouseeventsave.infra.memory.dto.PointerClickEventRequest;
import com.dajava.backend.domain.mouseeventsave.infra.memory.dto.PointerMoveEventRequest;
import com.dajava.backend.domain.mouseeventsave.infra.memory.dto.PointerScrollEventRequest;
import com.dajava.backend.domain.mouseeventsave.infra.memory.dto.SessionDataKey;
import com.dajava.backend.domain.mouseeventsave.infra.memory.entity.SessionDataDocument;
import com.dajava.backend.domain.mouseeventsave.infra.memory.repository.SessionDataDocumentRepository;
import com.dajava.backend.global.component.buffer.EventBuffer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * EventLogServiceImpl
 * EventLogService 인터페이스 구현체
 * 각 이벤트를 통해 sessionDataKey 를 발급하고, 버퍼에 담습니다.
 *
 * @author NohDongHui, Metronon
 * @since 2025-03-24
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EventLogServiceImpl implements EventLogService {

	private final SessionCacheService sessionCacheService;
	private final EventBuffer eventBuffer;
	private final ActivityHandleService activityHandleService;
	private final SessionDataDocumentRepository sessionDataDocumentRepository;

	/**
	 * 클릭 이벤트 DTO 를 통해 sessionDataKey 를 발급하고, 버퍼에 담습니다.
	 */
	@Override
	@Transactional
	public void createClickEvent(PointerClickEventRequest request) {

		SessionDataKey sessionDataKey = new SessionDataKey(
			request.getSessionId(), request.getPageUrl(), request.getMemberSerialNumber()
		);

		// SessionDataKey 를 통해 Cache 확인, 없으면 생성
		sessionCacheService.createOrFindSessionDataDocument(sessionDataKey);

		// 클릭 이벤트 버퍼링
		eventBuffer.addClickEvent(request, sessionDataKey);
	}

	/**
	 * 무브 이벤트 DTO 를 통해 sessionDataKey 를 발급하고, 버퍼에 담습니다.
	 */
	@Override
	@Transactional
	public void createMoveEvent(PointerMoveEventRequest request) {

		SessionDataKey sessionDataKey = new SessionDataKey(
			request.getSessionId(), request.getPageUrl(), request.getMemberSerialNumber()
		);

		// SessionDataKey 를 통해 Cache 확인, 없으면 생성
		sessionCacheService.createOrFindSessionDataDocument(sessionDataKey);

		// 이동 이벤트 버퍼링
		eventBuffer.addMoveEvent(request, sessionDataKey);
	}

	/**
	 * 스크롤 이벤트 DTO 를 통해 sessionDataKey 를 발급하고, 버퍼에 담습니다.
	 */
	@Override
	@Transactional
	public void createScrollEvent(PointerScrollEventRequest request) {

		SessionDataKey sessionDataKey = new SessionDataKey(
			request.getSessionId(), request.getPageUrl(), request.getMemberSerialNumber()
		);

		// SessionDataKey 를 통해 Cache 확인, 없으면 생성
		sessionCacheService.createOrFindSessionDataDocument(sessionDataKey);

		// 스크롤 이벤트 버퍼링
		eventBuffer.addScrollEvent(request, sessionDataKey);
	}

	@Override
	@Transactional
	public void expireSession(String sessionId) {

		SessionDataDocument esData = sessionDataDocumentRepository.findBySessionId(sessionId)
			.orElseThrow();

		SessionDataKey sessionDataKey = new SessionDataKey(
			esData.getSessionId(), esData.getPageUrl(), esData.getMemberSerialNumber()
		);

		activityHandleService.processInactiveBatchForSession(sessionDataKey);
	}
}


