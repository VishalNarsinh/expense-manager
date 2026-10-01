package com.expensemanager.common.exception;

import com.expensemanager.common.error.ErrorType;

public class BadRequestException extends ApplicationException {

	public BadRequestException(String message) {
		super(message, ErrorType.INVALID_REQUEST, null);
	}

	public BadRequestException(String message, ErrorType errorType) {
		super(message, errorType, null);
	}

	public BadRequestException(String message, ErrorType errorType, String param) {
		super(message, errorType, param);
	}
}
