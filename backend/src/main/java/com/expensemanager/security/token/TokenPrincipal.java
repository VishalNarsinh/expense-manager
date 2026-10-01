package com.expensemanager.security.token;

import com.expensemanager.domain.enums.TokenTier;

/** What a verified access token asserts about the caller. */
public record TokenPrincipal(String userId, String sessionId, TokenTier tier, String tokenId) {

	public boolean isFullTier() {
		return tier == TokenTier.FULL;
	}
}
