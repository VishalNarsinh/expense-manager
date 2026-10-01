package com.expensemanager.security.token;

import java.time.Instant;

/** A freshly minted token and when it stops being accepted. */
public record IssuedToken(String value, Instant expiresAt) {
}
