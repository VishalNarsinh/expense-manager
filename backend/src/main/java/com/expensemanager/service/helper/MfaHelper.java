package com.expensemanager.service.helper;

import com.expensemanager.common.util.IdUtil;
import com.expensemanager.config.MfaProperties;
import com.expensemanager.domain.enums.MfaType;
import com.expensemanager.domain.jpa.MfaRecoveryCode;
import com.expensemanager.domain.jpa.QMfaRecoveryCode;
import com.expensemanager.domain.jpa.QUserMfaConfiguration;
import com.expensemanager.domain.jpa.User;
import com.expensemanager.domain.jpa.UserMfaConfiguration;
import com.expensemanager.repository.jpa.MfaRecoveryCodeRepository;
import com.expensemanager.repository.jpa.UserMfaConfigurationRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Enrolment state and recovery codes. */
@Component
public class MfaHelper {

	private static final QUserMfaConfiguration CONFIG = QUserMfaConfiguration.userMfaConfiguration;
	private static final QMfaRecoveryCode CODE = QMfaRecoveryCode.mfaRecoveryCode;

	/** Avoids characters that are easily confused when read off a screen and typed back. */
	private static final String CODE_ALPHABET = "abcdefghjkmnpqrstuvwxyz23456789";
	private static final int CODE_LENGTH = 10;

	private final UserMfaConfigurationRepository configurationRepository;
	private final MfaRecoveryCodeRepository recoveryCodeRepository;
	private final PasswordEncoder passwordEncoder;
	private final MfaProperties properties;
	private final SecureRandom secureRandom = new SecureRandom();

	public MfaHelper(UserMfaConfigurationRepository configurationRepository, MfaRecoveryCodeRepository recoveryCodeRepository,
			PasswordEncoder passwordEncoder, MfaProperties properties) {
		this.configurationRepository = configurationRepository;
		this.recoveryCodeRepository = recoveryCodeRepository;
		this.passwordEncoder = passwordEncoder;
		this.properties = properties;
	}

	public List<UserMfaConfiguration> configurations(String userId) {
		return configurationRepository.findAll(CONFIG.user.id.eq(userId), CONFIG);
	}

	public Optional<UserMfaConfiguration> configuration(String userId, MfaType type) {
		return configurationRepository.findOne(CONFIG.user.id.eq(userId).and(CONFIG.mfaType.eq(type)), CONFIG);
	}

	/** What the sign-in screen should offer. Recovery codes are always available as a fallback. */
	public List<MfaType> enabledFactors(String userId) {
		List<MfaType> factors = new ArrayList<>(configurationRepository
				.findAll(CONFIG.user.id.eq(userId).and(CONFIG.enabled.isTrue()), CONFIG)
				.stream()
				.map(UserMfaConfiguration::getMfaType)
				.toList());

		if (!factors.isEmpty() && unusedRecoveryCodeCount(userId) > 0) {
			factors.add(MfaType.RECOVERY_CODE);
		}
		return factors;
	}

	public boolean hasAnyEnabledFactor(String userId) {
		return configurationRepository.exists(CONFIG.user.id.eq(userId).and(CONFIG.enabled.isTrue()), CONFIG);
	}

	public long unusedRecoveryCodeCount(String userId) {
		return recoveryCodeRepository.count(CODE.user.id.eq(userId).and(CODE.usedAt.isNull()), CODE);
	}

	/**
	 * Replaces any existing codes and returns the new ones in plaintext.
	 *
	 * <p>This is the only moment they can be read; only hashes are stored, so a lost set can be
	 * regenerated but never recovered.
	 */
	@Transactional
	public List<String> regenerateRecoveryCodes(User user) {
		recoveryCodeRepository.deleteWhere(CODE.user.id.eq(user.getId()), CODE);

		List<String> plaintext = new ArrayList<>();
		for (int i = 0; i < properties.getRecoveryCodeCount(); i++) {
			String code = randomCode();
			plaintext.add(format(code));

			MfaRecoveryCode entity = new MfaRecoveryCode();
			entity.setUser(user);
			entity.setCodeHash(passwordEncoder.encode(code));
			recoveryCodeRepository.save(entity);
		}
		return plaintext;
	}

	@Transactional
	public UserMfaConfiguration saveConfiguration(UserMfaConfiguration configuration) {
		return configurationRepository.save(configuration);
	}

	@Transactional
	public void removeConfiguration(String userId, MfaType type) {
		configurationRepository.deleteWhere(CONFIG.user.id.eq(userId).and(CONFIG.mfaType.eq(type)), CONFIG);
	}

	@Transactional
	public void removeAllRecoveryCodes(String userId) {
		recoveryCodeRepository.deleteWhere(CODE.user.id.eq(userId), CODE);
	}

	private String randomCode() {
		StringBuilder builder = new StringBuilder(CODE_LENGTH);
		for (int i = 0; i < CODE_LENGTH; i++) {
			builder.append(CODE_ALPHABET.charAt(secureRandom.nextInt(CODE_ALPHABET.length())));
		}
		return builder.toString();
	}

	/** Grouped for legibility; verification strips the separator before comparing. */
	private String format(String code) {
		return code.substring(0, 5) + "-" + code.substring(5);
	}

	public String unusedIdPlaceholder() {
		return IdUtil.uuid();
	}
}
