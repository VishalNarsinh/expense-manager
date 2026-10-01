package com.expensemanager.common.domain;

import com.expensemanager.common.util.DateTimeUtil;
import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import lombok.Getter;
import lombok.Setter;

/**
 * Creation and modification stamps as UTC epoch milliseconds.
 *
 * <p>Stored as {@code bigint} rather than a timestamp type so the value is unambiguous across time
 * zones and JDBC drivers.
 */
@Getter
@Setter
@MappedSuperclass
public abstract class TimestampJpaDomain {

	@Column(name = "created", nullable = false, updatable = false)
	private Long created;

	@Column(name = "modified", nullable = false)
	private Long modified;

	@PrePersist
	void onPersist() {
		created = DateTimeUtil.currentEpochMillisUtc();
		modified = created;
	}

	@PreUpdate
	void onUpdate() {
		modified = DateTimeUtil.currentEpochMillisUtc();
	}
}
