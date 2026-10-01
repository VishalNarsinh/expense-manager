package com.expensemanager.security.mfa;

import com.expensemanager.common.error.ErrorType;
import com.expensemanager.common.exception.UnauthorizedException;
import com.expensemanager.domain.enums.MfaType;
import com.expensemanager.domain.jpa.User;
import com.expensemanager.security.passkey.PasskeyHelper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * A registered passkey used as a second factor.
 *
 * <p>The proof here is a serialized assertion rather than a short code, so it arrives as JSON.
 */
@Component
public class PasskeyMfaHandler implements MfaFactorHandler {

	private final PasskeyHelper passkeyHelper;
	private final ObjectMapper objectMapper;

	public PasskeyMfaHandler(PasskeyHelper passkeyHelper, ObjectMapper objectMapper) {
		this.passkeyHelper = passkeyHelper;
		this.objectMapper = objectMapper;
	}

	@Override
	public MfaType type() {
		return MfaType.PASSKEY;
	}

	@Override
	public void verify(User user, String proof) {
		try {
			String provenUserId = passkeyHelper.finishAuthentication(user, null, objectMapper.readTree(proof));
			if (!Objects.equals(provenUserId, user.getId())) {
				// The assertion verified, but against a different account than the one signing in.
				throw new UnauthorizedException("That passkey belongs to another account", ErrorType.PASSKEY_ASSERTION_FAILED);
			}
		} catch (UnauthorizedException e) {
			throw e;
		} catch (Exception e) {
			throw new UnauthorizedException("That passkey could not be verified", ErrorType.PASSKEY_ASSERTION_FAILED);
		}
	}
}
