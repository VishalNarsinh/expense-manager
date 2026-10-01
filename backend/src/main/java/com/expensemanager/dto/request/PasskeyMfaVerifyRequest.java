package com.expensemanager.dto.request;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PasskeyMfaVerifyRequest(
		@NotBlank(message = "Session is required") String session,
		@NotNull(message = "Credential is required") JsonNode credential) {
}
