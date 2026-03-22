package com.fpm_2025.reportingservice.service;

import com.fpm_2025.reportingservice.domain.TransactionData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
// import com.fpm2025.protocol.transaction.TransactionGrpcServiceGrpc;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionGrpcClient {

    // private final TransactionGrpcServiceGrpc.TransactionGrpcServiceBlockingStub stub;

    public List<TransactionData> getTransactionsByDateRange(Long userId, LocalDateTime startDate, LocalDateTime endDate) {
        log.info("Mock getting transactions for user: {} from {} to {}", userId, startDate, endDate);
        // FIXME: Connect to actual gRPC
        return new ArrayList<>(); 
    }
}
