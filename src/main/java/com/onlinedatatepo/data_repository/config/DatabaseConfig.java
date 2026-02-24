package com.onlinedatatepo.data_repository.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Database configuration for PostgreSQL connection pooling and JPA setup.
 * 
 * This class configures:
 * - JPA/Hibernate properties for entity management
 * - Transaction management for database operations
 * 
 * Database connection settings are defined in application.properties:
 * - spring.datasource.url
 * - spring.datasource.username
 * - spring.datasource.password
 * 
 * HikariCP connection pooling is auto-configured by Spring Boot.
 */
@Configuration
@EnableJpaRepositories(basePackages = "com.onlinedatatepo.data_repository.repository")
@EnableTransactionManagement
public class DatabaseConfig {
    // Spring Boot auto-configures DataSource from application.properties
    // No manual bean configuration needed
}

