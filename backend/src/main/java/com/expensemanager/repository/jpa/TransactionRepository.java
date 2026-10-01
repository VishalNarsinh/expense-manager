package com.expensemanager.repository.jpa;

import com.expensemanager.common.repository.CustomRepository;
import com.expensemanager.domain.jpa.Transaction;

import java.util.Optional;

public interface TransactionRepository extends CustomRepository<Transaction, String> {

	Optional<Transaction> findByIdAndUserId(String id, String userId);

	long countByCategoryId(String categoryId);

	long countByCategoryIdAndUserId(String categoryId, String userId);
}
