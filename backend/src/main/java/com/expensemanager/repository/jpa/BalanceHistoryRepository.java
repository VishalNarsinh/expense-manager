package com.expensemanager.repository.jpa;

import com.expensemanager.common.repository.CustomRepository;
import com.expensemanager.domain.jpa.BalanceHistory;

import java.util.List;

public interface BalanceHistoryRepository extends CustomRepository<BalanceHistory, String> {

	List<BalanceHistory> findByUserIdOrderByOccurredAtDesc(String userId);

	long countByUserId(String userId);

	long deleteByUserId(String userId);
}
