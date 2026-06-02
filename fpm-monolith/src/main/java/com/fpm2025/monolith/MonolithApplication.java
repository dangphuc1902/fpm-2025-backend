package com.fpm2025.monolith;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@ComponentScan(basePackages = {
    "com.fpm2025.monolith",
    "com.fpm2025.user_auth_service",
    "com.fpm2025.wallet_service",
    "com.fpm2025.transaction_service",
    "com.fpm2025.reporting_service",
    "com.fpm2025.notification_service",
    "com.fpm2025.ocr_service",
    "com.fpm2025.ai_service",
    "com.fpm2025.security"
})
@EnableJpaRepositories(basePackages = "com.fpm2025")
@EntityScan(basePackages = "com.fpm2025")
@EnableAsync
public class MonolithApplication {
    public static void main(String[] args) {
        SpringApplication.run(MonolithApplication.class, args);
    }
}

