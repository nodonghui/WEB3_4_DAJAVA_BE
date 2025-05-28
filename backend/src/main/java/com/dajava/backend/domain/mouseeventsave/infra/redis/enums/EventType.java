package com.dajava.backend.domain.mouseeventsave.infra.redis.enums;

import lombok.Getter;

@Getter
public enum EventType {
	CLICK("click:"),
	MOVE("move:"),
	SCROLL("scroll:");

	private final String prefix;

	EventType(String prefix) {
		this.prefix = prefix;
	}
}
