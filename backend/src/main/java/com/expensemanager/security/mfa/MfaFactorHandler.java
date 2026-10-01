package com.expensemanager.security.mfa;

import com.expensemanager.domain.enums.MfaType;
import com.expensemanager.domain.jpa.User;

/**
 * Checks one kind of second factor.
 *
 * <p>Separate from the login handlers: those decide whether a second factor is owed at all, these
 * decide whether a particular proof satisfies it. Adding a factor means adding a handler.
 */
public interface MfaFactorHandler {

	MfaType type();

	/**
	 * @throws com.expensemanager.common.exception.UnauthorizedException if the proof is not valid
	 */
	void verify(User user, String proof);
}
