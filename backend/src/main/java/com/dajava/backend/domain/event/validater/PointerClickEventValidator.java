package com.dajava.backend.domain.event.validater;

import org.springframework.stereotype.Component;

import com.dajava.backend.domain.event.dto.PointerClickEventRequest;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class PointerClickEventValidator implements EventValidator<PointerClickEventRequest> {

	@Override
	public void validate(PointerClickEventRequest request) {
		EventValidation.validateNonZeroFields(
			request.getBrowserWidth(), request.getViewportHeight(), request.getScrollHeight()
		);
		EventValidation.validateCoordinateRange(
			request.getClientX(), request.getClientY(), request.getBrowserWidth(), request.getViewportHeight()
		);
		EventValidation.validateTimestamp(request.getTimestamp());
		EventValidation.validateElementHtml(request.getElement());
	}
}