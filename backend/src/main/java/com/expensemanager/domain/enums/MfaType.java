package com.expensemanager.domain.enums;

/** Second factors a user can enrol. */
public enum MfaType {

	/** Time-based one-time password, RFC 6238. */
	TOTP,

	/** A registered passkey used as a second factor. */
	PASSKEY,

	/** Single-use codes, for when the other factors are unavailable. */
	RECOVERY_CODE
}
