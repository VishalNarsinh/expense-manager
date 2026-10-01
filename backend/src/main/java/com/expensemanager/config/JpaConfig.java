package com.expensemanager.config;

import com.expensemanager.common.repository.BaseRepositoryImpl;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Makes {@link BaseRepositoryImpl} the base for every Spring Data repository, so the shared
 * QueryDSL vocabulary is available without each interface opting in.
 */
@Configuration
@EnableJpaRepositories(basePackages = "com.expensemanager", repositoryBaseClass = BaseRepositoryImpl.class)
public class JpaConfig {
}
