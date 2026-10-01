package com.expensemanager.service.helper;

import com.expensemanager.common.error.ErrorType;
import com.expensemanager.common.exception.UnauthorizedException;
import com.expensemanager.common.util.DateTimeUtil;
import com.expensemanager.config.GoogleProperties;
import com.expensemanager.domain.enums.AuthProvider;
import com.expensemanager.domain.enums.SignupMethod;
import com.expensemanager.domain.jpa.AccountBalance;
import com.expensemanager.domain.jpa.QUser;
import com.expensemanager.domain.jpa.QUserAuthIdentity;
import com.expensemanager.domain.jpa.User;
import com.expensemanager.domain.jpa.UserAuthIdentity;
import com.expensemanager.repository.jpa.AccountBalanceRepository;
import com.expensemanager.repository.jpa.UserAuthIdentityRepository;
import com.expensemanager.repository.jpa.UserRepository;
import com.expensemanager.security.google.GoogleIdTokenVerifierAdapter.GoogleIdentity;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.Optional;

/** Maps a verified Google identity onto a local account, creating one on first sign-in. */
@Component
public class GoogleAccountLinker {

	private static final QUserAuthIdentity IDENTITY = QUserAuthIdentity.userAuthIdentity;
	private static final QUser USER = QUser.user;
	private static final String DEFAULT_ROLE = "MEMBER";

	private final UserRepository userRepository;
	private final UserAuthIdentityRepository identityRepository;
	private final AccountBalanceRepository accountBalanceRepository;
	private final GoogleProperties properties;

	public GoogleAccountLinker(UserRepository userRepository, UserAuthIdentityRepository identityRepository,
			AccountBalanceRepository accountBalanceRepository, GoogleProperties properties) {
		this.userRepository = userRepository;
		this.identityRepository = identityRepository;
		this.accountBalanceRepository = accountBalanceRepository;
		this.properties = properties;
	}

	@Transactional
	public User resolve(GoogleIdentity identity) {
		// Matched on the provider subject first. It is stable, whereas an email address can be
		// changed at Google and later reassigned to someone else.
		Optional<UserAuthIdentity> existing = identityRepository.findOne(
				IDENTITY.provider.eq(AuthProvider.GOOGLE).and(IDENTITY.providerUserId.eq(identity.subject())), IDENTITY);

		if (existing.isPresent()) {
			return existing.get().getUser();
		}

		Optional<User> byEmail = userRepository.findOne(USER.email.equalsIgnoreCase(identity.email()), USER);
		if (byEmail.isPresent()) {
			// Same address, first time through Google. Safe to link only because the provider has
			// confirmed the address belongs to the person signing in.
			link(byEmail.get(), identity);
			return byEmail.get();
		}

		if (!properties.isAllowSignup()) {
			throw new UnauthorizedException("No account exists for that Google address", ErrorType.LOGIN_FAILED);
		}

		User created = createAccount(identity);
		link(created, identity);
		return created;
	}

	private void link(User user, GoogleIdentity identity) {
		UserAuthIdentity link = new UserAuthIdentity();
		link.setUser(user);
		link.setProvider(AuthProvider.GOOGLE);
		link.setProviderUserId(identity.subject());
		link.setEmail(identity.email());
		identityRepository.save(link);
	}

	private User createAccount(GoogleIdentity identity) {
		User user = new User();
		user.setEmail(identity.email());
		user.setUsername(uniqueUsername(identity.email()));
		user.setFirstName(firstNameOrFallback(identity));
		user.setLastName(identity.lastName());
		// No password at all, rather than a generated one. An account that never chose a password
		// should not have one that works.
		user.setPassword(null);
		user.setRole(DEFAULT_ROLE);
		user.setSignupMethod(SignupMethod.GOOGLE);
		user.setActive(true);
		user.setEmailVerified(true);

		User saved = userRepository.save(user);
		createOpeningBalance(saved);
		return saved;
	}

	private String firstNameOrFallback(GoogleIdentity identity) {
		if (identity.firstName() != null && !identity.firstName().isBlank()) {
			return identity.firstName();
		}
		return identity.email().split("@")[0];
	}

	/** Usernames are unique, and the local part of an address often is not. */
	private String uniqueUsername(String email) {
		String base = email.split("@")[0].toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9._-]", "");
		if (base.length() < 3) {
			base = "user" + base;
		}

		String candidate = base;
		int suffix = 1;
		while (userRepository.exists(USER.username.equalsIgnoreCase(candidate), USER)) {
			candidate = base + suffix++;
		}
		return candidate;
	}

	private void createOpeningBalance(User user) {
		AccountBalance balance = new AccountBalance();
		balance.setUser(user);
		balance.setAccountName(user.getFirstName() + "'s Account");
		balance.setCurrentBalance(BigDecimal.ZERO);
		balance.setLastUpdated(DateTimeUtil.currentEpochMillisUtc());
		accountBalanceRepository.save(balance);
	}
}
