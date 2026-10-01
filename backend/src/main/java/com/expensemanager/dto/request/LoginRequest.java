package com.expensemanager.dto.request;

import jakarta.validation.constraints.NotBlank;

/** Either the email address or the username is accepted as the identifier. */
public record LoginRequest(
		@NotBlank(message = "Email or username is required") String identifier,
		@NotBlank(message = "Password is required") String password) {
}
