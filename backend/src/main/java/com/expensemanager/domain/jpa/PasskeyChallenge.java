package com.expensemanager.domain.jpa;

import com.expensemanager.common.domain.IdentityJpaDomain;
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

	@Enumerated(EnumType.STRING)
	@Column(name = "challenge_type", nullable = false, length = 30)
	private PasskeyChallengeType challengeType;

	@Column(name = "expires_at", nullable = false)
	private Long expiresAt;
}
