package com.fpm2025.notification_service.repository;

import com.fpm2025.notification_service.entity.BankNotificationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BankNotificationRepository extends JpaRepository<BankNotificationEntity, Long> {

    /** Kiểm tra duplicate bằng checksum MD5 */
    Optional<BankNotificationEntity> findByChecksum(String checksum);

    boolean existsByChecksum(String checksum);
}
