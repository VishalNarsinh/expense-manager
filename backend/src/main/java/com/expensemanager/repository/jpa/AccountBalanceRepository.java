package com.expensemanager.repository.jpa;

import com.expensemanager.common.repository.CustomRepository;
import com.expensemanager.domain.jpa.AccountBalance;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface AccountBalanceRepository extends CustomRepository<AccountBalance, String> {

	Optional<AccountBalance> findByUserId(String userId);

	/**
	 * Takes a row lock for the duration of the transaction.
	 *
	 * <p>The balance is updated as read-modify-write, so two concurrent requests for the same user
	 * would otherwise both read the old value and one update would be lost silently and
	 * permanently. Every write path must load the row through this method.
	 */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("SELECT a FROM AccountBalance a WHERE a.user.id = :userId")
	Optional<AccountBalance> findByUserIdForUpdate(@Param("userId") String userId);
}
