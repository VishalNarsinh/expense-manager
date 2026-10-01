package com.expensemanager.security.credential;

import com.expensemanager.common.exception.BadRequestException;
import com.expensemanager.domain.jpa.User;
import org.springframework.stereotype.Component;

import java.util.List;

/** Routes a credential to the authenticator that understands it. */
@Component
public class CredentialAuthenticatorRegistry {

	private final List<CredentialAuthenticator> authenticators;

	public CredentialAuthenticatorRegistry(List<CredentialAuthenticator> authenticators) {
		this.authenticators = authenticators;
	}

	public User authenticate(Credential credential) {
		return authenticators.stream()
				.filter(authenticator -> authenticator.supports(credential))
				.findFirst()
				.orElseThrow(() -> new BadRequestException("Unsupported sign-in method: " + credential.method()))
				.authenticate(credential);
	}
}
