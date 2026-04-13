package com.fpm2025.transaction_service.grpc;

import com.fpm2025.protocol.common.Money;
import com.fpm2025.protocol.common.PageResponse;
import com.fpm2025.protocol.transaction.*;
import com.fpm2025.domain.dto.request.TransactionRequest;
import com.fpm2025.domain.dto.response.TransactionResponse;
import com.fpm2025.domain.enums.CategoryType;
import com.fpm2025.transaction_service.entity.TransactionEntity;
import com.fpm2025.transaction_service.service.TransactionService;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@GrpcService
@RequiredArgsConstructor
@Slf4j
public class TransactionServiceGrpcImpl extends TransactionGrpcServiceGrpc.TransactionGrpcServiceImplBase {

    private final TransactionService transactionService;

    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_DATE_TIME;

    @Override
    public void getTransactionById(TransactionIdRequest request,
                                   StreamObserver<com.fpm2025.protocol.transaction.TransactionResponse> responseObserver) {
        log.info("gRPC: getTransactionById called for id: {}", request.getTransactionId());
        try {
            TransactionEntity entity = transactionService.findById(request.getTransactionId());
            responseObserver.onNext(toProto(entity));
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("gRPC: getTransactionById failed", e);
            responseObserver.onError(Status.NOT_FOUND
                    .withDescription(e.getMessage())
                    .asRuntimeException());
        }
    }

    @Override
    public void getTransactionsByWallet(WalletTransactionsRequest request,
                                        StreamObserver<TransactionsResponse> responseObserver) {
        log.info("gRPC: getTransactionsByWallet called for walletId: {}, page: {}, size: {}",
                request.getWalletId(), request.getPage(), request.getSize());
        try {
            int page = Math.max(0, request.getPage());
            int size = request.getSize() > 0 ? request.getSize() : 20;

            var pagedResult = transactionService.findByWalletIdRaw(request.getWalletId(), page, size);

            TransactionsResponse.Builder builder = TransactionsResponse.newBuilder();
            for (TransactionEntity e : pagedResult.getContent()) {
                builder.addTransactions(toProto(e));
            }

            builder.setPageInfo(PageResponse.newBuilder()
                    .setCurrentPage(page)
                    .setTotalPages(pagedResult.getTotalPages())
                    .setTotalElements((int) pagedResult.getTotalElements())
                    .build());

            responseObserver.onNext(builder.build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("gRPC: getTransactionsByWallet failed", e);
            responseObserver.onError(Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
        }
    }

    @Override
    public void getTransactionsByDateRange(DateRangeRequest request,
                                           StreamObserver<TransactionsResponse> responseObserver) {
        log.info("gRPC: getTransactionsByDateRange called for userId: {} [{} → {}]",
                request.getUserId(), request.getStartDate(), request.getEndDate());
        try {
            LocalDateTime start = LocalDateTime.parse(request.getStartDate(), ISO_FORMATTER);
            LocalDateTime end   = LocalDateTime.parse(request.getEndDate(), ISO_FORMATTER);
            List<Long> walletIds = request.getWalletIdsList().isEmpty() ? null : request.getWalletIdsList();

            List<TransactionEntity> entities = transactionService.findByUserAndDateRange(
                    request.getUserId(), start, end, walletIds);

            TransactionsResponse.Builder builder = TransactionsResponse.newBuilder();
            for (TransactionEntity e : entities) {
                builder.addTransactions(toProto(e));
            }
            responseObserver.onNext(builder.build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("gRPC: getTransactionsByDateRange failed", e);
            responseObserver.onError(Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
        }
    }

    @Override
    public void getTransactionsByUser(UserTransactionsRequest request,
                                      StreamObserver<TransactionsResponse> responseObserver) {
        log.info("gRPC: getTransactionsByUser called for userId: {}", request.getUserId());
        try {
            int page = Math.max(0, request.getPage());
            int size = request.getSize() > 0 ? request.getSize() : 20;

            var pagedResult = transactionService.listTransactions(
                    request.getUserId(), null, null, null, null, null, page, size);

            TransactionsResponse.Builder builder = TransactionsResponse.newBuilder();

            for (TransactionResponse r : pagedResult.getContent()) {
                builder.addTransactions(dtoToProto(r));
            }

            builder.setPageInfo(PageResponse.newBuilder()
                    .setCurrentPage(page)
                    .setTotalPages(pagedResult.getTotalPages())
                    .setTotalElements((int) pagedResult.getTotalElements())
                    .build());

            responseObserver.onNext(builder.build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("gRPC: getTransactionsByUser failed", e);
            responseObserver.onError(Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
        }
    }

    @Override
    public void createTransaction(com.fpm2025.protocol.transaction.CreateTransactionRequest request,
                                  StreamObserver<com.fpm2025.protocol.transaction.TransactionResponse> responseObserver) {
        log.info("gRPC: createTransaction called by external service (source: {})", request.getSource());
        try {
            TransactionRequest dto = new TransactionRequest();
            dto.setWalletId(request.getWalletId());
            dto.setAmount(BigDecimal.valueOf(request.getAmount().getAmount()));
            dto.setCurrency(request.getAmount().getCurrency());
            dto.setType(CategoryType.valueOf(request.getType().toUpperCase()));
            dto.setDescription(request.getDescription());
            dto.setCategoryId(request.getCategoryId() != 0 ? request.getCategoryId() : null);
            dto.setNote(request.getNote());

            if (!request.getTransactionDate().isEmpty()) {
                dto.setTransactionDate(LocalDateTime.parse(request.getTransactionDate(), ISO_FORMATTER));
            } else {
                dto.setTransactionDate(LocalDateTime.now());
            }
            dto.setIsRecurring(false);

            TransactionResponse created = transactionService.createTransaction(request.getUserId(), dto);

            responseObserver.onNext(dtoToProto(created));
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("gRPC: createTransaction failed", e);
            responseObserver.onError(Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
        }
    }

    @Override
    public void getTotalSpending(SpendingRequest request,
                                 StreamObserver<SpendingResponse> responseObserver) {
        log.info("gRPC: getTotalSpending called for userId: {} [{} → {}]",
                request.getUserId(), request.getStartDate(), request.getEndDate());
        try {
            LocalDateTime start = LocalDateTime.parse(request.getStartDate(), ISO_FORMATTER);
            LocalDateTime end   = LocalDateTime.parse(request.getEndDate(), ISO_FORMATTER);
            Long categoryId = request.getCategoryId() != 0 ? request.getCategoryId() : null;

            BigDecimal total = transactionService.sumExpense(request.getUserId(), start, end, categoryId);

            List<TransactionEntity> entities = transactionService.findByUserAndDateRange(
                    request.getUserId(), start, end, null);
            long expenseCount = entities.stream()
                    .filter(e -> CategoryType.EXPENSE == e.getType())
                    .count();

            SpendingResponse response = SpendingResponse.newBuilder()
                    .setTotalAmount(Money.newBuilder()
                            .setAmount(total.doubleValue())
                            .setCurrency("VND")
                    .build())
                    .setTransactionCount((int) expenseCount)
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("gRPC: getTotalSpending failed", e);
            responseObserver.onError(Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
        }
    }

    private com.fpm2025.protocol.transaction.TransactionResponse toProto(TransactionEntity e) {
        return com.fpm2025.protocol.transaction.TransactionResponse.newBuilder()
                .setId(e.getId() != null ? e.getId() : 0L)
                .setWalletId(e.getWalletId() != null ? e.getWalletId() : 0L)
                .setUserId(e.getUserId() != null ? e.getUserId() : 0L)
                .setAmount(Money.newBuilder()
                        .setAmount(e.getAmount() != null ? e.getAmount().doubleValue() : 0.0)
                        .setCurrency(e.getCurrency() != null ? e.getCurrency() : "VND")
                        .build())
                .setType(e.getType() != null ? e.getType().name() : "")
                .setDescription(e.getDescription() != null ? e.getDescription() : "")
                .setCategoryId(e.getCategoryId() != null ? e.getCategoryId() : 0L)
                .setNote(e.getNote() != null ? e.getNote() : "")
                .setTransactionDate(e.getTransactionDate() != null
                        ? e.getTransactionDate().format(ISO_FORMATTER) : "")
                .setCreatedAt(e.getCreatedAt() != null
                        ? e.getCreatedAt().format(ISO_FORMATTER) : "")
                .build();
    }

    private com.fpm2025.protocol.transaction.TransactionResponse dtoToProto(TransactionResponse r) {
        return com.fpm2025.protocol.transaction.TransactionResponse.newBuilder()
                .setId(r.getId() != null ? r.getId() : 0L)
                .setWalletId(r.getWalletId() != null ? r.getWalletId() : 0L)
                .setUserId(r.getUserId() != null ? r.getUserId() : 0L)
                .setAmount(Money.newBuilder()
                        .setAmount(r.getAmount() != null ? r.getAmount().doubleValue() : 0.0)
                        .setCurrency(r.getCurrency() != null ? r.getCurrency() : "VND")
                        .build())
                .setType(r.getType() != null ? r.getType().name() : "")
                .setDescription(r.getDescription() != null ? r.getDescription() : "")
                .setCategoryId(r.getCategoryId() != null ? r.getCategoryId() : 0L)
                .setNote(r.getNote() != null ? r.getNote() : "")
                .setTransactionDate(r.getTransactionDate() != null
                        ? r.getTransactionDate().format(ISO_FORMATTER) : "")
                .setCreatedAt(r.getCreatedAt() != null
                        ? r.getCreatedAt().format(ISO_FORMATTER) : "")
                .build();
    }
}