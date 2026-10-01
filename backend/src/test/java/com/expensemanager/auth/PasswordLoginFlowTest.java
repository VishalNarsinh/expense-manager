package com.expensemanager.auth;

import com.expensemanager.domain.jpa.QAccountBalance;
import com.expensemanager.repository.jpa.AccountBalanceRepository;
import com.expensemanager.repository.jpa.UserRepository;
import com.expensemanager.repository.predicate.UserPredicates;
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

import java.math.BigDecimal;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Sign up, sign in, use the token, refresh it, and sign out. */
@AutoConfigureMockMvc
class PasswordLoginFlowTest extends PostgresIntegrationTest {

	private static final String EMAIL = "flow@example.com";
	private static final String USERNAME = "flowuser";
	private static final String PASSWORD = "correct-horse-battery";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private AccountBalanceRepository accountBalanceRepository;

	@BeforeEach
	void signUpOnce() throws Exception {
		if (userRepository.exists(UserPredicates.byEmail(EMAIL))) {
			return;
		}
		mockMvc.perform(json(post("/signup"), Map.of(
						"first_name", "Flow",
						"last_name", "Tester",
						"email", EMAIL,
						"username", USERNAME,
						"password", PASSWORD)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.email").value(EMAIL))
				.andExpect(jsonPath("$.display_name").value("Flow Tester"))
				.andExpect(jsonPath("$.password").doesNotExist());
	}

	@Test
	void signupCreatesAnOpeningBalance() {
		var user = userRepository.findOne(UserPredicates.byEmail(EMAIL)).orElseThrow();

		var balance = accountBalanceRepository.findOne(QAccountBalance.accountBalance.user.id.eq(user.getId()));

		assertThat(balance).isPresent();
		assertThat(balance.get().getCurrentBalance()).isEqualByComparingTo(BigDecimal.ZERO);
	}

	@Test
	void rejectsADuplicateEmail() throws Exception {
		mockMvc.perform(json(post("/signup"), Map.of(
						"first_name", "Someone",
						"email", EMAIL,
						"username", "a-different-username",
						"password", PASSWORD)))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.errors[0].param").value("email"));
	}

	@Test
	void rejectsAShortPassword() throws Exception {
		mockMvc.perform(json(post("/signup"), Map.of(
						"first_name", "Someone",
						"email", "short@example.com",
						"username", "shortpw",
						"password", "abc")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors[0].type").value("validation_failed"))
				.andExpect(jsonPath("$.errors[0].param").value("password"));
	}

	@Test
	void signsInWithEmail() throws Exception {
		JsonNode tokens = login(EMAIL, PASSWORD);

		assertThat(tokens.get("session_state").asText()).isEqualTo("ACTIVE");
		assertThat(tokens.get("access_token").asText()).isNotBlank();
		assertThat(tokens.get("refresh_token").asText()).isNotBlank();
		assertThat(tokens.get("session").asText()).startsWith("ses_");
	}

	@Test
	void signsInWithUsernameToo() throws Exception {
		assertThat(login(USERNAME, PASSWORD).get("session_state").asText()).isEqualTo("ACTIVE");
	}

	@Test
	void refusesAWrongPassword() throws Exception {
		mockMvc.perform(json(post("/login"), Map.of("identifier", EMAIL, "password", "not-the-password")))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.errors[0].type").value("login_failed"));
	}

	@Test
	void unknownAccountAndWrongPasswordAreIndistinguishable() throws Exception {
		MvcResult missing = mockMvc.perform(json(post("/login"), Map.of("identifier", "nobody@example.com", "password", PASSWORD)))
				.andExpect(status().isUnauthorized())
				.andReturn();
		MvcResult wrongPassword = mockMvc.perform(json(post("/login"), Map.of("identifier", EMAIL, "password", "wrong")))
				.andExpect(status().isUnauthorized())
				.andReturn();

		// Identical bodies, so neither response reveals whether the account exists.
		assertThat(missing.getResponse().getContentAsString())
				.isEqualTo(wrongPassword.getResponse().getContentAsString());
	}

	@Test
	void accessTokenReachesAProtectedEndpoint() throws Exception {
		String accessToken = login(EMAIL, PASSWORD).get("access_token").asText();

		mockMvc.perform(get("/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.email").value(EMAIL))
				.andExpect(jsonPath("$.mfa_enabled").value(false));
	}

	@Test
	void refreshIssuesNewTokensAndRetiresTheOldOne() throws Exception {
		String firstRefresh = login(EMAIL, PASSWORD).get("refresh_token").asText();

		JsonNode refreshed = asJson(mockMvc.perform(json(post("/generate-token"), Map.of("refresh_token", firstRefresh)))
				.andExpect(status().isOk())
				.andReturn());

		assertThat(refreshed.get("refresh_token").asText()).isNotEqualTo(firstRefresh);

		// Rotation means a captured token is useless the moment the real client uses it.
		mockMvc.perform(json(post("/generate-token"), Map.of("refresh_token", firstRefresh)))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.errors[0].type").value("invalid_refresh_token"));
	}

	@Test
	void logoutRevokesTheRefreshToken() throws Exception {
		JsonNode tokens = login(EMAIL, PASSWORD);
		String accessToken = tokens.get("access_token").asText();

		mockMvc.perform(json(post("/logout"), Map.of("session", tokens.get("session").asText()))
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
				.andExpect(status().isOk());

		mockMvc.perform(json(post("/generate-token"), Map.of("refresh_token", tokens.get("refresh_token").asText())))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void cannotLogOutSomeoneElsesSession() throws Exception {
		JsonNode victim = login(EMAIL, PASSWORD);

		mockMvc.perform(json(post("/signup"), Map.of(
				"first_name", "Other", "email", "other@example.com", "username", "otheruser", "password", PASSWORD)));
		String attackerToken = login("other@example.com", PASSWORD).get("access_token").asText();

		mockMvc.perform(json(post("/logout"), Map.of("session", victim.get("session").asText()))
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + attackerToken))
				.andExpect(status().isForbidden());
	}

	@Test
	void signOutEverywhereEndsEverySession() throws Exception {
		login(EMAIL, PASSWORD);
		JsonNode second = login(EMAIL, PASSWORD);

		mockMvc.perform(delete("/sessions").header(HttpHeaders.AUTHORIZATION, "Bearer " + second.get("access_token").asText()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.count").value(org.hamcrest.Matchers.greaterThanOrEqualTo(2)));

		mockMvc.perform(json(post("/generate-token"), Map.of("refresh_token", second.get("refresh_token").asText())))
				.andExpect(status().isUnauthorized());
	}

	private JsonNode login(String identifier, String password) throws Exception {
		return asJson(mockMvc.perform(json(post("/login"), Map.of("identifier", identifier, "password", password)))
				.andExpect(status().isOk())
				.andReturn());
	}

	private JsonNode asJson(MvcResult result) throws Exception {
		return objectMapper.readTree(result.getResponse().getContentAsString());
	}

	private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder json(
			org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder builder, Map<String, ?> body) throws Exception {
		return builder.contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(body));
	}
}
