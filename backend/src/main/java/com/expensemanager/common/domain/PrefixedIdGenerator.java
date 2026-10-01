package com.expensemanager.common.domain;

import com.expensemanager.common.util.IdUtil;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.IdentifierGenerator;

/** Produces {@code <prefix>_<uuid>} identifiers, where the prefix comes from {@link ModulePrefix}. */
public class PrefixedIdGenerator implements IdentifierGenerator {

	@Override
	public Object generate(SharedSessionContractImplementor session, Object entity) {
		return ModulePrefix.forEntity(entity.getClass()) + "_" + IdUtil.uuid();
	}
}
