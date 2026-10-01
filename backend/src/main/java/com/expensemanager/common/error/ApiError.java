package com.expensemanager.common.error;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * The single error envelope for every failed request:
 * <pre>{"errors":[{"type":"...","message":"...","param":"...","reason_code":"..."}]}</pre>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiError(@JsonProperty("errors") List<Error> errors) {

	public static ApiError of(ErrorType type, String message) {
		return new ApiError(List.of(new Error(type.getValue(), message, null, null)));
	}

	public static ApiError of(ErrorType type, String message, String param) {
		return new ApiError(List.of(new Error(type.getValue(), message, param, null)));
	}

	public static ApiError of(List<Error> errors) {
		return new ApiError(errors);
	}

	@JsonInclude(JsonInclude.Include.NON_NULL)
	public record Error(@JsonProperty("type") String type, @JsonProperty("message") String message, @JsonProperty("param") String param,
			@JsonProperty("reason_code") String reasonCode) {

		public static Error of(ErrorType type, String message, String param) {
			return new Error(type.getValue(), message, param, null);
		}
	}
}
