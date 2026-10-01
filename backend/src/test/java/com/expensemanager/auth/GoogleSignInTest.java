package com.expensemanager.auth;

import com.expensemanager.domain.enums.AuthProvider;
import com.expensemanager.domain.enums.SignupMethod;
import com.expensemanager.domain.jpa.QAccountBalance;
import com.expensemanager.domain.jpa.QUserAuthIdentity;
import com.expensemanager.domain.jpa.User;
import com.expensemanager.repository.jpa.AccountBalanceRepository;
import com.expensemanager.repository.jpa.UserAuthIdentityRepository;
import com.expensemanager.repository.jpa.UserRepository;
import com.expensemanager.repository.predicate.UserPredicates;
import com.expensemanager.security.google.GoogleIdTokenVerifierAdapter.GoogleIdentity;
import com.expensemanager.service.helper.GoogleAccountLinker;
import com.expensemanager.support.PostgresIntegrationTest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * How a verified Google identity maps onto a local account.
 *
 * <p>Token verification itself belongs to Google's library and cannot be exercised without their
 * signing key, so these tests start from an already-verified identity and cover the matching rules,
 * which are ours.
 */
@AutoConfigureMockMvc
class GoogleSignInTest extends PostgresIntegrationTest {

	private static final AtomicInteger SEQUENCE = new AtomicInteger();

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private GoogleAccountLinker accountLinker;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private UserAuthIdentityRepository identityRepository;

	@Autowired
	private AccountBalanceRepository accountBalanceRepository;

	@Test
	void firstSignInCreatesAnAccountWithNoPassword() {
		String email = email();

		User created = accountLinker.resolve(new GoogleIdentity(subject(), email, "Ada", "Lovelace"));

		assertThat(created.getEmail()).isEqualTo(email);
		assertThat(created.getFirstName()).isEqualTo("Ada");
		assertThat(created.getLastName()).isEqualTo("Lovelace");
		assertThat(created.getSignupMethod()).isEqualTo(SignupMethod.GOOGLE);
		assertThat(created.isEmailVerified()).isTrue();
		// Not a generated password: an account that never chose one should not have one that works.
		assertThat(created.getPassword()).isNull();

		assertThat(accountBalanceRepository.findOne(QAccountBalance.accountBalance.user.id.eq(created.getId()),
				QAccountBalance.accountBalance)).isPresent();
	}

	@Test
	void signingInAgainReturnsTheSameAccount() {
		String subject = subject();
		String email = email();

		User first = accountLinker.resolve(new GoogleIdentity(subject, email, "Grace", "Hopper"));
		User second = accountLinker.resolve(new GoogleIdentity(subject, email, "Grace", "Hopper"));

		assertThat(second.getId()).isEqualTo(first.getId());
		assertThat(identityRepository.count(QUserAuthIdentity.userAuthIdentity.user.id.eq(first.getId()),
				QUserAuthIdentity.userAuthIdentity)).isEqualTo(1);
	}

	@Test
	void aChangedGoogleAddressStillFindsTheSameAccount() {
		String subject = subject();

		User original = accountLinker.resolve(new GoogleIdentity(subject, email(), "Alan", "Turing"));
		// The subject is stable where an address is not, which is why it is matched on first.
		User afterRename = accountLinker.resolve(new GoogleIdentity(subject, email(), "Alan", "Turing"));

		assertThat(afterRename.getId()).isEqualTo(original.getId());
	}

	@Test
	void anExistingPasswordAccountIsLinkedRatherThanDuplicated() throws Exception {
		String email = email();
		mockMvc.perform(post("/signup")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(Map.of(
								"first_name", "Existing", "email", email,
								"username", "existing" + SEQUENCE.get(), "password", "correct-horse-battery"))))
				.andExpect(status().isCreated());

		User linked = accountLinker.resolve(new GoogleIdentity(subject(), email, "Existing", "User"));

		assertThat(userRepository.count(UserPredicates.byEmail(email), UserPredicates.USER)).isEqualTo(1);
		assertThat(linked.getSignupMethod()).isEqualTo(SignupMethod.EMAIL);
		// The password still works; linking adds a way in rather than replacing one.
		assertThat(linked.getPassword()).isNotNull();

		assertThat(identityRepository.findOne(QUserAuthIdentity.userAuthIdentity.user.id.eq(linked.getId())
				.and(QUserAuthIdentity.userAuthIdentity.provider.eq(AuthProvider.GOOGLE)),
				QUserAuthIdentity.userAuthIdentity)).isPresent();
	}

	@Test
	void usernamesStayUniqueWhenTwoAddressesShareALocalPart() {
		String localPart = "sameperson" + SEQUENCE.incrementAndGet();

		User first = accountLinker.resolve(new GoogleIdentity(subject(), localPart + "@one.example.com", "One", null));
		User second = accountLinker.resolve(new GoogleIdentity(subject(), localPart + "@two.example.com", "Two", null));

		assertThat(first.getUsername()).isEqualTo(localPart);
		assertThat(second.getUsername()).isEqualTo(localPart + "1");
	}

	@Test
	void aMissingGivenNameFallsBackToTheAddress() {
		String email = email();

		User created = accountLinker.resolve(new GoogleIdentity(subject(), email, null, null));

		assertThat(created.getFirstName()).isEqualTo(email.split("@")[0]);
	}

	@Test
	void theEndpointRefusesATokenItCannotVerify() throws Exception {
		// No client ids are configured in tests, so every token is refused. That is also the
		// behaviour in a deployment that has not been given any.
		mockMvc.perform(post("/sso-login")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(Map.of("id_token", "not-a-real-google-token"))))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.errors[0].type").value("login_failed"));
	}

	@Test
	void theEndpointRequiresAToken() throws Exception {
		mockMvc.perform(post("/sso-login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors[0].type").value("validation_failed"));
	}

	private String email() {
		return "google" + SEQUENCE.incrementAndGet() + "@example.com";
	}

	private String subject() {
		return "google-subject-" + SEQUENCE.incrementAndGet();
	}
}
