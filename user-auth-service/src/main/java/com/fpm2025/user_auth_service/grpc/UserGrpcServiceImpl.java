package com.fpm2025.user_auth_service.grpc;

import com.fpm2025.protocol.user.*;
import com.fpm2025.user_auth_service.entity.FamilyEntity;
import com.fpm2025.user_auth_service.entity.FamilyMemberEntity;
import com.fpm2025.user_auth_service.entity.UserEntity;
import com.fpm2025.user_auth_service.repository.FamilyMemberRepository;
import com.fpm2025.user_auth_service.repository.FamilyRepository;
import com.fpm2025.user_auth_service.repository.UserRepository;
import com.fpm2025.security.jwt.JwtTokenProvider;
import org.springframework.beans.factory.annotation.Autowired;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.grpc.server.service.GrpcService;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * gRPC Server Implementation: UserGrpcService
 *
 * Được gọi bởi:
 * - API Gateway    → ValidateToken (mỗi request)
 * - Wallet Service → GetUserById (khi tạo ví)
 * - Reporting      → GetFamilyMembers (báo cáo family)
 */
@GrpcService
@RequiredArgsConstructor
@Slf4j
public class UserGrpcServiceImpl extends UserGrpcServiceGrpc.UserGrpcServiceImplBase {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FamilyRepository familyRepository;

    @Autowired
    private FamilyMemberRepository familyMemberRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_DATE_TIME;

    // =========================================================================
    // 1. ValidateToken — API Gateway gọi trên MỌI request để xác thực JWT
    // =========================================================================

    @Override
    public void validateToken(TokenRequest request,
                              StreamObserver<TokenValidationResponse> responseObserver) {
        log.info("gRPC: ValidateToken called");
        try {
            String token = request.getToken();

            if (token == null || token.isBlank()) {
                responseObserver.onNext(TokenValidationResponse.newBuilder()
                        .setValid(false)
                        .setMessage("Token is empty")
                        .build());
                responseObserver.onCompleted();
                return;
            }

            // Kiểm tra token hợp lệ
            if (!jwtTokenProvider.validateToken(token)) {
                responseObserver.onNext(TokenValidationResponse.newBuilder()
                        .setValid(false)
                        .setMessage("Token is expired or invalid")
                        .build());
                responseObserver.onCompleted();
                return;
            }

            Long userId = jwtTokenProvider.extractUserId(token);
            String email = jwtTokenProvider.extractEmail(token);

            // Verify user tồn tại trong DB
            boolean userExists = userRepository.existsByEmail(email);
            if (!userExists) {
                responseObserver.onNext(TokenValidationResponse.newBuilder()
                        .setValid(false)
                        .setMessage("User not found")
                        .build());
                responseObserver.onCompleted();
                return;
            }

            responseObserver.onNext(TokenValidationResponse.newBuilder()
                    .setValid(true)
                    .setUserId(userId)
                    .setEmail(email)
                    .setMessage("Token is valid")
                    .build());
            responseObserver.onCompleted();

            log.info("gRPC: Token validated successfully for userId: {}", userId);

        } catch (Exception e) {
            log.error("gRPC: ValidateToken failed", e);
            responseObserver.onNext(TokenValidationResponse.newBuilder()
                    .setValid(false)
                    .setMessage("Validation error: " + e.getMessage())
                    .build());
            responseObserver.onCompleted();
        }
    }

    // =========================================================================
    // 2. GetUserById — Các service khác lấy thông tin user
    // =========================================================================

    @Override
    public void getUserById(UserIdRequest request,
                            StreamObserver<UserResponse> responseObserver) {
        log.info("gRPC: GetUserById called for userId: {}", request.getUserId());
        try {
            UserEntity user = userRepository.findById((int) request.getUserId())
                    .orElseThrow(() -> new RuntimeException("User not found: " + request.getUserId()));

            responseObserver.onNext(toUserResponse(user));
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("gRPC: GetUserById failed for id: {}", request.getUserId(), e);
            responseObserver.onError(Status.NOT_FOUND
                    .withDescription(e.getMessage())
                    .asRuntimeException());
        }
    }

    // =========================================================================
    // 3. GetUsersByIds — Lấy nhiều user (dùng cho Family dashboard)
    // =========================================================================

    @Override
    public void getUsersByIds(UserIdsRequest request,
                              StreamObserver<UsersResponse> responseObserver) {
        log.info("gRPC: GetUsersByIds called for {} users", request.getUserIdsCount());
        try {
            UsersResponse.Builder builder = UsersResponse.newBuilder();

            for (long userId : request.getUserIdsList()) {
                userRepository.findById((int) userId).ifPresent(user ->
                        builder.addUsers(toUserResponse(user)));
            }

            responseObserver.onNext(builder.build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("gRPC: GetUsersByIds failed", e);
            responseObserver.onError(Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
        }
    }

    // =========================================================================
    // 4. GetFamilyMembers — Reporting Service cần danh sách user trong family
    // =========================================================================

    @Override
    public void getFamilyMembers(FamilyIdRequest request,
                                 StreamObserver<FamilyMembersResponse> responseObserver) {
        log.info("gRPC: GetFamilyMembers called for familyId: {}", request.getFamilyId());
        try {
            FamilyEntity family = familyRepository.findById(request.getFamilyId())
                    .orElseThrow(() -> new RuntimeException("Family not found: " + request.getFamilyId()));

            List<FamilyMember> members = family.getMembers().stream()
                    .map(this::toFamilyMemberProto)
                    .collect(Collectors.toList());

            FamilyMembersResponse response = FamilyMembersResponse.newBuilder()
                    .setFamilyId(family.getId())
                    .setFamilyName(family.getName())
                    .addAllMembers(members)
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("gRPC: GetFamilyMembers failed for familyId: {}", request.getFamilyId(), e);
            responseObserver.onError(Status.NOT_FOUND.withDescription(e.getMessage()).asRuntimeException());
        }
    }

    // =========================================================================
    // 5. CheckUserExists
    // =========================================================================

    @Override
    public void checkUserExists(UserIdRequest request,
                                StreamObserver<UserExistsResponse> responseObserver) {
        log.info("gRPC: CheckUserExists called for userId: {}", request.getUserId());
        try {
            boolean exists = userRepository.existsById((int) request.getUserId());
            responseObserver.onNext(UserExistsResponse.newBuilder().setExists(exists).build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("gRPC: CheckUserExists failed", e);
            responseObserver.onError(Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
        }
    }

    // =========================================================================
    // Private Mappers
    // =========================================================================

    private UserResponse toUserResponse(UserEntity user) {
        return UserResponse.newBuilder()
                .setId(user.getId() != null ? user.getId() : 0)
                .setEmail(user.getEmail() != null ? user.getEmail() : "")
                .setFullName(user.getUsername() != null ? user.getUsername() : "")
                .setAvatarUrl(user.getAvatarUrl() != null ? user.getAvatarUrl() : "")
                .setPhone(user.getPhoneNumber() != null ? user.getPhoneNumber() : "")
                .setRole(user.getRole() != null ? user.getRole() : "USER")
                .setIsActive(user.getIsActive() != null ? user.getIsActive() : true)
                .setCreatedAt(user.getCreatedAt() != null
                        ? user.getCreatedAt().format(ISO_FORMATTER) : "")
                .build();
    }

    private FamilyMember toFamilyMemberProto(FamilyMemberEntity member) {
        UserEntity user = member.getUser();
        return FamilyMember.newBuilder()
                .setUserId(user.getId() != null ? user.getId() : 0)
                .setFullName(user.getUsername() != null ? user.getUsername() : "")
                .setAvatarUrl(user.getAvatarUrl() != null ? user.getAvatarUrl() : "")
                .setRole(member.getRole() != null ? member.getRole().name() : "MEMBER")
                .setJoinedAt(member.getJoinedAt() != null
                        ? member.getJoinedAt().format(ISO_FORMATTER) : "")
                .build();
    }
}
