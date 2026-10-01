package com.expensemanager.security.credential;

import com.expensemanager.common.error.ErrorType;
import com.expensemanager.common.exception.UnauthorizedException;
import com.expensemanager.domain.jpa.User;
import com.expensemanager.security.passkey.PasskeyHelper;
import com.expensemanager.service.helper.UserHelper;
import org.springframework.stereotype.Component;

/** Signing in with a passkey, no username and no password. */
@Component
public class PasskeyAuthenticator implements CredentialAuthenticator {

	private final PasskeyHelper passkeyHelper;
	private final UserHelper userHelper;

	public PasskeyAuthenticator(PasskeyHelper passkeyHelper, UserHelper userHelper) {
		this.passkeyHelper = passkeyHelper;
		this.userHelper = userHelper;
	}

	@Override
	public boolean supports(Credential credential) {
		return credential instanceof Credential.PasskeyAssertion;
	}

	@Override
	public User authenticate(Credential credential) {
		Credential.PasskeyAssertion presented = (Credential.PasskeyAssertion) credential;

		String userId = passkeyHelper.finishAuthentication(null, presented.sessionId(), presented.assertion());

		User user = userHelper.findById(userId)
				.orElseThrow(() -> new UnauthorizedException("That passkey could not be verified", ErrorType.PASSKEY_ASSERTION_FAILED));

		if (!user.isActive()) {
			throw new UnauthorizedException("This account has been deactivated", ErrorType.LOGIN_FAILED);
		}
		return user;
	}
}
