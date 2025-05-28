package com.dajava.backend.domain.mouseeventsave.infra.memory.validater;

public interface EventValidator<T> {
	void validate(T request);
}