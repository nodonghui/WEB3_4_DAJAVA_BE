package com.dajava.backend.domain.heatmap.service;

import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Service;

import com.dajava.backend.domain.event.es.entity.SolutionEventDocument;
import com.dajava.backend.domain.heatmap.validation.ScreenWidthValidator;
import com.dajava.backend.domain.heatmap.validation.UrlEqualityValidator;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class SolutionEventManager {

	// 검증 클래스
	private static final UrlEqualityValidator urlEqualityValidator = new UrlEqualityValidator();
	private static final ScreenWidthValidator screenWidthValidator = new ScreenWidthValidator();

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

	protected static int getMaxPageHeight(List<SolutionEventDocument> events) {
		return events.stream()
			.map(SolutionEventDocument::getBrowserWidth)
			.filter(Objects::nonNull)
			.max(Integer::compare)
			.orElse(0);
	}

	protected static int getMaxPageWidth(List<SolutionEventDocument> events) {
		return events.stream()
			.map(event -> event.getScrollHeight() != null ? event.getScrollHeight() :
				(event.getViewportHeight() != null ? event.getViewportHeight() : 0))
			.max(Integer::compare)
			.orElse(0);
	}
}
