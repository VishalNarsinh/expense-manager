package com.expensemanager.common.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.List;

/**
 * The standard list envelope: {@code {"has_more": bool, "object": "list", "data": [...]}}.
 *
 * <p>Keyset-style paging: callers advance until {@code has_more} is false. There is deliberately no
 * total count, which would cost a second aggregate query on every page.
 */
@JsonPropertyOrder({"has_more", "object", "data"})
public record ListResponse<T>(@JsonProperty("has_more") boolean hasMore, @JsonProperty("data") List<T> data) {

	@JsonProperty("object")
	public String object() {
		return "list";
	}

	public static <T> ListResponse<T> of(List<T> data, boolean hasMore) {
		return new ListResponse<>(hasMore, data);
	}

	public static <T> ListResponse<T> of(Slice<T> slice) {
		return new ListResponse<>(slice.hasMore(), slice.data());
	}
}
