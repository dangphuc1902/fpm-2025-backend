package com.fpm2025.transaction_service.grpc;

import com.fpm2025.protocol.common.Money;
import com.fpm2025.protocol.common.PageResponse;
import com.fpm2025.protocol.transaction.*;
import com.fpm2025.transaction_service.dto.TransactionRequest;
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

    // =========================================================================
    // 1. GetTransactionById
    // =========================================================================
    @Override
    public void getTransactionById(TransactionIdRequest request,
                                   StreamObserver<TransactionResponse> responseObserver) {
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

    // =========================================================================
    // 2. GetTransactionsByWallet
    // =========================================================================
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

            // FIX #1: Bỏ setPageSize nếu proto không có field đó
            // Chỉ set các field thực sự tồn tại trong PageResponse proto
            builder.setPageInfo(PageResponse.newBuilder()
                    .setCurrentPage(page)
                    .setTotalPages(pagedResult.getTotalPages())
                    .setTotalElements((int) pagedResult.getTotalElements())
                    // .setPageSize(size) // ← XÓA dòng này nếu proto không có field page_size
                    .build());

            responseObserver.onNext(builder.build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("gRPC: getTransactionsByWallet failed", e);
            responseObserver.onError(Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
        }
    }

    // =========================================================================
    // 3. GetTransactionsByDateRange
    // =========================================================================
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

    // =========================================================================
    // 4. GetTransactionsByUser
    // FIX #2: listTransactions() trả về Page<DTO>, không phải Page<proto>
    // Phải dùng dto.TransactionResponse, không nhầm với proto TransactionResponse
    // =========================================================================
    @Override
    public void getTransactionsByUser(UserTransactionsRequest request,
                                      StreamObserver<TransactionsResponse> responseObserver) {
        log.info("gRPC: getTransactionsByUser called for userId: {}", request.getUserId());
        try {
            int page = Math.max(0, request.getPage());
            int size = request.getSize() > 0 ? request.getSize() : 20;

            // pagedResult chứa DTO, đặt tên rõ ràng để tránh nhầm lẫn
            var pagedResult = transactionService.listTransactions(
                    request.getUserId(), null, null, null, null, null, page, size);

            TransactionsResponse.Builder builder = TransactionsResponse.newBuilder();

            for (com.fpm2025.transaction_service.dto.TransactionResponse r : pagedResult.getContent()) {
                builder.addTransactions(dtoToProto(r));
            }

            builder.setPageInfo(PageResponse.newBuilder()
                    .setCurrentPage(page)
                    .setTotalPages(pagedResult.getTotalPages())
                    .setTotalElements((int) pagedResult.getTotalElements())
                    // .setPageSize(size) // ← XÓA nếu proto không có
                    .build());

            responseObserver.onNext(builder.build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("gRPC: getTransactionsByUser failed", e);
            responseObserver.onError(Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
        }
    }

    // =========================================================================
    // 5. CreateTransaction
    // =========================================================================
    @Override
    public void createTransaction(CreateTransactionRequest request,
                                  StreamObserver<TransactionResponse> responseObserver) {
        log.info("gRPC: createTransaction called by external service (source: {})", request.getSource());
        try {
            TransactionRequest dto = new TransactionRequest();
            dto.setWalletId(request.getWalletId());
            dto.setAmount(BigDecimal.valueOf(request.getAmount().getAmount()));
            dto.setCurrency(request.getAmount().getCurrency());
            dto.setType(com.fpm2025.transaction_service.entity.enums.TransactionType
                    .valueOf(request.getType()));
            dto.setDescription(request.getDescription());
            dto.setCategoryId(request.getCategoryId() != 0 ? request.getCategoryId() : null);
            dto.setNote(request.getNote());

            if (!request.getTransactionDate().isEmpty()) {
                dto.setTransactionDate(LocalDateTime.parse(request.getTransactionDate(), ISO_FORMATTER));
            } else {
                dto.setTransactionDate(LocalDateTime.now());
            }
            dto.setIsRecurring(false);

            com.fpm2025.transaction_service.dto.TransactionResponse created =
                    transactionService.createTransaction(request.getUserId(), dto);

            responseObserver.onNext(dtoToProto(created));
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("gRPC: createTransaction failed", e);
            responseObserver.onError(Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
        }
    }

    // =========================================================================
    // 6. GetTotalSpending
    // =========================================================================
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
                    .filter(e -> "EXPENSE".equals(e.getType().name()))
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

    // =========================================================================
    // Mapper: Entity → Proto
    // =========================================================================
    private TransactionResponse toProto(TransactionEntity e) {
        return TransactionResponse.newBuilder()
                .setId(e.getId() != null ? e.getId() : 0L)
                .setWalletId(e.getWalletId() != null ? e.getWalletId() : 0L)
                .setUserId(e.getUserId() != null ? e.getUserId() : 0L)
                .setAmount(Money.newBuilder()
                        .setAmount(e.getAmount() != null ? e.getAmount().doubleValue() : 0.0)
                        // FIX #3: getCurrency() từ Entity string, không cần .name()
                        .setCurrency(e.getCurrency() != null ? e.getCurrency() : "VND")
                        .build())
                // FIX #4: getType() là Enum trên Entity → dùng .name()
                .setType(e.getType() != null ? e.getType().name() : "")
                .setDescription(e.getDescription() != null ? e.getDescription() : "")
                .setCategoryId(e.getCategoryId() != null ? e.getCategoryId() : 0L)
                .setNote(e.getNote() != null ? e.getNote() : "")
                // FIX #5: localDateTime.format(formatter), không phải String.format(formatter)
                .setTransactionDate(e.getTransactionDate() != null
                        ? e.getTransactionDate().format(ISO_FORMATTER) : "")
                .setCreatedAt(e.getCreatedAt() != null
                        ? e.getCreatedAt().format(ISO_FORMATTER) : "")
                .build();
    }

    // =========================================================================
    // Mapper: DTO → Proto
    // FIX #6: DTO dùng BigDecimal amount, String type (hoặc Enum) — xử lý đúng type
    // =========================================================================
    private TransactionResponse dtoToProto(com.fpm2025.transaction_service.dto.TransactionResponse r) {
        return TransactionResponse.newBuilder()
                .setId(r.getId() != null ? r.getId() : 0L)
                .setWalletId(r.getWalletId() != null ? r.getWalletId() : 0L)
                .setUserId(r.getUserId() != null ? r.getUserId() : 0L)
                .setAmount(Money.newBuilder()
                        // FIX: r.getAmount() là BigDecimal → gọi .doubleValue()
                        .setAmount(r.getAmount() != null ? r.getAmount().doubleValue() : 0.0)
                        // FIX: DTO không có getCurrency() → dùng default "VND"
                        // Nếu DTO có currency thì thêm lại: r.getCurrency()
                        .setCurrency("VND")
                        .build())
                // FIX: Nếu r.getType() là Enum → .name(), nếu là String → dùng trực tiếp
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