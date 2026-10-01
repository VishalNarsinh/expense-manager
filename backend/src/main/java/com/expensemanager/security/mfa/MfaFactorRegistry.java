package com.expensemanager.security.mfa;

import com.expensemanager.common.exception.BadRequestException;
import com.expensemanager.domain.enums.MfaType;
import com.expensemanager.domain.jpa.User;
import org.springframework.stereotype.Component;

import java.util.List;

/** Routes a second-factor proof to the handler for that factor. */
@Component
public class MfaFactorRegistry {

	private final List<MfaFactorHandler> handlers;

	public MfaFactorRegistry(List<MfaFactorHandler> handlers) {
		this.handlers = handlers;
	}

	public void verify(User user, MfaType type, String proof) {
		handlers.stream()
				.filter(handler -> handler.type() == type)
				.findFirst()
				.orElseThrow(() -> new BadRequestException("Unsupported second factor: " + type))
				.verify(user, proof);
	}
}
