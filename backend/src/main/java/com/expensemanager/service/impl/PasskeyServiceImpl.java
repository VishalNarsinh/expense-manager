package com.expensemanager.service.impl;

import com.expensemanager.common.context.RequestContext;
import com.expensemanager.common.error.ErrorType;
import com.expensemanager.common.exception.EntityNotFoundException;
import com.expensemanager.common.exception.UnauthorizedException;
import com.expensemanager.domain.enums.MfaType;
import com.expensemanager.domain.enums.PasskeyStatus;
import com.expensemanager.domain.jpa.PasskeyCredential;
import com.expensemanager.domain.jpa.QPasskeyCredential;
import com.expensemanager.domain.jpa.User;
import com.expensemanager.domain.jpa.UserMfaConfiguration;
import com.expensemanager.dto.response.PasskeyResponse;
import com.expensemanager.dto.response.TokenResponse;
import com.expensemanager.repository.jpa.PasskeyCredentialRepository;
import com.expensemanager.security.credential.Credential;
import com.expensemanager.security.passkey.PasskeyHelper;
import com.expensemanager.security.strategy.LoginHandler;
import com.expensemanager.service.AuthenticationService;
import com.expensemanager.service.MfaService;
import com.expensemanager.service.PasskeyService;
import com.expensemanager.service.helper.MfaHelper;
import com.expensemanager.service.helper.UserHelper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.querydsl.core.types.Path;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class PasskeyServiceImpl implements PasskeyService {

	private static final QPasskeyCredential CREDENTIAL = QPasskeyCredential.passkeyCredential;

	private final PasskeyHelper passkeyHelper;
	private final PasskeyCredentialRepository credentialRepository;
	private final UserHelper userHelper;
	private final MfaHelper mfaHelper;
	private final AuthenticationService authenticationService;
	private final MfaService mfaService;
	private final ObjectMapper objectMapper;

	public PasskeyServiceImpl(PasskeyHelper passkeyHelper, PasskeyCredentialRepository credentialRepository, UserHelper userHelper,
			MfaHelper mfaHelper, AuthenticationService authenticationService, MfaService mfaService, ObjectMapper objectMapper) {
		this.passkeyHelper = passkeyHelper;
		this.credentialRepository = credentialRepository;
		this.userHelper = userHelper;
		this.mfaHelper = mfaHelper;
		this.authenticationService = authenticationService;
		this.mfaService = mfaService;
		this.objectMapper = objectMapper;
	}

	@Override
	@Transactional
	public JsonNode startRegistration(String label) {
		return passkeyHelper.startRegistration(currentUser(), null, label);
	}

	@Override
	@Transactional
	public PasskeyResponse finishRegistration(JsonNode credential) {
		User user = currentUser();
		PasskeyCredential saved = passkeyHelper.finishRegistration(user, null, credential);

		// A registered passkey is also usable as a second factor, so record it as one. The account
		// is not switched to requiring MFA here; that stays an explicit choice.
		mfaHelper.configuration(user.getId(), MfaType.PASSKEY).orElseGet(() -> {
			UserMfaConfiguration configuration = new UserMfaConfiguration();
			configuration.setUser(user);
			configuration.setMfaType(MfaType.PASSKEY);
			configuration.setEnabled(true);
			return mfaHelper.saveConfiguration(configuration);
		});

		return PasskeyResponse.from(saved);
	}

	@Override
	@Transactional
	public JsonNode startLogin() {
		// No user: the authenticator reveals which account it holds.
		return passkeyHelper.startAuthentication(null, null);
	}

	@Override
	@Transactional
	public TokenResponse finishLogin(JsonNode credential, LoginHandler.LoginContext context) {
		return authenticationService.login(new Credential.PasskeyAssertion(credential, null), context);
	}

	@Override
	@Transactional
	public JsonNode startMfaAssertion(String sessionId) {
		User user = mfaService.pendingUser(sessionId);
		return passkeyHelper.startAuthentication(user, null);
	}

	@Override
	@Transactional
	public TokenResponse finishMfaAssertion(String sessionId, JsonNode credential) {
		try {
			return mfaService.completeLogin(sessionId, MfaType.PASSKEY, objectMapper.writeValueAsString(credential));
		} catch (com.fasterxml.jackson.core.JsonProcessingException e) {
			throw new UnauthorizedException("That passkey could not be verified", ErrorType.PASSKEY_ASSERTION_FAILED);
		}
	}

	@Override
	@Transactional(readOnly = true)
	public List<PasskeyResponse> list() {
		return credentialRepository
				.findAll(CREDENTIAL.user.id.eq(currentUserId()).and(CREDENTIAL.status.eq(PasskeyStatus.ACTIVE)),
						CREDENTIAL, CREDENTIAL.created.desc())
				.stream()
				.map(PasskeyResponse::from)
				.toList();
	}

	@Override
	@Transactional
	public void revoke(String passkeyId) {
		String userId = currentUserId();

		PasskeyCredential credential = credentialRepository
				.findOne(CREDENTIAL.id.eq(passkeyId).and(CREDENTIAL.user.id.eq(userId)), CREDENTIAL)
				.orElseThrow(() -> new EntityNotFoundException("Passkey not found"));

		// Marked revoked rather than deleted, so its credential id stays taken and cannot be
		// registered again.
		Map<Path<?>, Object> values = new LinkedHashMap<>();
		values.put(CREDENTIAL.status, PasskeyStatus.REVOKED);
		credentialRepository.updateFields(CREDENTIAL.id.eq(credential.getId()), CREDENTIAL, values);

		long remaining = credentialRepository.count(
				CREDENTIAL.user.id.eq(userId).and(CREDENTIAL.status.eq(PasskeyStatus.ACTIVE)), CREDENTIAL);
		if (remaining == 0) {
			mfaHelper.removeConfiguration(userId, MfaType.PASSKEY);
		}
	}

	private User currentUser() {
		return userHelper.findById(currentUserId()).orElseThrow(() -> new EntityNotFoundException("Account not found"));
	}

	private String currentUserId() {
		String userId = RequestContext.current().getUserId();
		if (userId == null) {
			throw new UnauthorizedException("Authentication is required", ErrorType.AUTHENTICATION_FAILED);
		}
		return userId;
	}
}
