package com.expensemanager;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.TimeZone;

@SpringBootApplication
public class ExpenseManagerApplication {

	static {
		// Run in UTC regardless of host settings. Timestamps are stored as UTC epoch millis, and
		// the JDBC driver forwards the JVM default zone to the server on connect, where a legacy
		// Olson name such as Asia/Calcutta is rejected outright by newer PostgreSQL versions.
		TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
	}

	public static void main(String[] args) {
		SpringApplication.run(ExpenseManagerApplication.class, args);
	}
}
