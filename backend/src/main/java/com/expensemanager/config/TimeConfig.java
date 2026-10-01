package com.expensemanager.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class TimeConfig {

	/** Everything that reads the current time takes this, so tests can control it. */
	@Bean
	public Clock clock() {
		return Clock.systemUTC();
	}
}
