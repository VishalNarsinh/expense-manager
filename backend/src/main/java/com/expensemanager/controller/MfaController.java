package com.expensemanager.controller;

import com.expensemanager.domain.enums.MfaType;
import com.expensemanager.dto.request.MfaLoginVerifyRequest;
import com.expensemanager.dto.request.TotpVerifyRequest;
import com.expensemanager.dto.response.MfaFactorResponse;
import com.expensemanager.dto.response.RecoveryCodesResponse;
import com.expensemanager.dto.response.TokenResponse;
import com.expensemanager.dto.response.TotpEnrollmentResponse;
import com.expensemanager.service.MfaService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Enrolment lives behind a full token; the verification endpoints under /login/mfa are reachable
 * with a step-up token, because that is all a half-finished sign-in holds.
 */
@RestController
public class MfaController {

	private final MfaService mfaService;

	public MfaController(MfaService mfaService) {
		this.mfaService = mfaService;
	}

	@PostMapping("/mfa/totp/enroll")
	public ResponseEntity<TotpEnrollmentResponse> startTotpEnrollment() {
		return ResponseEntity.ok(mfaService.startTotpEnrollment());
	}

	@PostMapping("/mfa/totp/verify")
	public ResponseEntity<RecoveryCodesResponse> confirmTotpEnrollment(@Valid @RequestBody TotpVerifyRequest request) {
		return ResponseEntity.ok(mfaService.confirmTotpEnrollment(request.code()));
	}

	@PostMapping("/mfa/totp/disable")
	public ResponseEntity<Map<String, Object>> disableTotp(@Valid @RequestBody TotpVerifyRequest request) {
		mfaService.disableTotp(request.code());
		return ResponseEntity.ok(Map.of("message", "Authenticator app removed"));
	}

	@GetMapping("/mfa")
	public ResponseEntity<List<MfaFactorResponse>> factors() {
		return ResponseEntity.ok(mfaService.factors());
	}

	@PostMapping("/mfa/recovery-codes/regenerate")
	public ResponseEntity<RecoveryCodesResponse> regenerateRecoveryCodes() {
		return ResponseEntity.ok(mfaService.regenerateRecoveryCodes());
	}

	@PostMapping("/login/mfa/totp/verify")
	public ResponseEntity<TokenResponse> verifyTotpAtLogin(@Valid @RequestBody MfaLoginVerifyRequest request) {
		return ResponseEntity.ok(mfaService.completeLogin(request.session(), MfaType.TOTP, request.code()));
	}

	@PostMapping("/login/mfa/recovery-code/verify")
	public ResponseEntity<TokenResponse> verifyRecoveryCodeAtLogin(@Valid @RequestBody MfaLoginVerifyRequest request) {
		return ResponseEntity.ok(mfaService.completeLogin(request.session(), MfaType.RECOVERY_CODE, request.code()));
	}
}
