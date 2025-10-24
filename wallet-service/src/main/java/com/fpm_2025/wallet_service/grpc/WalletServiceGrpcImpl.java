package com.fpm_2025.wallet_service.grpc;

import com.fpm_2025.wallet_service.*;
import com.fpm_2025.wallet_service.entity.WalletEntity;
import com.fpm_2025.wallet_service.entity.enums.WalletType;
import com.fpm_2025.wallet_service.repository.WalletRepository;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@GrpcService
@RequiredArgsConstructor
@Slf4j
public class WalletServiceGrpcImpl extends WalletServiceGrpc.WalletServiceImplBase {

    private final WalletRepository walletRepository;

    @Override
    public void listWallets(ListWalletsRequest request, StreamObserver<ListWalletsResponse> responseObserver) {
        log.info("gRPC: ListWallets called for userId: {}", request.getUserId());

        try {
            Long userId = Long.parseLong(request.getUserId());
            List<WalletEntity> wallets = walletRepository.findByUserIdAndIsActive(userId, true);

            List<Wallet> grpcWallets = wallets.stream()
                    .map(this::mapToGrpcWallet)
                    .collect(Collectors.toList());

            ListWalletsResponse response = ListWalletsResponse.newBuilder()
                    .addAllWallets(grpcWallets)
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("gRPC: ListWallets failed", e);
            responseObserver.onError(e);
        }
    }

    @Override
    public void createWallet(CreateWalletRequest request, StreamObserver<WalletResponse> responseObserver) {
        log.info("gRPC: CreateWallet called for userId: {}", request.getUserId());

        try {
            Long userId = Long.parseLong(request.getUserId());

            WalletEntity wallet = WalletEntity.builder()
                    .userId(userId)
                    .name(request.getName())
                    .type(WalletType.valueOf(request.getType().toUpperCase()))
                    .balance(BigDecimal.valueOf(request.getInitialBalance()))
                    .isActive(true)
                    .build();

            WalletEntity savedWallet = walletRepository.save(wallet);

            WalletResponse response = WalletResponse.newBuilder()
                    .setSuccess(true)
                    .setWallet(mapToGrpcWallet(savedWallet))
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("gRPC: CreateWallet failed", e);
            responseObserver.onError(e);
        }
    }

    @Override
    public void updateWallet(UpdateWalletRequest request, StreamObserver<WalletResponse> responseObserver) {
        log.info("gRPC: UpdateWallet called for walletId: {}", request.getWalletId());

        try {
            Long walletId = Long.parseLong(request.getWalletId());
            WalletEntity wallet = walletRepository.findById(walletId)
                    .orElseThrow(() -> new RuntimeException("Wallet not found"));

            wallet.setName(request.getName());
            wallet.setType(WalletType.valueOf(request.getType().toUpperCase()));

            WalletEntity updatedWallet = walletRepository.save(wallet);

            WalletResponse response = WalletResponse.newBuilder()
                    .setSuccess(true)
                    .setWallet(mapToGrpcWallet(updatedWallet))
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("gRPC: UpdateWallet failed", e);
            responseObserver.onError(e);
        }
    }

    @Override
    public void deleteWallet(DeleteWalletRequest request, StreamObserver<WalletResponse> responseObserver) {
        log.info("gRPC: DeleteWallet called for walletId: {}", request.getWalletId());

        try {
            Long walletId = Long.parseLong(request.getWalletId());
            WalletEntity wallet = walletRepository.findById(walletId)
                    .orElseThrow(() -> new RuntimeException("Wallet not found"));

            // Soft delete
            wallet.setIsActive(false);
            walletRepository.save(wallet);

            WalletResponse response = WalletResponse.newBuilder()
                    .setSuccess(true)
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("gRPC: DeleteWallet failed", e);
            responseObserver.onError(e);
        }
    }

    @Override
    public void getLedger(GetLedgerRequest request, StreamObserver<GetLedgerResponse> responseObserver) {
        log.info("gRPC: GetLedger called for walletId: {}", request.getWalletId());

        try {
            // Implementation would fetch transactions from transaction service
            // For now, return empty response
            GetLedgerResponse response = GetLedgerResponse.newBuilder().build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("gRPC: GetLedger failed", e);
            responseObserver.onError(e);
        }
    }

    private Wallet mapToGrpcWallet(WalletEntity entity) {
        return Wallet.newBuilder()
                .setWalletId(entity.getId().toString())
                .setName(entity.getName())
                .setType(entity.getType().getValue())
                .setBalance(entity.getBalance().doubleValue())
                .build();
    }
}