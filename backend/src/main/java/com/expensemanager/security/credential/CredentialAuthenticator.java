package com.expensemanager.security.credential;

import com.expensemanager.domain.jpa.User;

/**
 * Verifies one kind of credential and resolves the account behind it.
 *
 * <p>Stops at proving who the caller is. Whether that is enough to sign in, and what the session
 * then looks like, belongs to the login handler chain.
 */
public interface CredentialAuthenticator {

	boolean supports(Credential credential);

	/**
	 * @throws com.expensemanager.common.exception.UnauthorizedException if the credential does not
	 *     check out
	 */
	User authenticate(Credential credential);
}
