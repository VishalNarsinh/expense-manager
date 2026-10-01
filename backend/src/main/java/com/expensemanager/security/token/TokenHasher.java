package com.expensemanager.security.token;

import com.expensemanager.common.exception.InternalServerErrorException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Hashes refresh tokens for storage.
 *
 * <p>Only the hash is persisted, so a database disclosure does not hand over usable tokens. A plain
 * digest is right here, unlike for passwords: the input is already high-entropy random, so there is
 * nothing for a slow KDF to defend against.
 */
public final class TokenHasher {

	private TokenHasher() {
	}

	public static String sha256(String token) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			return HexFormat.of().formatHex(digest.digest(token.getBytes(StandardCharsets.UTF_8)));
		} catch (NoSuchAlgorithmException e) {
			throw new InternalServerErrorException("SHA-256 is unavailable");
		}
	}
}
