package com.expensemanager.dto.request;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotNull;

/** The credential object the browser returns, passed through as-is. */
public record PasskeyVerifyRequest(@NotNull(message = "Credential is required") JsonNode credential) {
}
