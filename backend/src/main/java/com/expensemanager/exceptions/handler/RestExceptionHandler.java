package com.expensemanager.exceptions.handler;

import com.expensemanager.common.error.ApiError;
import com.expensemanager.common.error.ErrorType;
import com.expensemanager.common.exception.ApplicationException;
import com.expensemanager.common.exception.BadRequestException;
import com.expensemanager.common.exception.EntityExistsException;
import com.expensemanager.common.exception.EntityNotFoundException;
import com.expensemanager.common.exception.ForbiddenException;
import com.expensemanager.common.exception.InternalServerErrorException;
import com.expensemanager.common.exception.UnauthorizedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

/**
 * The single place an exception becomes a response body. Controllers throw; they never assemble an
 * error themselves.
 */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RestExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(RestExceptionHandler.class);

	@ExceptionHandler(BadRequestException.class)
	public ResponseEntity<ApiError> onBadRequest(BadRequestException e) {
		return respond(HttpStatus.BAD_REQUEST, e);
	}

	@ExceptionHandler(EntityNotFoundException.class)
	public ResponseEntity<ApiError> onNotFound(EntityNotFoundException e) {
		return respond(HttpStatus.NOT_FOUND, e);
	}

	@ExceptionHandler(EntityExistsException.class)
	public ResponseEntity<ApiError> onConflict(EntityExistsException e) {
		return respond(HttpStatus.CONFLICT, e);
	}

	@ExceptionHandler(UnauthorizedException.class)
	public ResponseEntity<ApiError> onUnauthorized(UnauthorizedException e) {
		return respond(HttpStatus.UNAUTHORIZED, e);
	}

	@ExceptionHandler(ForbiddenException.class)
	public ResponseEntity<ApiError> onForbidden(ForbiddenException e) {
		return respond(HttpStatus.FORBIDDEN, e);
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ApiError> onValidationFailure(MethodArgumentNotValidException e) {
		List<ApiError.Error> errors = e.getBindingResult().getFieldErrors().stream()
				.map(this::toError)
				.toList();
		return ResponseEntity.badRequest().body(ApiError.of(errors));
	}

	@ExceptionHandler(DataIntegrityViolationException.class)
	public ResponseEntity<ApiError> onDataIntegrityViolation(DataIntegrityViolationException e) {
		// The underlying message names tables, columns and constraints, so it is logged rather than
		// returned.
		log.warn("Database constraint rejected the request", e);
		return ResponseEntity.status(HttpStatus.CONFLICT)
				.body(ApiError.of(ErrorType.RESOURCE_ALREADY_EXISTS, "That change conflicts with existing data"));
	}

	@ExceptionHandler(InternalServerErrorException.class)
	public ResponseEntity<ApiError> onInternalError(InternalServerErrorException e) {
		log.error("Internal error", e);
		return respond(HttpStatus.INTERNAL_SERVER_ERROR, e);
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiError> onUnexpected(Exception e) {
		log.error("Unhandled exception", e);
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(ApiError.of(ErrorType.INTERNAL_SERVER_ERROR, "Something went wrong"));
	}

	private ApiError.Error toError(FieldError fieldError) {
		return ApiError.Error.of(ErrorType.VALIDATION_FAILED, fieldError.getDefaultMessage(), fieldError.getField());
	}

	private ResponseEntity<ApiError> respond(HttpStatus status, ApplicationException e) {
		return ResponseEntity.status(status).body(ApiError.of(e.getErrorType(), e.getMessage(), e.getParam()));
	}
}
