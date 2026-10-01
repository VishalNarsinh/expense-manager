package com.expensemanager.dto.response;

import com.expensemanager.domain.enums.SignupMethod;
import com.expensemanager.domain.jpa.User;

/** The caller's own profile. Never carries the password hash or login counters. */
public record UserResponse(
		String id,
		String email,
		String username,
		String firstName,
		String lastName,
		String displayName,
		String role,
		SignupMethod signupMethod,
		boolean emailVerified,
		boolean mfaEnabled,
		Long lastLogin) {

	public static UserResponse from(User user) {
		return new UserResponse(
				user.getId(),
				user.getEmail(),
				user.getUsername(),
				user.getFirstName(),
				user.getLastName(),
				user.getDisplayName(),
				user.getRole(),
				user.getSignupMethod(),
				user.isEmailVerified(),
				user.isMfaEnabled(),
				user.getLastLogin());
	}
}
