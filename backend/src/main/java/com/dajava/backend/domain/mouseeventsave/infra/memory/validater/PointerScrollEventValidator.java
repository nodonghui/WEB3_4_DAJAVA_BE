package com.dajava.backend.domain.mouseeventsave.infra.memory.validater;

import org.springframework.stereotype.Component;

import com.dajava.backend.domain.mouseeventsave.infra.memory.dto.PointerScrollEventRequest;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class PointerScrollEventValidator implements EventValidator<PointerScrollEventRequest> {

	@Override
	public void validate(PointerScrollEventRequest request) {
		EventValidation.validateNonZeroFields(
			request.getBrowserWidth(), request.getViewportHeight(), request.getScrollHeight()
		);
		EventValidation.validateTimestamp(request.getTimestamp());
	}
}