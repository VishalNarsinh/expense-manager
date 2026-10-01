package com.expensemanager.common.repository;

import com.expensemanager.common.dto.Slice;
import com.querydsl.core.types.EntityPath;
import com.querydsl.core.types.Expression;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.Predicate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * QueryDSL vocabulary shared by every repository: keyset paging, projections, bulk updates and
 * predicate deletes.
 *
 * <p>Deliberately a small surface: only the operations this application actually uses, so every
 * method here is exercised and tested. Add to it when something needs more.
 */
@NoRepositoryBean
public interface BaseRepository<T, ID> extends JpaRepository<T, ID> {

	/** One page plus a lookahead row, so the caller learns whether another page exists. */
	Slice<T> findSlice(Predicate predicate, EntityPath<T> path, int page, int size, OrderSpecifier<?>... order);

	List<T> findAll(Predicate predicate, EntityPath<T> path, OrderSpecifier<?>... order);

	List<T> findAll(Predicate predicate, EntityPath<T> path, int limit, OrderSpecifier<?>... order);

	Optional<T> findOne(Predicate predicate, EntityPath<T> path);

	long count(Predicate predicate, EntityPath<T> path);

	boolean exists(Predicate predicate, EntityPath<T> path);

	/**
	 * Loads only the named columns into a partial entity.
	 *
	 * <p>Cheaper than fetching the whole row on a read path that needs a handful of fields. The
	 * result is a detached, partially populated instance: any field not named here reads back as
	 * null or zero rather than its stored value, so never hand one to code that will persist it.
	 */
	Optional<T> findOneProjected(Predicate predicate, EntityPath<T> path, Expression<?>... fields);

	/** List counterpart of {@link #findOneProjected}, with the same partial-entity caveat. */
	List<T> findAllProjected(Predicate predicate, EntityPath<T> path, List<OrderSpecifier<?>> order, Expression<?>... fields);

	/** Projects into an arbitrary type via its constructor. */
	<P> List<P> project(Predicate predicate, EntityPath<T> path, Class<P> projection, List<OrderSpecifier<?>> order, Expression<?>... fields);

	/** Grouped aggregate projected into an arbitrary type via its constructor. */
	<P> List<P> aggregate(Predicate predicate, EntityPath<T> path, List<Path<?>> groupBy, Class<P> projection, Expression<?>... fields);

	/**
	 * Bulk update. The {@code modified} stamp is applied automatically, because a bulk clause
	 * bypasses the JPA lifecycle callbacks and callers would otherwise have to remember it every
	 * time.
	 */
	long updateFields(Predicate predicate, EntityPath<T> path, Map<Path<?>, Object> values);

	long deleteWhere(Predicate predicate, EntityPath<T> path);
}
