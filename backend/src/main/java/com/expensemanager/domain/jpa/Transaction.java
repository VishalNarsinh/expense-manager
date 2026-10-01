package com.expensemanager.domain.jpa;

import com.expensemanager.common.domain.IdentityJpaDomain;
import com.expensemanager.domain.enums.PaymentMethod;
import com.expensemanager.domain.enums.TransactionType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/** A single income or expense entry. */
@Getter
@Setter
@Entity
@Table(name = "transactions")
public class Transaction extends IdentityJpaDomain {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "category_id", nullable = false)
	private Category category;

	@Enumerated(EnumType.STRING)
	@Column(name = "type", nullable = false, length = 20)
	private TransactionType type;

	/** Always positive; direction comes from {@link #type}, not the sign. */
	@Column(name = "amount", nullable = false, precision = 18, scale = 2)
	private BigDecimal amount;

	/** The calendar day the money moved, which is not necessarily when the row was written. */
	@Column(name = "date", nullable = false)
	private LocalDate date;

	@Column(name = "description", nullable = false, length = 150)
	private String description;

	@Enumerated(EnumType.STRING)
	@Column(name = "payment_method", nullable = false, length = 30)
	private PaymentMethod paymentMethod;

	@Column(name = "notes", length = 500)
	private String notes;

	/** The amount as it affects a balance: negative for an expense, positive for income. */
	public BigDecimal getSignedAmount() {
		return type == TransactionType.EXPENSE ? amount.negate() : amount;
	}
}
