package com.expensemanager.common.exception;

import com.expensemanager.common.error.ErrorType;

public class ForbiddenException extends ApplicationException {

	public ForbiddenException(String message) {
		super(message, ErrorType.FORBIDDEN, null);
	}

	public ForbiddenException(String message, ErrorType errorType) {
		super(message, errorType, null);
	}

	public ForbiddenException(String message, ErrorType errorType, String param) {
		super(message, errorType, param);
	}
}
