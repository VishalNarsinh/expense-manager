package com.expensemanager.common.exception;

import com.expensemanager.common.error.ErrorType;

public class EntityExistsException extends ApplicationException {

	public EntityExistsException(String message) {
		super(message, ErrorType.RESOURCE_ALREADY_EXISTS, null);
	}

	public EntityExistsException(String message, ErrorType errorType) {
		super(message, errorType, null);
	}

	public EntityExistsException(String message, ErrorType errorType, String param) {
		super(message, errorType, param);
	}
}
