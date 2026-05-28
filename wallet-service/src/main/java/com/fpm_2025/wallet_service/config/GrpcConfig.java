package com.fpm_2025.wallet_service.config;

import com.fpm2025.grpc.protocol.UserGrpcServiceGrpc;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.grpc.client.GrpcClientFactory;

@Configuration
@Profile("!monolith")
public class GrpcConfig {
    // Standard gRPC configuration for Spring gRPC server
}