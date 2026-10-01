package com.expensemanager.controller;

import com.expensemanager.common.context.RequestContext;
import com.expensemanager.dto.request.PasskeyMfaVerifyRequest;
import com.expensemanager.dto.request.PasskeyRegistrationRequest;
import com.expensemanager.dto.request.PasskeyVerifyRequest;
import com.expensemanager.dto.response.PasskeyResponse;
import com.expensemanager.dto.response.TokenResponse;
import com.expensemanager.security.strategy.LoginHandler;
import com.expensemanager.service.PasskeyService;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class PasskeyController {

	private final PasskeyService passkeyService;

	public PasskeyController(PasskeyService passkeyService) {
		this.passkeyService = passkeyService;
	}

	@PostMapping("/passkey/register/options")
	public ResponseEntity<JsonNode> registrationOptions(@Valid @RequestBody(required = false) PasskeyRegistrationRequest request) {
		return ResponseEntity.ok(passkeyService.startRegistration(request == null ? null : request.label()));
	}

	@PostMapping("/passkey/register/verify")
	public ResponseEntity<PasskeyResponse> registerPasskey(@Valid @RequestBody PasskeyVerifyRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(passkeyService.finishRegistration(request.credential()));
	}

	@PostMapping("/passkey/login/options")
	public ResponseEntity<JsonNode> loginOptions() {
		return ResponseEntity.ok(passkeyService.startLogin());
	}

	@PostMapping("/passkey/login/verify")
	public ResponseEntity<TokenResponse> loginWithPasskey(@Valid @RequestBody PasskeyVerifyRequest request) {
		return ResponseEntity.ok(passkeyService.finishLogin(request.credential(), loginContext()));
	}

	@PostMapping("/login/mfa/passkey/options")
	public ResponseEntity<JsonNode> mfaOptions(@Valid @RequestBody PasskeyMfaVerifyRequest request) {
		return ResponseEntity.ok(passkeyService.startMfaAssertion(request.session()));
	}

	@PostMapping("/login/mfa/passkey/verify")
	public ResponseEntity<TokenResponse> verifyMfaPasskey(@Valid @RequestBody PasskeyMfaVerifyRequest request) {
		return ResponseEntity.ok(passkeyService.finishMfaAssertion(request.session(), request.credential()));
	}

	@GetMapping("/passkeys")
	public ResponseEntity<List<PasskeyResponse>> list() {
		return ResponseEntity.ok(passkeyService.list());
	}

	@DeleteMapping("/passkeys/{id}")
	public ResponseEntity<Void> revoke(@PathVariable String id) {
		passkeyService.revoke(id);
		return ResponseEntity.noContent().build();
	}

	private LoginHandler.LoginContext loginContext() {
		RequestContext context = RequestContext.current();
		return new LoginHandler.LoginContext(context.getIpAddress(), context.getUserAgent());
	}
}
