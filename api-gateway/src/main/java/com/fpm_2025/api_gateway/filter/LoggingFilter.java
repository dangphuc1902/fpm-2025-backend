package com.fpm_2025.api_gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;

@Slf4j
@Component
public class LoggingFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        Instant startTime = Instant.now();
        
        log.info("Gateway Request: {} {} from {}",
                request.getMethod(),
                request.getPath(),
                request.getRemoteAddress()
        );
        
        return chain.filter(exchange).then(Mono.fromRunnable(() -> {
            Duration duration = Duration.between(startTime, Instant.now());
            log.info("Gateway Response: {} {} - Status: {} - Duration: {}ms",
                    request.getMethod(),
                    request.getPath(),
                    exchange.getResponse().getStatusCode(),
                    duration.toMillis()
            );
        }));
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}