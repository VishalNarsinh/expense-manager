package com.expensemanager.service.helper;

import com.expensemanager.common.util.DateTimeUtil;
import com.expensemanager.domain.jpa.QUser;
import com.expensemanager.domain.jpa.User;
import com.expensemanager.repository.jpa.UserRepository;
import com.expensemanager.repository.predicate.UserPredicates;
import com.expensemanager.security.token.TokenHasher;
import com.querydsl.core.types.Expression;
import com.querydsl.core.types.Path;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/** Account lookup and password checking. */
@Component
public class UserHelper {

	private static final QUser USER = UserPredicates.USER;

	/** Just enough to decide whether a sign-in succeeds and which handler should run. */
	private static final Expression<?>[] SIGN_IN_FIELDS = {
			USER.id, USER.email, USER.username, USER.password, USER.active, USER.mfaEnabled, USER.signupMethod};

	/**
	 * Verifying against this when no account exists keeps the work, and so the response time,
	 * roughly equal to a real check. Without it a missing account returns noticeably faster, which
	 * is enough to test whether an address is registered.
	 */
	private static final String DUMMY_HASH = "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy";

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;

	public UserHelper(UserRepository userRepository, PasswordEncoder passwordEncoder) {
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
	}

	/** Partial entity: carries only {@link #SIGN_IN_FIELDS} and must not be persisted. */
	public Optional<User> findForSignIn(String identifier) {
		if (identifier == null || identifier.isBlank()) {
			return Optional.empty();
		}
		return userRepository.findOneProjected(UserPredicates.byIdentifier(identifier.trim()), USER, SIGN_IN_FIELDS);
	}

	public Optional<User> findById(String userId) {
		return userRepository.findOne(USER.id.eq(userId), USER);
	}

	public boolean emailTaken(String email) {
		return userRepository.exists(UserPredicates.byEmail(email.trim()), USER);
	}

	public boolean usernameTaken(String username) {
		return userRepository.exists(UserPredicates.byUsername(username.trim()), USER);
	}

	public boolean verifyPassword(User user, String presented) {
		if (presented == null || presented.isEmpty()) {
			return false;
		}
		if (user.getPassword() == null) {
			// Registered through Google or a passkey and never set a password. Still run a
			// comparison so the timing matches an account that has one.
			passwordEncoder.matches(presented, DUMMY_HASH);
			return false;
		}
		return passwordEncoder.matches(presented, user.getPassword());
	}

	public String hashPassword(String rawPassword) {
		return passwordEncoder.encode(rawPassword);
	}

	public String hashRefreshToken(String refreshToken) {
		return TokenHasher.sha256(refreshToken);
	}

	@Transactional
	public void recordSuccessfulLogin(String userId) {
		Map<Path<?>, Object> values = new LinkedHashMap<>();
		values.put(USER.failedLoginCount, 0);
		values.put(USER.lastLogin, DateTimeUtil.currentEpochMillisUtc());
		userRepository.updateFields(USER.id.eq(userId), USER, values);
	}

	@Transactional
	public void recordFailedLogin(String userId) {
		Map<Path<?>, Object> values = new LinkedHashMap<>();
		// Incremented in the database rather than read-then-written, so simultaneous failed
		// attempts cannot overwrite each other and undercount.
		values.put(USER.failedLoginCount, USER.failedLoginCount.add(1));
		userRepository.updateFields(USER.id.eq(userId), USER, values);
	}

	@Transactional
	public void setMfaEnabled(String userId, boolean enabled) {
		Map<Path<?>, Object> values = new LinkedHashMap<>();
		values.put(USER.mfaEnabled, enabled);
		userRepository.updateFields(USER.id.eq(userId), USER, values);
	}
}
