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

/** A single-use MFA recovery code, stored hashed and marked used rather than deleted. */
@Getter
@Setter
@Entity
@Table(name = "mfa_recovery_codes")
public class MfaRecoveryCode extends IdentityJpaDomain {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@Column(name = "code_hash", nullable = false, length = 100)
	private String codeHash;

	@Column(name = "used_at")
	private Long usedAt;

	public boolean isUsed() {
		return usedAt != null;
	}
}
