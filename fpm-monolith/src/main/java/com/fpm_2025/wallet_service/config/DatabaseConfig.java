package com.fpm_2025.wallet_service.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(basePackages = "com.fpm_2025.wallet_service.repository")
public class DatabaseConfig {
    // Additional database configurations if needed
}