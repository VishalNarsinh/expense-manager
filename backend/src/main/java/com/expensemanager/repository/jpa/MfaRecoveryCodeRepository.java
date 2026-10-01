package com.expensemanager.repository.jpa;

import com.expensemanager.common.repository.CustomRepository;
import com.expensemanager.domain.jpa.MfaRecoveryCode;

import java.util.List;

public interface MfaRecoveryCodeRepository extends CustomRepository<MfaRecoveryCode, String> {

	List<MfaRecoveryCode> findByUserIdAndUsedAtIsNull(String userId);

	long countByUserIdAndUsedAtIsNull(String userId);

	long deleteByUserId(String userId);
}
