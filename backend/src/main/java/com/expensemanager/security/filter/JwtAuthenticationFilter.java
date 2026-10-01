package com.expensemanager.security.filter;

import com.expensemanager.common.context.RequestContext;
import com.expensemanager.common.error.ErrorType;
import com.expensemanager.common.exception.UnauthorizedException;
import com.expensemanager.domain.enums.TokenTier;
import com.expensemanager.security.ApiErrorWriter;
import com.expensemanager.security.token.TokenIssuer;
import com.expensemanager.security.token.TokenPrincipal;
import com.expensemanager.util.constants.Endpoints;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Verifies the bearer token, publishes the caller's identity, and enforces the token tier.
 *
 * <p>The tier check is the point of this filter. A token issued while a second factor is still
 * outstanding is a valid, unexpired, correctly signed token; without a central check it would open
 * every endpoint in the application, and a short expiry would be the only thing limiting the
 * damage.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

	private static final String BEARER_PREFIX = "Bearer ";
	private static final String MDC_USER_ID = "user_id";

	private final TokenIssuer tokenIssuer;
	private final ApiErrorWriter errorWriter;
	private final AntPathMatcher pathMatcher = new AntPathMatcher();

	public JwtAuthenticationFilter(TokenIssuer tokenIssuer, ApiErrorWriter errorWriter) {
		this.tokenIssuer = tokenIssuer;
		this.errorWriter = errorWriter;
	}

	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) {
		return matchesAny(request.getRequestURI(), Endpoints.PUBLIC);
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws ServletException, IOException {

		String token = bearerToken(request);
		if (token == null) {
			// No credential at all. Spring Security turns this into a 401 further down the chain.
			chain.doFilter(request, response);
			return;
		}

		TokenPrincipal principal;
		try {
			principal = tokenIssuer.verify(token);
		} catch (UnauthorizedException e) {
			errorWriter.write(response, HttpStatus.UNAUTHORIZED.value(), e.getErrorType(), e.getMessage());
			return;
		}

		if (principal.tier() != TokenTier.FULL && !matchesAny(request.getRequestURI(), Endpoints.STEP_UP_ALLOWED)) {
			// 403 rather than 401: the credential is genuine, it is simply not sufficient here, and
			// the client should finish the second factor rather than log in again.
			errorWriter.write(response, HttpStatus.FORBIDDEN.value(), ErrorType.INSUFFICIENT_TOKEN_TIER,
					"Complete multi-factor authentication before using this endpoint");
			return;
		}

		publish(principal);

		try {
			chain.doFilter(request, response);
		} finally {
			SecurityContextHolder.clearContext();
		}
	}

	private void publish(TokenPrincipal principal) {
		RequestContext context = RequestContext.current();
		context.setUserId(principal.userId());
		context.setSessionId(principal.sessionId());
		context.setTokenTier(principal.tier().name());
		MDC.put(MDC_USER_ID, principal.userId());

		UsernamePasswordAuthenticationToken authentication =
				new UsernamePasswordAuthenticationToken(principal.userId(), null, List.of());
		authentication.setDetails(principal);
		SecurityContextHolder.getContext().setAuthentication(authentication);
	}

	private String bearerToken(HttpServletRequest request) {
		String header = request.getHeader(HttpHeaders.AUTHORIZATION);
		if (header == null || !header.startsWith(BEARER_PREFIX)) {
			return null;
		}
		String token = header.substring(BEARER_PREFIX.length()).trim();
		return token.isEmpty() ? null : token;
	}

	private boolean matchesAny(String path, List<String> patterns) {
		return patterns.stream().anyMatch(pattern -> pathMatcher.match(pattern, path));
	}
}
