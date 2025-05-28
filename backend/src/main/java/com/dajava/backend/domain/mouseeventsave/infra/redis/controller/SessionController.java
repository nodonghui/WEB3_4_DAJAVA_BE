package com.dajava.backend.domain.mouseeventsave.infra.redis.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.dajava.backend.domain.mouseeventsave.infra.redis.service.SessionService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/v1/logs")
@RequiredArgsConstructor
@Tag(name = "SessionController", description = "세션 컨트롤러")
public class SessionController {
	final SessionService sessionService;

	@Operation(summary = "세션 종료 요청", description = "세션 종료 요청이 들어오면 해당 세션을 종료합니다.")
	@PostMapping("/end/{sessionId}")
	@ResponseStatus(HttpStatus.OK)
	public void logEnd(
		@PathVariable String sessionId
	) {
		sessionService.expireSession(sessionId);
	}
}
