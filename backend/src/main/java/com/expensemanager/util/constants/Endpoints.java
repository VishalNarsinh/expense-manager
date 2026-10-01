package com.expensemanager.util.constants;

import java.util.List;

/**
 * Request paths grouped by the access they require. Kept in one place so the security rules can be
 * read in full without tracing annotations across controllers.
 */
public final class Endpoints {

	private Endpoints() {
	}

	public static final String SIGNUP = "/signup";
	public static final String LOGIN = "/login";
	public static final String SSO_LOGIN = "/sso-login";
	public static final String SSO_SIGNUP = "/sso-signup";
	public static final String GENERATE_TOKEN = "/generate-token";
	public static final String LOGOUT = "/logout";
	public static final String SESSIONS = "/sessions";
	public static final String ME = "/me";

	public static final String PASSKEY_LOGIN_OPTIONS = "/passkey/login/options";
	public static final String PASSKEY_LOGIN_VERIFY = "/passkey/login/verify";

	/** Reachable without any token. */
	public static final List<String> PUBLIC = List.of(
			SIGNUP,
			LOGIN,
			SSO_LOGIN,
			SSO_SIGNUP,
			GENERATE_TOKEN,
			PASSKEY_LOGIN_OPTIONS,
			PASSKEY_LOGIN_VERIFY,
			"/actuator/health",
			"/swagger-ui.html",
			"/swagger-ui/**",
			"/v3/api-docs/**");

	/**
	 * The only paths a step-up token may reach: finishing or abandoning a login that is waiting on
	 * a second factor. Everything else requires a full token.
	 */
	public static final List<String> STEP_UP_ALLOWED = List.of(
			"/login/mfa/**",
			LOGOUT);
}
