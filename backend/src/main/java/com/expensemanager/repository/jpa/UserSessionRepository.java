package com.expensemanager.repository.jpa;

import com.expensemanager.common.repository.CustomRepository;
import com.expensemanager.domain.jpa.UserSession;

import java.util.Optional;

public interface UserSessionRepository extends CustomRepository<UserSession, String> {

	Optional<UserSession> findByIdAndUserId(String id, String userId);

	Optional<UserSession> findByRefreshTokenHash(String refreshTokenHash);
}
