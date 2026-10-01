package com.expensemanager.common.exception;

import com.expensemanager.common.error.ErrorType;

public class UnauthorizedException extends ApplicationException {

	public UnauthorizedException(String message) {
		super(message, ErrorType.AUTHENTICATION_FAILED, null);
	}

	public UnauthorizedException(String message, ErrorType errorType) {
		super(message, errorType, null);
	}

	public UnauthorizedException(String message, ErrorType errorType, String param) {
		super(message, errorType, param);
	}
}
