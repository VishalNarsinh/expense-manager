package com.expensemanager.controller;

import com.expensemanager.common.context.RequestContext;
import com.expensemanager.dto.request.GoogleLoginRequest;
import com.expensemanager.dto.request.LoginRequest;
import com.expensemanager.dto.request.LogoutRequest;
import com.expensemanager.dto.request.RefreshTokenRequest;
import com.expensemanager.dto.request.SignupRequest;
import com.expensemanager.dto.response.TokenResponse;
import com.expensemanager.dto.response.UserResponse;
import com.expensemanager.security.credential.Credential;
import com.expensemanager.security.strategy.LoginHandler;
import com.expensemanager.service.AuthenticationService;
import com.expensemanager.util.constants.Endpoints;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class AuthenticationController {

	private final AuthenticationService authenticationService;

	public AuthenticationController(AuthenticationService authenticationService) {
		this.authenticationService = authenticationService;
	}

	@PostMapping(Endpoints.SIGNUP)
	public ResponseEntity<UserResponse> signup(@Valid @RequestBody SignupRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(authenticationService.signup(request));
	}

	@PostMapping(Endpoints.LOGIN)
	public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
		Credential credential = new Credential.EmailPassword(request.identifier(), request.password());
		return ResponseEntity.ok(authenticationService.login(credential, loginContext()));
	}

	/**
	 * Sign-in and first-time sign-up share one endpoint. Google has already told us whether this
	 * identity is new, so making the client guess which to call would only invite it to guess wrong.
	 */
	@PostMapping({Endpoints.SSO_LOGIN, Endpoints.SSO_SIGNUP})
	public ResponseEntity<TokenResponse> googleLogin(@Valid @RequestBody GoogleLoginRequest request) {
		Credential credential = new Credential.GoogleIdToken(request.idToken());
		return ResponseEntity.ok(authenticationService.login(credential, loginContext()));
	}

	@PostMapping(Endpoints.GENERATE_TOKEN)
	public ResponseEntity<TokenResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
		return ResponseEntity.ok(authenticationService.refresh(request.refreshToken()));
	}

	@PostMapping(Endpoints.LOGOUT)
	public ResponseEntity<Map<String, Object>> logout(@Valid @RequestBody LogoutRequest request) {
		authenticationService.logout(request.session());
		return ResponseEntity.ok(Map.of("message", "Signed out"));
	}

	@DeleteMapping(Endpoints.SESSIONS)
	public ResponseEntity<Map<String, Object>> logoutEverywhere() {
		int ended = authenticationService.logoutEverywhere();
		return ResponseEntity.ok(Map.of("message", "Signed out of " + ended + " session(s)", "count", ended));
	}

	@GetMapping(Endpoints.ME)
	public ResponseEntity<UserResponse> me() {
		return ResponseEntity.ok(authenticationService.currentUser());
	}

	private LoginHandler.LoginContext loginContext() {
		RequestContext context = RequestContext.current();
		return new LoginHandler.LoginContext(context.getIpAddress(), context.getUserAgent());
	}
}
