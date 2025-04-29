package com.dajava.backend.domain.event.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.dajava.backend.domain.event.dto.PointerClickEventRequest;
import com.dajava.backend.domain.event.dto.PointerMoveEventRequest;
import com.dajava.backend.domain.event.dto.PointerScrollEventRequest;
import com.dajava.backend.domain.event.service.EventLogService;
import com.dajava.backend.domain.event.validater.EventValidation;
import com.dajava.backend.domain.event.validater.EventValidator;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * EventLog 의 컨트롤러 입니다.
 * /v1/logs 로 들어온 마우스 이벤트 로그의 엔드포인트
 * @author Metronon
 */
@RestController
@RequestMapping("/v1/logs")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "EventLogController", description = "이벤트 로깅 컨트롤러")
public class EventLogController {

	private final EventLogService eventLogService;
	private final EventValidator<PointerClickEventRequest> pointerClickEventValidator;
	private final EventValidator<PointerMoveEventRequest> pointerMoveEventValidator;
	private final EventValidator<PointerScrollEventRequest> pointerScrollEventValidator;

	/**
	 * Click(Touch) 이벤트 로깅
	 * type 이 "click" 인 이벤트를 로깅합니다.
	 */
	//@Operation(summary = "클릭 이벤트 로깅", description = "마우스 이벤트 클릭(터치) 타입의 이벤트를 로깅합니다.")
	//@PostMapping("/click")
	@ResponseStatus(HttpStatus.OK)
	public String logClick(
		@Valid @RequestBody PointerClickEventRequest clickEventRequest
	) {
		log.debug("[ClickEvent] 수신: sessionId={}, eventId={}", clickEventRequest.getSessionId(), clickEventRequest.getEventId());
		log.debug("[ClickEvent] Validation 시작");
		pointerClickEventValidator.validate(clickEventRequest);
		log.debug("[ClickEvent] Validation 완료, 저장 시작");
		eventLogService.createClickEvent(clickEventRequest);
		log.debug("[ClickEvent] 버퍼에 저장 완료");
		return "클릭 이벤트 수신 완료";
	}

	/**
	 * mousemove 이벤트 로깅
	 * type 이 "mousemove"인 이벤트를 로깅합니다.
	 */
	//@Operation(summary = "이동 이벤트 로깅", description = "마우스 이벤트 이동 타입의 이벤트를 로깅합니다.")
	//@PostMapping("/movement")
	@ResponseStatus(HttpStatus.OK)
	public String logMovement(
		@Valid @RequestBody PointerMoveEventRequest moveEventRequest
	) {
		log.debug("[MoveEvent] 수신: sessionId={}, eventId={}", moveEventRequest.getSessionId(), moveEventRequest.getEventId());
		log.debug("[MoveEvent] Validation 시작");
		pointerMoveEventValidator.validate(moveEventRequest);
		log.debug("[MoveEvent] Validation 완료, 저장 시작");
		eventLogService.createMoveEvent(moveEventRequest);
		log.debug("[MoveEvent] 버퍼에 저장 완료");
		return "이동 이벤트 수신 완료";
	}

	/**
	 * scroll 이벤트 로깅
	 * type 이 "scroll"인 이벤트를 로깅합니다.
	 */
	//@Operation(summary = "스크롤 이벤트 로깅", description = "마우스 이벤트 스크롤 타입의 이벤트를 로깅합니다.")
	//@PostMapping("/scroll")
	@ResponseStatus(HttpStatus.OK)
	public String logScroll(
		@Valid @RequestBody PointerScrollEventRequest scrollEventRequest
	) {
		log.debug("[ScrollEvent] 수신: sessionId={}, eventId={}", scrollEventRequest.getSessionId(), scrollEventRequest.getEventId());
		log.debug("[ScrollEvent] Validation 시작");
		pointerScrollEventValidator.validate(scrollEventRequest);
		log.debug("[ScrollEvent] Validation 완료, 저장 시작");
		eventLogService.createScrollEvent(scrollEventRequest);
		log.debug("[ScrollEvent] 버퍼에 저장 완료");
		return "스크롤 이벤트 수신 완료";
	}

	//@Operation(summary = "세션 종료 요청", description = "세션 종료 요청이 들어오면 해당 세션을 종료합니다.")
	//@PostMapping("/end/{sessionId}")
	@ResponseStatus(HttpStatus.OK)
	public void logEnd(
		@PathVariable String sessionId
	) {
		log.debug("[SessionEnd] 종료 요청 수신: sessionId={}", sessionId);
		eventLogService.expireSession(sessionId);
		log.debug("[SessionEnd] 세션 종료 완료: sessionId={}", sessionId);
	}



}
