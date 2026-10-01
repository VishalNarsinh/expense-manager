package com.expensemanager.service.impl;

import com.expensemanager.common.context.RequestContext;
import com.expensemanager.common.error.ErrorType;
import com.expensemanager.common.exception.EntityExistsException;
import com.expensemanager.common.exception.EntityNotFoundException;
import com.expensemanager.common.exception.ForbiddenException;
import com.expensemanager.common.exception.UnauthorizedException;
import com.expensemanager.domain.enums.SignupMethod;
import com.expensemanager.domain.enums.TokenTier;
import com.expensemanager.domain.jpa.AccountBalance;
import com.expensemanager.domain.jpa.User;
import com.expensemanager.domain.jpa.UserSession;
import com.expensemanager.dto.request.SignupRequest;
import com.expensemanager.dto.response.TokenResponse;
import com.expensemanager.dto.response.UserResponse;
import com.expensemanager.repository.jpa.AccountBalanceRepository;
import com.expensemanager.repository.jpa.UserRepository;
import com.expensemanager.security.credential.Credential;
import com.expensemanager.security.credential.CredentialAuthenticatorRegistry;
import com.expensemanager.security.strategy.LoginHandler;
import com.expensemanager.security.token.IssuedToken;
import com.expensemanager.security.token.TokenIssuer;
import com.expensemanager.service.AuthenticationService;
import com.expensemanager.service.helper.SessionHelper;
import com.expensemanager.service.helper.UserHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

@Service
public class AuthenticationServiceImpl implements AuthenticationService {

	private static final String DEFAULT_ROLE = "MEMBER";
	private static final String DEFAULT_ACCOUNT_SUFFIX = "'s Account";

	private final UserRepository userRepository;
	private final AccountBalanceRepository accountBalanceRepository;
	private final CredentialAuthenticatorRegistry credentialAuthenticators;
	private final List<LoginHandler> loginHandlers;
	private final SessionHelper sessionHelper;
	private final UserHelper userHelper;
	private final TokenIssuer tokenIssuer;

	public AuthenticationServiceImpl(UserRepository userRepository, AccountBalanceRepository accountBalanceRepository,
			CredentialAuthenticatorRegistry credentialAuthenticators, List<LoginHandler> loginHandlers,
			SessionHelper sessionHelper, UserHelper userHelper, TokenIssuer tokenIssuer) {
		this.userRepository = userRepository;
		this.accountBalanceRepository = accountBalanceRepository;
		this.credentialAuthenticators = credentialAuthenticators;
		this.loginHandlers = loginHandlers;
		this.sessionHelper = sessionHelper;
		this.userHelper = userHelper;
		this.tokenIssuer = tokenIssuer;
	}

	@Override
	@Transactional
	public UserResponse signup(SignupRequest request) {
		String email = request.email().trim();
		String username = request.username().trim();

		if (userHelper.emailTaken(email)) {
			throw new EntityExistsException("An account with this email address already exists", ErrorType.RESOURCE_ALREADY_EXISTS, "email");
		}
		if (userHelper.usernameTaken(username)) {
			throw new EntityExistsException("That username is taken", ErrorType.RESOURCE_ALREADY_EXISTS, "username");
		}

		User user = new User();
		user.setEmail(email);
		user.setUsername(username);
		user.setFirstName(request.firstName().trim());
		user.setLastName(request.lastName() == null ? null : request.lastName().trim());
		user.setPassword(userHelper.hashPassword(request.password()));
		user.setRole(DEFAULT_ROLE);
		user.setSignupMethod(SignupMethod.EMAIL);
		user.setActive(true);

		User saved = userRepository.save(user);
		createOpeningBalance(saved);

		return UserResponse.from(saved);
	}

	@Override
	@Transactional
	public TokenResponse login(Credential credential, LoginHandler.LoginContext context) {
		User user = credentialAuthenticators.authenticate(credential);

		// First matching handler wins; the terminal one is ordered last.
		LoginHandler handler = loginHandlers.stream()
				.filter(candidate -> candidate.canHandle(user))
				.findFirst()
				.orElseThrow(() -> new IllegalStateException("No login handler accepted the authenticated user"));

		return handler.handle(user, credential.method(), context);
	}

	@Override
	@Transactional
	public TokenResponse refresh(String refreshToken) {
		UserSession session = sessionHelper.findActiveByRefreshToken(refreshToken)
				.orElseThrow(() -> new UnauthorizedException("That refresh token is no longer valid", ErrorType.INVALID_REFRESH_TOKEN));

		User user = session.getUser();
		if (!user.isActive()) {
			sessionHelper.expire(session.getId());
			throw new UnauthorizedException("This account has been deactivated", ErrorType.LOGIN_FAILED);
		}

		// Rotate on every use: a refresh token presented twice means it was captured, and the
		// second presentation finds a hash that no longer resolves.
		IssuedToken accessToken = tokenIssuer.issueAccessToken(user.getId(), session.getId(), TokenTier.FULL);
		IssuedToken rotated = tokenIssuer.issueRefreshToken();
		sessionHelper.attachRefreshToken(session.getId(), rotated.value(), rotated.expiresAt().toEpochMilli());

		return TokenResponse.active(accessToken.value(), rotated.value(), accessToken.expiresAt().toEpochMilli(),
				session.getId(), session.getSessionSource());
	}

	@Override
	@Transactional
	public void logout(String sessionId) {
		String userId = requireUserId();
		UserSession session = sessionHelper.require(sessionId);

		if (!Objects.equals(session.getUser().getId(), userId)) {
			throw new ForbiddenException("That session belongs to another account");
		}
		sessionHelper.expire(session.getId());
	}

	@Override
	@Transactional
	public int logoutEverywhere() {
		return sessionHelper.expireAllForUser(requireUserId());
	}

	@Override
	@Transactional(readOnly = true)
	public UserResponse currentUser() {
		return userHelper.findById(requireUserId())
				.map(UserResponse::from)
				.orElseThrow(() -> new EntityNotFoundException("Account not found"));
	}

	/**
	 * Every account starts with a balance row, so the dashboard and reconciliation have something
	 * to read before the first transaction is entered.
	 */
	private void createOpeningBalance(User user) {
		AccountBalance balance = new AccountBalance();
		balance.setUser(user);
		balance.setAccountName(user.getFirstName() + DEFAULT_ACCOUNT_SUFFIX);
		balance.setCurrentBalance(BigDecimal.ZERO);
		balance.setLastUpdated(com.expensemanager.common.util.DateTimeUtil.currentEpochMillisUtc());
		accountBalanceRepository.save(balance);
	}

	private String requireUserId() {
		String userId = RequestContext.current().getUserId();
		if (userId == null) {
			throw new UnauthorizedException("Authentication is required", ErrorType.AUTHENTICATION_FAILED);
		}
		return userId;
	}
}
