package com.expensemanager.common.util;

import java.security.SecureRandom;
import java.util.UUID;

/** Identifier generation. Ids are {@code <prefix>_<32 hex chars>} and fit the 40-character id column. */
public final class IdUtil {

	private static final SecureRandom SECURE_RANDOM = new SecureRandom();

	private IdUtil() {
	}

	/** A compact, dash-free UUID. */
	public static String uuid() {
		UUID uuid = UUID.randomUUID();
		return String.format("%016x%016x", uuid.getMostSignificantBits(), uuid.getLeastSignificantBits());
	}

	/** URL-safe random token of the requested byte length, hex encoded. */
	public static String randomToken(int byteLength) {
		byte[] bytes = new byte[byteLength];
		SECURE_RANDOM.nextBytes(bytes);
		StringBuilder sb = new StringBuilder(bytes.length * 2);
		for (byte b : bytes) {
			sb.append(String.format("%02x", b));
		}
		return sb.toString();
	}

	/** A zero-padded numeric code, used for OTPs and recovery codes. */
	public static String numericCode(int digits) {
		int bound = (int) Math.pow(10, digits);
		return String.format("%0" + digits + "d", SECURE_RANDOM.nextInt(bound));
	}
}
