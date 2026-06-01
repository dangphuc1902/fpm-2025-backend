package com.fpm2025.transaction_service.controller;

import com.fpm2025.domain.common.BaseResponse;
import com.fpm2025.domain.dto.request.BankNotificationRequest;
import com.fpm2025.domain.dto.request.TransactionRequest;
import com.fpm2025.domain.dto.request.UpdateTransactionRequest;
import com.fpm2025.domain.dto.response.TransactionResponse;
import com.fpm2025.domain.enums.CategoryType;
import com.fpm2025.transaction_service.service.TransactionService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

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

    @PostMapping("/notification")
    @Operation(summary = "Process bank notification")
    public ResponseEntity<BaseResponse<TransactionResponse>> processNotification(
            @Valid @RequestBody BankNotificationRequest request,
            @AuthenticationPrincipal Long userId) {

        TransactionResponse response = transactionService.processBankNotification(userId, request);
        return ResponseEntity.ok(BaseResponse.success(response, "Automated transaction created"));
    }

    @GetMapping
    @Operation(summary = "List transactions")
    public ResponseEntity<BaseResponse<Page<TransactionResponse>>> listTransactions(
            @AuthenticationPrincipal Long userId,
            @RequestParam(required = false) Long walletId,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) CategoryType type,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Page<TransactionResponse> result = transactionService.listTransactions(
                userId, walletId, categoryId, type, startDate, endDate, page, size);
        return ResponseEntity.ok(BaseResponse.success(result, "Transactions retrieved successfully"));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get transaction by ID")
    public ResponseEntity<BaseResponse<TransactionResponse>> getTransaction(
            @PathVariable Long id,
            @AuthenticationPrincipal Long userId) {
        TransactionResponse response = transactionService.getTransaction(userId, id);
        return ResponseEntity.ok(BaseResponse.success(response, "Transaction retrieved successfully"));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update transaction")
    public ResponseEntity<BaseResponse<TransactionResponse>> updateTransaction(
            @PathVariable Long id,
            @Valid @RequestBody UpdateTransactionRequest request,
            @AuthenticationPrincipal Long userId) {
        TransactionResponse response = transactionService.updateTransaction(userId, id, request);
        return ResponseEntity.ok(BaseResponse.success(response, "Transaction updated successfully"));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete transaction")
    public ResponseEntity<BaseResponse<Void>> deleteTransaction(
            @PathVariable Long id,
            @AuthenticationPrincipal Long userId) {
        transactionService.deleteTransaction(userId, id);
        return ResponseEntity.ok(BaseResponse.success(null, "Transaction deleted successfully"));
    }
}
