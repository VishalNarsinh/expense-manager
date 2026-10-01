package com.expensemanager.config;

import com.expensemanager.security.passkey.WebAuthnCredentialStore;
import com.yubico.webauthn.RelyingParty;
import com.yubico.webauthn.data.AuthenticatorSelectionCriteria;
import com.yubico.webauthn.data.RelyingPartyIdentity;
import com.yubico.webauthn.data.ResidentKeyRequirement;
import com.yubico.webauthn.data.UserVerificationRequirement;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(WebAuthnProperties.class)
public class WebAuthnConfig {

	@Bean
	public RelyingParty relyingParty(WebAuthnProperties properties, WebAuthnCredentialStore credentialStore) {
		return RelyingParty.builder()
				.identity(RelyingPartyIdentity.builder().id(properties.getRpId()).name(properties.getRpName()).build())
				.credentialRepository(credentialStore)
				.origins(properties.getOrigins())
				.allowOriginPort(properties.isAllowOriginPort())
				.allowOriginSubdomain(properties.isAllowOriginSubdomain())
				.build();
	}

	/**
	 * Discoverable credential plus user verification. The authenticator stores the account, so
	 * sign-in needs no username, and it checks a PIN or biometric itself, which is what makes a
	 * passkey two factors in one gesture rather than merely something you have.
	 */
	@Bean
	public AuthenticatorSelectionCriteria authenticatorSelectionCriteria() {
		return AuthenticatorSelectionCriteria.builder()
				.residentKey(ResidentKeyRequirement.REQUIRED)
				.userVerification(UserVerificationRequirement.REQUIRED)
				.build();
	}
}
