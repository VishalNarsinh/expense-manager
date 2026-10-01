package com.expensemanager.security.mfa;

import com.expensemanager.common.error.ErrorType;
import com.expensemanager.common.exception.UnauthorizedException;
import com.expensemanager.common.util.DateTimeUtil;
import com.expensemanager.domain.enums.MfaType;
import com.expensemanager.domain.jpa.MfaRecoveryCode;
import com.expensemanager.domain.jpa.QMfaRecoveryCode;
import com.expensemanager.domain.jpa.User;
import com.expensemanager.repository.jpa.MfaRecoveryCodeRepository;
import com.querydsl.core.types.Path;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Verifies a single-use recovery code, for when the authenticator is unavailable. */
@Component
public class RecoveryCodeMfaHandler implements MfaFactorHandler {

	private static final QMfaRecoveryCode CODE = QMfaRecoveryCode.mfaRecoveryCode;

	private final MfaRecoveryCodeRepository recoveryCodeRepository;
	private final PasswordEncoder passwordEncoder;

	public RecoveryCodeMfaHandler(MfaRecoveryCodeRepository recoveryCodeRepository, PasswordEncoder passwordEncoder) {
		this.recoveryCodeRepository = recoveryCodeRepository;
		this.passwordEncoder = passwordEncoder;
	}

	@Override
	public MfaType type() {
		return MfaType.RECOVERY_CODE;
	}

	@Override
	@Transactional
	public void verify(User user, String proof) {
		if (proof == null || proof.isBlank()) {
			throw invalid();
		}
		String normalised = proof.trim().toLowerCase().replace("-", "");

		List<MfaRecoveryCode> unused = recoveryCodeRepository.findAll(
				CODE.user.id.eq(user.getId()).and(CODE.usedAt.isNull()), CODE);

		// Codes are stored hashed, so finding the match means comparing against each in turn
		// rather than looking one up. The list is small and this path is rarely taken.
		MfaRecoveryCode matched = unused.stream()
				.filter(candidate -> passwordEncoder.matches(normalised, candidate.getCodeHash()))
				.findFirst()
				.orElseThrow(this::invalid);

		markUsed(matched.getId());
	}

	private void markUsed(String codeId) {
		Map<Path<?>, Object> values = new LinkedHashMap<>();
		values.put(CODE.usedAt, DateTimeUtil.currentEpochMillisUtc());
		recoveryCodeRepository.updateFields(CODE.id.eq(codeId), CODE, values);
	}

	private UnauthorizedException invalid() {
		return new UnauthorizedException("That recovery code is not valid", ErrorType.INCORRECT_OTP, "code");
	}
}
