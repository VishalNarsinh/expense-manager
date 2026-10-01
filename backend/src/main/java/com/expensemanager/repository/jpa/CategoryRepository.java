package com.expensemanager.repository.jpa;

import com.expensemanager.common.repository.CustomRepository;
import com.expensemanager.domain.enums.TransactionType;
import com.expensemanager.domain.jpa.Category;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends CustomRepository<Category, String> {

	/**
	 * Everything a user can pick from: their own categories plus the system ones. Written as an
	 * explicit query because a derived name cannot express "mine or nobody's".
	 */
	@Query("""
			SELECT c FROM Category c
			WHERE (c.user.id = :userId OR c.user IS NULL)
			ORDER BY c.name ASC
			""")
	List<Category> findVisibleTo(@Param("userId") String userId);

	@Query("""
			SELECT c FROM Category c
			WHERE (c.user.id = :userId OR c.user IS NULL) AND c.type = :type
			ORDER BY c.name ASC
			""")
	List<Category> findVisibleTo(@Param("userId") String userId, @Param("type") TransactionType type);

	/** Resolves one visible category, so a user cannot address another account's category by id. */
	@Query("""
			SELECT c FROM Category c
			WHERE c.id = :id AND (c.user.id = :userId OR c.user IS NULL)
			""")
	Optional<Category> findVisibleById(@Param("id") String id, @Param("userId") String userId);

	Optional<Category> findByIdAndUserId(String id, String userId);

	List<Category> findByUserId(String userId);
}
