package com.fpm2025.user_auth_service.controller;

import com.fpm2025.user_auth_service.payload.request.CreateFamilyRequest;
import com.fpm2025.user_auth_service.payload.request.InviteMemberRequest;
import com.fpm2025.user_auth_service.payload.response.BaseResponse;
import com.fpm2025.user_auth_service.payload.response.FamilyMemberResponse;
import com.fpm2025.user_auth_service.payload.response.FamilyResponse;
import com.fpm2025.user_auth_service.service.FamilyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/families")
@RequiredArgsConstructor
@Tag(name = "Family", description = "Family group management APIs")
public class FamilyController {

    private final FamilyService familyService;

    @PostMapping
    @Operation(summary = "Create a new family group")
    public ResponseEntity<BaseResponse<FamilyResponse>> createFamily(
            @RequestHeader("X-User-Id") String userIdStr,
            @Valid @RequestBody CreateFamilyRequest request) {
        
        Long userId = Long.parseLong(userIdStr);
        FamilyResponse response = familyService.createFamily(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(BaseResponse.success(response, "Family created successfully"));
    }

    @GetMapping
    @Operation(summary = "Get all families for current user")
    public ResponseEntity<BaseResponse<List<FamilyResponse>>> getUserFamilies(
            @RequestHeader("X-User-Id") String userIdStr) {
        
        Long userId = Long.parseLong(userIdStr);
        List<FamilyResponse> response = familyService.getUserFamilies(userId);
        return ResponseEntity.ok(BaseResponse.success(response, "Families retrieved successfully"));
    }

    @GetMapping("/{familyId}/members")
    @Operation(summary = "Get all members of a family")
    public ResponseEntity<BaseResponse<List<FamilyMemberResponse>>> getFamilyMembers(
            @RequestHeader("X-User-Id") String userIdStr,
            @PathVariable Long familyId) {
        
        Long userId = Long.parseLong(userIdStr);
        List<FamilyMemberResponse> response = familyService.getFamilyMembers(familyId, userId);
        return ResponseEntity.ok(BaseResponse.success(response, "Family members retrieved"));
    }

    @PostMapping("/{familyId}/invite")
    @Operation(summary = "Invite a member to the family")
    public ResponseEntity<BaseResponse<FamilyMemberResponse>> inviteMember(
            @RequestHeader("X-User-Id") String userIdStr,
            @PathVariable Long familyId,
            @Valid @RequestBody InviteMemberRequest request) {
        
        Long userId = Long.parseLong(userIdStr);
        FamilyMemberResponse response = familyService.inviteMember(familyId, userId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(BaseResponse.success(response, "Member invited successfully"));
    }
}
