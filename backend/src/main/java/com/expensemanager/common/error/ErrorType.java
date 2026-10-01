package com.expensemanager.common.error;

/** Stable, machine-readable error discriminators returned in the {@code type} field of an error. */
public enum ErrorType {

	INVALID_REQUEST("invalid_request_error"),
	VALIDATION_FAILED("validation_failed"),
	AUTHENTICATION_FAILED("authentication_failed"),
	LOGIN_FAILED("login_failed"),
	INVALID_TOKEN("invalid_token"),
	INSUFFICIENT_TOKEN_TIER("insufficient_token_tier"),
	INVALID_REFRESH_TOKEN("invalid_refresh_token"),
	INVALID_SESSION_STATE("invalid_session_state"),
	MFA_REQUIRED("mfa_required"),
	OTP_EXPIRED("otp_expired"),
	INCORRECT_OTP("incorrect_otp"),
	PASSKEY_CHALLENGE_FAILED("passkey_challenge_failed"),
	PASSKEY_ASSERTION_FAILED("passkey_assertion_failed"),
	RESOURCE_NOT_FOUND("resource_not_found"),
	RESOURCE_ALREADY_EXISTS("resource_already_exists"),
	FORBIDDEN("forbidden"),
	RATE_LIMIT_EXCEEDED("rate_limit_exceeded"),
	INTERNAL_SERVER_ERROR("internal_server_error");

	private final String value;

	ErrorType(String value) {
		this.value = value;
	}

	public String getValue() {
		return value;
	}
}
