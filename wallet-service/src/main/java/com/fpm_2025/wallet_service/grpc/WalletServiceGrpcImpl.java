package com.fpm_2025.wallet_service.grpc;

import com.fpm2025.protocol.common.Money;
import com.fpm2025.protocol.wallet.*;
import com.fpm_2025.wallet_service.entity.WalletEntity;
import com.fpm_2025.wallet_service.repository.WalletRepository;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.grpc.server.service.GrpcService;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@GrpcService
@RequiredArgsConstructor
@Slf4j
public class WalletServiceGrpcImpl extends WalletGrpcServiceGrpc.WalletGrpcServiceImplBase {

    private final WalletRepository walletRepository;
    private final com.fpm_2025.wallet_service.repository.WalletPermissionRepository walletPermissionRepository;

    @Override
    public void getWalletById(WalletIdRequest request, StreamObserver<WalletResponse> responseObserver) {
        log.info("gRPC: getWalletById called for walletId: {}", request.getWalletId());
        try {
            WalletEntity wallet = walletRepository.findById(request.getWalletId())
                    .orElseThrow(() -> new RuntimeException("Wallet not found"));
            responseObserver.onNext(mapToGrpcWallet(wallet));
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("gRPC: getWalletById failed", e);
            responseObserver.onError(e);
        }
    }

    @Override
    public void getWalletsByUserId(UserWalletsRequest request, StreamObserver<WalletsResponse> responseObserver) {
        log.info("gRPC: getWalletsByUserId called for userId: {}", request.getUserId());
        try {
            List<WalletEntity> wallets;
            if (request.getActiveOnly()) {
                wallets = walletRepository.findActiveWalletsByUserId(request.getUserId());
            } else {
                wallets = walletRepository.findByUserId(request.getUserId());
            }

            List<WalletResponse> grpcWallets = wallets.stream()
                    .map(this::mapToGrpcWallet)
                    .collect(Collectors.toList());

            WalletsResponse response = WalletsResponse.newBuilder()
                    .addAllWallets(grpcWallets)
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("gRPC: getWalletsByUserId failed", e);
            responseObserver.onError(e);
        }
    }

    @Override
    public void updateBalance(UpdateBalanceRequest request, StreamObserver<WalletResponse> responseObserver) {
        log.info("gRPC: updateBalance called for walletId: {} with operation: {}", request.getWalletId(), request.getOperation());
        try {
            WalletEntity wallet = walletRepository.findById(request.getWalletId())
                    .orElseThrow(() -> new RuntimeException("Wallet not found"));

            double amountToChange = request.getAmount().getAmount();
            BigDecimal change = BigDecimal.valueOf(amountToChange);

            if ("SUBTRACT".equalsIgnoreCase(request.getOperation())) {
                if (wallet.getBalance().compareTo(change) < 0) {
                    throw new RuntimeException("Insufficient balance");
                }
                wallet.setBalance(wallet.getBalance().subtract(change));
            } else if ("ADD".equalsIgnoreCase(request.getOperation())) {
                wallet.setBalance(wallet.getBalance().add(change));
            } else if ("SET".equalsIgnoreCase(request.getOperation())) {
                wallet.setBalance(change);
            }

            WalletEntity updatedWallet = walletRepository.save(wallet);

            responseObserver.onNext(mapToGrpcWallet(updatedWallet));
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("gRPC: updateBalance failed", e);
            responseObserver.onError(e);
        }
    }

    @Override
    public void validateWalletAccess(WalletAccessRequest request, StreamObserver<WalletAccessResponse> responseObserver) {
        log.info("gRPC: validateWalletAccess called for walletId: {} and userId: {}", request.getWalletId(), request.getUserId());
        try {
            WalletEntity wallet = walletRepository.findById(request.getWalletId()).orElse(null);
            
            boolean hasAccess = false;
            String permissionLevel = "NONE";
            
            if (wallet != null) {
                // 1. Check if user is the OWNER
                if (wallet.getUserId().equals(request.getUserId())) {
                    hasAccess = true;
                    permissionLevel = "OWNER";
                } else {
                    // 2. Check if user has SHARED PERMISSION
                    java.util.Optional<com.fpm_2025.wallet_service.entity.WalletPermissionEntity> permission = 
                        walletPermissionRepository.findByWalletIdAndUserId(request.getWalletId(), request.getUserId());
                        
                    if (permission.isPresent()) {
                        hasAccess = true;
                        permissionLevel = permission.get().getPermissionLevel().name();
                    }
                }
            }
            
            WalletAccessResponse response = WalletAccessResponse.newBuilder()
                    .setHasAccess(hasAccess)
                    .setPermissionLevel(permissionLevel)
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("gRPC: validateWalletAccess failed", e);
            responseObserver.onError(e);
        }
    }

    @Override
    public void checkSufficientBalance(BalanceCheckRequest request, StreamObserver<BalanceCheckResponse> responseObserver) {
        try {
            WalletEntity wallet = walletRepository.findById(request.getWalletId())
                    .orElseThrow(() -> new RuntimeException("Wallet not found"));

            double requiredAmount = request.getAmount().getAmount();
            BigDecimal required = BigDecimal.valueOf(requiredAmount);
            
            boolean sufficient = wallet.getBalance().compareTo(required) >= 0;

            BalanceCheckResponse response = BalanceCheckResponse.newBuilder()
                    .setSufficient(sufficient)
                    .setCurrentBalance(Money.newBuilder().setAmount(wallet.getBalance().doubleValue()).setCurrency(wallet.getCurrency() != null ? wallet.getCurrency() : "VND").build())
                    .setRequiredAmount(request.getAmount())
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("gRPC: checkSufficientBalance failed", e);
            responseObserver.onError(e);
        }
    }

    private WalletResponse mapToGrpcWallet(WalletEntity entity) {
        return WalletResponse.newBuilder()
                .setId(entity.getId() != null ? entity.getId() : 0)
                .setUserId(entity.getUserId() != null ? entity.getUserId() : 0)
                .setName(entity.getName() == null ? "" : entity.getName())
                .setType(entity.getType() != null ? entity.getType().name() : "")
                .setBalance(Money.newBuilder().setAmount(entity.getBalance() != null ? entity.getBalance().doubleValue() : 0.0).setCurrency(entity.getCurrency() != null ? entity.getCurrency() : "VND").build())
                .setIcon(entity.getIcon() != null ? entity.getIcon() : "")
                .setIsActive(entity.getIsActive() != null ? entity.getIsActive() : false)
                .build();
    }
}