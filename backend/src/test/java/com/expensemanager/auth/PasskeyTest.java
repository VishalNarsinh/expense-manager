package com.expensemanager.auth;

import com.expensemanager.common.exception.BadRequestException;
import com.expensemanager.domain.enums.PasskeyChallengeStatus;
import com.expensemanager.domain.enums.PasskeyChallengeType;
import com.expensemanager.domain.jpa.QPasskeyChallenge;
import com.expensemanager.domain.jpa.User;
import com.expensemanager.repository.jpa.PasskeyChallengeRepository;
import com.expensemanager.repository.jpa.UserRepository;
import com.expensemanager.repository.predicate.UserPredicates;
import com.expensemanager.security.passkey.PasskeyChallengeHelper;
import com.expensemanager.support.PostgresIntegrationTest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Covers the parts of the passkey flow this server owns: issuing options, the single-use rules
 * around a challenge, and who may reach which endpoint.
 *
 * <p>The cryptographic ceremony itself is not exercised here. Producing a valid attestation or
 * assertion needs an authenticator, so that half is covered by the library and by browser testing;
 * what is tested here is everything the library trusts us to get right.
 */
@AutoConfigureMockMvc
class PasskeyTest extends PostgresIntegrationTest {

	private static final String PASSWORD = "correct-horse-battery";
	private static final AtomicInteger SEQUENCE = new AtomicInteger();
	private static final QPasskeyChallenge CHALLENGE = QPasskeyChallenge.passkeyChallenge;

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private PasskeyChallengeRepository challengeRepository;

	@Autowired
	private PasskeyChallengeHelper challengeHelper;

	private String email;
	private String accessToken;
	private User user;

	@BeforeEach
	void signUp() throws Exception {
		int n = SEQUENCE.incrementAndGet();
		email = "passkey" + n + "@example.com";

		mockMvc.perform(json(post("/signup"), Map.of(
						"first_name", "Passkey", "email", email,
						"username", "passkeyuser" + n, "password", PASSWORD)))
				.andExpect(status().isCreated());

		accessToken = asJson(mockMvc.perform(json(post("/login"), Map.of("identifier", email, "password", PASSWORD)))
				.andExpect(status().isOk()).andReturn()).get("access_token").asText();

		user = userRepository.findOne(UserPredicates.byEmail(email), UserPredicates.USER).orElseThrow();
	}

	@Test
	void loginOptionsAreAvailableWithoutSigningIn() throws Exception {
		JsonNode options = asJson(mockMvc.perform(post("/passkey/login/options"))
				.andExpect(status().isOk())
				.andReturn());

		JsonNode publicKey = options.get("publicKey");
		assertThat(publicKey.get("challenge").asText()).isNotBlank();
		// Required user verification is what makes a passkey count as two factors.
		assertThat(publicKey.get("userVerification").asText()).isEqualTo("required");
	}

	@Test
	void everyRequestGetsItsOwnChallenge() throws Exception {
		String first = challengeFrom(mockMvc.perform(post("/passkey/login/options")).andReturn());
		String second = challengeFrom(mockMvc.perform(post("/passkey/login/options")).andReturn());

		assertThat(first).isNotEqualTo(second);
	}

	@Test
	void registrationOptionsRequireSigningIn() throws Exception {
		mockMvc.perform(post("/passkey/register/options"))
				.andExpect(status().isUnauthorized());

		JsonNode options = asJson(mockMvc.perform(json(post("/passkey/register/options"), Map.of("label", "Work laptop"))
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
				.andExpect(status().isOk())
				.andReturn());

		JsonNode publicKey = options.get("publicKey");
		assertThat(publicKey.get("user").get("name").asText()).isEqualTo(email);
		assertThat(publicKey.get("authenticatorSelection").get("residentKey").asText()).isEqualTo("required");
	}

	@Test
	void theIssuedRequestIsStoredWholeAlongsideTheChallenge() throws Exception {
		mockMvc.perform(json(post("/passkey/register/options"), Map.of("label", "Phone"))
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)).andExpect(status().isOk());

		var stored = challengeRepository.findAll(CHALLENGE.user.id.eq(user.getId()), CHALLENGE);

		assertThat(stored).hasSize(1);
		assertThat(stored.get(0).getStatus()).isEqualTo(PasskeyChallengeStatus.PENDING);
		assertThat(stored.get(0).getChallengeType()).isEqualTo(PasskeyChallengeType.REGISTRATION);
		assertThat(stored.get(0).getDisplayName()).isEqualTo("Phone");
		// Verification replays this rather than rebuilding it, which is what keeps the rules the
		// response is checked against identical to the ones the client was given.
		assertThat(stored.get(0).getChallengeRequest()).contains("residentKey").contains("required");
	}

	@Test
	void aChallengeCanOnlyBeSpentOnce() {
		challengeHelper.persist(user, null, PasskeyChallengeType.LOGIN, "challenge-single-use", "{}", null);

		assertThat(challengeHelper.consume("challenge-single-use", user.getId(), null)).isNotNull();

		assertThatThrownBy(() -> challengeHelper.consume("challenge-single-use", user.getId(), null))
				.isInstanceOf(BadRequestException.class)
				.hasMessageContaining("already been used");
	}

	@Test
	void anUnknownChallengeIsRefused() {
		assertThatThrownBy(() -> challengeHelper.consume("never-issued", user.getId(), null))
				.isInstanceOf(BadRequestException.class)
				.hasMessageContaining("not recognised");
	}

	@Test
	void aChallengeCannotBeSpentByAnotherAccount() {
		challengeHelper.persist(user, null, PasskeyChallengeType.LOGIN, "challenge-other-user", "{}", null);

		assertThatThrownBy(() -> challengeHelper.consume("challenge-other-user", "usr_someone_else", null))
				.isInstanceOf(BadRequestException.class)
				.hasMessageContaining("different account");
	}

	@Test
	void aChallengeCannotBeSpentFromAnotherSignIn() {
		challengeHelper.persist(user, "ses_original", PasskeyChallengeType.MFA, "challenge-other-session", "{}", null);

		assertThatThrownBy(() -> challengeHelper.consume("challenge-other-session", user.getId(), "ses_different"))
				.isInstanceOf(BadRequestException.class)
				.hasMessageContaining("different sign-in");
	}

	@Test
	void listingPasskeysIsScopedToTheCaller() throws Exception {
		mockMvc.perform(get("/passkeys").header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$").isArray())
				.andExpect(jsonPath("$.length()").value(0));
	}

	@Test
	void revokingSomethingThatIsNotYoursIsNotFound() throws Exception {
		mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
						.delete("/passkeys/pkc_does_not_exist")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
				.andExpect(status().isNotFound());
	}

	private String challengeFrom(MvcResult result) throws Exception {
		return asJson(result).get("publicKey").get("challenge").asText();
	}

	private JsonNode asJson(MvcResult result) throws Exception {
		return objectMapper.readTree(result.getResponse().getContentAsString());
	}

	private MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder builder, Map<String, ?> body) throws Exception {
		return builder.contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(body));
	}
}
