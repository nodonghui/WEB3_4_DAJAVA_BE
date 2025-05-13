package com.dajava.backend.domain.event.exception;

import com.dajava.backend.global.exception.ErrorCode;

public class AbusingBaseLineException extends RuntimeException {

	public final ErrorCode errorCode;

	public AbusingBaseLineException(final ErrorCode errorCode) {
		super(errorCode.getDescription());
		this.errorCode = errorCode;
	}
}
