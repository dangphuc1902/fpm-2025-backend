package com.fpm_2025.api_gateway.config;

import com.fpm2025.security.jwt.JwtProperties;
import com.fpm2025.security.jwt.JwtTokenProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JwtConfig {

    @Bean
    public JwtProperties jwtProperties() {
        return new JwtProperties();
    }

    @Bean
    public JwtTokenProvider jwtTokenProvider(JwtProperties jwtProperties) {
        return new JwtTokenProvider(jwtProperties);
    }
}
