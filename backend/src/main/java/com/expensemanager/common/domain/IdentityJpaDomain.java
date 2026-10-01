package com.expensemanager.common.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;

import java.util.Objects;

/** Base for every persisted entity: a prefixed string id plus creation and modification stamps. */
@Getter
@Setter
@MappedSuperclass
public abstract class IdentityJpaDomain extends TimestampJpaDomain {

	@Id
	@PrefixedId
	@Column(name = "id", nullable = false, length = 40, updatable = false)
	private String id;

	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof IdentityJpaDomain that) || !getClass().equals(other.getClass())) {
			return false;
		}
		return id != null && id.equals(that.getId());
	}

	@Override
	public int hashCode() {
		// Constant so that a transient entity keeps its hash once an id is assigned on persist.
		return Objects.hashCode(getClass());
	}
}
