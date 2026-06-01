package com.fpm2025.user_auth_service.repository;

import java.util.Optional;
import java.util.UUID;

import com.fpm2025.user_auth_service.entity.UserEntity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, Integer> {
	Optional<UserEntity> findByEmail(String email);
	boolean existsByEmail(String email);
}
