package com.expensemanager.security.passkey;

import com.expensemanager.domain.enums.PasskeyStatus;
import com.expensemanager.domain.jpa.PasskeyCredential;
import com.expensemanager.domain.jpa.QPasskeyCredential;
import com.expensemanager.domain.jpa.QUser;
import com.expensemanager.repository.jpa.PasskeyCredentialRepository;
import com.expensemanager.repository.jpa.UserRepository;
import com.yubico.webauthn.CredentialRepository;
import com.yubico.webauthn.RegisteredCredential;
import com.yubico.webauthn.data.AuthenticatorTransport;
import com.yubico.webauthn.data.ByteArray;
import com.yubico.webauthn.data.PublicKeyCredentialDescriptor;
import com.yubico.webauthn.data.PublicKeyCredentialType;
import com.yubico.webauthn.data.exception.Base64UrlException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * How the WebAuthn library reaches our stored credentials.
 *
 * <p>The username here is the account email, and the user handle is the account id as bytes, which
 * is what lets a discoverable credential name its owner without the user typing anything.
 */
@Component
public class WebAuthnCredentialStore implements CredentialRepository {

	private static final QPasskeyCredential CREDENTIAL = QPasskeyCredential.passkeyCredential;
	private static final QUser USER = QUser.user;

	private static final Map<String, AuthenticatorTransport> TRANSPORTS_BY_ID = Arrays.stream(AuthenticatorTransport.values())
			.collect(Collectors.toMap(AuthenticatorTransport::getId, Function.identity()));

	private final PasskeyCredentialRepository credentialRepository;
	private final UserRepository userRepository;

	public WebAuthnCredentialStore(PasskeyCredentialRepository credentialRepository, UserRepository userRepository) {
		this.credentialRepository = credentialRepository;
		this.userRepository = userRepository;
	}

	@Override
	@Transactional(readOnly = true)
	public Set<PublicKeyCredentialDescriptor> getCredentialIdsForUsername(String username) {
		return userRepository.findOne(USER.email.equalsIgnoreCase(username), USER)
				.map(user -> credentialRepository
						.findAll(CREDENTIAL.user.id.eq(user.getId()).and(CREDENTIAL.status.eq(PasskeyStatus.ACTIVE)), CREDENTIAL)
						.stream()
						.map(this::toDescriptor)
						.collect(Collectors.toSet()))
				.orElseGet(Collections::emptySet);
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<ByteArray> getUserHandleForUsername(String username) {
		return userRepository.findOne(USER.email.equalsIgnoreCase(username), USER).map(user -> userHandle(user.getId()));
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<String> getUsernameForUserHandle(ByteArray userHandle) {
		return userRepository.findOne(USER.id.eq(toUserId(userHandle)), USER).map(user -> user.getEmail());
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<RegisteredCredential> lookup(ByteArray credentialId, ByteArray userHandle) {
		// Returns empty rather than throwing when nothing matches. The library turns empty into one
		// uniform assertion failure; a distinct "no such credential" error would tell a caller
		// whether a given credential is registered here.
		return credentialRepository
				.findOne(CREDENTIAL.credentialId.eq(credentialId.getBase64Url())
						.and(CREDENTIAL.user.id.eq(toUserId(userHandle)))
						.and(CREDENTIAL.status.eq(PasskeyStatus.ACTIVE)), CREDENTIAL)
				.map(this::toRegisteredCredential);
	}

	@Override
	@Transactional(readOnly = true)
	public Set<RegisteredCredential> lookupAll(ByteArray credentialId) {
		// Used to refuse registering a credential id that already exists. A unique index on the
		// column is the real guard; this keeps the rejection inside the library where it reads as
		// a registration failure rather than a constraint violation.
		return credentialRepository
				.findOne(CREDENTIAL.credentialId.eq(credentialId.getBase64Url()).and(CREDENTIAL.status.eq(PasskeyStatus.ACTIVE)), CREDENTIAL)
				.map(credential -> Set.of(toRegisteredCredential(credential)))
				.orElseGet(Collections::emptySet);
	}

	private PublicKeyCredentialDescriptor toDescriptor(PasskeyCredential credential) {
		PublicKeyCredentialDescriptor.PublicKeyCredentialDescriptorBuilder builder = PublicKeyCredentialDescriptor.builder()
				.id(decode(credential.getCredentialId()))
				.type(PublicKeyCredentialType.PUBLIC_KEY);

		if (credential.getTransports() != null && !credential.getTransports().isBlank()) {
			builder.transports(parseTransports(credential.getTransports()));
		}
		return builder.build();
	}

	private RegisteredCredential toRegisteredCredential(PasskeyCredential credential) {
		return RegisteredCredential.builder()
				.credentialId(decode(credential.getCredentialId()))
				.userHandle(userHandle(credential.getUser().getId()))
				.publicKeyCose(new ByteArray(credential.getPublicKeyCose()))
				.signatureCount(credential.getSignCount())
				.build();
	}

	private Set<AuthenticatorTransport> parseTransports(String transports) {
		return Arrays.stream(transports.split(","))
				.map(String::trim)
				.filter(value -> !value.isEmpty())
				.map(TRANSPORTS_BY_ID::get)
				.filter(java.util.Objects::nonNull)
				.collect(Collectors.toSet());
	}

	private ByteArray userHandle(String userId) {
		return new ByteArray(userId.getBytes(StandardCharsets.UTF_8));
	}

	private String toUserId(ByteArray userHandle) {
		return new String(userHandle.getBytes(), StandardCharsets.UTF_8);
	}

	private ByteArray decode(String base64Url) {
		try {
			return ByteArray.fromBase64Url(base64Url);
		} catch (Base64UrlException e) {
			throw new IllegalStateException("Stored credential id is not valid base64url", e);
		}
	}
}
