package com.dajava.backend.utils;

import java.util.List;

import com.dajava.backend.domain.mouseeventsave.infra.memory.entity.PointerClickEventDocument;
import com.dajava.backend.domain.mouseeventsave.infra.memory.entity.PointerMoveEventDocument;
import com.dajava.backend.domain.mouseeventsave.infra.memory.entity.PointerScrollEventDocument;

public class EventsUtils {

	private EventsUtils() {
	}

	public static List<PointerClickEventDocument> filterValidClickEvents(List<PointerClickEventDocument> events) {
		return events.stream()
			.filter(PointerClickEventDocument::isValid)
			.toList();
	}

	public static List<PointerMoveEventDocument> filterValidMoveEvents(List<PointerMoveEventDocument> events) {
		return events.stream()
			.filter(PointerMoveEventDocument::isValid)
			.toList();
	}

	public static List<PointerScrollEventDocument> filterValidScrollEvents(List<PointerScrollEventDocument> events) {
		return events.stream()
			.filter(PointerScrollEventDocument::isValid)
			.toList();
	}
}
