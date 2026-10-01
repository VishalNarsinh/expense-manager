package com.expensemanager.service.helper;

import com.expensemanager.common.exception.InternalServerErrorException;
import org.apache.commons.codec.binary.Base32;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.net.URLEncoder;
import java.time.Clock;

/**
 * Time-based one-time passwords, RFC 6238.
 *
 * <p>HMAC-SHA1 with six digits over thirty-second steps, which is what authenticator apps assume.
 * The algorithm is fixed rather than configurable because the secret is shared with an app that
 * cannot be told to use anything else.
 */
@Component
public class TotpGenerator {

	private static final String HMAC_ALGORITHM = "HmacSHA1";
	private static final int SECRET_BYTES = 20;
	private static final int DIGITS = 6;
	private static final int MODULO = 1_000_000;

	public static final long TIME_STEP_SECONDS = 30;

	private final SecureRandom secureRandom = new SecureRandom();
	private final Base32 base32 = new Base32();

	/**
	 * Injected rather than read from the system, so time-dependent behaviour such as refusing a
	 * reused step can be exercised without waiting for a real thirty seconds to elapse.
	 */
	private final Clock clock;

	public TotpGenerator(Clock clock) {
		this.clock = clock;
	}

	/** A new shared secret, base32 encoded as authenticator apps expect. */
	public String generateSecret() {
		byte[] secret = new byte[SECRET_BYTES];
		secureRandom.nextBytes(secret);
		return base32.encodeToString(secret).replace("=", "");
	}

	public long currentTimeStep() {
		return clock.millis() / 1000L / TIME_STEP_SECONDS;
	}

	/**
	 * Checks a code against the steps around now and returns the step it matched, or empty.
	 *
	 * <p>Returning the step rather than a boolean lets the caller record it and refuse the same
	 * step twice, which is what stops a code observed in transit being replayed while still valid.
	 */
	public java.util.OptionalLong matchingTimeStep(String secret, String code, int tolerance) {
		if (code == null || code.length() != DIGITS || !code.chars().allMatch(Character::isDigit)) {
			return java.util.OptionalLong.empty();
		}
		long now = currentTimeStep();
		for (long step = now - tolerance; step <= now + tolerance; step++) {
			if (MessageDigest.isEqual(generate(secret, step).getBytes(StandardCharsets.UTF_8),
					code.getBytes(StandardCharsets.UTF_8))) {
				return java.util.OptionalLong.of(step);
			}
		}
		return java.util.OptionalLong.empty();
	}

	public String generate(String base32Secret, long timeStep) {
		try {
			byte[] key = base32.decode(base32Secret);
			byte[] data = new byte[8];
			long value = timeStep;
			for (int i = 7; i >= 0; i--) {
				data[i] = (byte) (value & 0xFF);
				value >>= 8;
			}

			Mac mac = Mac.getInstance(HMAC_ALGORITHM);
			mac.init(new SecretKeySpec(key, HMAC_ALGORITHM));
			byte[] hash = mac.doFinal(data);

			// Dynamic truncation, RFC 4226 section 5.3.
			int offset = hash[hash.length - 1] & 0x0F;
			int binary = ((hash[offset] & 0x7F) << 24)
					| ((hash[offset + 1] & 0xFF) << 16)
					| ((hash[offset + 2] & 0xFF) << 8)
					| (hash[offset + 3] & 0xFF);

			return String.format("%0" + DIGITS + "d", binary % MODULO);
		} catch (Exception e) {
			throw new InternalServerErrorException("Could not generate a one-time password");
		}
	}

	/** The otpauth URI an authenticator app scans. */
	public String provisioningUri(String issuer, String accountName, String secret) {
		String label = encode(issuer) + ":" + encode(accountName);
		return "otpauth://totp/" + label
				+ "?secret=" + secret
				+ "&issuer=" + encode(issuer)
				+ "&algorithm=SHA1&digits=" + DIGITS
				+ "&period=" + TIME_STEP_SECONDS;
	}

	private String encode(String value) {
		return URLEncoder.encode(value, StandardCharsets.UTF_8);
	}
}
