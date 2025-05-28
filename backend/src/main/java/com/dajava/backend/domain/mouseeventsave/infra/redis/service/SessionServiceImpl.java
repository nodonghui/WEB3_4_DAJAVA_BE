package com.dajava.backend.domain.mouseeventsave.infra.redis.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dajava.backend.domain.mouseeventsave.infra.memory.entity.SessionDataDocument;
import com.dajava.backend.domain.mouseeventsave.infra.memory.repository.SessionDataDocumentRepository;
import com.dajava.backend.domain.mouseeventsave.infra.redis.dto.identifier.SessionIdentifier;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SessionServiceImpl implements SessionService  {
	private final SessionDataDocumentRepository sessionDataDocumentRepository;
	private final RedisEventBatchService redisEventBatchService;
	@Override
	@Transactional
	public void expireSession(String sessionId) {
		SessionDataDocument esData = sessionDataDocumentRepository.findBySessionId(sessionId)
			.orElseThrow();
		SessionIdentifier sessionIdentifier = new SessionIdentifier(
			esData.getSessionId(), esData.getPageUrl(), esData.getMemberSerialNumber()
		);
		SessionFlagInActive(sessionIdentifier);
	}

	@Override
	@Transactional
	public void SessionFlagActive(SessionIdentifier sessionIdentifier) {
		redisEventBatchService.processBatchForSession(sessionIdentifier, false);
	}

	@Override
	@Transactional
	public void SessionFlagInActive(SessionIdentifier sessionIdentifier) {
		redisEventBatchService.processBatchForSession(sessionIdentifier, true);
	}

}
