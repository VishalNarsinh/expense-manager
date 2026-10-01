package com.expensemanager.domain.jpa;

import com.expensemanager.common.domain.IdentityJpaDomain;
import com.expensemanager.domain.enums.PasskeyStatus;
import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/** A registered WebAuthn credential. */
@Getter
@Setter
@Entity
@Table(name = "passkey_credentials")
public class PasskeyCredential extends IdentityJpaDomain {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	/** Base64url credential id as presented by the authenticator. */
	@Column(name = "credential_id", nullable = false, length = 255)
	private String credentialId;

	@Column(name = "public_key_cose", nullable = false)
	private byte[] publicKeyCose;

	/**
	 * Authenticator signature counter. A value that fails to advance signals a cloned
	 * authenticator, so it is persisted on every successful assertion.
	 */
	@Column(name = "sign_count", nullable = false)
	private long signCount;

	@Column(name = "aaguid", length = 64)
	private String aaguid;

	@Column(name = "transports", length = 100)
	private String transports;

	@Column(name = "label", length = 100)
	private String label;

	@Column(name = "display_name", length = 64)
	private String displayName;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 20)
	private PasskeyStatus status = PasskeyStatus.ACTIVE;

	/** Whether the authenticator may sync this credential to a provider backup. */
	@Column(name = "backup_eligible", nullable = false)
	private boolean backupEligible;

	@Column(name = "last_used_at")
	private Long lastUsedAt;

	public boolean isActive() {
		return status == PasskeyStatus.ACTIVE;
	}
}
