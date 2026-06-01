package com.fpm2025.wallet_service.grpc.client;

import com.fpm2025.user_auth_service.repository.FamilyMemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserGrpcClient {

    private final FamilyMemberRepository familyMemberRepository;

    /**
     * Kiểm tra một user có thuộc familyId hay không thông qua truy vấn DB trực tiếp.
     */
    public boolean isUserInFamily(Long userId, Long familyId) {
        log.info("[Local] Checking family membership directly: userId={} familyId={}", userId, familyId);
        try {
            return familyMemberRepository.existsByFamilyIdAndUserId(familyId, userId);
        } catch (Exception e) {
            log.error("[Local] Direct check failed: {}", e.getMessage());
            return false;
        }
    }
}

