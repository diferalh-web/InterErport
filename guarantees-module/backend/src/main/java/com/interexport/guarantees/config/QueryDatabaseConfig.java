package com.interexport.guarantees.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Query Database Configuration for CQRS
 * 
 * This configuration sets up JPA repositories for the query (read) side.
 * Only active when 'cqrs' profile is enabled.
 */
@Configuration
@Profile("cqrs")
@EnableJpaRepositories(
    basePackages = "com.interexport.guarantees.repository.query",
    entityManagerFactoryRef = "queryEntityManagerFactory",
    transactionManagerRef = "queryTransactionManager"
)
public class QueryDatabaseConfig {
    // Configuration is handled by annotations
    // Query datasource and entity manager are defined in CQRSConfig
}



