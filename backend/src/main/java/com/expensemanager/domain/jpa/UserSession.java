package com.expensemanager.domain.jpa;

import com.expensemanager.common.domain.IdentityJpaDomain;
import com.expensemanager.domain.enums.SessionState;
import com.expensemanager.domain.enums.SignupMethod;
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

/**
 * One login and its outcome. While a second factor is outstanding this row is the challenge handle
 * the client sends back, and its state decides which token tier the user holds.
 */
@Getter
@Setter
@Entity
@Table(name = "user_sessions")
public class UserSession extends IdentityJpaDomain {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@Enumerated(EnumType.STRING)
	@Column(name = "session_state", nullable = false, length = 40)
	private SessionState sessionState;

	@Enumerated(EnumType.STRING)
	@Column(name = "session_source", nullable = false, length = 30)
	private SignupMethod sessionSource;

	/** SHA-256 of the refresh token. The token itself is never stored. */
	@Column(name = "refresh_token_hash", length = 64)
	private String refreshTokenHash;

	@Column(name = "otp", length = 10)
	private String otp;

	@Column(name = "otp_expires_at")
	private Long otpExpiresAt;

	@Column(name = "otp_resend_count", nullable = false)
	private int otpResendCount;

	@Column(name = "ip_address", length = 45)
	private String ipAddress;

	@Column(name = "user_agent", length = 255)
	private String userAgent;

	@Column(name = "expires_at")
	private Long expiresAt;
}
