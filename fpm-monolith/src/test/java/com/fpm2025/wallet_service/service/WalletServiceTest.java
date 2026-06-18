package com.fpm2025.wallet_service.service;

import com.fpm2025.domain.dto.response.WalletResponse;
import com.fpm2025.domain.enums.WalletType;
import com.fpm2025.wallet_service.client.UserAuthClient;
import com.fpm2025.wallet_service.dto.mapper.WalletMapper;
import com.fpm2025.wallet_service.dto.payload.request.CreateWalletRequest;
import com.fpm2025.wallet_service.entity.WalletEntity;
import com.fpm2025.wallet_service.repository.WalletPermissionRepository;
import com.fpm2025.wallet_service.repository.WalletRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class WalletServiceTest {

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private WalletPermissionRepository permissionRepository;

    @Mock
    private WalletMapper walletMapper;

    @Mock
    private UserAuthClient userAuthClient;

    private WalletService walletService;

    @BeforeEach
    void setUp() {
        walletService = new WalletService(
                walletRepository,
                permissionRepository,
                walletMapper,
                userAuthClient
        );
    }

    @Test
    void getUserWallets_Success() {
        // Arrange
        Long userId = 1L;
        WalletEntity entity = WalletEntity.builder()
                .id(10L)
                .userId(userId)
                .name("Main Wallet")
                .type(WalletType.BANK)
                .balance(BigDecimal.valueOf(1000))
                .currency("VND")
                .build();

        WalletResponse responseDto = WalletResponse.builder()
                .id(10L)
                .name("Main Wallet")
                .type(WalletType.BANK)
                .balance(BigDecimal.valueOf(1000))
                .currency("VND")
                .build();

        when(walletRepository.findByUserId(userId)).thenReturn(Collections.singletonList(entity));
        when(walletMapper.toResponse(entity)).thenReturn(responseDto);

        // Act
        List<WalletResponse> result = walletService.getUserWallets(userId);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Main Wallet", result.get(0).getName());
        verify(walletRepository, times(1)).findByUserId(userId);
    }

    @Test
    void createWallet_Success() {
        // Arrange
        Long userId = 1L;
        CreateWalletRequest request = new CreateWalletRequest();
        request.setName("Savings Wallet");
        request.setType(WalletType.SAVINGS);
        request.setInitialBalance(BigDecimal.valueOf(500));

        WalletEntity unSavedEntity = WalletEntity.builder()
                .userId(userId)
                .name("Savings Wallet")
                .type(WalletType.SAVINGS)
                .balance(BigDecimal.valueOf(500))
                .build();

        WalletEntity savedEntity = WalletEntity.builder()
                .id(11L)
                .userId(userId)
                .name("Savings Wallet")
                .type(WalletType.SAVINGS)
                .balance(BigDecimal.valueOf(500))
                .build();

        WalletResponse responseDto = WalletResponse.builder()
                .id(11L)
                .name("Savings Wallet")
                .type(WalletType.SAVINGS)
                .balance(BigDecimal.valueOf(500))
                .build();

        when(walletRepository.existsByUserIdAndName(userId, "Savings Wallet")).thenReturn(false);
        when(walletMapper.toEntity(request, userId)).thenReturn(unSavedEntity);
        when(walletRepository.save(unSavedEntity)).thenReturn(savedEntity);
        when(walletMapper.toResponse(savedEntity)).thenReturn(responseDto);

        // Act
        WalletResponse result = walletService.createWallet(request, userId);

        // Assert
        assertNotNull(result);
        assertEquals(11L, result.getId());
        assertEquals("Savings Wallet", result.getName());
        verify(walletRepository, times(1)).save(unSavedEntity);
    }

    @Test
    void createWallet_DuplicateName_ThrowsException() {
        // Arrange
        Long userId = 1L;
        CreateWalletRequest request = new CreateWalletRequest();
        request.setName("Main Wallet");

        when(walletRepository.existsByUserIdAndName(userId, "Main Wallet")).thenReturn(true);

        // Act & Assert
        assertThrows(RuntimeException.class, () -> walletService.createWallet(request, userId));
        verify(walletRepository, never()).save(any());
    }

    @Test
    void createDefaultWallet_IdempotentSuccess() {
        // Arrange
        Long userId = 1L;
        when(walletRepository.existsByUserIdAndName(userId, "Ví Tiền Mặt")).thenReturn(false);

        // Act
        walletService.createDefaultWallet(userId);

        // Assert
        verify(walletRepository, times(1)).save(any(WalletEntity.class));
    }

    @Test
    void createDefaultWallet_AlreadyExists_DoesNotSave() {
        // Arrange
        Long userId = 1L;
        when(walletRepository.existsByUserIdAndName(userId, "Ví Tiền Mặt")).thenReturn(true);

        // Act
        walletService.createDefaultWallet(userId);

        // Assert
        verify(walletRepository, never()).save(any(WalletEntity.class));
    }

    @Test
    void updateBalance_Addition_Success() {
        // Arrange
        Long walletId = 10L;
        Long userId = 1L;
        BigDecimal amount = BigDecimal.valueOf(150);

        WalletEntity entity = WalletEntity.builder()
                .id(walletId)
                .userId(userId)
                .name("Main Wallet")
                .balance(BigDecimal.valueOf(1000))
                .build();

        when(walletRepository.findById(walletId)).thenReturn(Optional.of(entity));

        // Act
        walletService.updateBalance(walletId, userId, amount, true);

        // Assert
        assertEquals(BigDecimal.valueOf(1150), entity.getBalance());
        verify(walletRepository, times(1)).save(entity);
    }

    @Test
    void updateBalance_Subtraction_Success() {
        // Arrange
        Long walletId = 10L;
        Long userId = 1L;
        BigDecimal amount = BigDecimal.valueOf(150);

        WalletEntity entity = WalletEntity.builder()
                .id(walletId)
                .userId(userId)
                .name("Main Wallet")
                .balance(BigDecimal.valueOf(1000))
                .build();

        when(walletRepository.findById(walletId)).thenReturn(Optional.of(entity));

        // Act
        walletService.updateBalance(walletId, userId, amount, false);

        // Assert
        assertEquals(BigDecimal.valueOf(850), entity.getBalance());
        verify(walletRepository, times(1)).save(entity);
    }

    @Test
    void validateWalletAccess_Owner_ReturnsTrue() {
        // Arrange
        Long walletId = 10L;
        Long userId = 1L;

        WalletEntity entity = WalletEntity.builder()
                .id(walletId)
                .userId(userId)
                .build();

        when(walletRepository.findById(walletId)).thenReturn(Optional.of(entity));

        // Act
        boolean hasAccess = walletService.validateWalletAccess(walletId, userId);

        // Assert
        assertTrue(hasAccess);
    }

    @Test
    void validateWalletAccess_NotOwner_NoPermissions_ReturnsFalse() {
        // Arrange
        Long walletId = 10L;
        Long ownerId = 1L;
        Long strangerId = 2L;

        WalletEntity entity = WalletEntity.builder()
                .id(walletId)
                .userId(ownerId)
                .build();

        when(walletRepository.findById(walletId)).thenReturn(Optional.of(entity));
        when(permissionRepository.findByWalletIdAndUserId(walletId, strangerId)).thenReturn(Optional.empty());

        // Act
        boolean hasAccess = walletService.validateWalletAccess(walletId, strangerId);

        // Assert
        false_assert: assertFalse(hasAccess);
    }
}
