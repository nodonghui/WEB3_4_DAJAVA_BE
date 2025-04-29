package com.dajava.backend.domain.event.validater;

import java.time.Instant;

import com.dajava.backend.domain.event.es.entity.SessionDataDocument;
import com.dajava.backend.domain.event.es.scheduler.vaildation.htmlparser.FSMHtmlParser;
import com.dajava.backend.domain.event.es.scheduler.vaildation.htmlparser.HtmlNode;
import com.dajava.backend.domain.event.exception.PointerEventException;
import com.dajava.backend.global.exception.ErrorCode;

import lombok.extern.slf4j.Slf4j;

/**
 *
 * 서비스 로직에서 발생하는 event 예외 처리를 담당합니다.
 *
 * @author NohDongHui
 */
@Slf4j
public class EventValidation {

	private EventValidation() {
	}

	private static final long MAX_PAST_ALLOW_MILLIS = 1000L * 60 * 60 * 24 * 365; // 1년
	private static final long MAX_FUTURE_ALLOW_MILLIS = 1000L * 60; // 1분

	public static void validateTimestamp(Long timestamp) {
		long now = Instant.now().toEpochMilli();

		if (timestamp < now - MAX_PAST_ALLOW_MILLIS) {
			log.warn("Timestamp too old: timestamp={}, now={}", timestamp, now);
			throw new PointerEventException(ErrorCode.INVALID_TIMESTAMP);
		}

		if (timestamp > now + MAX_FUTURE_ALLOW_MILLIS) {
			log.warn("Timestamp from the future: timestamp={}, now={}", timestamp, now);
			throw new PointerEventException(ErrorCode.INVALID_TIMESTAMP);
		}
	}

	public static void validateSessionExists(SessionDataDocument sessionDataDocument, String sessionId) {
		if (sessionDataDocument == null) {
			log.warn("SessionDataDocument is null for sessionId: {}", sessionId);
			throw new PointerEventException(ErrorCode.SESSION_DATA_DOCUMENT_NOT_FOUND);
		}
	}

	private static void validateBrowserWidth(Integer browserWidth) {
		if (browserWidth <= 0) {
			log.warn("Invalid browserWidth: {}", browserWidth);
			throw new PointerEventException(ErrorCode.INVALID_BROWSER_WIDTH);
		}
	}

	private static void validateViewportHeight(Integer viewportHeight) {
		if (viewportHeight <= 0) {
			log.warn("Invalid viewportHeight: {}", viewportHeight);
			throw new PointerEventException(ErrorCode.INVALID_VIEWPORT_HEIGHT);
		}
	}

	public static void validateNonZeroFields(
		Integer browserWidth,
		Integer viewportHeight,
		Integer scrollHeight
	) {
		validateBrowserWidth(browserWidth);
		validateViewportHeight(viewportHeight);

		if (scrollHeight <= 0) {
			log.warn("Invalid scrollHeight: {}", scrollHeight);
			throw new PointerEventException(ErrorCode.INVALID_SCROLL_HEIGHT);
		}
	}

	public static void validateCoordinateRange(
		Integer clientX,
		Integer clientY,
		Integer browserWidth,
		Integer viewportHeight
	) {
		validateBrowserWidth(browserWidth);
		validateViewportHeight(viewportHeight);

		if (clientX < 0 || clientX > browserWidth) {
			log.warn("Invalid clientX: {} (browserWidth: {})", clientX, browserWidth);
			throw new PointerEventException(ErrorCode.INVALID_CLIENT_X);
		}

		if (clientY < 0 || clientY > viewportHeight) {
			log.warn("Invalid clientY: {} (viewportHeight: {})", clientY, viewportHeight);
			throw new PointerEventException(ErrorCode.INVALID_CLIENT_Y);
		}
	}

	public static void validateElementHtml(String elementHtml) {
		try {
			FSMHtmlParser parser = new FSMHtmlParser();
			HtmlNode root = parser.parse(elementHtml);

			if (root == null) {
				log.warn("Parsed root is null for elementHtml: {}", elementHtml);
				throw new PointerEventException(ErrorCode.INVALID_ELEMENT_HTML);
			}

			// 추가로 필요한 검증이 있다면 여기에 삽입
			// 예를 들어, 필수 속성이나 태그 체크 등

		} catch (Exception e) {
			log.warn("elementHtml 파싱 실패: {}", e.getMessage());
			throw new PointerEventException(ErrorCode.INVALID_ELEMENT_HTML);
		}
	}
}
