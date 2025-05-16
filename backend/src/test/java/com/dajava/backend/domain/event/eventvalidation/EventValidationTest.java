package com.dajava.backend.domain.event.eventvalidation;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.dajava.backend.domain.event.es.entity.SessionDataDocument;
import com.dajava.backend.domain.event.exception.PointerEventException;
import com.dajava.backend.domain.event.validater.EventValidation;
import com.dajava.backend.global.exception.ErrorCode;

class EventValidationTest {

	@Test
	@DisplayName("validateTimestamp - 과거 데이터 예외 발생")
	void validateTimestamp_tooOld() {
		long oldTimestamp = Instant.now().minusMillis(1000L * 60 * 60 * 24 * 366).toEpochMilli(); // 1년 + 1일
		assertThatThrownBy(() -> EventValidation.validateTimestamp(oldTimestamp))
			.isInstanceOf(PointerEventException.class)
			.hasMessageContaining(ErrorCode.INVALID_TIMESTAMP.getDescription());
	}

	@Test
	@DisplayName("validateTimestamp - 미래 데이터 예외 발생")
	void validateTimestamp_future() {
		long futureTimestamp = Instant.now().plusMillis(1000L * 120).toEpochMilli(); // 2분 뒤
		assertThatThrownBy(() -> EventValidation.validateTimestamp(futureTimestamp))
			.isInstanceOf(PointerEventException.class)
			.hasMessageContaining(ErrorCode.INVALID_TIMESTAMP.getDescription());
	}

	@Test
	@DisplayName("validateTimestamp - 정상 데이터")
	void validateTimestamp_valid() {
		long nowTimestamp = Instant.now().toEpochMilli();
		assertThatCode(() -> EventValidation.validateTimestamp(nowTimestamp))
			.doesNotThrowAnyException();
	}

	@Test
	@DisplayName("validateSessionExists - 세션이 null이면 예외")
	void validateSessionExists_null() {
		assertThatThrownBy(() -> EventValidation.validateSessionExists(null, "testSession"))
			.isInstanceOf(PointerEventException.class)
			.hasMessageContaining(ErrorCode.SESSION_DATA_DOCUMENT_NOT_FOUND.getDescription());
	}

	@Test
	@DisplayName("validateSessionExists - 세션이 존재하면 정상")
	void validateSessionExists_valid() {
		SessionDataDocument dummySession = new SessionDataDocument(); // 필요한 경우 더미 객체 생성
		assertThatCode(() -> EventValidation.validateSessionExists(dummySession, "testSession"))
			.doesNotThrowAnyException();
	}

	@Test
	@DisplayName("validateNonZeroFields - 필드 중 0 이면 예외")
	void validateNonZeroFields_zeroBrowserWidth() {
		assertThatThrownBy(() -> EventValidation.validateNonZeroFields(0, 500, 1000))
			.isInstanceOf(PointerEventException.class)
			.hasMessageContaining(ErrorCode.INVALID_BROWSER_WIDTH.getDescription());
	}

	@Test
	@DisplayName("validateNonZeroFields - 정상 값")
	void validateNonZeroFields_valid() {
		assertThatCode(() -> EventValidation.validateNonZeroFields(1280, 720, 3000))
			.doesNotThrowAnyException();
	}

	@Test
	@DisplayName("validateCoordinateRange - clientX 범위 초과 시 예외")
	void validateCoordinateRange_invalidClientX() {
		assertThatThrownBy(() -> EventValidation.validateCoordinateRange(2000, 100, 1280, 720))
			.isInstanceOf(PointerEventException.class)
			.hasMessageContaining(ErrorCode.INVALID_CLIENT_X.getDescription());
	}

	@Test
	@DisplayName("validateCoordinateRange - clientY 범위 초과 시 예외")
	void validateCoordinateRange_invalidClientY() {
		assertThatThrownBy(() -> EventValidation.validateCoordinateRange(100, 800, 1280, 720))
			.isInstanceOf(PointerEventException.class)
			.hasMessageContaining(ErrorCode.INVALID_CLIENT_Y.getDescription());
	}

	@Test
	@DisplayName("validateCoordinateRange - 정상 범위")
	void validateCoordinateRange_valid() {
		assertThatCode(() -> EventValidation.validateCoordinateRange(100, 100, 1280, 720))
			.doesNotThrowAnyException();
	}

	//@Test
	@DisplayName("validateElementHtml - 비정상 HTML 파싱 실패")
	void validateElementHtml_invalid() {
		String brokenHtml = "<div><span>unclosed"; // 닫히지 않은 태그
		assertThatThrownBy(() -> EventValidation.validateElementHtml(brokenHtml))
			.isInstanceOf(PointerEventException.class)
			.hasMessageContaining(ErrorCode.INVALID_ELEMENT_HTML.name());
	}

	@Test
	@DisplayName("validateElementHtml - 정상 HTML 파싱 성공")
	void validateElementHtml_valid() {
		String validHtml = "<div><span>hello</span></div>";
		assertThatCode(() -> EventValidation.validateElementHtml(validHtml))
			.doesNotThrowAnyException();
	}
}
