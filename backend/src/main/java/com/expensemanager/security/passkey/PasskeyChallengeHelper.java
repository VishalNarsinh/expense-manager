package com.expensemanager.security.passkey;

import com.expensemanager.common.error.ErrorType;
import com.expensemanager.common.exception.BadRequestException;
import com.expensemanager.common.util.DateTimeUtil;
import com.expensemanager.config.WebAuthnProperties;
import com.expensemanager.domain.enums.PasskeyChallengeStatus;
import com.expensemanager.domain.enums.PasskeyChallengeType;
import com.expensemanager.domain.jpa.PasskeyChallenge;
import com.expensemanager.domain.jpa.QPasskeyChallenge;
import com.expensemanager.domain.jpa.User;
import com.expensemanager.repository.jpa.PasskeyChallengeRepository;
import com.querydsl.core.types.Path;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/** Issues and consumes the one-time challenges that make an assertion non-replayable. */
@Component
public class PasskeyChallengeHelper {

	private static final QPasskeyChallenge CHALLENGE = QPasskeyChallenge.passkeyChallenge;

	private final PasskeyChallengeRepository challengeRepository;
	private final WebAuthnProperties properties;

	public PasskeyChallengeHelper(PasskeyChallengeRepository challengeRepository, WebAuthnProperties properties) {
		this.challengeRepository = challengeRepository;
		this.properties = properties;
	}

	/**
	 * Stores the challenge together with the request exactly as issued.
	 *
	 * <p>Keeping the whole request matters: verification replays it rather than rebuilding it, and
	 * a rebuilt request would quietly lose the allowed-credentials list and the user-verification
	 * requirement, checking the response against weaker rules than the client was given.
	 */
	@Transactional
	public void persist(User user, String sessionId, PasskeyChallengeType type, String challenge, String serializedRequest,
			String displayName) {
		PasskeyChallenge entity = new PasskeyChallenge();
		entity.setUser(user);
		entity.setSessionId(sessionId);
		entity.setChallengeType(type);
		entity.setChallenge(challenge);
		entity.setChallengeRequest(serializedRequest);
		entity.setStatus(PasskeyChallengeStatus.PENDING);
		entity.setDisplayName(displayName);
		entity.setExpiresAt(DateTimeUtil.currentEpochMillisUtc() + properties.getChallengeTtlMillis());
		challengeRepository.save(entity);
	}

	/**
	 * Claims a challenge, or fails.
	 *
	 * <p>The row is locked without waiting, so two requests racing to redeem the same challenge
	 * cannot both observe it as pending; the loser is reported as an already-used challenge instead
	 * of blocking.
	 */
	@Transactional
	public PasskeyChallenge consume(String challenge, String expectedUserId, String expectedSessionId) {
		PasskeyChallenge entity;
		try {
			entity = challengeRepository.findByChallenge(challenge).orElseThrow(() -> invalid("That challenge is not recognised"));
		} catch (PessimisticLockingFailureException e) {
			throw invalid("That challenge has already been used");
		}

		if (entity.getStatus() != PasskeyChallengeStatus.PENDING) {
			throw invalid("That challenge has already been used");
		}
		if (DateTimeUtil.currentEpochMillisUtc() > entity.getExpiresAt()) {
			throw invalid("That challenge has expired");
		}
		if (expectedUserId != null && (entity.getUser() == null || !Objects.equals(expectedUserId, entity.getUser().getId()))) {
			throw invalid("That challenge belongs to a different account");
		}
		if (expectedSessionId != null && !Objects.equals(expectedSessionId, entity.getSessionId())) {
			throw invalid("That challenge belongs to a different sign-in");
		}

		markUsed(entity.getId());
		return entity;
	}

	@Transactional
	public long deleteExpired() {
		return challengeRepository.deleteWhere(
				CHALLENGE.expiresAt.lt(DateTimeUtil.currentEpochMillisUtc())
						.or(CHALLENGE.status.eq(PasskeyChallengeStatus.USED)),
				CHALLENGE);
	}

	private void markUsed(String challengeId) {
		Map<Path<?>, Object> values = new LinkedHashMap<>();
		values.put(CHALLENGE.status, PasskeyChallengeStatus.USED);
		challengeRepository.updateFields(CHALLENGE.id.eq(challengeId), CHALLENGE, values);
	}

	private BadRequestException invalid(String message) {
		return new BadRequestException(message, ErrorType.PASSKEY_CHALLENGE_FAILED);
	}
}
