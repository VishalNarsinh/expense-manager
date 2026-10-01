package com.expensemanager.security.token;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/** Token signing and lifetime configuration, bound from {@code app.token}. */
@ConfigurationProperties(prefix = "app.token")
public class TokenProperties {

	private String issuer = "expense-manager";
	private Duration accessTokenTtl = Duration.ofMinutes(15);
	private Duration refreshTokenTtl = Duration.ofDays(30);
	private Duration stepUpTokenTtl = Duration.ofMinutes(5);

	/** Base64 PKCS#8 Ed25519 private key, with or without PEM armour. */
	private String privateKey;

	/** Base64 X.509 Ed25519 public key, with or without PEM armour. */
	private String publicKey;

	public String getIssuer() {
		return issuer;
	}

	public void setIssuer(String issuer) {
		this.issuer = issuer;
	}

	public Duration getAccessTokenTtl() {
		return accessTokenTtl;
	}

	public void setAccessTokenTtl(Duration accessTokenTtl) {
		this.accessTokenTtl = accessTokenTtl;
	}

	public Duration getRefreshTokenTtl() {
		return refreshTokenTtl;
	}

	public void setRefreshTokenTtl(Duration refreshTokenTtl) {
		this.refreshTokenTtl = refreshTokenTtl;
	}

	public Duration getStepUpTokenTtl() {
		return stepUpTokenTtl;
	}

	public void setStepUpTokenTtl(Duration stepUpTokenTtl) {
		this.stepUpTokenTtl = stepUpTokenTtl;
	}

	public String getPrivateKey() {
		return privateKey;
	}

	public void setPrivateKey(String privateKey) {
		this.privateKey = privateKey;
	}

	public String getPublicKey() {
		return publicKey;
	}

	public void setPublicKey(String publicKey) {
		this.publicKey = publicKey;
	}
}
