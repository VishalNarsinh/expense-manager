package com.expensemanager.domain.enums;

/** Lifecycle of a registered passkey. Revoked credentials are kept so their id cannot be reused. */
public enum PasskeyStatus {

	ACTIVE,
	REVOKED
}
