package com.expensemanager.repository.jpa;

import com.expensemanager.common.repository.CustomRepository;
import com.expensemanager.domain.jpa.PasskeyChallenge;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.hibernate.jpa.SpecHints;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.QueryHints;

import java.util.Optional;

public interface PasskeyChallengeRepository extends CustomRepository<PasskeyChallenge, String> {

	/**
	 * Locks the challenge row for the duration of the transaction, failing immediately rather than
	 * waiting.
	 *
	 * <p>Consuming a challenge is read-check-write, so without the lock two concurrent replays of
	 * the same challenge could both observe it as pending and both succeed. The zero lock timeout
	 * turns the loser into an immediate failure, which the caller reports as an already-used
	 * challenge.
	 */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@QueryHints(@QueryHint(name = SpecHints.HINT_SPEC_LOCK_TIMEOUT, value = "0"))
	Optional<PasskeyChallenge> findByChallenge(String challenge);

	long deleteByExpiresAtLessThan(long epochMillis);
}
