package com.expensemanager.security.passkey;

import com.expensemanager.common.error.ErrorType;
import com.expensemanager.common.exception.BadRequestException;
import com.expensemanager.common.exception.InternalServerErrorException;
import com.expensemanager.common.exception.UnauthorizedException;
import com.expensemanager.common.util.DateTimeUtil;
import com.expensemanager.config.WebAuthnProperties;
import com.expensemanager.domain.enums.PasskeyChallengeType;
import com.expensemanager.domain.enums.PasskeyStatus;
import com.expensemanager.domain.jpa.PasskeyChallenge;
import com.expensemanager.domain.jpa.PasskeyCredential;
import com.expensemanager.domain.jpa.QPasskeyCredential;
import com.expensemanager.domain.jpa.User;
import com.expensemanager.repository.jpa.PasskeyCredentialRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.querydsl.core.types.Path;
import com.yubico.webauthn.AssertionRequest;
import com.yubico.webauthn.AssertionResult;
import com.yubico.webauthn.FinishAssertionOptions;
import com.yubico.webauthn.FinishRegistrationOptions;
import com.yubico.webauthn.RegistrationResult;
import com.yubico.webauthn.RelyingParty;
import com.yubico.webauthn.StartAssertionOptions;
import com.yubico.webauthn.StartRegistrationOptions;
import com.yubico.webauthn.data.AuthenticatorAssertionResponse;
import com.yubico.webauthn.data.AuthenticatorAttestationResponse;
import com.yubico.webauthn.data.AuthenticatorResponse;
import com.yubico.webauthn.data.AuthenticatorSelectionCriteria;
import com.yubico.webauthn.data.AuthenticatorTransport;
import com.yubico.webauthn.data.ByteArray;
import com.yubico.webauthn.data.ClientAssertionExtensionOutputs;
import com.yubico.webauthn.data.ClientExtensionOutputs;
import com.yubico.webauthn.data.ClientRegistrationExtensionOutputs;
import com.yubico.webauthn.data.PublicKeyCredential;
import com.yubico.webauthn.data.PublicKeyCredentialCreationOptions;
import com.yubico.webauthn.data.UserIdentity;
import com.yubico.webauthn.data.UserVerificationRequirement;
import com.yubico.webauthn.exception.AssertionFailedException;
import com.yubico.webauthn.exception.RegistrationFailedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/** Registration and authentication ceremonies, on top of the WebAuthn library. */
@Component
public class PasskeyHelper {

	private static final Logger log = LoggerFactory.getLogger(PasskeyHelper.class);
	private static final QPasskeyCredential CREDENTIAL = QPasskeyCredential.passkeyCredential;

	private final RelyingParty relyingParty;
	private final AuthenticatorSelectionCriteria authenticatorSelection;
	private final PasskeyCredentialRepository credentialRepository;
	private final PasskeyChallengeHelper challengeHelper;
	private final WebAuthnProperties properties;
	private final ObjectMapper objectMapper;

	public PasskeyHelper(RelyingParty relyingParty, AuthenticatorSelectionCriteria authenticatorSelection,
			PasskeyCredentialRepository credentialRepository, PasskeyChallengeHelper challengeHelper,
			WebAuthnProperties properties, ObjectMapper objectMapper) {
		this.relyingParty = relyingParty;
		this.authenticatorSelection = authenticatorSelection;
		this.credentialRepository = credentialRepository;
		this.challengeHelper = challengeHelper;
		this.properties = properties;
		this.objectMapper = objectMapper;
	}

	@Transactional
	public JsonNode startRegistration(User user, String sessionId, String label) {
		enforceCredentialLimit(user.getId());

		PublicKeyCredentialCreationOptions options = relyingParty.startRegistration(StartRegistrationOptions.builder()
				.user(UserIdentity.builder()
						.name(user.getEmail())
						.displayName(user.getDisplayName())
						.id(new ByteArray(user.getId().getBytes(StandardCharsets.UTF_8)))
						.build())
				.authenticatorSelection(authenticatorSelection)
				.timeout(properties.getChallengeTtlMillis())
				.build());

		challengeHelper.persist(user, sessionId, PasskeyChallengeType.REGISTRATION,
				options.getChallenge().getBase64Url(), serialize(options), label);

		return readTree(toJson(options));
	}

	@Transactional
	public PasskeyCredential finishRegistration(User user, String sessionId, JsonNode response) {
		enforceCredentialLimit(user.getId());

		PublicKeyCredential<AuthenticatorAttestationResponse, ClientRegistrationExtensionOutputs> parsed = parseRegistration(response);
		requireUserVerification(parsed, "Your device must verify it is you before registering a passkey");

		PasskeyChallenge challenge = challengeHelper.consume(
				parsed.getResponse().getClientData().getChallenge().getBase64Url(), user.getId(), sessionId);

		RegistrationResult result;
		try {
			result = relyingParty.finishRegistration(FinishRegistrationOptions.builder()
					.request(deserializeCreationOptions(challenge.getChallengeRequest()))
					.response(parsed)
					.build());
		} catch (RegistrationFailedException e) {
			log.warn("Passkey registration rejected", e);
			throw new BadRequestException("That passkey could not be registered", ErrorType.PASSKEY_CHALLENGE_FAILED);
		}

		PasskeyCredential credential = new PasskeyCredential();
		credential.setUser(user);
		credential.setCredentialId(result.getKeyId().getId().getBase64Url());
		credential.setPublicKeyCose(result.getPublicKeyCose().getBytes());
		credential.setSignCount(result.getSignatureCount());
		credential.setAaguid(result.getAaguid() == null ? null : result.getAaguid().getHex());
		credential.setTransports(result.getKeyId().getTransports()
				.filter(transports -> !transports.isEmpty())
				.map(transports -> transports.stream().map(AuthenticatorTransport::getId).collect(Collectors.joining(",")))
				.orElse(null));
		credential.setDisplayName(challenge.getDisplayName());
		credential.setLabel(challenge.getDisplayName());
		credential.setStatus(PasskeyStatus.ACTIVE);
		return credentialRepository.save(credential);
	}

	/**
	 * Starts an assertion. A null user means usernameless sign-in, where the authenticator reveals
	 * which account it holds.
	 */
	@Transactional
	public JsonNode startAuthentication(User user, String sessionId) {
		StartAssertionOptions.StartAssertionOptionsBuilder builder = StartAssertionOptions.builder()
				.userVerification(UserVerificationRequirement.REQUIRED)
				.timeout(properties.getChallengeTtlMillis());

		if (user != null) {
			builder.userHandle(new ByteArray(user.getId().getBytes(StandardCharsets.UTF_8)));
		}

		AssertionRequest request = relyingParty.startAssertion(builder.build());

		challengeHelper.persist(user, sessionId, PasskeyChallengeType.LOGIN,
				request.getPublicKeyCredentialRequestOptions().getChallenge().getBase64Url(), serialize(request), null);

		return readTree(toJson(request));
	}

	/** Verifies an assertion and returns the account id it proves. */
	@Transactional
	public String finishAuthentication(User expectedUser, String sessionId, JsonNode response) {
		PublicKeyCredential<AuthenticatorAssertionResponse, ClientAssertionExtensionOutputs> parsed = parseAssertion(response);
		requireUserVerification(parsed, "Your device must verify it is you before signing in");

		PasskeyChallenge challenge = challengeHelper.consume(
				parsed.getResponse().getClientData().getChallenge().getBase64Url(),
				expectedUser == null ? null : expectedUser.getId(), sessionId);

		AssertionResult result;
		try {
			result = relyingParty.finishAssertion(FinishAssertionOptions.builder()
					.request(deserializeAssertionRequest(challenge.getChallengeRequest()))
					.response(parsed)
					.build());
		} catch (AssertionFailedException e) {
			log.warn("Passkey assertion rejected", e);
			throw new UnauthorizedException("That passkey could not be verified", ErrorType.PASSKEY_ASSERTION_FAILED);
		}

		if (!result.isSuccess()) {
			throw new UnauthorizedException("That passkey could not be verified", ErrorType.PASSKEY_ASSERTION_FAILED);
		}

		String userId = new String(result.getCredential().getUserHandle().getBytes(), StandardCharsets.UTF_8);
		recordUse(result.getCredential().getCredentialId().getBase64Url(), result.getSignatureCount());
		return userId;
	}

	/**
	 * Persists the authenticator's signature counter. The library refuses a counter that fails to
	 * advance, which is how a cloned authenticator is detected, but only if the latest value is
	 * stored after every use.
	 */
	private void recordUse(String credentialId, long signatureCount) {
		Map<Path<?>, Object> values = new LinkedHashMap<>();
		values.put(CREDENTIAL.signCount, signatureCount);
		values.put(CREDENTIAL.lastUsedAt, DateTimeUtil.currentEpochMillisUtc());
		credentialRepository.updateFields(CREDENTIAL.credentialId.eq(credentialId), CREDENTIAL, values);
	}

	private void enforceCredentialLimit(String userId) {
		long active = credentialRepository.count(
				CREDENTIAL.user.id.eq(userId).and(CREDENTIAL.status.eq(PasskeyStatus.ACTIVE)), CREDENTIAL);
		if (active >= properties.getMaxCredentialsPerUser()) {
			throw new BadRequestException("You have reached the maximum number of passkeys for this account");
		}
	}

	/**
	 * The library enforces user verification when the request asked for it; this checks the flag
	 * directly as well, so a response that somehow reaches us without it is refused outright.
	 */
	private void requireUserVerification(PublicKeyCredential<? extends AuthenticatorResponse, ? extends ClientExtensionOutputs> credential,
			String message) {
		if (!credential.getResponse().getParsedAuthenticatorData().getFlags().UV) {
			throw new BadRequestException(message, ErrorType.PASSKEY_CHALLENGE_FAILED);
		}
	}

	private PublicKeyCredential<AuthenticatorAttestationResponse, ClientRegistrationExtensionOutputs> parseRegistration(JsonNode response) {
		try {
			return PublicKeyCredential.parseRegistrationResponseJson(objectMapper.writeValueAsString(response));
		} catch (Exception e) {
			throw new BadRequestException("That passkey response could not be read", ErrorType.INVALID_REQUEST);
		}
	}

	private PublicKeyCredential<AuthenticatorAssertionResponse, ClientAssertionExtensionOutputs> parseAssertion(JsonNode response) {
		try {
			return PublicKeyCredential.parseAssertionResponseJson(objectMapper.writeValueAsString(stripEmptyUserHandle(response)));
		} catch (Exception e) {
			throw new BadRequestException("That passkey response could not be read", ErrorType.INVALID_REQUEST);
		}
	}

	/**
	 * Some clients send an empty string for userHandle instead of omitting it, which the parser
	 * rejects as a malformed value.
	 */
	private JsonNode stripEmptyUserHandle(JsonNode response) {
		JsonNode inner = response.get("response");
		if (inner instanceof ObjectNode objectNode) {
			JsonNode userHandle = objectNode.get("userHandle");
			if (userHandle != null && userHandle.isTextual() && userHandle.textValue().isEmpty()) {
				objectNode.remove("userHandle");
			}
		}
		return response;
	}

	private String serialize(PublicKeyCredentialCreationOptions options) {
		try {
			return options.toJson();
		} catch (Exception e) {
			throw new InternalServerErrorException("Could not store the registration request");
		}
	}

	private String serialize(AssertionRequest request) {
		try {
			return request.toJson();
		} catch (Exception e) {
			throw new InternalServerErrorException("Could not store the assertion request");
		}
	}

	private PublicKeyCredentialCreationOptions deserializeCreationOptions(String raw) {
		try {
			return PublicKeyCredentialCreationOptions.fromJson(raw);
		} catch (Exception e) {
			throw new InternalServerErrorException("Could not read the stored registration request");
		}
	}

	private AssertionRequest deserializeAssertionRequest(String raw) {
		try {
			return AssertionRequest.fromJson(raw);
		} catch (Exception e) {
			throw new InternalServerErrorException("Could not read the stored assertion request");
		}
	}

	private String toJson(PublicKeyCredentialCreationOptions options) {
		try {
			return options.toCredentialsCreateJson();
		} catch (Exception e) {
			throw new InternalServerErrorException("Could not build the registration options");
		}
	}

	private String toJson(AssertionRequest request) {
		try {
			return request.toCredentialsGetJson();
		} catch (Exception e) {
			throw new InternalServerErrorException("Could not build the assertion options");
		}
	}

	private JsonNode readTree(String raw) {
		try {
			return objectMapper.readTree(raw);
		} catch (Exception e) {
			throw new InternalServerErrorException("Could not build the passkey options");
		}
	}
}
