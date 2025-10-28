package com.fpm_2025.wallet_service.repository;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import com.fpm_2025.wallet_service.entity.WalletEntity;
import com.fpm_2025.wallet_service.entity.enums.WalletType;
import com.google.common.base.Optional;

public interface WalletRepository extends JpaRepository<WalletEntity, Long>{
	Optional<WalletEntity> findByUserIdAndId(Long userId, Long id);
    List<WalletEntity> findByUserId(Long userId);
    List<WalletEntity>findActiveWalletsByUserId(Long userId);
    boolean existsByUserIdAndName(Long userId, String name);
    List<WalletEntity> findByUserIdAndType(Long userId,WalletType type);
    Optional<WalletEntity> findByIdAndUserId(Long id,Long userId);
    BigDecimal getTotalBalanceByUserId(Long userId);
}
