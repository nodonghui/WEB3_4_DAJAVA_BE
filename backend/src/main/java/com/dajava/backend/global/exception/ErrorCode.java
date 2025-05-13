package com.dajava.backend.global.exception;

import static com.dajava.backend.domain.register.constant.RegisterConstant.*;

import org.springframework.http.HttpStatus;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum ErrorCode {

	// Register
	INVALID_REGISTER_REQUEST(HttpStatus.BAD_REQUEST, "요청 값에 대한 유효성 검증에 실패하였습니다"),
	ALREADY_REGISTER_URL(HttpStatus.BAD_REQUEST, "이미 등록되어 있습니다"),
	REGISTER_URL_EMPTY(HttpStatus.BAD_REQUEST, "URL 정보가 없습니다."),
	MODIFY_DATE_EXCEEDED(HttpStatus.BAD_REQUEST, "현재 종료일로부터 " + DEFAULT_REGISTER_DURATION + "일을 초과할 수 없습니다."),
	INVALID_PAGE_CAPTURE(HttpStatus.BAD_REQUEST, "pageCapture 리스트는 공백이거나 null 일 수 없습니다."),
	REGISTER_NOT_FOUND(HttpStatus.NOT_FOUND, "Register 정보를 찾을 수 없습니다."),
	MOBILE_VIEW_NOT_SUPPORTED(HttpStatus.BAD_REQUEST, "이미지 파일의 너비가 조건을 충족하지 않습니다."),

	// Solution
	SOLUTION_SERIAL_NUMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "시리얼 넘버가 없습니다."),
	SOLUTION_DATA_NOT_FOUND(HttpStatus.NOT_FOUND, "솔루션 데이터가 없습니다."),
	SOLUTION_EVENT_DATA_NOT_FOUND(HttpStatus.NOT_FOUND, "이벤트 데이터가 없습니다."),
	SOLUTION_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 솔루션이 없습니다."),
	SOLUTION_SERIAL_NUMBER_INVALID(HttpStatus.UNAUTHORIZED, "시리얼 넘버가 일치하지 않습니다."),
	SOLUTION_PASSWORD_INVALID(HttpStatus.UNAUTHORIZED, "비밀번호가 일치하지 않습니다."),
	SOLUTION_TEXT_EMPTY(HttpStatus.BAD_REQUEST, "솔루션 요청할 데이터가 없습니다."),
	SOLUTION_PARSING_ERROR(HttpStatus.BAD_REQUEST, "JSON 파싱에 오류가 발생했습니다."),
	SOLUTION_RESPONSE_ERROR(HttpStatus.BAD_REQUEST, "응답 처리 중 오류가 발생했습니다."),
	SOLUTION_EXPIRED_ERROR(HttpStatus.BAD_REQUEST, "솔루션이 이미 종료되었습니다."),
	INVALID_EVENT_TYPE(HttpStatus.BAD_REQUEST, "유효하지 않은 이벤트 타입입니다."),

	// Admin
	INVALID_ADMIN_CODE(HttpStatus.UNAUTHORIZED, "관리자 코드가 올바르지 않습니다."),
	AUTHORIZE_ERROR(HttpStatus.FORBIDDEN, "인증이 필요합니다."),

	// Event
	ALREADY_ENDED_SESSION(HttpStatus.BAD_REQUEST, "세션이 이미 종료되었습니다."),
	ALREADY_VERIFIED_SESSION(HttpStatus.BAD_REQUEST, "이미 검증이 완료된 세션 데이터 입니다."),

	EVENT_DOCUMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "ES 이벤트 데이터가 없습니다"),
	SESSION_DATA_DOCUMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "ES 세션 데이터가 없습니다"),
	ALREADY_OUTLIER_DOCUMENT(HttpStatus.BAD_REQUEST, "이미 이상치로 판별된 ES 이벤트 데이터 입니다."),
	EVENT_DTO_NOT_FOUND(HttpStatus.NOT_FOUND, "버퍼에 이벤트 DTO가 없습니다"),
	INVALID_BROWSER_WIDTH(HttpStatus.BAD_REQUEST, "입력받은 DTO의 browserWidth가 문제있습니다."),
	INVALID_VIEWPORT_HEIGHT(HttpStatus.BAD_REQUEST, "입력받은 DTO의 viewPortHeight가 문제있습니다."),
	INVALID_SCROLL_HEIGHT(HttpStatus.BAD_REQUEST, "입력받은 DTO의 scrollHeight가 문제있습니다."),
	INVALID_CLIENT_X(HttpStatus.BAD_REQUEST, "입력받은 DTO의 X좌표가 문제있습니다."),
	INVALID_CLIENT_Y(HttpStatus.BAD_REQUEST, "입력받은 DTO의 Y좌표가 문제있습니다."),
	INVALID_TIMESTAMP(HttpStatus.BAD_REQUEST, "입력받은 timeStamp가 문제있습니다."),
	INVALID_ELEMENT_HTML(HttpStatus.BAD_REQUEST, "입력받은 element가 문제있습니다."),

	// Response
	DATA_TO_STRING_ERROR(HttpStatus.VARIANT_ALSO_NEGOTIATES, "String 응답을 Json 형태로 변환 중 에러가 발생했습니다."),

	// Log
	SESSION_IDENTIFIER_NOT_FOUND(HttpStatus.NOT_FOUND, "sessionIdentify가 없습니다."),
	SESSION_IDENTIFIER_PARSING_NOT_FOUND(HttpStatus.BAD_REQUEST, "Key 문자열이 없습니다."),
	SESSION_IDENTIFIER_PARSING_ERROR(HttpStatus.BAD_REQUEST, "문자열 양식이 맞지 않습니다."),
	REDIS_CACHING_ERROR(HttpStatus.BAD_REQUEST, "Redis에 저장하지 못했습니다."),

	// Image
	INVALID_IMAGE_FILE(HttpStatus.BAD_REQUEST, "유효한 이미지 파일이 아닙니다."),
	IMAGE_IO_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "이미지 읽기 중 오류가 발생했습니다."),

	// heatmap
	ELASTICSEARCH_QUERY_FAILED(HttpStatus.BAD_REQUEST, "es 쿼리문이 실패했습니다"),

	// htmlparser
	PARSING_ERROR(HttpStatus.BAD_REQUEST, "검증 로직 자식 요소 파싱에서 문제가 발생했습니다."),

	// abusingBaseLine
	ABUSING_BASE_LINE_NOT_FOUND(HttpStatus.NOT_FOUND, "검증 로직 자식 요소 파싱에서 문제가 발생했습니다."),
	ALREADY_PRIOR_AVERAGE(HttpStatus.BAD_REQUEST, "이미 priorAverage가 설정되어있습니다.");

	private final HttpStatus httpStatus;
	private final String description;
}
