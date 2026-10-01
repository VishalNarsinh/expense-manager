package com.expensemanager.domain.jpa;

import com.expensemanager.common.domain.IdentityJpaDomain;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * The running bank balance, one row per user.
 *
 * <p>This is a stored total rather than a projection of the transactions, because it also absorbs
 * manual reconciliations against a real statement. Every change to it is written together with a
 * {@link BalanceHistory} row inside one transaction.
 */
@Getter
@Setter
@Entity
@Table(name = "account_balances")
public class AccountBalance extends IdentityJpaDomain {

	@OneToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false, unique = true)
	private User user;

	@Column(name = "account_name", nullable = false, length = 100)
	private String accountName;

	@Column(name = "current_balance", nullable = false, precision = 18, scale = 2)
	private BigDecimal currentBalance;

	@Column(name = "last_updated", nullable = false)
	private Long lastUpdated;

	@Column(name = "notes", length = 250)
	private String notes;
}
