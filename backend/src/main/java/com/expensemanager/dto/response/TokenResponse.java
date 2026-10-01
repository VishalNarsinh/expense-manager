package com.expensemanager.dto.response;

import com.expensemanager.domain.enums.MfaType;
import com.expensemanager.domain.enums.SessionState;
import com.expensemanager.domain.enums.SignupMethod;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/**
 * The response to every successful authentication step.
 *
 * <p>One shape for login, social login, passkey login, MFA verification and refresh, so a client
 * has a single thing to parse. {@code sessionState} is the field to branch on: {@code ACTIVE} means
 * signed in, {@code MFA_IN_PROGRESS} means the accompanying token only opens the step-up endpoints.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record TokenResponse(
		String accessToken,
		String refreshToken,
		Long accessTokenExpiresAt,
		String session,
		SessionState sessionState,
		SignupMethod sessionSource,
		boolean mfaEnabled,
		List<MfaType> mfaFactors) {

	public static TokenResponse active(String accessToken, String refreshToken, long accessTokenExpiresAt,
			String sessionId, SignupMethod source) {
		return new TokenResponse(accessToken, refreshToken, accessTokenExpiresAt, sessionId, SessionState.ACTIVE,
				source, false, null);
	}

	/** No refresh token here: an unfinished login must not be able to extend itself. */
	public static TokenResponse mfaRequired(String stepUpToken, long expiresAt, String sessionId, SignupMethod source,
			List<MfaType> factors) {
		return new TokenResponse(stepUpToken, null, expiresAt, sessionId, SessionState.MFA_IN_PROGRESS, source, true,
				factors);
	}
}
