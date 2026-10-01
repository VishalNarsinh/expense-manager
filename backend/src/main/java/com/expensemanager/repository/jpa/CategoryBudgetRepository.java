package com.expensemanager.repository.jpa;

import com.expensemanager.common.repository.CustomRepository;
import com.expensemanager.domain.jpa.CategoryBudget;

import java.util.List;

public interface CategoryBudgetRepository extends CustomRepository<CategoryBudget, String> {

	List<CategoryBudget> findByBudgetId(String budgetId);

	long countByCategoryId(String categoryId);
}
