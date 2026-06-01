package com.fpm2025.notification_service.repository;

import com.fpm2025.notification_service.entity.NotificationHistoryEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface NotificationHistoryRepository extends JpaRepository<NotificationHistoryEntity, Long> {

    Page<NotificationHistoryEntity> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    Page<NotificationHistoryEntity> findByUserIdAndIsReadFalseOrderByCreatedAtDesc(Long userId, Pageable pageable);

    long countByUserIdAndIsReadFalse(Long userId);

    @Modifying
    @Query("UPDATE NotificationHistoryEntity n SET n.isRead = true, n.readAt = CURRENT_TIMESTAMP WHERE n.userId = :userId AND n.isRead = false")
    int markAllAsRead(@Param("userId") Long userId);
}
