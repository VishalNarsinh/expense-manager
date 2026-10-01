package com.expensemanager.domain.enums;

/**
 * How a transaction was paid. Display labels live in the frontend; this is storage and filtering
 * only.
 */
public enum PaymentMethod {

	UPI,
	CREDIT_CARD,
	DEBIT_CARD,
	NET_BANKING,
	CASH,
	BANK_TRANSFER,
	CHEQUE,
	OTHER
}
