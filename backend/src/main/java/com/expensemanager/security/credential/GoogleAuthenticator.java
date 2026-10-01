package com.expensemanager.security.credential;

import com.expensemanager.common.error.ErrorType;
import com.expensemanager.common.exception.UnauthorizedException;
import com.expensemanager.domain.jpa.User;
import com.expensemanager.security.google.GoogleIdTokenVerifierAdapter;
import com.expensemanager.service.helper.GoogleAccountLinker;
import org.springframework.stereotype.Component;

/** Signing in with a Google account. */
@Component
public class GoogleAuthenticator implements CredentialAuthenticator {

	private final GoogleIdTokenVerifierAdapter verifier;
	private final GoogleAccountLinker accountLinker;

	public GoogleAuthenticator(GoogleIdTokenVerifierAdapter verifier, GoogleAccountLinker accountLinker) {
		this.verifier = verifier;
		this.accountLinker = accountLinker;
	}

	@Override
	public boolean supports(Credential credential) {
		return credential instanceof Credential.GoogleIdToken;
	}

	@Override
	public User authenticate(Credential credential) {
		Credential.GoogleIdToken presented = (Credential.GoogleIdToken) credential;

		GoogleIdTokenVerifierAdapter.GoogleIdentity identity = verifier.verify(presented.idToken());
		User user = accountLinker.resolve(identity);

		if (!user.isActive()) {
			throw new UnauthorizedException("This account has been deactivated", ErrorType.LOGIN_FAILED);
		}
		return user;
	}
}
