package com.expensemanager.repository.jpa;

import com.expensemanager.common.repository.CustomRepository;
import com.expensemanager.domain.jpa.User;

import java.util.Optional;

public interface UserRepository extends CustomRepository<User, String> {

	Optional<User> findByEmailIgnoreCase(String email);

	Optional<User> findByUsernameIgnoreCase(String username);

	/** Login accepts either identifier, matching what the sign-in form offers. */
	Optional<User> findByEmailIgnoreCaseOrUsernameIgnoreCase(String email, String username);

	boolean existsByEmailIgnoreCase(String email);

	boolean existsByUsernameIgnoreCase(String username);
}
