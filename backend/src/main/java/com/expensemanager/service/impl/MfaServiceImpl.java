package com.expensemanager.service.impl;

import com.expensemanager.common.context.RequestContext;
import com.expensemanager.common.error.ErrorType;
import com.expensemanager.common.exception.BadRequestException;
import com.expensemanager.common.exception.EntityNotFoundException;
import com.expensemanager.common.exception.UnauthorizedException;
import com.expensemanager.common.util.DateTimeUtil;
import com.expensemanager.config.MfaProperties;
import com.expensemanager.domain.enums.MfaType;
import com.expensemanager.domain.enums.SessionState;
import com.expensemanager.domain.enums.TokenTier;
import com.expensemanager.domain.jpa.User;
import com.expensemanager.domain.jpa.UserMfaConfiguration;
import com.expensemanager.domain.jpa.UserSession;
import com.expensemanager.dto.response.MfaFactorResponse;
import com.expensemanager.dto.response.RecoveryCodesResponse;
import com.expensemanager.dto.response.TokenResponse;
import com.expensemanager.dto.response.TotpEnrollmentResponse;
import com.expensemanager.security.mfa.MfaFactorRegistry;
import com.expensemanager.security.token.IssuedToken;
import com.expensemanager.security.token.TokenIssuer;
import com.expensemanager.service.MfaService;
import com.expensemanager.service.helper.MfaHelper;
import com.expensemanager.service.helper.SecretCipher;
import com.expensemanager.service.helper.SessionHelper;
import com.expensemanager.service.helper.TotpGenerator;
import com.expensemanager.service.helper.UserHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class MfaServiceImpl implements MfaService {

	private final MfaHelper mfaHelper;
	private final MfaFactorRegistry factorRegistry;
	private final UserHelper userHelper;
	private final SessionHelper sessionHelper;
	private final TotpGenerator totpGenerator;
	private final SecretCipher secretCipher;
	private final TokenIssuer tokenIssuer;
	private final MfaProperties properties;

	public MfaServiceImpl(MfaHelper mfaHelper, MfaFactorRegistry factorRegistry, UserHelper userHelper, SessionHelper sessionHelper,
			TotpGenerator totpGenerator, SecretCipher secretCipher, TokenIssuer tokenIssuer, MfaProperties properties) {
		this.mfaHelper = mfaHelper;
		this.factorRegistry = factorRegistry;
		this.userHelper = userHelper;
		this.sessionHelper = sessionHelper;
		this.totpGenerator = totpGenerator;
		this.secretCipher = secretCipher;
		this.tokenIssuer = tokenIssuer;
		this.properties = properties;
	}

	@Override
	@Transactional
	public TotpEnrollmentResponse startTotpEnrollment() {
		User user = currentUser();
		String secret = totpGenerator.generateSecret();

		// Stored disabled. Only a correct code proves the app actually holds the secret, so until
		// then enabling it could lock the account out of its own second factor.
		UserMfaConfiguration configuration = mfaHelper.configuration(user.getId(), MfaType.TOTP)
				.orElseGet(UserMfaConfiguration::new);
		configuration.setUser(user);
		configuration.setMfaType(MfaType.TOTP);
		configuration.setTotpSecretEncrypted(secretCipher.encrypt(secret));
		configuration.setEnabled(false);
		configuration.setVerifiedAt(null);
		configuration.setLastUsedTimeStep(null);
		mfaHelper.saveConfiguration(configuration);

		return new TotpEnrollmentResponse(secret, totpGenerator.provisioningUri(properties.getIssuer(), user.getEmail(), secret));
	}

	@Override
	@Transactional
	public RecoveryCodesResponse confirmTotpEnrollment(String code) {
		User user = currentUser();
		UserMfaConfiguration configuration = mfaHelper.configuration(user.getId(), MfaType.TOTP)
				.orElseThrow(() -> new BadRequestException("Start setting up your authenticator app first", ErrorType.MFA_REQUIRED));

		String secret = secretCipher.decrypt(configuration.getTotpSecretEncrypted());
		var matched = totpGenerator.matchingTimeStep(secret, code, properties.getTimeStepTolerance());
		if (matched.isEmpty()) {
			throw new UnauthorizedException("That code is not correct", ErrorType.INCORRECT_OTP, "code");
		}

		configuration.setEnabled(true);
		configuration.setPreferred(true);
		configuration.setVerifiedAt(DateTimeUtil.currentEpochMillisUtc());
		configuration.setLastUsedTimeStep(matched.getAsLong());
		mfaHelper.saveConfiguration(configuration);

		userHelper.setMfaEnabled(user.getId(), true);

		// Issued at the moment the factor is switched on, so there is never a window where the
		// account depends on a single device with no way back in.
		return new RecoveryCodesResponse(mfaHelper.regenerateRecoveryCodes(user));
	}

	@Override
	@Transactional
	public void disableTotp(String code) {
		User user = currentUser();
		// Turning a factor off is as sensitive as using it, so it takes a current code.
		factorRegistry.verify(user, MfaType.TOTP, code);

		mfaHelper.removeConfiguration(user.getId(), MfaType.TOTP);

		if (!mfaHelper.hasAnyEnabledFactor(user.getId())) {
			userHelper.setMfaEnabled(user.getId(), false);
			mfaHelper.removeAllRecoveryCodes(user.getId());
		}
	}

	@Override
	@Transactional(readOnly = true)
	public List<MfaFactorResponse> factors() {
		return mfaHelper.configurations(currentUserId()).stream().map(MfaFactorResponse::from).toList();
	}

	@Override
	@Transactional
	public RecoveryCodesResponse regenerateRecoveryCodes() {
		return new RecoveryCodesResponse(mfaHelper.regenerateRecoveryCodes(currentUser()));
	}

	@Override
	@Transactional(readOnly = true)
	public User pendingUser(String sessionId) {
		return sessionHelper.requireState(sessionId, SessionState.MFA_IN_PROGRESS, "session").getUser();
	}

	@Override
	@Transactional
	public TokenResponse completeLogin(String sessionId, MfaType type, String proof) {
		UserSession pending = sessionHelper.requireState(sessionId, SessionState.MFA_IN_PROGRESS, "session");
		User user = pending.getUser();

		factorRegistry.verify(user, type, proof);

		// The pending session is retired rather than promoted, and the caller receives a session
		// identifier that only existed after the second factor was proven. The step-up token still
		// names the old session, so replaying it reaches a session that is no longer usable.
		sessionHelper.expire(pending.getId());
		UserSession active = sessionHelper.create(user, SessionState.ACTIVE, pending.getSessionSource(),
				pending.getIpAddress(), pending.getUserAgent());

		IssuedToken accessToken = tokenIssuer.issueAccessToken(user.getId(), active.getId(), TokenTier.FULL);
		IssuedToken refreshToken = tokenIssuer.issueRefreshToken();
		sessionHelper.attachRefreshToken(active.getId(), refreshToken.value(), refreshToken.expiresAt().toEpochMilli());

		userHelper.recordSuccessfulLogin(user.getId());

		return TokenResponse.active(accessToken.value(), refreshToken.value(), accessToken.expiresAt().toEpochMilli(),
				active.getId(), pending.getSessionSource());
	}

	private User currentUser() {
		return userHelper.findById(currentUserId())
				.orElseThrow(() -> new EntityNotFoundException("Account not found"));
	}

	private String currentUserId() {
		String userId = RequestContext.current().getUserId();
		if (userId == null) {
			throw new UnauthorizedException("Authentication is required", ErrorType.AUTHENTICATION_FAILED);
		}
		return userId;
	}
}
