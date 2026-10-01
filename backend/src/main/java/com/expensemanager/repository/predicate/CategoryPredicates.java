package com.expensemanager.repository.predicate;

import com.expensemanager.domain.enums.TransactionType;
import com.expensemanager.domain.jpa.QCategory;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Predicate;

/** Reusable predicates over {@link com.expensemanager.domain.jpa.Category}. */
public final class CategoryPredicates {

	public static final QCategory CATEGORY = QCategory.category;

	private CategoryPredicates() {
	}

	/**
	 * Everything the user may choose from: their own categories plus the system ones, which have no
	 * owner.
	 */
	public static Predicate visibleTo(String userId) {
		return CATEGORY.user.id.eq(userId).or(CATEGORY.user.isNull());
	}

	public static Predicate visibleTo(String userId, TransactionType type) {
		return CATEGORY.type.eq(type).and(visibleTo(userId));
	}

	/** Scoped by owner, so one account cannot address another account's category by id. */
	public static Predicate ownedBy(String userId) {
		return CATEGORY.user.id.eq(userId);
	}

	public static OrderSpecifier<String> byName() {
		return CATEGORY.name.asc();
	}
}
