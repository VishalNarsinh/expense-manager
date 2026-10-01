package com.expensemanager.security;

import com.expensemanager.common.error.ApiError;
import com.expensemanager.common.error.ErrorType;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Writes the standard error envelope straight to the response.
 *
 * <p>Needed because filters run outside the controller advice, and without it a rejection from the
 * security chain would return an HTML error page while every other failure returns JSON.
 */
@Component
public class ApiErrorWriter {

	private final ObjectMapper objectMapper;

	public ApiErrorWriter(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	public void write(HttpServletResponse response, int status, ErrorType type, String message) throws IOException {
		response.setStatus(status);
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		response.setCharacterEncoding("UTF-8");
		objectMapper.writeValue(response.getOutputStream(), ApiError.of(type, message));
	}
}
