package com.expensemanager.service.helper;

import com.expensemanager.common.exception.InternalServerErrorException;
import com.expensemanager.config.MfaProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

/**
 * Authenticated encryption for secrets that must be recoverable, such as TOTP shared secrets.
 *
 * <p>Hashing is not an option here: verification has to regenerate codes from the original secret.
 * Anyone able to read these values in plaintext could produce valid codes indefinitely, so the
 * column holds ciphertext and the key lives outside the database.
 */
@Component
public class SecretCipher {

	private static final Logger log = LoggerFactory.getLogger(SecretCipher.class);

	private static final String ALGORITHM = "AES";
	private static final String TRANSFORMATION = "AES/GCM/NoPadding";
	private static final int IV_LENGTH = 12;
	private static final int TAG_LENGTH_BITS = 128;

	private final SecureRandom secureRandom = new SecureRandom();
	private final SecretKey key;

	public SecretCipher(MfaProperties properties) {
		this.key = resolveKey(properties.getTotpSecretEncryptionKey());
	}

	public String encrypt(String plaintext) {
		try {
			byte[] iv = new byte[IV_LENGTH];
			secureRandom.nextBytes(iv);

			Cipher cipher = Cipher.getInstance(TRANSFORMATION);
			cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_LENGTH_BITS, iv));
			byte[] ciphertext = cipher.doFinal(plaintext.getBytes(java.nio.charset.StandardCharsets.UTF_8));

			// The IV is not secret, but it must be unique per message and available to decrypt.
			byte[] combined = new byte[iv.length + ciphertext.length];
			System.arraycopy(iv, 0, combined, 0, iv.length);
			System.arraycopy(ciphertext, 0, combined, iv.length, ciphertext.length);
			return Base64.getEncoder().encodeToString(combined);
		} catch (Exception e) {
			throw new InternalServerErrorException("Could not encrypt secret");
		}
	}

	public String decrypt(String encoded) {
		try {
			byte[] combined = Base64.getDecoder().decode(encoded);
			byte[] iv = Arrays.copyOfRange(combined, 0, IV_LENGTH);
			byte[] ciphertext = Arrays.copyOfRange(combined, IV_LENGTH, combined.length);

			Cipher cipher = Cipher.getInstance(TRANSFORMATION);
			cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_LENGTH_BITS, iv));
			return new String(cipher.doFinal(ciphertext), java.nio.charset.StandardCharsets.UTF_8);
		} catch (Exception e) {
			// Also the path taken when the ciphertext has been tampered with, since GCM
			// authenticates as well as encrypts.
			throw new InternalServerErrorException("Could not decrypt secret");
		}
	}

	private SecretKey resolveKey(String configured) {
		if (configured == null || configured.isBlank()) {
			log.warn("No TOTP secret encryption key configured; generating an ephemeral one. "
					+ "Enrolled authenticators stop working when this process restarts. "
					+ "Set TOTP_ENCRYPTION_KEY outside local development.");
			byte[] generated = new byte[32];
			new SecureRandom().nextBytes(generated);
			return new SecretKeySpec(generated, ALGORITHM);
		}

		byte[] decoded = Base64.getDecoder().decode(configured.trim());
		if (decoded.length != 16 && decoded.length != 24 && decoded.length != 32) {
			throw new IllegalStateException("TOTP encryption key must decode to 16, 24 or 32 bytes, got " + decoded.length);
		}
		return new SecretKeySpec(decoded, ALGORITHM);
	}
}
