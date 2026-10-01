package com.expensemanager.service;

import com.expensemanager.dto.response.PasskeyResponse;
import com.expensemanager.dto.response.TokenResponse;
import com.expensemanager.security.strategy.LoginHandler;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;

public interface PasskeyService {

	JsonNode startRegistration(String label);

	PasskeyResponse finishRegistration(JsonNode credential);

	JsonNode startLogin();

	TokenResponse finishLogin(JsonNode credential, LoginHandler.LoginContext context);

	JsonNode startMfaAssertion(String sessionId);

	TokenResponse finishMfaAssertion(String sessionId, JsonNode credential);

	List<PasskeyResponse> list();

	void revoke(String passkeyId);
}
