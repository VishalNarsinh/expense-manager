package com.expensemanager.repository.jpa;

import com.expensemanager.common.repository.CustomRepository;
import com.expensemanager.domain.jpa.PasskeyChallenge;

import java.util.Optional;

public interface PasskeyChallengeRepository extends CustomRepository<PasskeyChallenge, String> {

	Optional<PasskeyChallenge> findByChallenge(String challenge);

	/** Housekeeping for challenges that were issued but never completed. */
	long deleteByExpiresAtLessThan(long epochMillis);
}
