package com.fpm_2025.api_gateway.filter;

import com.fpm2025.security.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import java.util.Collections;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter implements WebFilter {
    private Logger logger  = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private final JwtTokenProvider jwtTokenProvider = null;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String path = exchange.getRequest().getPath().value();
        
        // Skip JWT validation for public endpoints
        if (isPublicPath(path)) {
            return chain.filter(exchange);
        }

        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        String token = jwtTokenProvider.extractTokenFromHeader(authHeader);
        
        if (token == null || !jwtTokenProvider.validateToken(token)) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        try {
            String email = jwtTokenProvider.extractEmail(token);
            Long userId = jwtTokenProvider.extractUserId(token);
            
            // Add user info to headers for downstream services
            ServerWebExchange modifiedExchange = exchange.mutate()
                    .request(r -> r
                            .header("X-User-Id", String.valueOf(userId))
                            .header("X-User-Email", email)
                    )
                    .build();
            
            UsernamePasswordAuthenticationToken authentication = 
                    new UsernamePasswordAuthenticationToken(email, null, Collections.emptyList());
            
            return chain.filter(modifiedExchange)
                    .contextWrite(ReactiveSecurityContextHolder.withAuthentication(authentication));
            
        } catch (Exception e) {
        	logger.error("JWT validation error", e.getMessage());
        	exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }
    }

    private boolean isPublicPath(String path) {
        return path.startsWith("/api/v1/auth/register") ||
               path.startsWith("/api/v1/auth/login") ||
               path.startsWith("/api/v1/auth/google") ||
               path.startsWith("/api/v1/auth/refresh") ||
               path.startsWith("/v3/api-docs") ||
               path.startsWith("/swagger-ui") ||
               path.startsWith("/actuator");
    }
}