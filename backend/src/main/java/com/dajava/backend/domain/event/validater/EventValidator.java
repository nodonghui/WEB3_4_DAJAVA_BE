package com.dajava.backend.domain.event.validater;

public interface EventValidator<T> {
	void validate(T request);
}