package com.expensemanager.service;

import com.expensemanager.dto.request.SignupRequest;
import com.expensemanager.dto.response.TokenResponse;
import com.expensemanager.dto.response.UserResponse;
import com.expensemanager.security.credential.Credential;
import com.expensemanager.security.strategy.LoginHandler;

public interface AuthenticationService {

	UserResponse signup(SignupRequest request);

	TokenResponse login(Credential credential, LoginHandler.LoginContext context);

	TokenResponse refresh(String refreshToken);

	void logout(String sessionId);

	int logoutEverywhere();

	UserResponse currentUser();
}
