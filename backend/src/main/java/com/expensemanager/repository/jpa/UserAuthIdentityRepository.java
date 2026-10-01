package com.expensemanager.repository.jpa;

import com.expensemanager.common.repository.CustomRepository;
import com.expensemanager.domain.enums.AuthProvider;
import com.expensemanager.domain.jpa.UserAuthIdentity;

import java.util.List;
import java.util.Optional;

public interface UserAuthIdentityRepository extends CustomRepository<UserAuthIdentity, String> {

	Optional<UserAuthIdentity> findByProviderAndProviderUserId(AuthProvider provider, String providerUserId);

	List<UserAuthIdentity> findByUserId(String userId);
}
