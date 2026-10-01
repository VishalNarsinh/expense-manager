package com.expensemanager.security.credential;

import com.expensemanager.domain.enums.SignupMethod;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * A credential presented at login, in whichever form the chosen method takes.
 *
 * <p>Sealed so the set of ways to prove identity is closed and visible in one place; adding one
 * means adding a permitted variant here and an authenticator that handles it.
 */
public sealed interface Credential {

	SignupMethod method();

	record EmailPassword(String identifier, String password) implements Credential {

		@Override
		public SignupMethod method() {
			return SignupMethod.EMAIL;
		}
	}

	record GoogleIdToken(String idToken) implements Credential {

		@Override
		public SignupMethod method() {
			return SignupMethod.GOOGLE;
		}
	}

	record PasskeyAssertion(JsonNode assertion, String sessionId) implements Credential {

		@Override
		public SignupMethod method() {
			return SignupMethod.PASSKEY;
		}
	}
}
