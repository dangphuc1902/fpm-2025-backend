package com.fpm_2025.wallet_service.controller;

import com.fpm_2025.wallet_service.dto.request.CreateTransactionRequest;
import com.fpm_2025.wallet_service.dto.request.UpdateTransactionRequest;
import com.fpm_2025.wallet_service.dto.response.BaseResponse;
import com.fpm_2025.wallet_service.dto.response.PageResponse;
import com.fpm_2025.wallet_service.dto.response.TransactionResponse;
import com.fpm_2025.wallet_service.enums.CategoryType;
import com.fpm_2025.wallet_service.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
@Tag(name = "Transaction", description = "Transaction management APIs")
public class TransactionController {

 private final TransactionService transactionService;

 @PostMapping
 @Operation(summary = "Create new transaction")
 public ResponseEntity<BaseResponse<TransactionResponse>> createTransaction(
         @Valid @RequestBody CreateTransactionRequest request,
         @AuthenticationPrincipal Long userId) {
     TransactionResponse transaction = transactionService.createTransaction(request, userId);
     return ResponseEntity.status(HttpStatus.CREATED)
         .body(BaseResponse.success(transaction, "Transaction created successfully"));
 }

 @GetMapping
 @Operation(summary = "Get all transactions (paginated)")
 public ResponseEntity<BaseResponse<PageResponse<TransactionResponse>>> getUserTransactions(
         @AuthenticationPrincipal Long userId,
         @RequestParam(defaultValue = "0") int page,
         @RequestParam(defaultValue = "20") int size,
         @RequestParam(defaultValue = "transactionDate") String sortBy,
         @RequestParam(defaultValue = "DESC") Sort.Direction direction) {
     
     Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
     Page<TransactionResponse> transactions = transactionService.getUserTransactions(userId, pageable);
     
     PageResponse<TransactionResponse> pageResponse = PageResponse.<TransactionResponse>builder()
         .content(transactions.getContent())
         .pageNumber(transactions.getNumber())
         .pageSize(transactions.getSize())
         .totalElements(transactions.getTotalElements())
         .totalPages(transactions.getTotalPages())
         .last(transactions.isLast())
         .build();
         
     return ResponseEntity.ok(BaseResponse.success(pageResponse, "Transactions retrieved successfully"));
 }

 @GetMapping("/type/{type}")
 @Operation(summary = "Get transactions by type (paginated)")
 public ResponseEntity<BaseResponse<PageResponse<TransactionResponse>>> getTransactionsByType(
         @PathVariable CategoryType type,
         @AuthenticationPrincipal Long userId,
         @RequestParam(defaultValue = "0") int page,
         @RequestParam(defaultValue = "20") int size) {
     
     Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "transactionDate"));
     Page<TransactionResponse> transactions = transactionService.getUserTransactionsByType(userId, type, pageable);
     
     PageResponse<TransactionResponse> pageResponse = PageResponse.<TransactionResponse>builder()
         .content(transactions.getContent())
         .pageNumber(transactions.getNumber())
         .pageSize(transactions.getSize())
         .totalElements(transactions.getTotalElements())
         .totalPages(transactions.getTotalPages())
         .last(transactions.isLast())
         .build();
         
     return ResponseEntity.ok(BaseResponse.success(pageResponse, "Transactions retrieved successfully"));
 }

 @GetMapping("/{id}")
 @Operation(summary = "Get transaction by ID")
 public ResponseEntity<BaseResponse<TransactionResponse>> getTransactionById(
         @PathVariable Long id,
         @AuthenticationPrincipal Long userId) {
     TransactionResponse transaction = transactionService.getTransactionById(id, userId);
     return ResponseEntity.ok(BaseResponse.success(transaction, "Transaction retrieved successfully"));
 }

 @PutMapping("/{id}")
 @Operation(summary = "Update transaction")
 public ResponseEntity<BaseResponse<TransactionResponse>> updateTransaction(
         @PathVariable Long id,
         @Valid @RequestBody UpdateTransactionRequest request,
         @AuthenticationPrincipal Long userId) {
     TransactionResponse transaction = transactionService.updateTransaction(id, request, userId);
     return ResponseEntity.ok(BaseResponse.success(transaction, "Transaction updated successfully"));
 }

 @DeleteMapping("/{id}")
 @Operation(summary = "Delete transaction")
 public ResponseEntity<BaseResponse<Void>> deleteTransaction(
         @PathVariable Long id,
         @AuthenticationPrincipal Long userId) {
     transactionService.deleteTransaction(id, userId);
     return ResponseEntity.ok(BaseResponse.success(null, "Transaction deleted successfully"));
 }

 @GetMapping("/date-range")
 @Operation(summary = "Get transactions by date range")
 public ResponseEntity<BaseResponse<List<TransactionResponse>>> getTransactionsByDateRange(
         @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
         @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
         @AuthenticationPrincipal Long userId) {
     List<TransactionResponse> transactions = transactionService
         .getTransactionsByDateRange(userId, startDate, endDate);
     return ResponseEntity.ok(BaseResponse.success(transactions, "Transactions retrieved successfully"));
 }

 @GetMapping("/wallet/{walletId}")
 @Operation(summary = "Get wallet transactions (paginated)")
 public ResponseEntity<BaseResponse<PageResponse<TransactionResponse>>> getWalletTransactions(
         @PathVariable Long walletId,
         @AuthenticationPrincipal Long userId,
         @RequestParam(defaultValue = "0") int page,
         @RequestParam(defaultValue = "20") int size) {
     
     Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "transactionDate"));
     Page<TransactionResponse> transactions = transactionService.getWalletTransactions(walletId, userId, pageable);
     
     PageResponse<TransactionResponse> pageResponse = PageResponse.<TransactionResponse>builder()
         .content(transactions.getContent())
         .pageNumber(transactions.getNumber())
         .pageSize(transactions.getSize())
         .totalElements(transactions.getTotalElements())
         .totalPages(transactions.getTotalPages())
         .last(transactions.isLast())
         .build();
         
     return ResponseEntity.ok(BaseResponse.success(pageResponse, "Wallet transactions retrieved successfully"));
 }

 @GetMapping("/category/{categoryId}")
 @Operation(summary = "Get category transactions")
 public ResponseEntity<BaseResponse<List<TransactionResponse>>> getCategoryTransactions(
         @PathVariable Long categoryId,
         @AuthenticationPrincipal Long userId) {
     List<TransactionResponse> transactions = transactionService.getCategoryTransactions(categoryId, userId);
     return ResponseEntity.ok(BaseResponse.success(transactions, "Category transactions retrieved successfully"));
 }

 @GetMapping("/statistics/total")
 @Operation(summary = "Get total amount by type and date range")
 public ResponseEntity<BaseResponse<BigDecimal>> getTotalAmount(
         @RequestParam CategoryType type,
         @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
         @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
         @AuthenticationPrincipal Long userId) {
     BigDecimal total = transactionService.getTotalAmount(userId, type, startDate, endDate);
     return ResponseEntity.ok(BaseResponse.success(total, "Total amount calculated successfully"));
 }

 @GetMapping("/count")
 @Operation(summary = "Get transaction count")
 public ResponseEntity<BaseResponse<Long>> getTransactionCount(
         @AuthenticationPrincipal Long userId) {
     long count = transactionService.getUserTransactionCount(userId);
     return ResponseEntity.ok(BaseResponse.success(count, "Transaction count retrieved successfully"));
 }
}

