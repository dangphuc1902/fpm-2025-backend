package com.fpm2025.monolith.config;

import com.fpm2025.security.jwt.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;

@Configuration
@EnableWebSecurity
@Profile("monolith")
@RequiredArgsConstructor
@Slf4j
public class MonolithSecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/api/v1/auth/**",
                    "/swagger-ui/**",
                    "/v3/api-docs/**",
                    "/actuator/**"
                ).permitAll()
                .anyRequest().authenticated()
            )
            .sessionManagement(session -> 
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterAfter(new MonolithHeaderBridgeFilter(), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public org.springframework.security.crypto.password.PasswordEncoder passwordEncoder() {
        return new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(Arrays.asList("*"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "Cache-Control", "Accept", "X-Requested-With"));
        configuration.setExposedHeaders(Arrays.asList("Authorization"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    /**
     * Filter bridge to automatically parse authenticated user's ID and inject 
     * it as the HTTP request header "X-User-Id" to match API Gateway legacy behavior.
     */
    private static class MonolithHeaderBridgeFilter extends OncePerRequestFilter {
        @Override
        protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
                throws ServletException, IOException {
            
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
                Object principal = auth.getPrincipal();
                String userId = getUserIdFromPrincipal(principal);
                
                if (userId != null && !userId.isBlank()) {
                    HttpServletRequestWrapper wrappedRequest = new HttpServletRequestWrapper(request) {
                        @Override
                        public String getHeader(String name) {
                            if ("X-User-Id".equalsIgnoreCase(name)) {
                                return userId;
                            }
                            return super.getHeader(name);
                        }
                        
                        @Override
                        public java.util.Enumeration<String> getHeaders(String name) {
                            if ("X-User-Id".equalsIgnoreCase(name)) {
                                return java.util.Collections.enumeration(java.util.Collections.singletonList(userId));
                            }
                            return super.getHeaders(name);
                        }
                        
                        @Override
                        public java.util.Enumeration<String> getHeaderNames() {
                            java.util.List<String> names = java.util.Collections.list(super.getHeaderNames());
                            if (!names.contains("X-User-Id")) {
                                names.add("X-User-Id");
                            }
                            return java.util.Collections.enumeration(names);
                        }
                    };
                    filterChain.doFilter(wrappedRequest, response);
                    return;
                }
            }
            filterChain.doFilter(request, response);
        }

        private String getUserIdFromPrincipal(Object principal) {
            if (principal == null) return null;
            if (principal instanceof String) {
                return (String) principal;
            }
            if (principal instanceof Number) {
                return String.valueOf(principal);
            }
            try {
                java.lang.reflect.Method getIdMethod = principal.getClass().getMethod("getId");
                Object id = getIdMethod.invoke(principal);
                if (id != null) return String.valueOf(id);
            } catch (Exception ignored) {}
            try {
                java.lang.reflect.Method getUserIdMethod = principal.getClass().getMethod("getUserId");
                Object id = getUserIdMethod.invoke(principal);
                if (id != null) return String.valueOf(id);
            } catch (Exception ignored) {}
            try {
                java.lang.reflect.Field idField = principal.getClass().getDeclaredField("id");
                idField.setAccessible(true);
                Object id = idField.get(principal);
                if (id != null) return String.valueOf(id);
            } catch (Exception ignored) {}
            try {
                java.lang.reflect.Field userIdField = principal.getClass().getDeclaredField("userId");
                userIdField.setAccessible(true);
                Object id = userIdField.get(principal);
                if (id != null) return String.valueOf(id);
            } catch (Exception ignored) {}
            
            return principal.toString();
        }
    }
}
