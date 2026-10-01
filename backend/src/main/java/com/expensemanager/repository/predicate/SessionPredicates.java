package com.expensemanager.repository.predicate;

import com.expensemanager.domain.enums.SessionState;
import com.expensemanager.domain.jpa.QUserSession;
import com.querydsl.core.types.Predicate;

/** Reusable predicates over {@link com.expensemanager.domain.jpa.UserSession}. */
public final class SessionPredicates {

	public static final QUserSession SESSION = QUserSession.userSession;

	private SessionPredicates() {
	}

	/**
	 * An expired session keeps no refresh hash, so this matches only sessions that can still be
	 * extended.
	 */
	public static Predicate activeByRefreshTokenHash(String refreshTokenHash) {
		return SESSION.refreshTokenHash.eq(refreshTokenHash).and(SESSION.sessionState.eq(SessionState.ACTIVE));
	}

	public static Predicate liveForUser(String userId) {
		return SESSION.user.id.eq(userId).and(SESSION.sessionState.ne(SessionState.EXPIRED));
	}

	public static Predicate byIdAndUser(String sessionId, String userId) {
		return SESSION.id.eq(sessionId).and(SESSION.user.id.eq(userId));
	}
}
