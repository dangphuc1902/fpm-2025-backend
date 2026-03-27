package com.fpm2025.transaction_service.controller;

import com.fpm2025.transaction_service.dto.BaseResponse;
import com.fpm2025.transaction_service.dto.TransactionRequest;
import com.fpm2025.transaction_service.dto.TransactionResponse;
import com.fpm2025.transaction_service.service.TransactionService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
@Tag(name = "Transaction", description = "Transaction management APIs")
public class TransactionController {

    private final TransactionService transactionService;

    @PostMapping
    @Operation(summary = "Create a new transaction")
    public ResponseEntity<BaseResponse<TransactionResponse>> createTransaction(
            @Valid @RequestBody TransactionRequest request,
            @AuthenticationPrincipal Long userId) {
        TransactionResponse response = transactionService.createTransaction(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(BaseResponse.success(response, "Transaction created successfully"));
    }

    @GetMapping("/wallet/{walletId}")
    @Operation(summary = "Get transactions for a specific wallet")
    public ResponseEntity<BaseResponse<Page<TransactionResponse>>> getTransactionsByWallet(
            @PathVariable Long walletId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal Long userId) {
        Page<TransactionResponse> response = transactionService.getTransactionsByWallet(userId, walletId, page, size);
        return ResponseEntity.ok(BaseResponse.success(response, "Transactions retrieved successfully"));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a specific transaction by ID")
    public ResponseEntity<BaseResponse<TransactionResponse>> getTransaction(
            @PathVariable Long id,
            @AuthenticationPrincipal Long userId) {
        TransactionResponse response = transactionService.getTransaction(userId, id);
        return ResponseEntity.ok(BaseResponse.success(response, "Transaction retrieved successfully"));
    }
}
