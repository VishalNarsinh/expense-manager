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

/** An audit entry for every balance change, whether automatic or a manual reconciliation. */
@Getter
@Setter
@Entity
@Table(name = "balance_histories")
public class BalanceHistory extends IdentityJpaDomain {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@Column(name = "occurred_at", nullable = false)
	private Long occurredAt;

	@Column(name = "previous_balance", nullable = false, precision = 18, scale = 2)
	private BigDecimal previousBalance;

	@Column(name = "new_balance", nullable = false, precision = 18, scale = 2)
	private BigDecimal newBalance;

	/** Signed: negative when the balance fell. */
	@Column(name = "change_amount", nullable = false, precision = 18, scale = 2)
	private BigDecimal changeAmount;

	@Column(name = "reason", nullable = false, length = 100)
	private String reason;

	@Column(name = "notes", length = 250)
	private String notes;

	/**
	 * Deliberately a plain column and not an association: the audit trail outlives the transaction
	 * it describes, so deleting a transaction must leave this row intact and still readable.
	 */
	@Column(name = "related_transaction_id", length = 40)
	private String relatedTransactionId;
}
