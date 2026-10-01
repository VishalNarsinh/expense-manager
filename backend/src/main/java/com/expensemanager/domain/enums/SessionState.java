package com.expensemanager.domain.enums;

/**
 * Where a login session has got to. A session that is not {@link #ACTIVE} only ever holds a
 * step-up token, which the security filter refuses outside the step-up endpoints.
 */
public enum SessionState {

	/** Fully authenticated; the session holds a full-tier token. */
	ACTIVE(TokenTier.FULL),

	/** Credentials accepted, second factor outstanding. Step-up tier only. */
	MFA_IN_PROGRESS(TokenTier.STEP_UP),

	/** Logged out or refresh rejected. No token of any tier is valid. */
	EXPIRED(null);

	private final TokenTier tokenTier;

	SessionState(TokenTier tokenTier) {
		this.tokenTier = tokenTier;
	}

	/** The token tier a session in this state may hold, or null when it may hold none. */
	public TokenTier getTokenTier() {
		return tokenTier;
	}

	public boolean isActive() {
		return this == ACTIVE;
	}
}
