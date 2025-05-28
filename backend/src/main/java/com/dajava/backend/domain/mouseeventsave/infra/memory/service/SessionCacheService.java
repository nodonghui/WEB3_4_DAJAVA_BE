package com.dajava.backend.domain.mouseeventsave.infra.memory.service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

import com.dajava.backend.domain.mouseeventsave.infra.memory.dto.SessionDataKey;
import com.dajava.backend.domain.mouseeventsave.infra.memory.entity.SessionDataDocument;
import com.dajava.backend.domain.mouseeventsave.infra.memory.repository.SessionDataDocumentRepository;

import lombok.RequiredArgsConstructor;

/**
 * SessionData 가 현재 Cache 에 존재하는지 확인하고, 없을 시 생성 및 Cache 에 저장하는 로직입니다.
 * computeIfAbsent 로직을 통해 Cache 에 데이터가 존재하지 않을시 생성 및 Cache 에 올려놓습니다.
 *
 * @author Metronon
 */
@Service
@RequiredArgsConstructor
public class SessionCacheService {
	private final SessionDataDocumentRepository sessionDataDocumentRepository;

	private final Map<SessionDataKey, SessionDataDocument> sessionCache = new ConcurrentHashMap<>();

	//session 엔티티 일련번호는 sessionId+url+serialNum으로 한다.
	//트랜잭션이 보장 되지 않기 때문에 중복된 데이터가 들어간 경우 원래 있던 데이터에 덮어쓰기 형태가 되어야함.
	public SessionDataDocument createOrFindSessionDataDocument(SessionDataKey key) {
		return sessionCache.computeIfAbsent(key, k ->
			// k 가 있으면
			sessionDataDocumentRepository.findByPageUrlAndSessionIdAndMemberSerialNumber(
					k.pageUrl(), k.sessionId(), k.memberSerialNumber()
				)
				// 없으면
				.orElseGet(() -> {
					SessionDataDocument newSession = SessionDataDocument.create(
						k.sessionId(),
						k.memberSerialNumber(),
						k.pageUrl(),
						System.currentTimeMillis()
					);
					return sessionDataDocumentRepository.save(newSession);
				})
		);
	}

	// DB에 반영 완료시 Cache 에서 제거하는 로직
	public void removeFromEsCache(SessionDataKey key) {
		sessionCache.remove(key);
	}
}
