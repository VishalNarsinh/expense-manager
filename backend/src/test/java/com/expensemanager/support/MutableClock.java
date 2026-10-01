package com.expensemanager.support;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;

/** A clock tests can move forward, so time-dependent rules can be exercised without waiting. */
public class MutableClock extends Clock {

	private Instant instant = Instant.now();
	private final ZoneId zone;

	public MutableClock() {
		this(ZoneId.of("UTC"));
	}

	private MutableClock(ZoneId zone) {
		this.zone = zone;
	}

	public void advance(Duration amount) {
		instant = instant.plus(amount);
	}

	@Override
	public ZoneId getZone() {
		return zone;
	}

	@Override
	public Clock withZone(ZoneId newZone) {
		return new MutableClock(newZone);
	}

	@Override
	public Instant instant() {
		return instant;
	}
}
