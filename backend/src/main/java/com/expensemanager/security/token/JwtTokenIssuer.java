package com.expensemanager.security.token;

import com.expensemanager.common.exception.InternalServerErrorException;
import com.expensemanager.common.exception.UnauthorizedException;
import com.expensemanager.common.error.ErrorType;
import com.expensemanager.common.util.IdUtil;
import com.expensemanager.domain.enums.TokenTier;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;

/** Signs and verifies access tokens as Ed25519 JWTs. */
@Component
public class JwtTokenIssuer implements TokenIssuer {

	private static final Logger log = LoggerFactory.getLogger(JwtTokenIssuer.class);

	private static final String ALGORITHM = "Ed25519";
	private static final String CLAIM_SESSION_ID = "sid";
	private static final String CLAIM_TIER = "tier";

	private final TokenProperties properties;
	private final PrivateKey privateKey;
	private final PublicKey publicKey;

	public JwtTokenIssuer(TokenProperties properties) {
		this.properties = properties;

		if (isBlank(properties.getPrivateKey()) || isBlank(properties.getPublicKey())) {
			// Ephemeral keys keep local development working without a setup step. Every restart
			// invalidates outstanding tokens, which is why this must never be the case in a
			// deployed environment.
			log.warn("No token signing key configured; generating an ephemeral keypair. "
					+ "All issued tokens become invalid when this process restarts. "
					+ "Set TOKEN_PRIVATE_KEY and TOKEN_PUBLIC_KEY outside local development.");
			KeyPair keyPair = generateKeyPair();
			this.privateKey = keyPair.getPrivate();
			this.publicKey = keyPair.getPublic();
		} else {
			this.privateKey = readPrivateKey(properties.getPrivateKey());
			this.publicKey = readPublicKey(properties.getPublicKey());
		}
	}

	@Override
	public IssuedToken issueAccessToken(String userId, String sessionId, TokenTier tier) {
		Duration ttl = tier == TokenTier.STEP_UP ? properties.getStepUpTokenTtl() : properties.getAccessTokenTtl();
		Instant now = Instant.now();
		Instant expiresAt = now.plus(ttl);

		String token = Jwts.builder()
				.issuer(properties.getIssuer())
				.subject(userId)
				.id(IdUtil.uuid())
				.claim(CLAIM_SESSION_ID, sessionId)
				.claim(CLAIM_TIER, tier.name())
				.issuedAt(Date.from(now))
				.expiration(Date.from(expiresAt))
				.signWith(privateKey, Jwts.SIG.EdDSA)
				.compact();

		return new IssuedToken(token, expiresAt);
	}

	@Override
	public IssuedToken issueRefreshToken() {
		return new IssuedToken(IdUtil.randomToken(32), Instant.now().plus(properties.getRefreshTokenTtl()));
	}

	@Override
	public TokenPrincipal verify(String token) {
		try {
			Claims claims = Jwts.parser()
					.verifyWith(publicKey)
					.requireIssuer(properties.getIssuer())
					.build()
					.parseSignedClaims(token)
					.getPayload();

			return new TokenPrincipal(
					claims.getSubject(),
					claims.get(CLAIM_SESSION_ID, String.class),
					readTier(claims),
					claims.getId());
		} catch (JwtException | IllegalArgumentException e) {
			// Deliberately opaque: distinguishing expired from forged from malformed tells an
			// attacker which part of their guess was wrong.
			throw new UnauthorizedException("Invalid or expired token", ErrorType.INVALID_TOKEN);
		}
	}

	private TokenTier readTier(Claims claims) {
		String tier = claims.get(CLAIM_TIER, String.class);
		if (tier == null) {
			throw new UnauthorizedException("Token is missing its tier", ErrorType.INVALID_TOKEN);
		}
		try {
			return TokenTier.valueOf(tier);
		} catch (IllegalArgumentException e) {
			throw new UnauthorizedException("Token carries an unknown tier", ErrorType.INVALID_TOKEN);
		}
	}

	private static boolean isBlank(String value) {
		return value == null || value.isBlank();
	}

	private static KeyPair generateKeyPair() {
		try {
			return KeyPairGenerator.getInstance(ALGORITHM).generateKeyPair();
		} catch (NoSuchAlgorithmException e) {
			throw new InternalServerErrorException("Ed25519 is unavailable in this JVM");
		}
	}

	private static PrivateKey readPrivateKey(String configured) {
		try {
			return KeyFactory.getInstance(ALGORITHM).generatePrivate(new PKCS8EncodedKeySpec(decode(configured)));
		} catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
			throw new InternalServerErrorException("Configured token private key could not be read");
		}
	}

	private static PublicKey readPublicKey(String configured) {
		try {
			return KeyFactory.getInstance(ALGORITHM).generatePublic(new X509EncodedKeySpec(decode(configured)));
		} catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
			throw new InternalServerErrorException("Configured token public key could not be read");
		}
	}

	/** Accepts raw base64 or a PEM block, since both are what key tooling tends to produce. */
	private static byte[] decode(String configured) {
		String base64 = configured.replaceAll("-----[A-Z ]+-----", "").replaceAll("\\s", "");
		return Base64.getDecoder().decode(base64);
	}
}
