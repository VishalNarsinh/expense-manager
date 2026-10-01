package com.expensemanager.common.repository;

import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.repository.NoRepositoryBean;

/** What application repositories extend: CRUD, the QueryDSL vocabulary, and predicate execution. */
@NoRepositoryBean
public interface CustomRepository<T, ID> extends BaseRepository<T, ID>, QuerydslPredicateExecutor<T> {
}
