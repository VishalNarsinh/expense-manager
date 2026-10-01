package com.expensemanager.common.dto;

import java.util.List;

/** A page of rows plus whether another page exists. The repository-level counterpart of {@link ListResponse}. */
public record Slice<T>(List<T> data, boolean hasMore) {

	public static <T> Slice<T> empty() {
		return new Slice<>(List.of(), false);
	}

	public <R> Slice<R> map(java.util.function.Function<T, R> mapper) {
		return new Slice<>(data.stream().map(mapper).toList(), hasMore);
	}
}
