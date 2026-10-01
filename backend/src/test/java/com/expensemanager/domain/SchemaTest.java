package com.expensemanager.domain;

import com.expensemanager.common.domain.ModulePrefix;
import com.expensemanager.domain.enums.TransactionType;
import com.expensemanager.domain.jpa.Category;
import com.expensemanager.domain.jpa.User;
import com.expensemanager.repository.jpa.CategoryRepository;
import com.expensemanager.repository.jpa.UserRepository;
import com.expensemanager.repository.predicate.CategoryPredicates;
import com.expensemanager.repository.predicate.UserPredicates;
import com.expensemanager.support.PostgresIntegrationTest;
import jakarta.persistence.EntityManager;
import jakarta.persistence.metamodel.EntityType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Checks that the Liquibase schema and the JPA mapping agree.
 *
 * <p>Context startup alone proves a lot here: {@code ddl-auto: validate} fails the boot if any
 * entity references a column or table the changelog did not create.
 */
class SchemaTest extends PostgresIntegrationTest {

	@Autowired
	private EntityManager entityManager;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private CategoryRepository categoryRepository;

	@Test
	void everyEntityHasAnIdPrefixRegistered() {
		Set<EntityType<?>> entities = entityManager.getMetamodel().getEntities();
		assertThat(entities).isNotEmpty();

		// An unregistered entity throws rather than silently producing ids prefixed "null".
		for (EntityType<?> entity : entities) {
			assertThat(ModulePrefix.forEntity(entity.getJavaType()))
					.as("id prefix for %s", entity.getName())
					.isNotBlank();
		}
	}

	@Test
	void defaultCategoriesAreSeededAsSystemCategories() {
		List<Category> seeded = categoryRepository.findAll(
				CategoryPredicates.visibleTo("usr_nobody"), CategoryPredicates.CATEGORY, CategoryPredicates.byName());

		assertThat(seeded).hasSize(19);
		assertThat(seeded).allMatch(Category::isSystemCategory);
		assertThat(seeded).allMatch(Category::isSystemDefault);
		assertThat(seeded.stream().filter(c -> c.getType() == TransactionType.EXPENSE)).hasSize(12);
		assertThat(seeded.stream().filter(c -> c.getType() == TransactionType.INCOME)).hasSize(7);
		assertThat(seeded).allSatisfy(category -> {
			assertThat(category.getIcon()).startsWith("bi-");
			assertThat(category.getColorCode()).matches("#[0-9a-f]{6}");
		});
	}

	@Test
	void generatedIdsCarryTheEntityPrefix() {
		User saved = userRepository.save(newUser("prefix@example.com", "prefixuser"));

		assertThat(saved.getId()).startsWith("usr_");
		assertThat(saved.getId()).hasSizeLessThanOrEqualTo(40);
		assertThat(saved.getCreated()).isPositive();
		assertThat(saved.getModified()).isEqualTo(saved.getCreated());
	}

	@Test
	@Transactional
	void emailUniquenessIsCaseInsensitive() {
		userRepository.saveAndFlush(newUser("Casing@Example.com", "casinguser"));

		assertThatThrownBy(() -> userRepository.saveAndFlush(newUser("casing@example.com", "otheruser")))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	private User newUser(String email, String username) {
		User user = new User();
		user.setEmail(email);
		user.setUsername(username);
		user.setFirstName("Test");
		user.setLastName("User");
		user.setRole("MEMBER");
		user.setSignupMethod(com.expensemanager.domain.enums.SignupMethod.EMAIL);
		user.setActive(true);
		return user;
	}
}
