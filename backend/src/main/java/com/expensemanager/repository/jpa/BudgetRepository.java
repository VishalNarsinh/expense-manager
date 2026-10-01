package com.expensemanager.repository.jpa;

import com.expensemanager.common.repository.CustomRepository;
import com.expensemanager.domain.jpa.Budget;

import java.util.List;
import java.util.Optional;

public interface BudgetRepository extends CustomRepository<Budget, String> {

	Optional<Budget> findByUserIdAndMonthAndYear(String userId, int month, int year);

	List<Budget> findByUserId(String userId);

	long countByUserId(String userId);
}
