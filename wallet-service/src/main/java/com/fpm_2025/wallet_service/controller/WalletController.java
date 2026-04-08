package com.fpm_2025.wallet_service.controller;

import com.fpm2025.domain.common.BaseResponse;
import com.fpm_2025.wallet_service.dto.payload.request.CreateWalletRequest;
import com.fpm_2025.wallet_service.dto.payload.request.UpdateWalletRequest;
import com.fpm_2025.wallet_service.dto.payload.response.*;
import com.fpm_2025.wallet_service.dto.payload.response.WalletResponse;
import com.fpm_2025.wallet_service.entity.enums.WalletType;
import com.fpm_2025.wallet_service.service.WalletService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/wallets")
@RequiredArgsConstructor
@Tag(name = "Wallet", description = "Wallet management APIs")
public class WalletController {
	
	@Autowired
	private WalletService walletService;

    @Autowired
    private com.fpm_2025.wallet_service.grpc.client.UserGrpcClient userGrpcClient;

	@PostMapping
	@Operation(summary = "Create new wallet")
	public ResponseEntity<BaseResponse<WalletResponse>> createWallet(@Valid @RequestBody CreateWalletRequest request,
			@AuthenticationPrincipal Long userId) {
		WalletResponse wallet = walletService.createWallet(request, userId);
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(BaseResponse.success(wallet, "Wallet created successfully"));
	}

	@GetMapping
	@Operation(summary = "Get all user wallets")
	public ResponseEntity<BaseResponse<List<WalletResponse>>> getUserWallets(@AuthenticationPrincipal Long userId) {
		List<WalletResponse> wallets = walletService.getUserWallets(userId);
		return ResponseEntity.ok(BaseResponse.success(wallets, "Wallets retrieved successfully"));
	}

	@GetMapping("/active")
	@Operation(summary = "Get active wallets")
	public ResponseEntity<BaseResponse<List<WalletResponse>>> getActiveWallets(@AuthenticationPrincipal Long userId) {
		List<WalletResponse> wallets = walletService.getUserActiveWallets(userId);
		return ResponseEntity.ok(BaseResponse.success(wallets, "Active wallets retrieved successfully"));
	}

	@GetMapping("/type/{type}")
	@Operation(summary = "Get wallets by type")
	public ResponseEntity<BaseResponse<List<WalletResponse>>> getWalletsByType(@PathVariable WalletType type,
			@AuthenticationPrincipal Long userId) {
		List<WalletResponse> wallets = walletService.getUserWalletsByType(userId, type);
		return ResponseEntity.ok(BaseResponse.success(wallets, "Wallets retrieved successfully"));
	}

	@GetMapping("/shared")
	@Operation(summary = "Get wallets shared with the user")
	public ResponseEntity<BaseResponse<List<WalletResponse>>> getSharedWallets(@AuthenticationPrincipal Long userId) {
		List<WalletResponse> wallets = walletService.getSharedWallets(userId);
		return ResponseEntity.ok(BaseResponse.success(wallets, "Shared wallets retrieved successfully"));
	}

	@GetMapping("/{id}")
	@Operation(summary = "Get wallet by ID")
	public ResponseEntity<BaseResponse<WalletResponse>> getWalletById(@PathVariable Long id,
			@AuthenticationPrincipal Long userId) {
		WalletResponse wallet = walletService.getWalletById(id, userId);
		return ResponseEntity.ok(BaseResponse.success(wallet, "Wallet retrieved successfully"));
	}

	@PatchMapping("/{id}/toggle")
	@Operation(summary = "Toggle wallet active status")
	public ResponseEntity<BaseResponse<WalletResponse>> toggleWallet(@PathVariable Long id,
			@AuthenticationPrincipal Long userId) {
		WalletResponse wallet = walletService.toggleWallet(id, userId);
		return ResponseEntity.ok(BaseResponse.success(wallet, "Wallet status toggled successfully"));
	}

	@PutMapping("/{id}")
	@Operation(summary = "Update wallet")
	public ResponseEntity<BaseResponse<WalletResponse>> updateWallet(@PathVariable Long id,
			@Valid @RequestBody UpdateWalletRequest request, @AuthenticationPrincipal Long userId) {
		WalletResponse wallet = walletService.updateWallet(id, request, userId);
		return ResponseEntity.ok(BaseResponse.success(wallet, "Wallet updated successfully"));
	}

	@DeleteMapping("/{id}")
	@Operation(summary = "Delete wallet")
	public ResponseEntity<BaseResponse<Void>> deleteWallet(@PathVariable Long id,
			@AuthenticationPrincipal Long userId) {
		walletService.deleteWallet(id, userId);
		return ResponseEntity.ok(BaseResponse.success(null, "Wallet deleted successfully"));
	}

	@GetMapping("/total-balance")
	@Operation(summary = "Get total balance across all wallets")
	public ResponseEntity<BaseResponse<BigDecimal>> getTotalBalance(@AuthenticationPrincipal Long userId) {
		BigDecimal total = walletService.getTotalBalance(userId);
		return ResponseEntity.ok(BaseResponse.success(total, "Total balance calculated successfully"));
	}

	@GetMapping("/count")
	@Operation(summary = "Get wallet count")
	public ResponseEntity<BaseResponse<Long>> getWalletCount(@AuthenticationPrincipal Long userId) {
		long count = walletService.getUserWalletCount(userId);
		return ResponseEntity.ok(BaseResponse.success(count, "Wallet count retrieved successfully"));
	}

	@PostMapping("/{id}/share")
	@Operation(summary = "Share wallet with a user")
	public ResponseEntity<BaseResponse<com.fpm_2025.wallet_service.dto.payload.response.WalletPermissionResponse>> shareWallet(
		@PathVariable Long id,
		@Valid @RequestBody com.fpm_2025.wallet_service.dto.payload.request.ShareWalletRequest request,
		@AuthenticationPrincipal Long userId) {
		com.fpm_2025.wallet_service.dto.payload.response.WalletPermissionResponse res = walletService.shareWallet(id, request, userId);
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(BaseResponse.success(res, "Wallet shared successfully"));
	}

	@GetMapping("/{id}/shares")
	@Operation(summary = "Get all users who have access to this wallet")
	public ResponseEntity<BaseResponse<List<com.fpm_2025.wallet_service.dto.payload.response.WalletPermissionResponse>>> getSharedUsers(
		@PathVariable Long id,
		@AuthenticationPrincipal Long userId) {
		List<com.fpm_2025.wallet_service.dto.payload.response.WalletPermissionResponse> shares = walletService.getSharedUsers(id, userId);
		return ResponseEntity.ok(BaseResponse.success(shares, "Shared users retrieved successfully"));
	}

	@DeleteMapping("/{id}/share/{targetUserId}")
	@Operation(summary = "Remove user access from wallet")
	public ResponseEntity<BaseResponse<Void>> removeShare(
		@PathVariable Long id,
		@PathVariable Long targetUserId,
		@AuthenticationPrincipal Long userId) {
		walletService.removeShare(id, targetUserId, userId);
		return ResponseEntity.ok(BaseResponse.success(null, "Wallet share removed successfully"));
	}

    @GetMapping("/family/{familyId}")
    @Operation(summary = "Get all wallets for a specific family")
    public ResponseEntity<BaseResponse<List<WalletResponse>>> getFamilyWallets(
            @PathVariable Long familyId,
            @AuthenticationPrincipal Long userId) {
        
        // 1️ Kiểm tra xem user có thuộc family này không (qua gRPC gọi user-auth-service)
        if (!userGrpcClient.isUserInFamily(userId, familyId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new BaseResponse<>(403, "You are not a member of this family", null));
        }

        List<WalletResponse> wallets = walletService.getFamilyWallets(familyId);
        return ResponseEntity.ok(BaseResponse.success(wallets, "Family wallets retrieved successfully"));
    }
}
