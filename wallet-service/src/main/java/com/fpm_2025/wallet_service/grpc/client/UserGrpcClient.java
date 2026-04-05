package com.fpm_2025.wallet_service.grpc.client;

import com.fpm2025.protocol.user.*;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * gRPC Client kết nối tới user-auth-service.
 */
@Slf4j
@Service
public class UserGrpcClient {

    @GrpcClient("user-auth-service")
    private UserGrpcServiceGrpc.UserGrpcServiceBlockingStub stub;

    /**
     * Kiểm tra một user có thuộc familyId hay không thông qua gRPC.
     */
    public boolean isUserInFamily(Long userId, Long familyId) {
        log.info("[gRPC] isUserInFamily: userId={} familyId={}", userId, familyId);
        try {
            FamilyIdRequest request = FamilyIdRequest.newBuilder()
                    .setFamilyId(familyId)
                    .build();

            FamilyMembersResponse response = stub.getFamilyMembers(request);

            return response.getMembersList().stream()
                    .anyMatch(member -> member.getUserId() == userId);
        } catch (Exception e) {
            log.error("[gRPC] getFamilyMembers failed: {}", e.getMessage());
            return false;
        }
    }
}
