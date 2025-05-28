package com.dajava.backend.domain.mouseeventsave.infra.redis.exception;

import com.dajava.backend.global.exception.ErrorCode;

public class LogException extends RuntimeException {
	public final ErrorCode errorCode;

	public LogException(final ErrorCode errorCode){
		super(errorCode.getDescription());
		this.errorCode = errorCode;
	}
}
