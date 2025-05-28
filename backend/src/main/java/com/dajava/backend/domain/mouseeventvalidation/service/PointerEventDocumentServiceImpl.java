package com.dajava.backend.domain.mouseeventvalidation.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.dajava.backend.domain.mouseeventsave.infra.memory.repository.PointerClickEventDocumentRepository;
import com.dajava.backend.domain.mouseeventsave.infra.memory.repository.PointerMoveEventDocumentRepository;
import com.dajava.backend.domain.mouseeventsave.infra.memory.repository.PointerScrollEventDocumentRepository;
import com.dajava.backend.domain.mouseeventsave.infra.memory.entity.PointerClickEventDocument;
import com.dajava.backend.domain.mouseeventsave.infra.memory.entity.PointerMoveEventDocument;
import com.dajava.backend.domain.mouseeventsave.infra.memory.entity.PointerScrollEventDocument;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * PointerEventDocument 인터페이스 구현체
 * eventBuffer 에 존재하는 캐싱 리스트의 배치 처리를 담당하는 로직입니다.
 * 스케쥴러와 연계되어 비동기적으로 작동합니다.
 * @author NohDongHui
 */
@RequiredArgsConstructor
@Service
@Slf4j
public class PointerEventDocumentServiceImpl implements PointerEventDocumentService {

	private final PointerClickEventDocumentRepository clickEventDocumentRepository;
	private final PointerMoveEventDocumentRepository moveEventDocumentRepository;
	private final PointerScrollEventDocumentRepository scrollEventDocumentRepository;

	private <T> List<T> fetchAllEventsBySessionId(
		String sessionId,
		int batchSize,
		Function<PageRequest, Page<T>> pageFetcher,
		Supplier<Boolean> sessionExistsChecker,
		String eventType
	) {
		if (!sessionExistsChecker.get()) {
			log.info("[PointerEventDocumentService] {} 이벤트 없음: {}", eventType, sessionId);
			return Collections.emptyList();
		}

		List<T> allEvents = new ArrayList<>();
		int page = 0;
		Page<T> resultPage;

		do {
			PageRequest pageRequest = PageRequest.of(page, batchSize, Sort.by(Sort.Direction.ASC, "timestamp"));
			resultPage = pageFetcher.apply(pageRequest);
			allEvents.addAll(resultPage.getContent());
			page++;
		} while (!resultPage.isLast());

		return allEvents;
	}


	@Override
	public List<PointerClickEventDocument> fetchAllClickEventDocumentsBySessionId(String sessionId, int batchSize) {
		return fetchAllEventsBySessionId(
			sessionId,
			batchSize,
			pageRequest -> clickEventDocumentRepository.findBySessionId(sessionId, pageRequest),
			() -> clickEventDocumentRepository.existsBySessionId(sessionId),
			"Click"
		);
	}

	@Override
	public List<PointerMoveEventDocument> fetchAllMoveEventDocumentsBySessionId(String sessionId, int batchSize) {
		return fetchAllEventsBySessionId(
			sessionId,
			batchSize,
			pageRequest -> moveEventDocumentRepository.findBySessionId(sessionId, pageRequest),
			() -> moveEventDocumentRepository.existsBySessionId(sessionId),
			"Move"
		);
	}

	@Override
	public List<PointerScrollEventDocument> fetchAllScrollEventDocumentsBySessionId(String sessionId, int batchSize) {
		return fetchAllEventsBySessionId(
			sessionId,
			batchSize,
			pageRequest -> scrollEventDocumentRepository.findBySessionId(sessionId, pageRequest),
			() -> scrollEventDocumentRepository.existsBySessionId(sessionId),
			"Scroll"
		);
	}

	@Override
	public long countClickEvents(String sessionId, String pageUrl) {
		return clickEventDocumentRepository.countBySessionIdAndPageUrl(sessionId, pageUrl);
	}

	@Override
	public long countMoveEvents(String sessionId, String pageUrl) {
		return moveEventDocumentRepository.countBySessionIdAndPageUrl(sessionId, pageUrl);
	}

	@Override
	public long countScrollEvents(String sessionId, String pageUrl) {
		return scrollEventDocumentRepository.countBySessionIdAndPageUrl(sessionId, pageUrl);
	}

	@Override
	public long countAllEvents(String sessionId, String pageUrl) {
		return countClickEvents(sessionId, pageUrl)
			+ countMoveEvents(sessionId, pageUrl)
			+ countScrollEvents(sessionId, pageUrl);
	}



}
