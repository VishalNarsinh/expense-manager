package com.expensemanager.domain.jpa;

import com.expensemanager.common.domain.IdentityJpaDomain;
import com.expensemanager.domain.enums.MfaType;
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

/** One enrolled second factor. A user may hold several and mark one preferred. */
@Getter
@Setter
@Entity
@Table(name = "user_mfa_configurations")
public class UserMfaConfiguration extends IdentityJpaDomain {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@Enumerated(EnumType.STRING)
	@Column(name = "mfa_type", nullable = false, length = 30)
	private MfaType mfaType;

	/**
	 * TOTP shared secret, encrypted at rest. Anyone able to read this column in plaintext could
	 * generate valid codes, which would make the second factor worthless.
	 */
	@Column(name = "totp_secret_encrypted", length = 512)
	private String totpSecretEncrypted;

	@Column(name = "is_enabled", nullable = false)
	private boolean enabled;

	@Column(name = "is_preferred", nullable = false)
	private boolean preferred;

	@Column(name = "verified_at")
	private Long verifiedAt;
}
