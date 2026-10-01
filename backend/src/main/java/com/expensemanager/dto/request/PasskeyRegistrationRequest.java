package com.expensemanager.dto.request;

import jakarta.validation.constraints.Size;

/** A label helps the owner tell one registered device from another. */
public record PasskeyRegistrationRequest(@Size(max = 64) String label) {
}
