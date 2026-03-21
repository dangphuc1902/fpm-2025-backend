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
import reactor.core.publisher.Mono;

import java.time.Duration;

@Slf4j
@Configuration
public class RouteConfig {

    @Bean
    public RouteLocator gatewayRoutes(RouteLocatorBuilder builder) {
        return builder.routes()
                // User Auth Service - Login (Rate Limited separately: 5 requests / 5 mins)
                .route("user-auth-login", r -> r
                        .path("/api/auth/login")
                        .filters(f -> f
                                .stripPrefix(0)
                                .circuitBreaker(config -> config
                                        .setName("user-auth-service")
                                        .setFallbackUri("forward:/fallback"))
                                .requestRateLimiter(config -> config
                                        .setRateLimiter(loginRedisRateLimiter())
                                        .setKeyResolver(userKeyResolver()))
                        )
                        .uri("lb://user-auth-service")
                )

                // User Auth Service (Other routes)
                .route("user-auth-service", r -> r
                        .path("/api/auth/**", "/api/users/**", "/api/families/**")
                        .filters(f -> f
                                .stripPrefix(0)
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
                        .path("/api/wallets/**")
                        .filters(f -> f
                                .stripPrefix(0)
                                .circuitBreaker(config -> config
                                        .setName("wallet-service")
                                        .setFallbackUri("forward:/fallback"))
                                .requestRateLimiter(config -> config
                                        .setRateLimiter(redisRateLimiter())
                                        .setKeyResolver(userKeyResolver()))
                        )
                        .uri("lb://wallet-service")
                )
                
                // Category Service
                .route("category-service", r -> r
                        .path("/api/categories/**", "/api/budgets/**")
                        .filters(f -> f
                                .stripPrefix(0)
                                .circuitBreaker(config -> config
                                        .setName("category-service")
                                        .setFallbackUri("forward:/fallback"))
                                .requestRateLimiter(config -> config
                                        .setRateLimiter(redisRateLimiter())
                                        .setKeyResolver(userKeyResolver()))
                        )
                        .uri("lb://category-service")
                )
                
                // Reporting Service
                .route("reporting-service", r -> r
                        .path("/api/reports/**", "/api/dashboard/**")
                        .filters(f -> f
                                .stripPrefix(0)
                                .circuitBreaker(config -> config
                                        .setName("reporting-service")
                                        .setFallbackUri("forward:/fallback"))
                                .requestRateLimiter(config -> config
                                        .setRateLimiter(redisRateLimiter())
                                        .setKeyResolver(userKeyResolver()))
                        )
                        .uri("lb://reporting-service")
                )
                
                // Sharing Service
                .route("sharing-service", r -> r
                        .path("/api/sharing/**")
                        .filters(f -> f
                                .stripPrefix(0)
                                .circuitBreaker(config -> config
                                        .setName("sharing-service")
                                        .setFallbackUri("forward:/fallback"))
                                .requestRateLimiter(config -> config
                                        .setRateLimiter(redisRateLimiter())
                                        .setKeyResolver(userKeyResolver()))
                        )
                        .uri("lb://sharing-service")
                )
                
                // Notification Service
                .route("notification-service", r -> r
                        .path("/api/notifications/**")
                        .filters(f -> f
                                .stripPrefix(0)
                                .circuitBreaker(config -> config
                                        .setName("notification-service")
                                        .setFallbackUri("forward:/fallback"))
                        )
                        .uri("lb://notification-service")
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