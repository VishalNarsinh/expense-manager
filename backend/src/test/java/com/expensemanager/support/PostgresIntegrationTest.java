package com.expensemanager.support;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Boots the application against a real PostgreSQL instance.
 *
 * <p>Deliberately not an in-memory database: the schema relies on partial unique indexes and check
 * constraints that only Postgres enforces, so testing elsewhere would validate a schema we never
 * actually run.
 *
 * <p>The container is static, so one instance is reused across every subclass rather than started
 * per test class.
 */
@Testcontainers
@SpringBootTest
public abstract class PostgresIntegrationTest {

	static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17")
			.withDatabaseName("expensemanager")
			.withUsername("expense")
			.withPassword("test");

	static {
		POSTGRES.start();
	}

	@DynamicPropertySource
	static void datasourceProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
		registry.add("spring.datasource.username", POSTGRES::getUsername);
		registry.add("spring.datasource.password", POSTGRES::getPassword);
	}
}
