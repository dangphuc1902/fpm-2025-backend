package com.fpm2025.user_auth_service.service.imp;

import com.fpm2025.user_auth_service.entity.FamilyEntity;
import com.fpm2025.user_auth_service.entity.FamilyMemberEntity;
import com.fpm2025.user_auth_service.entity.FamilyRole;
import com.fpm2025.user_auth_service.entity.UserEntity;
import com.fpm2025.user_auth_service.payload.request.CreateFamilyRequest;
import com.fpm2025.user_auth_service.payload.request.InviteMemberRequest;
import com.fpm2025.user_auth_service.payload.response.FamilyMemberResponse;
import com.fpm2025.user_auth_service.payload.response.FamilyResponse;
import com.fpm2025.user_auth_service.repository.FamilyMemberRepository;
import com.fpm2025.user_auth_service.repository.FamilyRepository;
import com.fpm2025.user_auth_service.repository.UserRepository;
import com.fpm2025.user_auth_service.service.FamilyService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FamilyServiceImpl implements FamilyService {

    private final FamilyRepository familyRepository;
    private final FamilyMemberRepository familyMemberRepository;
    private final UserRepository userRepository;
    private final com.fpm2025.user_auth_service.repository.FamilyInvitationRepository familyInvitationRepository;

    @Override
    @Transactional
    public FamilyResponse createFamily(Long userId, CreateFamilyRequest request) {
        UserEntity user = userRepository.findById(userId.intValue())
                .orElseThrow(() -> new RuntimeException("User not found"));

        FamilyEntity family = FamilyEntity.builder()
                .name(request.getName())
                .description(request.getDescription())
                .build();
        family = familyRepository.save(family);

        FamilyMemberEntity ownerMember = FamilyMemberEntity.builder()
                .family(family)
                .user(user)
                .role(FamilyRole.OWNER)
                .build();
        familyMemberRepository.save(ownerMember);

        return FamilyResponse.builder()
                .id(family.getId())
                .name(family.getName())
                .description(family.getDescription())
                .createdAt(family.getCreatedAt())
                .currentUserRole(FamilyRole.OWNER.name())
                .build();
    }

    @Override
    public List<FamilyMemberResponse> getFamilyMembers(Long familyId, Long userId) {
        // Validate user belongs to family
        if (!familyMemberRepository.existsByFamilyIdAndUserId(familyId, userId)) {
            throw new RuntimeException("You do not have permission to view members of this family");
        }

        List<FamilyMemberEntity> members = familyMemberRepository.findByFamilyId(familyId);
        return members.stream().map(m -> FamilyMemberResponse.builder()
                .userId(m.getUser().getId())
                .email(m.getUser().getEmail())
                .username(m.getUser().getUsername())
                .avatarUrl(m.getUser().getAvatarUrl())
                .role(m.getRole().name())
                .joinedAt(m.getJoinedAt())
                .build()
        ).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public FamilyMemberResponse inviteMember(Long familyId, Long inviterId, InviteMemberRequest request) {
        FamilyEntity family = familyRepository.findById(familyId)
                .orElseThrow(() -> new RuntimeException("Family not found"));

        FamilyMemberEntity inviter = familyMemberRepository.findByFamilyIdAndUserId(familyId, inviterId)
                .orElseThrow(() -> new RuntimeException("You are not a member of this family"));
                
        if (inviter.getRole() != FamilyRole.OWNER && inviter.getRole() != FamilyRole.ADMIN) {
            throw new RuntimeException("Only Owners and Admins can invite new members");
        }

        // Thay vì add trực tiếp, tạo lời mời
        com.fpm2025.user_auth_service.entity.FamilyInvitationEntity invitation = com.fpm2025.user_auth_service.entity.FamilyInvitationEntity.builder()
                .family(family)
                .inviterId(inviterId)
                .inviteeEmail(request.getEmail())
                .role(FamilyRole.valueOf(request.getRole() != null ? request.getRole() : "MEMBER"))
                .status("PENDING")
                .build();
        
        familyInvitationRepository.save(invitation);
        
        // TODO: Gửi Kafka event cho notification-service để báo cho user
        
        return FamilyMemberResponse.builder()
                .email(request.getEmail())
                .role(invitation.getRole().name())
                .build();
    }

    @Override
    @Transactional
    public void acceptInvitation(Long invitationId, String userEmail) {
        com.fpm2025.user_auth_service.entity.FamilyInvitationEntity invitation = familyInvitationRepository.findByIdAndInviteeEmail(invitationId, userEmail)
                .orElseThrow(() -> new RuntimeException("Invitation not found or not for you"));
        
        if (!"PENDING".equals(invitation.getStatus())) {
            throw new RuntimeException("Invitation is already " + invitation.getStatus());
        }

        UserEntity user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found. Please register first."));

        // Add to family
        FamilyMemberEntity newMember = FamilyMemberEntity.builder()
                .family(invitation.getFamily())
                .user(user)
                .role(invitation.getRole())
                .joinedAt(LocalDateTime.now())
                .build();
        
        familyMemberRepository.save(newMember);

        // Update invitation status
        invitation.setStatus("ACCEPTED");
        invitation.setRespondedAt(LocalDateTime.now());
        familyInvitationRepository.save(invitation);
    }

    @Override
    @Transactional
    public void rejectInvitation(Long invitationId, String userEmail) {
        com.fpm2025.user_auth_service.entity.FamilyInvitationEntity invitation = familyInvitationRepository.findByIdAndInviteeEmail(invitationId, userEmail)
                .orElseThrow(() -> new RuntimeException("Invitation not found or not for you"));
        
        invitation.setStatus("REJECTED");
        invitation.setRespondedAt(LocalDateTime.now());
        familyInvitationRepository.save(invitation);
    }

    @Override
    public List<com.fpm2025.user_auth_service.entity.FamilyInvitationEntity> getUserInvitations(String userEmail) {
        return familyInvitationRepository.findByInviteeEmailAndStatus(userEmail, "PENDING");
    }

    @Override
    public List<FamilyResponse> getUserFamilies(Long userId) {
        List<FamilyMemberEntity> memberships = familyMemberRepository.findByUserId(userId);
        
        return memberships.stream().map(m -> FamilyResponse.builder()
                .id(m.getFamily().getId())
                .name(m.getFamily().getName())
                .description(m.getFamily().getDescription())
                .createdAt(m.getFamily().getCreatedAt())
                .currentUserRole(m.getRole().name())
                .build()
        ).collect(Collectors.toList());
    }
}
