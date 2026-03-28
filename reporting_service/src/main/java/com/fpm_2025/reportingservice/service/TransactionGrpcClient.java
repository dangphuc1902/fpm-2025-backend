package com.fpm_2025.reportingservice.service;

import com.fpm2025.protocol.transaction.*;
import com.fpm_2025.reportingservice.domain.TransactionData;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * gRPC Client kết nối tới transaction-service.
 *
 * Cấu hình trong application.yml:
 *   grpc.client.transaction-service.address=static://localhost:9092
 *   grpc.client.transaction-service.negotiation-type=plaintext
 */
@Slf4j
@Service
public class TransactionGrpcClient {

    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_DATE_TIME;

    @GrpcClient("transaction-service")
    private TransactionGrpcServiceGrpc.TransactionGrpcServiceBlockingStub stub;

    /**
     * Lấy danh sách giao dịch theo khoảng thời gian và userId.
     * Dùng bởi: ReportingService, StatisticsService
     */
    public List<TransactionData> getTransactionsByDateRange(
            Long userId, LocalDateTime startDate, LocalDateTime endDate) {

        log.info("[gRPC] getTransactionsByDateRange: userId={} [{} → {}]", userId, startDate, endDate);

        try {
            DateRangeRequest request = DateRangeRequest.newBuilder()
                    .setUserId(userId)
                    .setStartDate(startDate.format(ISO_FORMATTER))
                    .setEndDate(endDate.format(ISO_FORMATTER))
                    .build();

            TransactionsResponse response = stub.getTransactionsByDateRange(request);

            List<TransactionData> result = new ArrayList<>();
            for (TransactionResponse t : response.getTransactionsList()) {
                result.add(TransactionData.builder()
                        .id(t.getId())
                        .userId(t.getUserId())
                        .walletId(t.getWalletId())
                        .walletName("")   // không có trong proto — có thể bổ sung sau
                        .categoryName(t.getCategoryName())
                        .type(t.getType())
                        .amount(java.math.BigDecimal.valueOf(t.getAmount().getAmount()))
                        .note(t.getNote())
                        .transactionDate(t.getTransactionDate().isEmpty()
                                ? null
                                : LocalDateTime.parse(t.getTransactionDate(), ISO_FORMATTER))
                        .build());
            }

            log.info("[gRPC] Received {} transactions from transaction-service", result.size());
            return result;

        } catch (Exception e) {
            log.error("[gRPC] Failed to get transactions via gRPC: {}", e.getMessage());
            // Fallback: trả về empty list để không fail toàn bộ report
            return new ArrayList<>();
        }
    }

    /**
     * Lấy giao dịch theo walletId (paged).
     */
    public List<TransactionData> getTransactionsByWallet(Long walletId, int page, int size) {
        log.info("[gRPC] getTransactionsByWallet: walletId={}", walletId);
        try {
            WalletTransactionsRequest req = WalletTransactionsRequest.newBuilder()
                    .setWalletId(walletId)
                    .setPage(page)
                    .setSize(size)
                    .build();

            TransactionsResponse response = stub.getTransactionsByWallet(req);

            List<TransactionData> result = new ArrayList<>();
            for (TransactionResponse t : response.getTransactionsList()) {
                result.add(TransactionData.builder()
                        .id(t.getId())
                        .userId(t.getUserId())
                        .walletId(t.getWalletId())
                        .categoryName(t.getCategoryName())
                        .type(t.getType())
                        .amount(java.math.BigDecimal.valueOf(t.getAmount().getAmount()))
                        .note(t.getNote())
                        .transactionDate(t.getTransactionDate().isEmpty()
                                ? null
                                : LocalDateTime.parse(t.getTransactionDate(), ISO_FORMATTER))
                        .build());
            }
            return result;
        } catch (Exception e) {
            log.error("[gRPC] getTransactionsByWallet failed: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Lấy tổng chi tiêu cho một期间 (dùng cho budget-comparison).
     */
    public java.math.BigDecimal getTotalSpending(Long userId, LocalDateTime startDate,
                                                  LocalDateTime endDate, Long categoryId) {
        log.info("[gRPC] getTotalSpending: userId={}, categoryId={}", userId, categoryId);
        try {
            SpendingRequest req = SpendingRequest.newBuilder()
                    .setUserId(userId)
                    .setStartDate(startDate.format(ISO_FORMATTER))
                    .setEndDate(endDate.format(ISO_FORMATTER))
                    .setCategoryId(categoryId != null ? categoryId : 0)
                    .build();

            SpendingResponse response = stub.getTotalSpending(req);
            return java.math.BigDecimal.valueOf(response.getTotalAmount().getAmount());
        } catch (Exception e) {
            log.error("[gRPC] getTotalSpending failed: {}", e.getMessage());
            return java.math.BigDecimal.ZERO;
        }
    }
}
