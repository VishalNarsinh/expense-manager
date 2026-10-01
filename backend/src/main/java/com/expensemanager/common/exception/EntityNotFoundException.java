package com.expensemanager.common.exception;

import com.expensemanager.common.error.ErrorType;

public class EntityNotFoundException extends ApplicationException {

	public EntityNotFoundException(String message) {
		super(message, ErrorType.RESOURCE_NOT_FOUND, null);
	}

	public EntityNotFoundException(String message, ErrorType errorType) {
		super(message, errorType, null);
	}

	public EntityNotFoundException(String message, ErrorType errorType, String param) {
		super(message, errorType, param);
	}
}
