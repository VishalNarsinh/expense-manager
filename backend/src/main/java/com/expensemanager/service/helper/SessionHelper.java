package com.expensemanager.service.helper;

import com.expensemanager.common.error.ErrorType;
import com.expensemanager.common.exception.BadRequestException;
import com.expensemanager.common.exception.EntityNotFoundException;
import com.expensemanager.common.util.DateTimeUtil;
import com.expensemanager.domain.enums.SessionState;
import com.expensemanager.domain.enums.SignupMethod;
import com.expensemanager.domain.jpa.QUserSession;
import com.expensemanager.domain.jpa.User;
import com.expensemanager.domain.jpa.UserSession;
import com.expensemanager.repository.jpa.UserSessionRepository;
import com.expensemanager.repository.predicate.SessionPredicates;
import com.expensemanager.security.token.TokenHasher;
import com.querydsl.core.types.Path;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Creates login sessions and guards the transitions between their states. */
@Component
public class SessionHelper {

	private static final QUserSession SESSION = SessionPredicates.SESSION;

	private final UserSessionRepository userSessionRepository;

	public SessionHelper(UserSessionRepository userSessionRepository) {
		this.userSessionRepository = userSessionRepository;
	}

	@Transactional
	public UserSession create(User user, SessionState state, SignupMethod source, String ipAddress, String userAgent) {
		UserSession session = new UserSession();
		session.setUser(user);
		session.setSessionState(state);
		session.setSessionSource(source);
		session.setIpAddress(ipAddress);
		session.setUserAgent(userAgent);
		return userSessionRepository.save(session);
	}

	@Transactional(readOnly = true)
	public UserSession require(String sessionId) {
		return userSessionRepository.findOne(SESSION.id.eq(sessionId))
				.orElseThrow(() -> new EntityNotFoundException("Session not found", ErrorType.INVALID_SESSION_STATE, "session"));
	}

	/**
	 * Asserts the session is in the state this step expects.
	 *
	 * <p>Checking the expected state, rather than merely that the session exists, is what stops one
	 * being pushed through a step it has already completed or never reached.
	 */
	@Transactional(readOnly = true)
	public UserSession requireState(String sessionId, SessionState expected, String param) {
		UserSession session = userSessionRepository.findOne(SESSION.id.eq(sessionId))
				.orElseThrow(() -> new EntityNotFoundException("Session not found", ErrorType.INVALID_SESSION_STATE, param));

		if (!Objects.equals(session.getSessionState(), expected)) {
			throw new BadRequestException("This session cannot be used for that step", ErrorType.INVALID_SESSION_STATE, param);
		}
		return session;
	}

	@Transactional(readOnly = true)
	public Optional<UserSession> findActiveByRefreshToken(String refreshToken) {
		return userSessionRepository.findOne(SessionPredicates.activeByRefreshTokenHash(TokenHasher.sha256(refreshToken)));
	}

	@Transactional
	public void attachRefreshToken(String sessionId, String refreshToken, long expiresAt) {
		Map<Path<?>, Object> values = new LinkedHashMap<>();
		values.put(SESSION.refreshTokenHash, TokenHasher.sha256(refreshToken));
		values.put(SESSION.expiresAt, expiresAt);
		userSessionRepository.updateFields(SESSION.id.eq(sessionId), SESSION, values);
	}

	@Transactional
	public void activate(String sessionId) {
		Map<Path<?>, Object> values = new LinkedHashMap<>();
		values.put(SESSION.sessionState, SessionState.ACTIVE);
		userSessionRepository.updateFields(SESSION.id.eq(sessionId), SESSION, values);
	}

	@Transactional
	public void expire(String sessionId) {
		Map<Path<?>, Object> values = new LinkedHashMap<>();
		values.put(SESSION.sessionState, SessionState.EXPIRED);
		// Clearing the hash is what actually revokes the token: a lookup by hash can no longer
		// reach this row, so the token is dead well before its own expiry.
		values.put(SESSION.refreshTokenHash, null);
		values.put(SESSION.otp, null);
		values.put(SESSION.expiresAt, DateTimeUtil.currentEpochMillisUtc());
		userSessionRepository.updateFields(SESSION.id.eq(sessionId), SESSION, values);
	}

	@Transactional
	public int expireAllForUser(String userId) {
		List<UserSession> live = userSessionRepository.findAll(SessionPredicates.liveForUser(userId), SESSION);
		live.forEach(session -> expire(session.getId()));
		return live.size();
	}
}
