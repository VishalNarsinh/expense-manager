package com.expensemanager.dto.response;

import java.util.List;

/**
 * The only time recovery codes are readable. Only hashes are kept, so a lost set can be regenerated
 * but never retrieved.
 */
public record RecoveryCodesResponse(List<String> recoveryCodes) {
}
