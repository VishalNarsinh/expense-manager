package com.expensemanager.security.credential;

import com.expensemanager.common.error.ErrorType;
import com.expensemanager.common.exception.UnauthorizedException;
import com.expensemanager.domain.jpa.User;
import com.expensemanager.service.helper.UserHelper;
import org.springframework.stereotype.Component;

/** Email or username plus password. */
@Component
public class EmailPasswordAuthenticator implements CredentialAuthenticator {

	private static final String INVALID_CREDENTIALS = "Invalid credentials. Check your email or username and password.";

	private final UserHelper userHelper;

	public EmailPasswordAuthenticator(UserHelper userHelper) {
		this.userHelper = userHelper;
	}

	@Override
	public boolean supports(Credential credential) {
		return credential instanceof Credential.EmailPassword;
	}

	@Override
	public User authenticate(Credential credential) {
		Credential.EmailPassword presented = (Credential.EmailPassword) credential;

		User user = userHelper.findByIdentifier(presented.identifier())
				// Same message and timing as a wrong password, so this cannot be used to discover
				// which accounts exist.
				.orElseThrow(() -> unauthorized());

		if (!user.isActive()) {
			throw new UnauthorizedException("This account has been deactivated", ErrorType.LOGIN_FAILED);
		}

		if (!userHelper.verifyPassword(user, presented.password())) {
			throw unauthorized();
		}

		return user;
	}

	private UnauthorizedException unauthorized() {
		return new UnauthorizedException(INVALID_CREDENTIALS, ErrorType.LOGIN_FAILED);
	}
}
