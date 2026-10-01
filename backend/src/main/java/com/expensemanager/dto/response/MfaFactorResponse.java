package com.expensemanager.dto.response;

import com.expensemanager.domain.enums.MfaType;
import com.expensemanager.domain.jpa.UserMfaConfiguration;

public record MfaFactorResponse(MfaType mfaType, boolean enabled, boolean preferred, Long verifiedAt) {

	public static MfaFactorResponse from(UserMfaConfiguration configuration) {
		return new MfaFactorResponse(configuration.getMfaType(), configuration.isEnabled(), configuration.isPreferred(),
				configuration.getVerifiedAt());
	}
}
