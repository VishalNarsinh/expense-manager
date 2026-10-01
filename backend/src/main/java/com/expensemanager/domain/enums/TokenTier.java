package com.expensemanager.domain.enums;

/**
 * How much a token is allowed to do. Carried as a claim and checked centrally, so a partially
 * authenticated caller cannot reach business endpoints merely because their token has not expired.
 */
public enum TokenTier {

	/** Every endpoint. */
	FULL,

	/** Only the step-up endpoints that complete a login. */
	STEP_UP
}
