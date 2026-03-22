package com.fpm_2025.reportingservice.service;

import com.fpm_2025.reportingservice.domain.WalletData;
import java.util.List;
import java.util.ArrayList;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class WalletGrpcClient {

    public List<WalletData> getUserWallets(Long userId) {
        log.info("Mock getting wallets for user: {}", userId);
        return new ArrayList<>();
    }
}
