package com.fpm2025.notification_service.repository;

import com.fpm2025.notification_service.entity.FcmTokenEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FcmTokenRepository extends JpaRepository<FcmTokenEntity, Long> {

    List<FcmTokenEntity> findByUserIdAndIsActiveTrue(Long userId);

    Optional<FcmTokenEntity> findByUserIdAndDeviceId(Long userId, String deviceId);

    @Modifying
    @Query("UPDATE FcmTokenEntity f SET f.isActive = false WHERE f.userId = :userId AND f.deviceId = :deviceId")
    int deactivateToken(@Param("userId") Long userId, @Param("deviceId") String deviceId);
}
