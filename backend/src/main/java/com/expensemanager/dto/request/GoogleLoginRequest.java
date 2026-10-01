package com.expensemanager.dto.request;

import jakarta.validation.constraints.NotBlank;

/** The ID token returned by Google Identity Services in the browser. */
public record GoogleLoginRequest(@NotBlank(message = "Google token is required") String idToken) {
}
