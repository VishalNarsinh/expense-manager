package com.expensemanager.domain.jpa;

import com.expensemanager.common.domain.IdentityJpaDomain;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * A spending target for one calendar month. Budgets do not roll over: a month with no row simply
 * has no target.
 */
@Getter
@Setter
@Entity
@Table(name = "budgets")
public class Budget extends IdentityJpaDomain {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@Column(name = "month", nullable = false)
	private int month;

	@Column(name = "year", nullable = false)
	private int year;

	@Column(name = "total_budget", nullable = false, precision = 18, scale = 2)
	private BigDecimal totalBudget;

	@Column(name = "notes", length = 250)
	private String notes;

	@OneToMany(mappedBy = "budget", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
	private List<CategoryBudget> categoryBudgets = new ArrayList<>();
}
