package com.fpm_2025.wallet_service.controller;

import com.fpm_2025.wallet_service.dto.request.CreateWalletRequest;
import com.fpm_2025.wallet_service.dto.request.UpdateWalletRequest;
import com.fpm_2025.wallet_service.dto.response.BaseResponse;
import com.fpm_2025.wallet_service.dto.response.WalletResponse;
import com.fpm_2025.wallet_service.enums.WalletType;
import com.fpm_2025.wallet_service.service.WalletService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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

	private final WalletService walletService;

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

	@GetMapping("/{id}")
	@Operation(summary = "Get wallet by ID")
	public ResponseEntity<BaseResponse<WalletResponse>> getWalletById(@PathVariable Long id,
			@AuthenticationPrincipal Long userId) {
		WalletResponse wallet = walletService.getWalletById(id, userId);
		return ResponseEntity.ok(BaseResponse.success(wallet, "Wallet retrieved successfully"));
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
}
