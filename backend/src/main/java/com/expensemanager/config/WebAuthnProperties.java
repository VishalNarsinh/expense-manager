package com.expensemanager.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.LinkedHashSet;
import java.util.Set;

/** WebAuthn relying-party configuration, bound from {@code app.webauthn}. */
@ConfigurationProperties(prefix = "app.webauthn")
public class WebAuthnProperties {

	/** The registrable domain. A credential is bound to this and will not work under another. */
	private String rpId = "localhost";

	private String rpName = "Expense Manager";

	/** Exact origins the browser may present. Anything else is refused. */
	private Set<String> origins = new LinkedHashSet<>(Set.of("http://localhost:5173"));

	private long challengeTtlMillis = 300_000;

	/** Only for local development, where the port varies. */
	private boolean allowOriginPort;

	private boolean allowOriginSubdomain;

	/** Caps how many credentials one account may register. */
	private int maxCredentialsPerUser = 10;

	public String getRpId() {
		return rpId;
	}

	public void setRpId(String rpId) {
		this.rpId = rpId;
	}

	public String getRpName() {
		return rpName;
	}

	public void setRpName(String rpName) {
		this.rpName = rpName;
	}

	public Set<String> getOrigins() {
		return origins;
	}

	public void setOrigins(Set<String> origins) {
		this.origins = origins;
	}

	public long getChallengeTtlMillis() {
		return challengeTtlMillis;
	}

	public void setChallengeTtlMillis(long challengeTtlMillis) {
		this.challengeTtlMillis = challengeTtlMillis;
	}

	public boolean isAllowOriginPort() {
		return allowOriginPort;
	}

	public void setAllowOriginPort(boolean allowOriginPort) {
		this.allowOriginPort = allowOriginPort;
	}

	public boolean isAllowOriginSubdomain() {
		return allowOriginSubdomain;
	}

	public void setAllowOriginSubdomain(boolean allowOriginSubdomain) {
		this.allowOriginSubdomain = allowOriginSubdomain;
	}

	public int getMaxCredentialsPerUser() {
		return maxCredentialsPerUser;
	}

	public void setMaxCredentialsPerUser(int maxCredentialsPerUser) {
		this.maxCredentialsPerUser = maxCredentialsPerUser;
	}
}
