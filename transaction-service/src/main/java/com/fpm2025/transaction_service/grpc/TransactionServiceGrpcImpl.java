package com.fpm2025.transaction_service.grpc;

import com.fpm2025.protocol.transaction.TransactionGrpcServiceGrpc;
import com.fpm2025.protocol.transaction.TransactionIdRequest;
import com.fpm2025.protocol.transaction.TransactionResponse;
import com.fpm2025.protocol.transaction.WalletTransactionsRequest;
import com.fpm2025.protocol.transaction.TransactionsResponse;
import com.fpm2025.protocol.transaction.DateRangeRequest;
import com.fpm2025.protocol.transaction.UserTransactionsRequest;
import com.fpm2025.protocol.transaction.CreateTransactionRequest;
import com.fpm2025.protocol.transaction.SpendingRequest;
import com.fpm2025.protocol.transaction.SpendingResponse;
import com.fpm2025.transaction_service.service.TransactionService;

import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;

@GrpcService
@RequiredArgsConstructor
@Slf4j
public class TransactionServiceGrpcImpl extends TransactionGrpcServiceGrpc.TransactionGrpcServiceImplBase {

    private final TransactionService transactionService;

    @Override
    public void getTransactionById(TransactionIdRequest request, StreamObserver<TransactionResponse> responseObserver) {
        log.info("gRPC: getTransactionById called for id: {}", request.getTransactionId());
        // TODO: Implement using transactionService
        responseObserver.onNext(TransactionResponse.newBuilder().build());
        responseObserver.onCompleted();
    }

    @Override
    public void getTransactionsByWallet(WalletTransactionsRequest request, StreamObserver<TransactionsResponse> responseObserver) {
        log.info("gRPC: getTransactionsByWallet called for walletId: {}", request.getWalletId());
        // TODO: Implement using transactionService
        responseObserver.onNext(TransactionsResponse.newBuilder().build());
        responseObserver.onCompleted();
    }

    @Override
    public void getTransactionsByDateRange(DateRangeRequest request, StreamObserver<TransactionsResponse> responseObserver) {
        log.info("gRPC: getTransactionsByDateRange called");
        // TODO: Implement using transactionService
        responseObserver.onNext(TransactionsResponse.newBuilder().build());
        responseObserver.onCompleted();
    }

    @Override
    public void getTransactionsByUser(UserTransactionsRequest request, StreamObserver<TransactionsResponse> responseObserver) {
        log.info("gRPC: getTransactionsByUser called for userId: {}", request.getUserId());
        responseObserver.onNext(TransactionsResponse.newBuilder().build());
        responseObserver.onCompleted();
    }

    @Override
    public void createTransaction(CreateTransactionRequest request, StreamObserver<TransactionResponse> responseObserver) {
        log.info("gRPC: createTransaction called");
        responseObserver.onNext(TransactionResponse.newBuilder().build());
        responseObserver.onCompleted();
    }

    @Override
    public void getTotalSpending(SpendingRequest request, StreamObserver<SpendingResponse> responseObserver) {
        log.info("gRPC: getTotalSpending called");
        responseObserver.onNext(SpendingResponse.newBuilder().build());
        responseObserver.onCompleted();
    }
}
