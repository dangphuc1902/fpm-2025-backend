package com.fpm2025.user_auth_service.repository;

import com.fpm2025.user_auth_service.entity.FamilyMemberEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FamilyMemberRepository extends JpaRepository<FamilyMemberEntity, Long> {
    List<FamilyMemberEntity> findByFamilyId(Long familyId);
    List<FamilyMemberEntity> findByUserId(Long userId);
    boolean existsByFamilyIdAndUserId(Long familyId, Long userId);
    java.util.Optional<FamilyMemberEntity> findByFamilyIdAndUserId(Long familyId, Long userId);
}
