package com.fpm_2025.api_gateway.config;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.circuitbreaker.resilience4j.ReactiveResilience4JCircuitBreakerFactory;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JConfigBuilder;
import org.springframework.cloud.client.circuitbreaker.Customizer;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Slf4j
@Configuration
public class RouteConfig {

    @Bean
    public RouteLocator gatewayRoutes(RouteLocatorBuilder builder) {
        return builder.routes()
                // User Auth Service - Login (Rate Limited separately: 5 burst requests)
                .route("user-auth-login", r -> r
                        .path("/api/v1/auth/login")
                        .filters(f -> f
                                .requestRateLimiter(config -> config
                                        .setRateLimiter(loginRedisRateLimiter())
                                        .setKeyResolver(userKeyResolver()))
                                .circuitBreaker(config -> config
                                        .setName("user-auth-service")
                                        .setFallbackUri("forward:/fallback"))
                        )
                        .uri("lb://user-auth-service")
                )

                // User Auth Service - Register (Rate Limited)
                .route("user-auth-register", r -> r
                        .path("/api/v1/auth/register")
                        .filters(f -> f
                                .requestRateLimiter(config -> config
                                        .setRateLimiter(loginRedisRateLimiter())
                                        .setKeyResolver(userKeyResolver()))
                                .circuitBreaker(config -> config
                                        .setName("user-auth-service")
                                        .setFallbackUri("forward:/fallback"))
                        )
                        .uri("lb://user-auth-service")
                )

                // User Auth Service (Other routes)
                .route("user-auth-service", r -> r
                        .path("/api/v1/auth/**", "/api/v1/users/**", "/api/v1/families/**")
                        .filters(f -> f
                                .circuitBreaker(config -> config
                                        .setName("user-auth-service")
                                        .setFallbackUri("forward:/fallback"))
                                .requestRateLimiter(config -> config
                                        .setRateLimiter(redisRateLimiter())
                                        .setKeyResolver(userKeyResolver()))
                        )
                        .uri("lb://user-auth-service")
                )
                
                // Wallet Service
                .route("wallet-service", r -> r
                        .path("/api/v1/wallets/**", "/api/v1/categories/**")
                        .filters(f -> f
                                .circuitBreaker(config -> config
                                        .setName("wallet-service")
                                        .setFallbackUri("forward:/fallback"))
                                .requestRateLimiter(config -> config
                                        .setRateLimiter(redisRateLimiter())
                                        .setKeyResolver(userKeyResolver()))
                        )
                        .uri("lb://wallet-service")
                )
                
                // Transaction Service
                .route("transaction-service", r -> r
                        .path("/api/v1/transactions/**")
                        .filters(f -> f
                                .circuitBreaker(config -> config
                                        .setName("transaction-service")
                                        .setFallbackUri("forward:/fallback"))
                                .requestRateLimiter(config -> config
                                        .setRateLimiter(redisRateLimiter())
                                        .setKeyResolver(userKeyResolver()))
                        )
                        .uri("lb://transaction-service")
                )
                
                // Budget & Reporting Service
                .route("reporting-service", r -> r
                        .path("/api/v1/reports/**", "/api/v1/dashboard/**", "/api/v1/budgets/**")
                        .filters(f -> f
                                .circuitBreaker(config -> config
                                        .setName("reporting-service")
                                        .setFallbackUri("forward:/fallback"))
                                .requestRateLimiter(config -> config
                                        .setRateLimiter(redisRateLimiter())
                                        .setKeyResolver(userKeyResolver()))
                        )
                        .uri("lb://reporting-service")
                )
                
                // Notification Service
                .route("notification-service", r -> r
                        .path("/api/v1/notifications/**")
                        .filters(f -> f
                                .circuitBreaker(config -> config
                                        .setName("notification-service")
                                        .setFallbackUri("forward:/fallback"))
                                .requestRateLimiter(config -> config
                                        .setRateLimiter(redisRateLimiter())
                                        .setKeyResolver(userKeyResolver()))
                        )
                        .uri("lb://notification-service")
                )
                
                // OCR Service
                .route("ocr-service", r -> r
                        .path("/api/v1/ocr/**")
                        .filters(f -> f
                                .circuitBreaker(config -> config
                                        .setName("ocr-service")
                                        .setFallbackUri("forward:/fallback"))
                                .requestRateLimiter(config -> config
                                        .setRateLimiter(redisRateLimiter())
                                        .setKeyResolver(userKeyResolver()))
                        )
                        .uri("lb://ocr-service")
                )
                
                // AI Service
                .route("ai-service", r -> r
                        .path("/api/v1/ai/**")
                        .filters(f -> f
                                .circuitBreaker(config -> config
                                        .setName("ai-service")
                                        .setFallbackUri("forward:/fallback"))
                                .requestRateLimiter(config -> config
                                        .setRateLimiter(redisRateLimiter())
                                        .setKeyResolver(userKeyResolver()))
                        )
                        .uri("lb://ai-service")
                )
                
                .build();
    }

    @Bean
    public RedisRateLimiter loginRedisRateLimiter() {
        // Since RedisRateLimiter uses replenishRate per second (int), we cannot set < 1 token/sec directly.
        // Instead, to achieve 5 requests per 5 minutes (300 seconds), we can set burstCapacity to 5
        // and we will rely on Resilience4j for accurate rate limiting.
        // We will just use Resilience4j annotations in the user-auth-service controller for login!
        return new RedisRateLimiter(1, 5, 1);
    }

    @Primary
    @Bean
    public RedisRateLimiter redisRateLimiter() {
        // 100 requests per minute per user
        return new RedisRateLimiter(100, 120, 1);
    }

    @Bean
    public KeyResolver userKeyResolver() {
        return exchange -> Mono.just(
                exchange.getRequest().getHeaders().getFirst("X-User-Id") != null
                        ? exchange.getRequest().getHeaders().getFirst("X-User-Id")
                        : exchange.getRequest().getRemoteAddress().getAddress().getHostAddress()
        );
    }

    @Bean
    public Customizer<ReactiveResilience4JCircuitBreakerFactory> defaultCustomizer() {
        return factory -> factory.configureDefault(id -> new Resilience4JConfigBuilder(id)
                .circuitBreakerConfig(CircuitBreakerConfig.custom()
                        .slidingWindowSize(10)
                        .minimumNumberOfCalls(5)
                        .failureRateThreshold(50)
                        .waitDurationInOpenState(Duration.ofSeconds(30))
                        .permittedNumberOfCallsInHalfOpenState(3)
                        .build())
                .timeLimiterConfig(TimeLimiterConfig.custom()
                        .timeoutDuration(Duration.ofSeconds(10))
                        .build())
                .build());
    }
}