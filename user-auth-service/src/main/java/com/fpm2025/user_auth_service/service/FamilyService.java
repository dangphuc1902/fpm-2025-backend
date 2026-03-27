package com.fpm2025.user_auth_service.service;

import com.fpm2025.user_auth_service.payload.request.CreateFamilyRequest;
import com.fpm2025.user_auth_service.payload.request.InviteMemberRequest;
import com.fpm2025.user_auth_service.payload.response.FamilyMemberResponse;
import com.fpm2025.user_auth_service.payload.response.FamilyResponse;

import java.util.List;

public interface FamilyService {
    FamilyResponse createFamily(Long userId, CreateFamilyRequest request);
    List<FamilyMemberResponse> getFamilyMembers(Long familyId, Long userId);
    FamilyMemberResponse inviteMember(Long familyId, Long inviterId, InviteMemberRequest request);
    List<FamilyResponse> getUserFamilies(Long userId);
}
