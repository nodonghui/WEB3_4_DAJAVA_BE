package com.dajava.backend.domain.register.exception;

import com.dajava.backend.global.exception.ErrorCode;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RegisterException extends RuntimeException {
	public final ErrorCode errorCode;

	public RegisterException(final ErrorCode errorCode) {
		super(errorCode.getDescription());
		this.errorCode = errorCode;

		log.error("[RegisterException] ErrorCode : {}, Description: {} "
			, errorCode.getHttpStatus(), errorCode.getDescription());
	}
}
