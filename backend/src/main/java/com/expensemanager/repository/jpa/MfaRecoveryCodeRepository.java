package com.expensemanager.repository.jpa;

import com.expensemanager.common.repository.CustomRepository;
import com.expensemanager.domain.jpa.MfaRecoveryCode;

/**
 * Queries are expressed as QueryDSL predicates at the call site rather than as derived method
 * names, so filters compose and stay type-checked. Declared methods here are limited to the ones
 * that need an annotation the vocabulary cannot express, such as a lock mode.
 */
public interface MfaRecoveryCodeRepository extends CustomRepository<MfaRecoveryCode, String> {
}
