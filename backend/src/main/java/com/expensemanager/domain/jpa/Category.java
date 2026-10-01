package com.expensemanager.domain.jpa;

import com.expensemanager.common.domain.IdentityJpaDomain;
import com.expensemanager.domain.enums.TransactionType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * A spending or income category.
 *
 * <p>A null user means a system category, visible to everyone and owned by no one. Anything else
 * belongs to that user alone, so renaming or deleting it cannot affect another account.
 */
@Getter
@Setter
@Entity
@Table(name = "categories")
public class Category extends IdentityJpaDomain {

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id")
	private User user;

	@Column(name = "name", nullable = false, length = 60)
	private String name;

	@Enumerated(EnumType.STRING)
	@Column(name = "type", nullable = false, length = 20)
	private TransactionType type;

	/** Bootstrap Icons class name, rendered as-is by the frontend. */
	@Column(name = "icon", nullable = false, length = 50)
	private String icon;

	@Column(name = "color_code", nullable = false, length = 20)
	private String colorCode;

	@Column(name = "is_default", nullable = false)
	private boolean systemDefault;

	public boolean isSystemCategory() {
		return user == null;
	}
}
