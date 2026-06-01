package com.fpm2025.user_auth_service.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.fpm2025.user_auth_service.entity.UserPreferencesEntity;

@Repository
public interface UserPreferencesRepository extends JpaRepository<UserPreferencesEntity, Long> {
    Optional<UserPreferencesEntity> findByUserId(Long userId);
}
