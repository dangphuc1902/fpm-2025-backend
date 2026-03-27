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
        List<FamilyMemberEntity> currentMembers = familyMemberRepository.findByFamilyId(familyId);
        
        FamilyMemberEntity inviter = currentMembers.stream()
                .filter(m -> m.getUser().getId().equals(inviterId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("You are not a member of this family"));
                
        if (inviter.getRole() != FamilyRole.OWNER && inviter.getRole() != FamilyRole.ADMIN) {
            throw new RuntimeException("Only Owners and Admins can invite new members");
        }

        UserEntity userToInvite = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User with email " + request.getEmail() + " not found. They must register first."));

        if (familyMemberRepository.existsByFamilyIdAndUserId(familyId, userToInvite.getId())) {
            throw new RuntimeException("User is already a member of this family");
        }

        FamilyEntity family = familyRepository.findById(familyId)
                .orElseThrow(() -> new RuntimeException("Family not found"));

        FamilyRole roleToAssign;
        try {
            roleToAssign = FamilyRole.valueOf(request.getRole());
        } catch (IllegalArgumentException e) {
            roleToAssign = FamilyRole.MEMBER;
        }

        if (roleToAssign == FamilyRole.OWNER) {
             throw new RuntimeException("Cannot assign OWNER role via invite");
        }

        FamilyMemberEntity newMember = FamilyMemberEntity.builder()
                .family(family)
                .user(userToInvite)
                .role(roleToAssign)
                .build();
        newMember = familyMemberRepository.save(newMember);

        return FamilyMemberResponse.builder()
                .userId(userToInvite.getId())
                .email(userToInvite.getEmail())
                .username(userToInvite.getUsername())
                .avatarUrl(userToInvite.getAvatarUrl())
                .role(newMember.getRole().name())
                .joinedAt(LocalDateTime.now())
                .build();
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
