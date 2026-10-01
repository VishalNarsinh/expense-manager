package com.expensemanager.common.domain;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Maps each entity to its id prefix, so ids read as {@code txn_1a2b...} rather than opaque UUIDs.
 *
 * <p>Lookup fails loudly for an unregistered entity. An earlier shared implementation returned null
 * for unknown types, which silently produced ids literally beginning {@code null_}; a new entity
 * must be added here or it will not persist.
 */
public enum ModulePrefix {

	USER("usr", "User"),
	USER_SESSION("ses", "UserSession"),
	USER_AUTH_IDENTITY("aid", "UserAuthIdentity"),
	PASSKEY_CREDENTIAL("pkc", "PasskeyCredential"),
	PASSKEY_CHALLENGE("pkch", "PasskeyChallenge"),
	USER_MFA_CONFIGURATION("mfa", "UserMfaConfiguration"),
	MFA_RECOVERY_CODE("rcv", "MfaRecoveryCode"),
	CATEGORY("cat", "Category"),
	TRANSACTION("txn", "Transaction"),
	BUDGET("bdg", "Budget"),
	CATEGORY_BUDGET("cbd", "CategoryBudget"),
	ACCOUNT_BALANCE("bal", "AccountBalance"),
	BALANCE_HISTORY("bhs", "BalanceHistory");

	private final String prefix;
	private final String entityName;

	private static final Map<String, String> BY_ENTITY_NAME = Arrays.stream(values())
			.collect(Collectors.toMap(ModulePrefix::getEntityName, ModulePrefix::getPrefix));

	ModulePrefix(String prefix, String entityName) {
		this.prefix = prefix;
		this.entityName = entityName;
	}

	public String getPrefix() {
		return prefix;
	}

	public String getEntityName() {
		return entityName;
	}

	public static String forEntity(Class<?> entityClass) {
		return forEntityName(entityClass.getSimpleName());
	}

	public static String forEntityName(String entityName) {
		String prefix = BY_ENTITY_NAME.get(entityName);
		if (prefix == null) {
			throw new IllegalStateException(
					"No id prefix registered for entity '" + entityName + "'. Add it to " + ModulePrefix.class.getSimpleName() + ".");
		}
		return prefix;
	}

	/** Every registered entity name to prefix, for schema checks and tests. */
	public static Map<String, String> all() {
		return Map.copyOf(BY_ENTITY_NAME);
	}
}
