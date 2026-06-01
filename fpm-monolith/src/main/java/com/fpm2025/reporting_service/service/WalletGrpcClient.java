package com.fpm2025.reporting_service.service;

import com.fpm2025.reporting_service.domain.WalletData;
import com.fpm2025.wallet_service.service.WalletService;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class WalletGrpcClient {

    private final WalletService walletService;

    public List<WalletData> getUserWallets(Long userId) {
        log.info("Getting wallets directly for user: {}", userId);
        try {
            return walletService.getUserWallets(userId).stream()
                    .map(w -> WalletData.builder()
                            .id(w.getId())
                            .name(w.getName())
                            .type(w.getType() != null ? w.getType().name() : null)
                            .balance(w.getBalance())
                            .build())
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Failed to query user wallets directly", e);
            return List.of();
        }
    }
}

