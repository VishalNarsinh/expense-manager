package com.expensemanager.common.util;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

/** Epoch-millisecond helpers. Every persisted timestamp in this application is UTC epoch millis. */
public final class DateTimeUtil {

	private DateTimeUtil() {
	}

	public static long currentEpochMillisUtc() {
		return Instant.now().toEpochMilli();
	}

	public static long toEpochMillis(Instant instant) {
		return instant.toEpochMilli();
	}

	public static Instant toInstant(Long epochMillis) {
		return epochMillis == null ? null : Instant.ofEpochMilli(epochMillis);
	}

	public static LocalDate todayUtc() {
		return LocalDate.now(ZoneOffset.UTC);
	}
}
