package com.expensemanager.security.strategy;

import com.expensemanager.domain.enums.SessionState;
import com.expensemanager.domain.enums.SignupMethod;
import com.expensemanager.domain.enums.TokenTier;
import com.expensemanager.domain.jpa.User;
import com.expensemanager.domain.jpa.UserSession;
import com.expensemanager.dto.response.TokenResponse;
import com.expensemanager.security.token.IssuedToken;
import com.expensemanager.security.token.TokenIssuer;
import com.expensemanager.service.helper.SessionHelper;
import com.expensemanager.service.helper.UserHelper;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/** Nothing further is owed: the session goes active and the caller gets full tokens. */
@Component
@Order(99)
public class LoginWithDirectHandler extends LoginHandler {

	public LoginWithDirectHandler(SessionHelper sessionHelper, UserHelper userHelper, TokenIssuer tokenIssuer) {
		super(sessionHelper, userHelper, tokenIssuer);
	}

	@Override
	public boolean canHandle(User user) {
		// Terminal handler: ordered last, so reaching it means no earlier policy applied.
		return true;
	}

	@Override
	public TokenResponse handle(User user, SignupMethod source, LoginContext context) {
		UserSession session = sessionHelper.create(user, SessionState.ACTIVE, source, context.ipAddress(), context.userAgent());

		IssuedToken accessToken = tokenIssuer.issueAccessToken(user.getId(), session.getId(), TokenTier.FULL);
		IssuedToken refreshToken = tokenIssuer.issueRefreshToken();
		sessionHelper.attachRefreshToken(session.getId(), refreshToken.value(), refreshToken.expiresAt().toEpochMilli());

		userHelper.recordSuccessfulLogin(user.getId());

		return TokenResponse.active(accessToken.value(), refreshToken.value(), accessToken.expiresAt().toEpochMilli(),
				session.getId(), source);
	}
}
