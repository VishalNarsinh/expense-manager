package com.expensemanager.repository.jpa;

import com.expensemanager.common.repository.CustomRepository;
import com.expensemanager.domain.jpa.PasskeyCredential;

import java.util.List;
import java.util.Optional;

public interface PasskeyCredentialRepository extends CustomRepository<PasskeyCredential, String> {

	Optional<PasskeyCredential> findByCredentialId(String credentialId);

	Optional<PasskeyCredential> findByIdAndUserId(String id, String userId);

	List<PasskeyCredential> findByUserIdOrderByCreatedDesc(String userId);

	long countByUserId(String userId);
}
