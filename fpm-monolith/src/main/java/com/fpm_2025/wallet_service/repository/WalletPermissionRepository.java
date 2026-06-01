package com.fpm_2025.wallet_service.repository;

import com.fpm_2025.wallet_service.entity.WalletPermissionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WalletPermissionRepository extends JpaRepository<WalletPermissionEntity, Long> {
    List<WalletPermissionEntity> findByWalletId(Long walletId);
    Optional<WalletPermissionEntity> findByWalletIdAndUserId(Long walletId, Long userId);
    boolean existsByWalletIdAndUserId(Long walletId, Long userId);
    List<WalletPermissionEntity> findByUserId(Long userId);
}
