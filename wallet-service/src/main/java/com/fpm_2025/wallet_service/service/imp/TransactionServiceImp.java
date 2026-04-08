package com.fpm_2025.wallet_service.service.imp;

import com.fpm2025.domain.dto.request.TransactionRequest;
import com.fpm2025.domain.dto.request.UpdateTransactionRequest;
import com.fpm2025.domain.dto.response.TransactionResponse;
import com.fpm2025.domain.enums.CategoryType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface TransactionServiceImp {
    TransactionResponse createTransaction(TransactionRequest request, Long userId);
    TransactionResponse updateTransaction(Long transactionId, UpdateTransactionRequest request, Long userId);
    void deleteTransaction(Long transactionId, Long userId);
    Page<TransactionResponse> getUserTransactions(Long userId, Pageable pageable);
    Page<TransactionResponse> getUserTransactionsByType(Long userId, CategoryType type, Pageable pageable);
    TransactionResponse getTransactionById(Long transactionId, Long userId);
    List<TransactionResponse> getTransactionsByDateRange(Long userId, LocalDateTime startDate, LocalDateTime endDate);
    Page<TransactionResponse> getWalletTransactions(Long walletId, Long userId, Pageable pageable);
    List<TransactionResponse> getCategoryTransactions(Long categoryId, Long userId);
    BigDecimal getTotalAmount(Long userId, CategoryType type, LocalDateTime startDate, LocalDateTime endDate);
    long getUserTransactionCount(Long userId);
}
