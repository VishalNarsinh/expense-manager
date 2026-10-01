package com.expensemanager.security.strategy;

import com.expensemanager.domain.enums.MfaType;
import com.expensemanager.domain.enums.SessionState;
import com.expensemanager.domain.enums.SignupMethod;
import com.expensemanager.domain.enums.TokenTier;
import com.expensemanager.domain.jpa.User;
import com.expensemanager.domain.jpa.UserSession;
import com.expensemanager.dto.response.TokenResponse;
import com.expensemanager.security.token.IssuedToken;
import com.expensemanager.security.token.TokenIssuer;
import com.expensemanager.service.helper.MfaHelper;
import com.expensemanager.service.helper.SessionHelper;
import com.expensemanager.service.helper.UserHelper;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * The credential checked out, but a second factor is still owed.
 *
 * <p>Hands back a short-lived step-up token and no refresh token. The step-up token opens only the
 * endpoints that finish a login, and withholding the refresh token means an unfinished login cannot
 * extend itself indefinitely.
 */
@Component
@Order(1)
public class LoginWithMfaHandler extends LoginHandler {

	private final MfaHelper mfaHelper;

	public LoginWithMfaHandler(SessionHelper sessionHelper, UserHelper userHelper, TokenIssuer tokenIssuer, MfaHelper mfaHelper) {
		super(sessionHelper, userHelper, tokenIssuer);
		this.mfaHelper = mfaHelper;
	}

	@Override
	public boolean canHandle(User user) {
		return user.isMfaEnabled();
	}

	@Override
	public TokenResponse handle(User user, SignupMethod source, LoginContext context) {
		UserSession session = sessionHelper.create(user, SessionState.MFA_IN_PROGRESS, source, context.ipAddress(), context.userAgent());

		IssuedToken stepUpToken = tokenIssuer.issueAccessToken(user.getId(), session.getId(), TokenTier.STEP_UP);
		List<MfaType> factors = mfaHelper.enabledFactors(user.getId());

		return TokenResponse.mfaRequired(stepUpToken.value(), stepUpToken.expiresAt().toEpochMilli(), session.getId(), source, factors);
	}
}
