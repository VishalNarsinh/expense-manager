package com.expensemanager.security.filter;

import com.expensemanager.common.context.RequestContext;
import com.expensemanager.common.util.IdUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Establishes the per-request context and tears it down again.
 *
 * <p>Runs first so that anything failing later still logs with a request id. Named to avoid
 * colliding with Spring's own {@code requestContextFilter} bean.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestContextPopulatingFilter extends OncePerRequestFilter {

	private static final String HEADER_REQUEST_ID = "X-Request-Id";
	private static final String MDC_REQUEST_ID = "request_id";
	private static final String MDC_USER_ID = "user_id";

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws ServletException, IOException {

		String requestId = request.getHeader(HEADER_REQUEST_ID);
		if (requestId == null || requestId.isBlank()) {
			requestId = IdUtil.uuid();
		}

		RequestContext context = RequestContext.current();
		context.setRequestId(requestId);
		context.setApiEndpoint(request.getRequestURI());
		context.setApiMethod(request.getMethod());
		context.setIpAddress(clientIp(request));
		context.setUserAgent(request.getHeader("User-Agent"));

		MDC.put(MDC_REQUEST_ID, requestId);
		response.setHeader(HEADER_REQUEST_ID, requestId);

		try {
			chain.doFilter(request, response);
		} finally {
			// Threads are pooled, so a context left behind would be inherited by whichever request
			// is served next on this thread.
			RequestContext.clear();
			MDC.remove(MDC_REQUEST_ID);
			MDC.remove(MDC_USER_ID);
		}
	}

	private String clientIp(HttpServletRequest request) {
		String forwarded = request.getHeader("X-Forwarded-For");
		if (forwarded != null && !forwarded.isBlank()) {
			// Left-most entry is the original client; the rest are proxies that appended themselves.
			return forwarded.split(",")[0].trim();
		}
		return request.getRemoteAddr();
	}
}
