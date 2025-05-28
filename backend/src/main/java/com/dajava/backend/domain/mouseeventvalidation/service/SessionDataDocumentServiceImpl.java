package com.dajava.backend.domain.mouseeventvalidation.service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import com.dajava.backend.domain.mouseeventsave.infra.memory.repository.SessionDataDocumentRepository;
import com.dajava.backend.domain.mouseeventsave.infra.memory.entity.SessionDataDocument;

import lombok.RequiredArgsConstructor;

/**
 * SessionDataDocumentService 인터페이스 구현체
 *
 * @author NohDongHui
 */
@RequiredArgsConstructor
@Service
public class SessionDataDocumentServiceImpl implements SessionDataDocumentService {

	private final SessionDataDocumentRepository sessionDataDocumentRepository;

	@Override
	public Page<SessionDataDocument> getEndedSessions(int page, int size) {
		PageRequest pageRequest = PageRequest.of(page, size);
		return sessionDataDocumentRepository.findByIsSessionEndedTrueAndIsVerifiedFalse(pageRequest);
	}

	@Override
	public void save(SessionDataDocument sessionDataDocument) {
		sessionDataDocumentRepository.save(sessionDataDocument);
	}

	@Override
	public List<SessionDataDocument> getRecentSessionsInLastHour() {
		LocalDateTime now = LocalDateTime.now();
		LocalDateTime oneHourAgo = now.minusHours(1);

		long from = oneHourAgo.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
		long to = now.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();

		return sessionDataDocumentRepository.findByLastEventTimestampBetween(from, to);
	}
}
