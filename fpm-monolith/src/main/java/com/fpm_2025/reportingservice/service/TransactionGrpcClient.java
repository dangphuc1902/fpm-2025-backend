package com.fpm_2025.reportingservice.service;

import com.fpm2025.transaction_service.service.TransactionService;
import com.fpm2025.transaction_service.entity.TransactionEntity;
import com.fpm_2025.reportingservice.domain.TransactionData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionGrpcClient {

    private final TransactionService transactionService;

    /**
     * Lấy danh sách giao dịch theo khoảng thời gian và userId.
     */
    public List<TransactionData> getTransactionsByDateRange(
            Long userId, LocalDateTime startDate, LocalDateTime endDate) {

        log.info("[Local] getTransactionsByDateRange directly: userId={} [{} → {}]", userId, startDate, endDate);

        try {
            List<TransactionEntity> entities = transactionService.findByUserAndDateRange(
                    userId, startDate, endDate, null);

            return entities.stream()
                    .map(this::mapToData)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("[Local] Failed to get transactions directly: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Lấy giao dịch theo walletId (paged).
     */
    public List<TransactionData> getTransactionsByWallet(Long walletId, int page, int size) {
        log.info("[Local] getTransactionsByWallet directly: walletId={}", walletId);
        try {
            var pagedResult = transactionService.findByWalletIdRaw(walletId, page, size);
            return pagedResult.getContent().stream()
                    .map(this::mapToData)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("[Local] getTransactionsByWallet failed: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Lấy tổng chi tiêu cho một khoảng thời gian (dùng cho budget-comparison).
     */
    public java.math.BigDecimal getTotalSpending(Long userId, LocalDateTime startDate,
                                                  LocalDateTime endDate, Long categoryId) {
        log.info("[Local] getTotalSpending directly: userId={}, categoryId={}", userId, categoryId);
        try {
            java.math.BigDecimal total = transactionService.sumExpense(userId, startDate, endDate, categoryId);
            return total != null ? total : java.math.BigDecimal.ZERO;
        } catch (Exception e) {
            log.error("[Local] getTotalSpending failed: {}", e.getMessage());
            return java.math.BigDecimal.ZERO;
        }
    }

    private TransactionData mapToData(TransactionEntity entity) {
        return TransactionData.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .walletId(entity.getWalletId())
                .walletName("")
                .categoryName("")
                .type(entity.getType() != null ? entity.getType().name() : "")
                .amount(entity.getAmount())
                .note(entity.getNote())
                .transactionDate(entity.getTransactionDate())
                .build();
    }
}
