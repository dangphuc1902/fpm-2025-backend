package com.fpm2025.user_auth_service.repository;

import com.fpm2025.user_auth_service.entity.FamilyEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FamilyRepository extends JpaRepository<FamilyEntity, Long> {
}
