package com.expensemanager.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Second-factor configuration, bound from {@code app.mfa}. */
@ConfigurationProperties(prefix = "app.mfa")
public class MfaProperties {

	/** Base64 AES key, 16, 24 or 32 bytes, used to encrypt TOTP secrets at rest. */
	private String totpSecretEncryptionKey;

	private int recoveryCodeCount = 10;

	/** Shown in the authenticator app alongside the account name. */
	private String issuer = "Expense Manager";

	/**
	 * How many time steps either side of now are accepted. One step tolerates ordinary clock drift;
	 * widening it lengthens the window in which an observed code is still usable.
	 */
	private int timeStepTolerance = 1;

	public String getTotpSecretEncryptionKey() {
		return totpSecretEncryptionKey;
	}

	public void setTotpSecretEncryptionKey(String totpSecretEncryptionKey) {
		this.totpSecretEncryptionKey = totpSecretEncryptionKey;
	}

	public int getRecoveryCodeCount() {
		return recoveryCodeCount;
	}

	public void setRecoveryCodeCount(int recoveryCodeCount) {
		this.recoveryCodeCount = recoveryCodeCount;
	}

	public String getIssuer() {
		return issuer;
	}

	public void setIssuer(String issuer) {
		this.issuer = issuer;
	}

	public int getTimeStepTolerance() {
		return timeStepTolerance;
	}

	public void setTimeStepTolerance(int timeStepTolerance) {
		this.timeStepTolerance = timeStepTolerance;
	}
}
