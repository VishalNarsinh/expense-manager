package com.expensemanager.domain.jpa;

import com.expensemanager.common.domain.IdentityJpaDomain;
import com.expensemanager.domain.enums.SignupMethod;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "users")
public class User extends IdentityJpaDomain {

	@Column(name = "email", nullable = false, length = 150)
	private String email;

	@Column(name = "username", nullable = false, length = 100)
	private String username;

	@Column(name = "first_name", nullable = false, length = 100)
	private String firstName;

	@Column(name = "last_name", length = 100)
	private String lastName;

	/**
	 * BCrypt hash, null for accounts that only sign in with Google or a passkey. Storing a
	 * generated placeholder instead would leave every such account with a password that works.
	 */
	@Column(name = "password", length = 255)
	private String password;

	@Column(name = "role", nullable = false, length = 50)
	private String role;

	@Enumerated(EnumType.STRING)
	@Column(name = "signup_method", nullable = false, length = 30)
	private SignupMethod signupMethod;

	@Column(name = "is_active", nullable = false)
	private boolean active = true;

	@Column(name = "is_email_verified", nullable = false)
	private boolean emailVerified;

	@Column(name = "is_mfa_enabled", nullable = false)
	private boolean mfaEnabled;

	@Column(name = "failed_login_count", nullable = false)
	private int failedLoginCount;

	@Column(name = "last_login")
	private Long lastLogin;

	public String getDisplayName() {
		return lastName == null || lastName.isBlank() ? firstName : firstName + " " + lastName;
	}
}
