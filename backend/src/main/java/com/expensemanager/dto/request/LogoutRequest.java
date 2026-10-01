package com.expensemanager.dto.request;

import jakarta.validation.constraints.NotBlank;

public record LogoutRequest(@NotBlank(message = "Session is required") String session) {
}
