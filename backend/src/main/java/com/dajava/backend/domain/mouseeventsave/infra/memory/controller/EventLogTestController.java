package com.dajava.backend.domain.mouseeventsave.infra.memory.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.dajava.backend.domain.sessionvalidation.scheduler.EsAbusingCheckScheduler;
import com.dajava.backend.domain.mouseeventsave.infra.memory.scheduler.EsEventCleanUpScheduler;
import com.dajava.backend.domain.mouseeventvalidation.scheduler.EsEventValidateScheduler;

import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/v1/logs")
@RequiredArgsConstructor
public class EventLogTestController {

	private final EsEventValidateScheduler esEventValidateScheduler;
	private final EsEventCleanUpScheduler esEventCleanUpScheduler;
	private final EsAbusingCheckScheduler esAbusingCheckScheduler;

	@Operation(summary = "검증 스케줄러 강제 푸쉬", description = "검증 스케줄러를 강제로 푸쉬합니다.")
	@GetMapping("/test/push/validateScheduler")
	@ResponseStatus(HttpStatus.OK)
	public String logClick() {
		esEventValidateScheduler.runScheduledValidation();
		return "검증 스케줄러 동작 완료";
	}

	@Operation(summary = "삭제 스케줄러 강제 푸쉬", description = "삭제 스케줄러를 강제로 푸쉬합니다.")
	@GetMapping("/test/remove")
	@ResponseStatus(HttpStatus.OK)
	public String removeLog() {
		esEventCleanUpScheduler.deleteOldEventDocuments();
		return "click, move, scroll 로그 이벤트 삭제 완료";
	}

	@GetMapping("/test/abusing")
	@ResponseStatus(HttpStatus.OK)
	public String abusingLog() {
		esAbusingCheckScheduler.runScheduledAbusingCheck();
		return "어뷰징 스케줄러 실행 완료";
	}
}
