package com.expensemanager.security.token;

import com.expensemanager.domain.enums.TokenTier;

/**
 * Mints and verifies access tokens.
 *
 * <p>An interface rather than a concrete class so credential verification and token minting can
 * move to an external identity provider later without touching the login handlers.
 */
public interface TokenIssuer {

	/** Signs an access token carrying the session and its tier. */
	IssuedToken issueAccessToken(String userId, String sessionId, TokenTier tier);

	/**
	 * A refresh token. Opaque and random rather than signed: it is only ever presented back to this
	 * server, which looks it up by hash, so it carries no claims worth signing.
	 */
	IssuedToken issueRefreshToken();

	/**
	 * Verifies signature, issuer and expiry.
	 *
	 * @throws com.expensemanager.common.exception.UnauthorizedException if the token is unusable
	 */
	TokenPrincipal verify(String token);
}
