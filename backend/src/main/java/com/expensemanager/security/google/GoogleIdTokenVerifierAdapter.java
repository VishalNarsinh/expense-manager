package com.expensemanager.security.google;

import com.expensemanager.common.error.ErrorType;
import com.expensemanager.common.exception.UnauthorizedException;
import com.expensemanager.config.GoogleProperties;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Verifies a Google ID token.
 *
 * <p>The SPA obtains the token through Google Identity Services and posts it here, rather than the
 * server running a redirect flow. The token is only trusted after its signature, issuer, expiry and
 * audience all check out, and the verifier fetches and caches Google's signing keys itself.
 */
@Component
public class GoogleIdTokenVerifierAdapter {

	private static final Logger log = LoggerFactory.getLogger(GoogleIdTokenVerifierAdapter.class);

	private final GoogleIdTokenVerifier verifier;
	private final boolean configured;

	public GoogleIdTokenVerifierAdapter(GoogleProperties properties) {
		this.configured = properties.getClientIds() != null && !properties.getClientIds().isEmpty();

		if (!configured) {
			log.warn("No Google client ids configured; Google sign-in will be refused. Set GOOGLE_CLIENT_IDS to enable it.");
		}

		this.verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), GsonFactory.getDefaultInstance())
				.setAudience(configured ? properties.getClientIds() : java.util.List.of())
				.build();
	}

	public GoogleIdentity verify(String idToken) {
		if (!configured) {
			throw new UnauthorizedException("Google sign-in is not available", ErrorType.LOGIN_FAILED);
		}

		GoogleIdToken token;
		try {
			token = verifier.verify(idToken);
		} catch (Exception e) {
			// Covers a malformed token and a failure to reach Google for its signing keys alike;
			// neither is something the caller should be able to tell apart.
			log.warn("Google ID token could not be verified", e);
			throw unauthorized();
		}

		if (token == null) {
			throw unauthorized();
		}

		GoogleIdToken.Payload payload = token.getPayload();
		if (!Boolean.TRUE.equals(payload.getEmailVerified())) {
			// Without this, anyone who can set an unverified address at an identity provider could
			// claim an account belonging to that address here.
			throw new UnauthorizedException("Your Google account email is not verified", ErrorType.LOGIN_FAILED);
		}

		return new GoogleIdentity(
				payload.getSubject(),
				payload.getEmail(),
				(String) payload.get("given_name"),
				(String) payload.get("family_name"));
	}

	private UnauthorizedException unauthorized() {
		return new UnauthorizedException("That Google sign-in could not be verified", ErrorType.LOGIN_FAILED);
	}

	/**
	 * @param subject Google's stable identifier for the account, which an email address is not
	 */
	public record GoogleIdentity(String subject, String email, String firstName, String lastName) {
	}
}
