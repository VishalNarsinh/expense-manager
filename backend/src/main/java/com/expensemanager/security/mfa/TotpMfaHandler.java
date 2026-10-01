package com.expensemanager.security.mfa;

import com.expensemanager.common.error.ErrorType;
import com.expensemanager.common.exception.UnauthorizedException;
import com.expensemanager.config.MfaProperties;
import com.expensemanager.domain.enums.MfaType;
import com.expensemanager.domain.jpa.QUserMfaConfiguration;
import com.expensemanager.domain.jpa.User;
import com.expensemanager.domain.jpa.UserMfaConfiguration;
import com.expensemanager.repository.jpa.UserMfaConfigurationRepository;
import com.expensemanager.service.helper.SecretCipher;
import com.expensemanager.service.helper.TotpGenerator;
import com.querydsl.core.types.Path;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.OptionalLong;

/** Verifies a code from an authenticator app. */
@Component
public class TotpMfaHandler implements MfaFactorHandler {

	private static final QUserMfaConfiguration CONFIG = QUserMfaConfiguration.userMfaConfiguration;

	private final UserMfaConfigurationRepository configurationRepository;
	private final TotpGenerator totpGenerator;
	private final SecretCipher secretCipher;
	private final MfaProperties properties;

	public TotpMfaHandler(UserMfaConfigurationRepository configurationRepository, TotpGenerator totpGenerator,
			SecretCipher secretCipher, MfaProperties properties) {
		this.configurationRepository = configurationRepository;
		this.totpGenerator = totpGenerator;
		this.secretCipher = secretCipher;
		this.properties = properties;
	}

	@Override
	public MfaType type() {
		return MfaType.TOTP;
	}

	@Override
	@Transactional
	public void verify(User user, String proof) {
		UserMfaConfiguration configuration = configurationRepository
				.findOne(CONFIG.user.id.eq(user.getId()).and(CONFIG.mfaType.eq(MfaType.TOTP)).and(CONFIG.enabled.isTrue()), CONFIG)
				.orElseThrow(() -> new UnauthorizedException("No authenticator app is set up for this account", ErrorType.MFA_REQUIRED));

		String secret = secretCipher.decrypt(configuration.getTotpSecretEncrypted());
		OptionalLong matched = totpGenerator.matchingTimeStep(secret, proof, properties.getTimeStepTolerance());

		if (matched.isEmpty()) {
			throw new UnauthorizedException("That code is not correct", ErrorType.INCORRECT_OTP, "code");
		}

		Long lastUsed = configuration.getLastUsedTimeStep();
		if (lastUsed != null && matched.getAsLong() <= lastUsed) {
			// A code stays valid for its whole step, so without this a code seen in transit could
			// be replayed until the step rolled over.
			throw new UnauthorizedException("That code has already been used", ErrorType.INCORRECT_OTP, "code");
		}

		recordUsedStep(configuration.getId(), matched.getAsLong());
	}

	private void recordUsedStep(String configurationId, long timeStep) {
		Map<Path<?>, Object> values = new LinkedHashMap<>();
		values.put(CONFIG.lastUsedTimeStep, timeStep);
		configurationRepository.updateFields(CONFIG.id.eq(configurationId), CONFIG, values);
	}
}
