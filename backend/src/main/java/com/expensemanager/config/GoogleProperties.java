package com.expensemanager.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/** Google sign-in configuration, bound from {@code app.google}. */
@ConfigurationProperties(prefix = "app.google")
public class GoogleProperties {

	/**
	 * OAuth client ids accepted as the token audience.
	 *
	 * <p>This is the check that makes the token ours. Google signs tokens for every application on
	 * the platform, so a validly signed token issued to a different client would otherwise be
	 * accepted here and sign its holder in as whoever it names.
	 */
	private List<String> clientIds = List.of();

	/** Whether an account may be created the first time someone signs in with Google. */
	private boolean allowSignup = true;

	public List<String> getClientIds() {
		return clientIds;
	}

	public void setClientIds(List<String> clientIds) {
		this.clientIds = clientIds;
	}

	public boolean isAllowSignup() {
		return allowSignup;
	}

	public void setAllowSignup(boolean allowSignup) {
		this.allowSignup = allowSignup;
	}
}
