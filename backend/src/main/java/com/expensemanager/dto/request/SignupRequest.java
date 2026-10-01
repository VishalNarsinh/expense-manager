package com.expensemanager.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SignupRequest(
		@NotBlank(message = "First name is required") @Size(max = 100) String firstName,

		@Size(max = 100) String lastName,

		@NotBlank(message = "Email is required") @Email(message = "Enter a valid email address") @Size(max = 150) String email,

		@NotBlank(message = "Username is required") @Size(min = 3, max = 100, message = "Username must be between 3 and 100 characters")
		String username,

		@NotBlank(message = "Password is required") @Size(min = 8, max = 100, message = "Password must be at least 8 characters")
		String password) {
}
