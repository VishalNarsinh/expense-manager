package com.expensemanager.dto.request;

import jakarta.validation.constraints.NotBlank;

public record TotpVerifyRequest(@NotBlank(message = "Code is required") String code) {
}
