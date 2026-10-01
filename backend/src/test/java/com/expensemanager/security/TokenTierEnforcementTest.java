package com.expensemanager.security;

import com.expensemanager.common.context.RequestContext;
import com.expensemanager.domain.enums.TokenTier;
import com.expensemanager.security.token.IssuedToken;
import com.expensemanager.security.token.TokenIssuer;
import com.expensemanager.support.PostgresIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.RouterFunctions;
import org.springframework.web.servlet.function.ServerResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The security boundary for partially authenticated callers.
 *
 * <p>A step-up token is genuine, unexpired and correctly signed, so nothing about the token itself
 * stops it reaching a business endpoint. Only this check does.
 */
@AutoConfigureMockMvc
@Import(TokenTierEnforcementTest.TestEndpoints.class)
class TokenTierEnforcementTest extends PostgresIntegrationTest {

	private static final String USER_ID = "usr_test";
	private static final String SESSION_ID = "ses_test";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private TokenIssuer tokenIssuer;

	@Test
	void rejectsARequestWithNoToken() throws Exception {
		mockMvc.perform(get("/test-protected"))
				.andExpect(status().isUnauthorized())
				.andExpect(content().contentTypeCompatibleWith("application/json"))
				.andExpect(jsonPath("$.errors[0].type").value("authentication_failed"));
	}

	@Test
	void rejectsAMalformedToken() throws Exception {
		mockMvc.perform(get("/test-protected").header(HttpHeaders.AUTHORIZATION, "Bearer not-a-real-token"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.errors[0].type").value("invalid_token"));
	}

	@Test
	void allowsAFullTokenEverywhere() throws Exception {
		mockMvc.perform(get("/test-protected").header(HttpHeaders.AUTHORIZATION, bearer(TokenTier.FULL)))
				.andExpect(status().isOk())
				.andExpect(content().string(USER_ID));
	}

	@Test
	void refusesAStepUpTokenOutsideTheAllowlist() throws Exception {
		mockMvc.perform(get("/test-protected").header(HttpHeaders.AUTHORIZATION, bearer(TokenTier.STEP_UP)))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.errors[0].type").value("insufficient_token_tier"));
	}

	@Test
	void allowsAStepUpTokenToFinishTheLogin() throws Exception {
		mockMvc.perform(get("/login/mfa/test-echo").header(HttpHeaders.AUTHORIZATION, bearer(TokenTier.STEP_UP)))
				.andExpect(status().isOk())
				.andExpect(content().string(USER_ID));
	}

	@Test
	void allowsAFullTokenToFinishTheLoginToo() throws Exception {
		mockMvc.perform(get("/login/mfa/test-echo").header(HttpHeaders.AUTHORIZATION, bearer(TokenTier.FULL)))
				.andExpect(status().isOk());
	}

	@Test
	void leavesPublicEndpointsOpen() throws Exception {
		mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
	}

	@Test
	void verifiedTokenCarriesItsClaimsBack() {
		IssuedToken issued = tokenIssuer.issueAccessToken(USER_ID, SESSION_ID, TokenTier.STEP_UP);

		var principal = tokenIssuer.verify(issued.value());

		assertThat(principal.userId()).isEqualTo(USER_ID);
		assertThat(principal.sessionId()).isEqualTo(SESSION_ID);
		assertThat(principal.tier()).isEqualTo(TokenTier.STEP_UP);
		assertThat(principal.isFullTier()).isFalse();
		assertThat(principal.tokenId()).isNotBlank();
	}

	@Test
	void stepUpTokensExpireSoonerThanFullOnes() {
		IssuedToken stepUp = tokenIssuer.issueAccessToken(USER_ID, SESSION_ID, TokenTier.STEP_UP);
		IssuedToken full = tokenIssuer.issueAccessToken(USER_ID, SESSION_ID, TokenTier.FULL);

		assertThat(stepUp.expiresAt()).isBefore(full.expiresAt());
	}

	private String bearer(TokenTier tier) {
		return "Bearer " + tokenIssuer.issueAccessToken(USER_ID, SESSION_ID, tier).value();
	}

	@TestConfiguration
	static class TestEndpoints {

		/**
		 * Stands in for real endpoints so the filter rules are exercised rather than the handlers.
		 *
		 * <p>Registered as functional routes rather than an annotated controller: a nested
		 * {@code @RestController} sits inside the scanned package and would be picked up by
		 * component scanning as well as by this configuration, mapping each path twice.
		 */
		@Bean
		RouterFunction<ServerResponse> tokenTierTestRoutes() {
			return RouterFunctions.route()
					.GET("/test-protected", request -> ServerResponse.ok().body(currentUserId()))
					.GET("/login/mfa/test-echo", request -> ServerResponse.ok().body(currentUserId()))
					.build();
		}

		private static String currentUserId() {
			return RequestContext.current().getUserId();
		}
	}
}
