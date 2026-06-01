package com.fpm2025.user_auth_service.repository;

import com.fpm2025.user_auth_service.entity.FamilyInvitationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FamilyInvitationRepository extends JpaRepository<FamilyInvitationEntity, Long> {
    List<FamilyInvitationEntity> findByInviteeEmailAndStatus(String email, String status);
    Optional<FamilyInvitationEntity> findByIdAndInviteeEmail(Long id, String email);
}
