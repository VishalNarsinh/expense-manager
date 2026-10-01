package com.expensemanager.dto.response;

import com.expensemanager.domain.enums.PasskeyStatus;
import com.expensemanager.domain.jpa.PasskeyCredential;

/** A registered passkey as shown in account settings. Never exposes the credential id or key. */
public record PasskeyResponse(String id, String label, PasskeyStatus status, Long lastUsedAt, Long created) {

	public static PasskeyResponse from(PasskeyCredential credential) {
		return new PasskeyResponse(credential.getId(), credential.getLabel(), credential.getStatus(),
				credential.getLastUsedAt(), credential.getCreated());
	}
}
