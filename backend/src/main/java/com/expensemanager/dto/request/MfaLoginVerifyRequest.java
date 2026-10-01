package com.expensemanager.dto.request;

import jakarta.validation.constraints.NotBlank;

/** Completes a sign-in that is waiting on a second factor. */
public record MfaLoginVerifyRequest(
		@NotBlank(message = "Session is required") String session,
		@NotBlank(message = "Code is required") String code) {
}
