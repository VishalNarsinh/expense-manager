package com.expensemanager.domain.jpa;

import com.expensemanager.common.domain.IdentityJpaDomain;
import com.expensemanager.domain.enums.PasskeyChallengeStatus;
import com.expensemanager.domain.enums.PasskeyChallengeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * A single-use WebAuthn challenge, held server side so an assertion can be checked against a
 * challenge this server actually issued, then discarded once consumed.
 */
@Getter
@Setter
@Entity
@Table(name = "passkey_challenges")
public class PasskeyChallenge extends IdentityJpaDomain {

	/** Null for usernameless login, where the user is unknown until the assertion arrives. */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id")
	private User user;

	@Column(name = "challenge", nullable = false, length = 255)
	private String challenge;

	/**
	 * The creation options or assertion request exactly as issued, serialized.
	 *
	 * <p>Verification replays this rather than rebuilding it. Reconstructing the options at
	 * verification time silently drops allowCredentials and the user-verification requirement, so
	 * the assertion would be checked against weaker constraints than the client was given.
	 */
	@Column(name = "challenge_request", nullable = false, columnDefinition = "TEXT")
	private String challengeRequest;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 20)
	private PasskeyChallengeStatus status = PasskeyChallengeStatus.PENDING;

	/** Binds the challenge to the login attempt that asked for it. */
	@Column(name = "session_id", length = 40)
	private String sessionId;

	@Column(name = "display_name", length = 64)
	private String displayName;

	@Enumerated(EnumType.STRING)
	@Column(name = "challenge_type", nullable = false, length = 30)
	private PasskeyChallengeType challengeType;

	@Column(name = "expires_at", nullable = false)
	private Long expiresAt;
}
