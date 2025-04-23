package com.dajava.backend.domain.heatmap.service;

import static com.dajava.backend.global.exception.ErrorCode.*;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.dajava.backend.domain.event.es.entity.SolutionEventDocument;
import com.dajava.backend.domain.event.es.repository.SolutionEventDocumentRepository;
import com.dajava.backend.domain.heatmap.exception.HeatmapException;
import com.dajava.backend.domain.heatmap.validation.ScreenWidthValidator;
import com.dajava.backend.domain.heatmap.validation.UrlEqualityValidator;

import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class SolutionEventManager {

	// 페이지 사이즈 고정 값
	private static final int PAGE_SIZE = 1000;

	// 검증 클래스
	private static final UrlEqualityValidator urlEqualityValidator = new UrlEqualityValidator();
	private static final ScreenWidthValidator screenWidthValidator = new ScreenWidthValidator();

	// Es 리포지드
	private final SolutionEventDocumentRepository solutionEventDocumentRepository;

	public SolutionEventManager(SolutionEventDocumentRepository solutionEventDocumentRepository) {
		this.solutionEventDocumentRepository = solutionEventDocumentRepository;
	}

	/**
	 * Pagenation 으로 1회 쿼리 호출시 1000개의 이벤트를 가져옵니다.
	 * sortByTimestamp 플래그로 정렬 여부를 결정합니다.
	 * 데이터가 없다면 중단되고 모든 이벤트를 반환합니다.
	 *
	 * @param serialNumber ES에 접근해 데이터를 가져오기 위한 값입니다.
	 * @param sortByTimestamp 플래그 여부에 따라 정렬을 할지 말지 결정합니다.
	 * @return List<SolutionEventDocument>
	 */
	protected static List<SolutionEventDocument> getAllEvents(String serialNumber, boolean sortByTimestamp) {
		List<SolutionEventDocument> allEvents = new ArrayList<>();
		int pageNumber = 0;
		List<SolutionEventDocument> pageData;

		do {
			PageRequest pageRequest;
			if (sortByTimestamp) {
				pageRequest = PageRequest.of(pageNumber, PAGE_SIZE, Sort.by(Sort.Direction.ASC, "timestamp"));
			} else {
				pageRequest = PageRequest.of(pageNumber, PAGE_SIZE);
			}
			try {
				pageData = solutionEventDocumentRepository.findBySerialNumber(serialNumber, pageRequest);
			} catch (ElasticsearchException ex) {
				log.error("Elasticsearch 쿼리 실패! 예외 메시지: {}", ex.getMessage());
				log.error("예외 클래스: {}", ex.getClass().getName());

				Throwable rootCause = ex.getCause(); // 혹은 getRootCause() 대신 getCause() 반복적으로 추적

				if (rootCause != null) {
					log.error("루트 원인 클래스: {}", rootCause.getClass().getName());
					log.error("루트 원인 메시지: {}", rootCause.getMessage());
				}

				// 전체 스택 트레이스 출력
				for (StackTraceElement trace : ex.getStackTrace()) {
					log.debug("TRACE: {}", trace.toString());
				}

				throw new HeatmapException(ELASTICSEARCH_QUERY_FAILED);
			}
			allEvents.addAll(pageData);
			pageNumber++;
		} while (!pageData.isEmpty());
		if (allEvents.isEmpty()) {
			throw new HeatmapException(SOLUTION_EVENT_DATA_NOT_FOUND);
		}

		return allEvents;
	}

	/**
	 * 프로토콜을 제외한 URL 비교 및 너비 범위 비교로 이벤트를 필터링하는 메서드 입니다.
	 *
	 * @param events 검증 대상 이벤트 리스트
	 * @param targetUrl 비교할 Register 의 URL
	 * @param widthRange 비교할 대상 너비 범위 값
	 * @return List<SolutionEventDocument>
	 */
	protected static List<SolutionEventDocument> getValidEvents(List<SolutionEventDocument> events, String targetUrl, int widthRange) {
		return events.stream()
			.filter(event -> urlEqualityValidator.isMatching(targetUrl, event.getPageUrl())
				&& screenWidthValidator.normalizeToWidthRange(event.getBrowserWidth()) == widthRange)
			.toList();
	}

	/**
	 * 목표 타입과 동일한 이벤트만 필터링하는 메서드 입니다.
	 *
	 * @param events 검증 대상 이벤트 리스트
	 * @param type 비교할 String 타입 값
	 * @return List<SolutionEventDocument>
	 */
	protected static List<SolutionEventDocument> getTargetEvents(List<SolutionEventDocument> events, String type) {
		return events.stream()
			.filter(event -> event.getType().equals(type))
			.toList();
	}
}
