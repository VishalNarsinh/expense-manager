package com.expensemanager.common.exception;

import com.expensemanager.common.error.ErrorType;

public class InternalServerErrorException extends ApplicationException {

	public InternalServerErrorException(String message) {
		super(message, ErrorType.INTERNAL_SERVER_ERROR, null);
	}

	public InternalServerErrorException(String message, ErrorType errorType) {
		super(message, errorType, null);
	}

	public InternalServerErrorException(String message, ErrorType errorType, String param) {
		super(message, errorType, param);
	}
}
