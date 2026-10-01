package com.expensemanager.dto.response;

/**
 * Handed out once, when enrolment starts.
 *
 * <p>The secret is readable here because the authenticator app needs it. It is stored encrypted and
 * never returned again.
 */
public record TotpEnrollmentResponse(String secret, String provisioningUri) {
}
