package com.expensemanager.service;

import com.expensemanager.domain.enums.MfaType;
import com.expensemanager.dto.response.MfaFactorResponse;
import com.expensemanager.dto.response.RecoveryCodesResponse;
import com.expensemanager.dto.response.TokenResponse;
import com.expensemanager.dto.response.TotpEnrollmentResponse;

import java.util.List;

public interface MfaService {

	TotpEnrollmentResponse startTotpEnrollment();

	RecoveryCodesResponse confirmTotpEnrollment(String code);

	void disableTotp(String code);

	List<MfaFactorResponse> factors();

	RecoveryCodesResponse regenerateRecoveryCodes();

	/** Finishes a sign-in that was waiting on a second factor. */
	TokenResponse completeLogin(String sessionId, MfaType type, String proof);
}
