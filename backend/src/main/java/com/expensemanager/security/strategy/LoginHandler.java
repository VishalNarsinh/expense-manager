package com.expensemanager.security.strategy;

import com.expensemanager.domain.enums.SignupMethod;
import com.expensemanager.domain.jpa.User;
import com.expensemanager.dto.response.TokenResponse;
import com.expensemanager.security.token.TokenIssuer;
import com.expensemanager.service.helper.SessionHelper;
import com.expensemanager.service.helper.UserHelper;

/**
 * Decides what a verified identity actually gets.
 *
 * <p>By the time a handler runs the credential is already proven; what remains is policy, such as
 * whether a second factor is still owed. Handlers are ordered and the first whose {@link
 * #canHandle} matches wins, so a new policy is a new handler rather than another branch in the
 * service.
 */
public abstract class LoginHandler {

	protected final SessionHelper sessionHelper;
	protected final UserHelper userHelper;
	protected final TokenIssuer tokenIssuer;

	protected LoginHandler(SessionHelper sessionHelper, UserHelper userHelper, TokenIssuer tokenIssuer) {
		this.sessionHelper = sessionHelper;
		this.userHelper = userHelper;
		this.tokenIssuer = tokenIssuer;
	}

	public abstract boolean canHandle(User user);

	public abstract TokenResponse handle(User user, SignupMethod source, LoginContext context);

	/** Where the request came from, for recording on the session. */
	public record LoginContext(String ipAddress, String userAgent) {
	}
}
