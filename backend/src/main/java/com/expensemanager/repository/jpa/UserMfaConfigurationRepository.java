package com.expensemanager.repository.jpa;

import com.expensemanager.common.repository.CustomRepository;
import com.expensemanager.domain.enums.MfaType;
import com.expensemanager.domain.jpa.UserMfaConfiguration;

import java.util.List;
import java.util.Optional;

public interface UserMfaConfigurationRepository extends CustomRepository<UserMfaConfiguration, String> {

	Optional<UserMfaConfiguration> findByUserIdAndMfaType(String userId, MfaType mfaType);

	List<UserMfaConfiguration> findByUserId(String userId);

	List<UserMfaConfiguration> findByUserIdAndEnabledTrue(String userId);

	boolean existsByUserIdAndEnabledTrue(String userId);
}
