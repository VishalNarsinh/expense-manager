package com.expensemanager.common.util;

import java.util.Collection;
import java.util.function.Supplier;

/**
 * Guard helpers that throw a caller-supplied exception. Mirrors the house style of asserting with an
 * explicit domain exception rather than returning error objects.
 */
public final class Assert {

	private Assert() {
	}

	public static void notNull(Object value, Supplier<? extends RuntimeException> exception) {
		if (value == null) {
			throw exception.get();
		}
	}

	public static void isNull(Object value, Supplier<? extends RuntimeException> exception) {
		if (value != null) {
			throw exception.get();
		}
	}

	public static void isTrue(boolean condition, Supplier<? extends RuntimeException> exception) {
		if (!condition) {
			throw exception.get();
		}
	}

	public static void isFalse(boolean condition, Supplier<? extends RuntimeException> exception) {
		if (condition) {
			throw exception.get();
		}
	}

	public static void notEmpty(Collection<?> collection, Supplier<? extends RuntimeException> exception) {
		if (CollectionUtil.isEmpty(collection)) {
			throw exception.get();
		}
	}
}
