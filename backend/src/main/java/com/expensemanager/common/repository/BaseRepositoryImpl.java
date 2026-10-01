package com.expensemanager.common.repository;

import com.expensemanager.common.domain.TimestampJpaDomain;
import com.expensemanager.common.dto.Slice;
import com.expensemanager.common.util.DateTimeUtil;
import com.querydsl.core.types.EntityPath;
import com.querydsl.core.types.Expression;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.QBean;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.Wildcard;
import com.querydsl.jpa.impl.JPADeleteClause;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAUpdateClause;
import jakarta.persistence.EntityManager;
import org.springframework.data.jpa.repository.support.JpaEntityInformation;
import org.springframework.data.jpa.repository.support.SimpleJpaRepository;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Wired in globally through {@code @EnableJpaRepositories(repositoryBaseClass = ...)}, so every
 * repository interface inherits these operations without extra configuration.
 */
@NoRepositoryBean
@Transactional(readOnly = true)
public class BaseRepositoryImpl<T extends TimestampJpaDomain, ID> extends SimpleJpaRepository<T, ID> implements BaseRepository<T, ID> {

	private static final String MODIFIED_FIELD = "modified";

	private final EntityManager entityManager;

	public BaseRepositoryImpl(JpaEntityInformation<T, ?> entityInformation, EntityManager entityManager) {
		super(entityInformation, entityManager);
		this.entityManager = entityManager;
	}

	@Override
	public Slice<T> findSlice(Predicate predicate, EntityPath<T> path, int page, int size, OrderSpecifier<?>... order) {
		int safePage = Math.max(page, 1);
		int safeSize = Math.max(size, 1);

		JPAQuery<T> query = newQuery().select(path).from(path);
		applyWhere(query, predicate);
		applyOrder(query, order);

		// Fetching one extra row reveals whether a further page exists, with no second count query.
		List<T> rows = query.offset((long) (safePage - 1) * safeSize).limit(safeSize + 1L).fetch();

		boolean hasMore = rows.size() > safeSize;
		if (hasMore) {
			rows = new ArrayList<>(rows.subList(0, safeSize));
		}
		return new Slice<>(rows, hasMore);
	}

	@Override
	public List<T> findAll(Predicate predicate, EntityPath<T> path, OrderSpecifier<?>... order) {
		JPAQuery<T> query = newQuery().select(path).from(path);
		applyWhere(query, predicate);
		applyOrder(query, order);
		return query.fetch();
	}

	@Override
	public List<T> findAll(Predicate predicate, EntityPath<T> path, int limit, OrderSpecifier<?>... order) {
		JPAQuery<T> query = newQuery().select(path).from(path);
		applyWhere(query, predicate);
		applyOrder(query, order);
		return query.limit(limit).fetch();
	}

	@Override
	public Optional<T> findOne(Predicate predicate, EntityPath<T> path) {
		JPAQuery<T> query = newQuery().select(path).from(path);
		applyWhere(query, predicate);
		return Optional.ofNullable(query.fetchFirst());
	}

	@Override
	public long count(Predicate predicate, EntityPath<T> path) {
		JPAQuery<Long> query = newQuery().select(Wildcard.count).from(path);
		applyWhere(query, predicate);
		Long total = query.fetchOne();
		return total == null ? 0L : total;
	}

	@Override
	public boolean exists(Predicate predicate, EntityPath<T> path) {
		JPAQuery<T> query = newQuery().select(path).from(path);
		applyWhere(query, predicate);
		return query.fetchFirst() != null;
	}

	@Override
	public Optional<T> findOneProjected(Predicate predicate, EntityPath<T> path, Expression<?>... fields) {
		JPAQuery<T> query = newQuery().select(entityProjection(path, fields)).from(path);
		applyWhere(query, predicate);
		return Optional.ofNullable(query.fetchFirst());
	}

	@Override
	public List<T> findAllProjected(Predicate predicate, EntityPath<T> path, List<OrderSpecifier<?>> order, Expression<?>... fields) {
		JPAQuery<T> query = newQuery().select(entityProjection(path, fields)).from(path);
		applyWhere(query, predicate);
		applyOrder(query, order);
		return query.fetch();
	}

	@SuppressWarnings("unchecked")
	private QBean<T> entityProjection(EntityPath<T> path, Expression<?>... fields) {
		// Binds by field rather than setter, so Lombok-generated accessors are irrelevant and
		// inherited columns such as the id are populated too.
		return Projections.fields((Class<T>) path.getType(), fields);
	}

	@Override
	public <P> List<P> project(Predicate predicate, EntityPath<T> path, Class<P> projection, List<OrderSpecifier<?>> order, Expression<?>... fields) {
		JPAQuery<P> query = newQuery().select(Projections.constructor(projection, fields)).from(path);
		applyWhere(query, predicate);
		applyOrder(query, order);
		return query.fetch();
	}

	@Override
	public <P> List<P> aggregate(Predicate predicate, EntityPath<T> path, List<Path<?>> groupBy, Class<P> projection, Expression<?>... fields) {
		JPAQuery<P> query = newQuery().select(Projections.constructor(projection, fields)).from(path);
		applyWhere(query, predicate);
		if (groupBy != null && !groupBy.isEmpty()) {
			query.groupBy(groupBy.toArray(new Expression<?>[0]));
		}
		return query.fetch();
	}

	@Override
	@Transactional
	public long updateFields(Predicate predicate, EntityPath<T> path, Map<Path<?>, Object> values) {
		JPAUpdateClause update = new JPAUpdateClause(entityManager, path);
		if (predicate != null) {
			update.where(predicate);
		}

		Map<Path<?>, Object> withTimestamp = new LinkedHashMap<>(values);
		withTimestamp.putIfAbsent(Expressions.numberPath(Long.class, path, MODIFIED_FIELD), DateTimeUtil.currentEpochMillisUtc());
		withTimestamp.forEach((field, value) -> applyValue(update, field, value));

		// Deliberately does not clear the persistence context. Clearing would detach every managed
		// entity mid-transaction, breaking lazy loading for callers that are still working. In-memory
		// copies of the updated rows are stale afterwards; re-read if the new values are needed.
		return update.execute();
	}

	@Override
	@Transactional
	public long deleteWhere(Predicate predicate, EntityPath<T> path) {
		JPADeleteClause delete = new JPADeleteClause(entityManager, path);
		if (predicate != null) {
			delete.where(predicate);
		}
		// Like updateFields, leaves the persistence context alone; deleted rows may still be held
		// as managed entities by the caller.
		return delete.execute();
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	private void applyValue(JPAUpdateClause update, Path<?> field, Object value) {
		if (value == null) {
			update.setNull(field);
		} else {
			update.set((Path) field, value);
		}
	}

	private JPAQuery<?> newQuery() {
		return new JPAQuery<>(entityManager);
	}

	private void applyWhere(JPAQuery<?> query, Predicate predicate) {
		if (predicate != null) {
			query.where(predicate);
		}
	}

	private void applyOrder(JPAQuery<?> query, OrderSpecifier<?>... order) {
		if (order != null && order.length > 0) {
			query.orderBy(order);
		}
	}

	private void applyOrder(JPAQuery<?> query, List<OrderSpecifier<?>> order) {
		if (order != null && !order.isEmpty()) {
			query.orderBy(order.toArray(new OrderSpecifier<?>[0]));
		}
	}
}
