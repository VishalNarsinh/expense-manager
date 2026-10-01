package com.expensemanager.repository.predicate;

import com.expensemanager.domain.jpa.QUser;
import com.querydsl.core.types.Predicate;

/** Reusable predicates over {@link com.expensemanager.domain.jpa.User}. */
public final class UserPredicates {

	public static final QUser USER = QUser.user;

	private UserPredicates() {
	}

	/** Matches on either identifier, which is what the sign-in form accepts. */
	public static Predicate byIdentifier(String identifier) {
		return USER.email.equalsIgnoreCase(identifier).or(USER.username.equalsIgnoreCase(identifier));
	}

	public static Predicate byEmail(String email) {
		return USER.email.equalsIgnoreCase(email);
	}

	public static Predicate byUsername(String username) {
		return USER.username.equalsIgnoreCase(username);
	}
}
