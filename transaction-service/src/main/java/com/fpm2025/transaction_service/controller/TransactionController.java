package com.fpm2025.transaction_service.controller;

import com.fpm2025.transaction_service.dto.BaseResponse;
import com.fpm2025.transaction_service.dto.TransactionRequest;
import com.fpm2025.transaction_service.dto.TransactionResponse;
import com.fpm2025.transaction_service.dto.UpdateTransactionRequest;
import com.fpm2025.transaction_service.dto.BankNotificationRequest;
import com.fpm2025.transaction_service.entity.enums.TransactionType;
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

    // =====================================================================
    // POST /api/v1/transactions — Tạo giao dịch mới
    // =====================================================================
    @PostMapping
    @Operation(summary = "Create a new transaction (gRPC→Wallet, Kafka, RabbitMQ)")
    public ResponseEntity<BaseResponse<TransactionResponse>> createTransaction(
            @Valid @RequestBody TransactionRequest request,
            @AuthenticationPrincipal Long userId) {

        TransactionResponse response = transactionService.createTransaction(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(BaseResponse.success(response, "Transaction created successfully"));
    }

    // =====================================================================
    // POST /api/v1/transactions/notification — Từ bank notification
    // =====================================================================
    @PostMapping("/notification")
    @Operation(summary = "Automatically process a parsed bank notification")
    public ResponseEntity<BaseResponse<TransactionResponse>> processNotification(
            @Valid @RequestBody com.fpm2025.transaction_service.dto.BankNotificationRequest request,
            @AuthenticationPrincipal Long userId) {

        TransactionResponse response = transactionService.processBankNotification(userId, request);
        return ResponseEntity.ok(BaseResponse.success(response, "Automated transaction created from notification"));
    }

    // =====================================================================
    // GET /api/v1/transactions — Danh sách giao dịch (filter, search, paginate)
    // =====================================================================
    @GetMapping
    @Operation(summary = "List transactions for current user with optional filters")
    public ResponseEntity<BaseResponse<Page<TransactionResponse>>> listTransactions(
            @AuthenticationPrincipal Long userId,
            @Parameter(description = "Filter by walletId") @RequestParam(required = false) Long walletId,
            @Parameter(description = "Filter by categoryId") @RequestParam(required = false) Long categoryId,
            @Parameter(description = "Filter by type: INCOME | EXPENSE") @RequestParam(required = false) TransactionType type,
            @Parameter(description = "Start date (yyyy-MM-dd'T'HH:mm:ss)") @RequestParam(required = false)
                @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @Parameter(description = "End date (yyyy-MM-dd'T'HH:mm:ss)") @RequestParam(required = false)
                @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Page<TransactionResponse> result = transactionService.listTransactions(
                userId, walletId, categoryId, type, startDate, endDate, page, size);
        return ResponseEntity.ok(BaseResponse.success(result, "Transactions retrieved successfully"));
    }

    // =====================================================================
    // GET /api/v1/transactions/wallet/{walletId} — Danh sách theo ví
    // =====================================================================
    @GetMapping("/wallet/{walletId}")
    @Operation(summary = "Get transactions for a specific wallet (paged)")
    public ResponseEntity<BaseResponse<Page<TransactionResponse>>> getTransactionsByWallet(
            @PathVariable Long walletId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal Long userId) {

        Page<TransactionResponse> response = transactionService.getTransactionsByWallet(userId, walletId, page, size);
        return ResponseEntity.ok(BaseResponse.success(response, "Transactions retrieved successfully"));
    }

    // =====================================================================
    // GET /api/v1/transactions/{id} — Chi tiết giao dịch
    // =====================================================================
    @GetMapping("/{id}")
    @Operation(summary = "Get a specific transaction by ID")
    public ResponseEntity<BaseResponse<TransactionResponse>> getTransaction(
            @PathVariable Long id,
            @AuthenticationPrincipal Long userId) {

        TransactionResponse response = transactionService.getTransaction(userId, id);
        return ResponseEntity.ok(BaseResponse.success(response, "Transaction retrieved successfully"));
    }

    // =====================================================================
    // PUT /api/v1/transactions/{id} — Cập nhật giao dịch
    // =====================================================================
    @PutMapping("/{id}")
    @Operation(summary = "Update a transaction (recalculates wallet balance if amount/type changed)")
    public ResponseEntity<BaseResponse<TransactionResponse>> updateTransaction(
            @PathVariable Long id,
            @Valid @RequestBody UpdateTransactionRequest request,
            @AuthenticationPrincipal Long userId) {

        TransactionResponse response = transactionService.updateTransaction(userId, id, request);
        return ResponseEntity.ok(BaseResponse.success(response, "Transaction updated successfully"));
    }

    // =====================================================================
    // DELETE /api/v1/transactions/{id} — Xóa giao dịch + revert balance
    // =====================================================================
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a transaction and revert wallet balance")
    public ResponseEntity<BaseResponse<Void>> deleteTransaction(
            @PathVariable Long id,
            @AuthenticationPrincipal Long userId) {

        transactionService.deleteTransaction(userId, id);
        return ResponseEntity.ok(BaseResponse.success(null, "Transaction deleted successfully"));
    }

    // =====================================================================
    // ATTACHMENTS
    // =====================================================================

    @PostMapping("/{id}/attachments")
    @Operation(summary = "Upload an attachment (receipt/image) for a transaction")
    public ResponseEntity<BaseResponse<com.fpm2025.transaction_service.entity.TransactionAttachmentEntity>> uploadAttachment(
            @PathVariable Long id,
            @RequestParam("file") org.springframework.web.multipart.MultipartFile file,
            @AuthenticationPrincipal Long userId) {

        com.fpm2025.transaction_service.entity.TransactionAttachmentEntity response = 
                transactionService.uploadAttachment(userId, id, file);
        return ResponseEntity.ok(BaseResponse.success(response, "Attachment uploaded successfully"));
    }

    @DeleteMapping("/{id}/attachments/{attachmentId}")
    @Operation(summary = "Delete a transaction attachment")
    public ResponseEntity<BaseResponse<Void>> deleteAttachment(
            @PathVariable Long id,
            @PathVariable Long attachmentId,
            @AuthenticationPrincipal Long userId) {

        transactionService.deleteAttachment(userId, id, attachmentId);
        return ResponseEntity.ok(BaseResponse.success(null, "Attachment deleted successfully"));
    }
}
