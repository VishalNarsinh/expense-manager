package com.expensemanager.common.exception;

import com.expensemanager.common.error.ErrorType;

/**
 * Base for every domain exception. Controllers never build error responses; they throw one of these
 * and a single {@code @RestControllerAdvice} renders it.
 */
public abstract class ApplicationException extends RuntimeException {

	private final transient ErrorType errorType;
	private final transient String param;

	protected ApplicationException(String message, ErrorType errorType, String param) {
		super(message);
		this.errorType = errorType;
		this.param = param;
	}

	public ErrorType getErrorType() {
		return errorType;
	}

	public String getParam() {
		return param;
	}
}
