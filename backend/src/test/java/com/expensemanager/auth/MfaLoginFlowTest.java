package com.expensemanager.auth;

import com.expensemanager.service.helper.TotpGenerator;
import com.expensemanager.support.MutableClock;
import com.expensemanager.support.PostgresIntegrationTest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Signing in with a second factor, and what the half-finished token is allowed to do.
 *
 * <p>Each test enrols a fresh account, because enabling a factor is a one-way change to the one it
 * uses.
 */
@AutoConfigureMockMvc
@Import(MfaLoginFlowTest.FixedClockConfig.class)
class MfaLoginFlowTest extends PostgresIntegrationTest {

	private static final String PASSWORD = "correct-horse-battery";
	private static final AtomicInteger ACCOUNT_SEQUENCE = new AtomicInteger();

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private TotpGenerator totpGenerator;

	@Autowired
	private MutableClock clock;

	private String email;
	private String accessToken;
	private String totpSecret;

	@BeforeEach
	void enrolAFreshAccount() throws Exception {
		email = "mfa" + ACCOUNT_SEQUENCE.incrementAndGet() + "@example.com";

		mockMvc.perform(json(post("/signup"), Map.of(
						"first_name", "Mfa", "email", email,
						"username", "mfauser" + ACCOUNT_SEQUENCE.get(), "password", PASSWORD)))
				.andExpect(status().isCreated());

		accessToken = login().get("access_token").asText();

		JsonNode enrollment = asJson(mockMvc.perform(post("/mfa/totp/enroll").header(HttpHeaders.AUTHORIZATION, bearer()))
				.andExpect(status().isOk())
				.andReturn());
		totpSecret = enrollment.get("secret").asText();
		assertThat(enrollment.get("provisioning_uri").asText()).startsWith("otpauth://totp/");
	}

	@Test
	void enrollingIsNotCompleteUntilACodeProvesTheAppHasTheSecret() throws Exception {
		// Still only one factor, and it is off, so sign-in must not demand it yet.
		assertThat(login().get("session_state").asText()).isEqualTo("ACTIVE");

		mockMvc.perform(json(post("/mfa/totp/verify"), Map.of("code", "000000")).header(HttpHeaders.AUTHORIZATION, bearer()))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.errors[0].type").value("incorrect_otp"));
	}

	@Test
	void confirmingEnrollmentReturnsRecoveryCodesAndTurnsOnTheFactor() throws Exception {
		JsonNode codes = confirmEnrollment();

		assertThat(codes.get("recovery_codes")).hasSize(10);
		codes.get("recovery_codes").forEach(code -> assertThat(code.asText()).matches("[a-z0-9]{5}-[a-z0-9]{5}"));

		assertThat(login().get("session_state").asText()).isEqualTo("MFA_IN_PROGRESS");
	}

	@Test
	void signInStopsAtTheSecondFactorAndOffersTheEnrolledOnes() throws Exception {
		confirmEnrollment();

		JsonNode pending = login();

		assertThat(pending.get("session_state").asText()).isEqualTo("MFA_IN_PROGRESS");
		assertThat(pending.get("mfa_enabled").asBoolean()).isTrue();
		assertThat(pending.get("access_token").asText()).isNotBlank();
		// No refresh token, so an unfinished sign-in cannot extend itself.
		assertThat(pending.has("refresh_token")).isFalse();
		assertThat(pending.get("mfa_factors").toString()).contains("TOTP").contains("RECOVERY_CODE");
	}

	@Test
	void theStepUpTokenCannotReachOrdinaryEndpoints() throws Exception {
		confirmEnrollment();
		String stepUpToken = login().get("access_token").asText();

		mockMvc.perform(get("/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + stepUpToken))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.errors[0].type").value("insufficient_token_tier"));
	}

	@Test
	void aCorrectCodeCompletesTheSignIn() throws Exception {
		confirmEnrollment();
		JsonNode pending = login();

		JsonNode completed = asJson(mockMvc.perform(json(post("/login/mfa/totp/verify"), Map.of(
						"session", pending.get("session").asText(), "code", currentCode()))
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + pending.get("access_token").asText()))
				.andExpect(status().isOk())
				.andReturn());

		assertThat(completed.get("session_state").asText()).isEqualTo("ACTIVE");
		assertThat(completed.get("refresh_token").asText()).isNotBlank();
		// A new session, so the identifier handed out before the factor was proven is not the one
		// the signed-in user ends up holding.
		assertThat(completed.get("session").asText()).isNotEqualTo(pending.get("session").asText());

		mockMvc.perform(get("/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + completed.get("access_token").asText()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.email").value(email));
	}

	@Test
	void theSameCodeCannotBeUsedTwice() throws Exception {
		confirmEnrollment();
		String code = currentCode();

		JsonNode first = login();
		mockMvc.perform(json(post("/login/mfa/totp/verify"), Map.of("session", first.get("session").asText(), "code", code))
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + first.get("access_token").asText()))
				.andExpect(status().isOk());

		// Codes stay valid for a whole time step, so replaying one within that window must fail.
		JsonNode second = login();
		mockMvc.perform(json(post("/login/mfa/totp/verify"), Map.of("session", second.get("session").asText(), "code", code))
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + second.get("access_token").asText()))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.errors[0].type").value("incorrect_otp"));
	}

	@Test
	void aSessionCannotBeVerifiedTwice() throws Exception {
		confirmEnrollment();
		JsonNode pending = login();
		String stepUpToken = "Bearer " + pending.get("access_token").asText();

		mockMvc.perform(json(post("/login/mfa/totp/verify"), Map.of("session", pending.get("session").asText(), "code", currentCode()))
						.header(HttpHeaders.AUTHORIZATION, stepUpToken))
				.andExpect(status().isOk());

		// The session is no longer pending, so a captured step-up token buys nothing.
		mockMvc.perform(json(post("/login/mfa/totp/verify"), Map.of("session", pending.get("session").asText(), "code", currentCode()))
						.header(HttpHeaders.AUTHORIZATION, stepUpToken))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors[0].type").value("invalid_session_state"));
	}

	@Test
	void aRecoveryCodeAlsoCompletesTheSignInAndIsThenSpent() throws Exception {
		String recoveryCode = confirmEnrollment().get("recovery_codes").get(0).asText();

		JsonNode pending = login();
		mockMvc.perform(json(post("/login/mfa/recovery-code/verify"), Map.of(
						"session", pending.get("session").asText(), "code", recoveryCode))
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + pending.get("access_token").asText()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.session_state").value("ACTIVE"));

		JsonNode second = login();
		mockMvc.perform(json(post("/login/mfa/recovery-code/verify"), Map.of(
						"session", second.get("session").asText(), "code", recoveryCode))
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + second.get("access_token").asText()))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void disablingTheFactorNeedsACurrentCodeAndRestoresOneStepSignIn() throws Exception {
		confirmEnrollment();

		mockMvc.perform(json(post("/mfa/totp/disable"), Map.of("code", "000000")).header(HttpHeaders.AUTHORIZATION, bearer()))
				.andExpect(status().isUnauthorized());

		String fullToken = completeSignIn();
		nextTimeStep();
		mockMvc.perform(json(post("/mfa/totp/disable"), Map.of("code", currentCode()))
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + fullToken))
				.andExpect(status().isOk());

		assertThat(login().get("session_state").asText()).isEqualTo("ACTIVE");
	}

	private JsonNode confirmEnrollment() throws Exception {
		JsonNode codes = asJson(mockMvc.perform(json(post("/mfa/totp/verify"), Map.of("code", currentCode()))
						.header(HttpHeaders.AUTHORIZATION, bearer()))
				.andExpect(status().isOk())
				.andReturn());
		// Confirming spends the current step. Move on so later codes are for a step not yet used.
		nextTimeStep();
		return codes;
	}

	private void nextTimeStep() {
		clock.advance(Duration.ofSeconds(TotpGenerator.TIME_STEP_SECONDS));
	}

	private String completeSignIn() throws Exception {
		JsonNode pending = login();
		if ("ACTIVE".equals(pending.get("session_state").asText())) {
			return pending.get("access_token").asText();
		}
		JsonNode completed = asJson(mockMvc.perform(json(post("/login/mfa/totp/verify"), Map.of(
						"session", pending.get("session").asText(), "code", currentCode()))
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + pending.get("access_token").asText()))
				.andExpect(status().isOk())
				.andReturn());
		return completed.get("access_token").asText();
	}

	private String currentCode() {
		return totpGenerator.generate(totpSecret, totpGenerator.currentTimeStep());
	}

	private JsonNode login() throws Exception {
		return asJson(mockMvc.perform(json(post("/login"), Map.of("identifier", email, "password", PASSWORD)))
				.andExpect(status().isOk())
				.andReturn());
	}

	private String bearer() {
		return "Bearer " + accessToken;
	}

	private JsonNode asJson(MvcResult result) throws Exception {
		return objectMapper.readTree(result.getResponse().getContentAsString());
	}

	private MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder builder, Map<String, ?> body) throws Exception {
		return builder.contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(body));
	}

	@TestConfiguration
	static class FixedClockConfig {

		/**
		 * One bean, deliberately not named "clock": that name is already taken by the application
		 * config, and a second definition under it is rejected rather than overriding. Being a
		 * Clock itself, and primary, it satisfies both this test and the generator under test.
		 */
		@Bean
		@Primary
		MutableClock testClock() {
			return new MutableClock();
		}
	}
}
