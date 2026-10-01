package com.expensemanager.common.context;

/**
 * Per-request identity and tracing, held in a {@link ThreadLocal} and populated by the security
 * filter chain from a verified token. Services read the current user from here rather than from a
 * Spring Security principal.
 *
 * <p>The filter that sets this is also responsible for calling {@link #clear()} in a finally block;
 * leaking a populated context onto a pooled thread would hand one user's identity to the next
 * request on that thread.
 */
public final class RequestContext {

	private static final ThreadLocal<RequestContext> CONTEXT = ThreadLocal.withInitial(RequestContext::new);

	private String requestId;
	private String userId;
	private String sessionId;
	private String tokenTier;
	private String apiEndpoint;
	private String apiMethod;
	private String ipAddress;
	private String userAgent;

	private RequestContext() {
	}

	public static RequestContext current() {
		return CONTEXT.get();
	}

	public static void clear() {
		CONTEXT.remove();
	}

	/** The authenticated user id, or null when the request is anonymous. */
	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public boolean isAuthenticated() {
		return userId != null;
	}

	public String getRequestId() {
		return requestId;
	}

	public void setRequestId(String requestId) {
		this.requestId = requestId;
	}

	public String getSessionId() {
		return sessionId;
	}

	public void setSessionId(String sessionId) {
		this.sessionId = sessionId;
	}

	public String getTokenTier() {
		return tokenTier;
	}

	public void setTokenTier(String tokenTier) {
		this.tokenTier = tokenTier;
	}

	public String getApiEndpoint() {
		return apiEndpoint;
	}

	public void setApiEndpoint(String apiEndpoint) {
		this.apiEndpoint = apiEndpoint;
	}

	public String getApiMethod() {
		return apiMethod;
	}

	public void setApiMethod(String apiMethod) {
		this.apiMethod = apiMethod;
	}

	public String getIpAddress() {
		return ipAddress;
	}

	public void setIpAddress(String ipAddress) {
		this.ipAddress = ipAddress;
	}

	public String getUserAgent() {
		return userAgent;
	}

	public void setUserAgent(String userAgent) {
		this.userAgent = userAgent;
	}
}
