package com.expensemanager.domain.jpa;

import com.expensemanager.common.domain.IdentityJpaDomain;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/** A per-category spending limit within a monthly budget. */
@Getter
@Setter
@Entity
@Table(name = "category_budgets")
public class CategoryBudget extends IdentityJpaDomain {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "budget_id", nullable = false)
	private Budget budget;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "category_id", nullable = false)
	private Category category;

	/** Zero means tracked but unlimited, which is how the UI renders an unset limit. */
	@Column(name = "monthly_limit", nullable = false, precision = 18, scale = 2)
	private BigDecimal monthlyLimit;
}
