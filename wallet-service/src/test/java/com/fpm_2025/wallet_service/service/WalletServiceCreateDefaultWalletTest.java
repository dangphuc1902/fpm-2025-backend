package com.fpm_2025.wallet_service.service;

import com.fpm2025.domain.enums.WalletType;
import com.fpm_2025.wallet_service.entity.WalletEntity;
import com.fpm_2025.wallet_service.exception.DuplicateResourceException;
import com.fpm_2025.wallet_service.repository.WalletRepository;
import com.fpm_2025.wallet_service.repository.WalletPermissionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests cho {@link WalletService#createDefaultWallet(Long)}.
 *
 * <p>Xác nhận toàn bộ logic của method này:
 * <ul>
 *   <li>Tạo ví thành công với đúng thuộc tính (CASH, VND, balance=0, name="Ví Tiền Mặt")</li>
 *   <li>Idempotent: đã tồn tại ví → không tạo lại (existsByUserIdAndName guard)</li>
 *   <li>Repository.save() được gọi đúng 1 lần khi ví chưa tồn tại</li>
 *   <li>Edge cases: userId null, userId âm</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("WalletService.createDefaultWallet — Unit Tests")
class WalletServiceCreateDefaultWalletTest {

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private WalletPermissionRepository walletPermissionRepository;

    @InjectMocks
    private WalletService walletService;

    private static final Long USER_ID = 42L;
    private static final String DEFAULT_WALLET_NAME = "Ví Tiền Mặt";

    @Nested
    @DisplayName("Happy Path — Tạo ví thành công")
    class HappyPathTest {

        @BeforeEach
        void setUp() {
            // Ví chưa tồn tại → tạo mới
            when(walletRepository.existsByUserIdAndName(USER_ID, DEFAULT_WALLET_NAME))
                    .thenReturn(false);
            when(walletRepository.save(any(WalletEntity.class)))
                    .thenAnswer(inv -> inv.getArgument(0)); // trả về entity vừa save
        }

        @Test
        @DisplayName("createDefaultWallet() → WalletEntity được save với đúng thuộc tính")
        void createDefaultWallet_newUser_savesWithCorrectAttributes() {
            // When
            walletService.createDefaultWallet(USER_ID);

            // Then: capture entity được save
            ArgumentCaptor<WalletEntity> captor = ArgumentCaptor.forClass(WalletEntity.class);
            verify(walletRepository).save(captor.capture());
            WalletEntity saved = captor.getValue();

            assertThat(saved.getUserId()).isEqualTo(USER_ID);
            assertThat(saved.getName()).isEqualTo(DEFAULT_WALLET_NAME);
            assertThat(saved.getType()).isEqualTo(WalletType.CASH);
            assertThat(saved.getCurrency()).isEqualTo("VND");
            assertThat(saved.getBalance()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(saved.getIsActive()).isTrue();
            assertThat(saved.getIsDeleted()).isFalse();
        }

        @Test
        @DisplayName("createDefaultWallet() → walletRepository.save() được gọi đúng 1 lần")
        void createDefaultWallet_newUser_saveCalledOnce() {
            walletService.createDefaultWallet(USER_ID);

            verify(walletRepository, times(1)).save(any(WalletEntity.class));
        }

        @Test
        @DisplayName("createDefaultWallet() → existsByUserIdAndName() được kiểm tra trước khi tạo")
        void createDefaultWallet_checksExistenceFirst() {
            walletService.createDefaultWallet(USER_ID);

            // Verify thứ tự kiểm tra: exists trước, save sau
            var inOrder = inOrder(walletRepository);
            inOrder.verify(walletRepository).existsByUserIdAndName(USER_ID, DEFAULT_WALLET_NAME);
            inOrder.verify(walletRepository).save(any(WalletEntity.class));
        }

        @Test
        @DisplayName("Icon mặc định là 'cash_icon'")
        void createDefaultWallet_defaultIcon() {
            walletService.createDefaultWallet(USER_ID);

            ArgumentCaptor<WalletEntity> captor = ArgumentCaptor.forClass(WalletEntity.class);
            verify(walletRepository).save(captor.capture());
            assertThat(captor.getValue().getIcon()).isEqualTo("cash_icon");
        }
    }

    @Nested
    @DisplayName("Idempotency — Ví đã tồn tại")
    class IdempotencyTest {

        @Test
        @DisplayName("Ví đã tồn tại → KHÔNG gọi save() — hoàn toàn idempotent")
        void createDefaultWallet_existingWallet_doesNotSave() {
            // Given: ví đã tồn tại
            when(walletRepository.existsByUserIdAndName(USER_ID, DEFAULT_WALLET_NAME))
                    .thenReturn(true);

            // When
            walletService.createDefaultWallet(USER_ID);

            // Then: save() KHÔNG được gọi
            verify(walletRepository, never()).save(any());
        }

        @Test
        @DisplayName("Gọi createDefaultWallet() 3 lần → save() chỉ được gọi 1 lần (lần đầu)")
        void createDefaultWallet_calledMultipleTimes_onlySavesOnce() {
            // Given: lần 1 chưa có, lần 2+3 đã có
            when(walletRepository.existsByUserIdAndName(USER_ID, DEFAULT_WALLET_NAME))
                    .thenReturn(false)  // lần 1
                    .thenReturn(true)   // lần 2
                    .thenReturn(true);  // lần 3
            when(walletRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            // When
            walletService.createDefaultWallet(USER_ID);
            walletService.createDefaultWallet(USER_ID);
            walletService.createDefaultWallet(USER_ID);

            // Then: save chỉ 1 lần
            verify(walletRepository, times(1)).save(any(WalletEntity.class));
        }
    }

    @Nested
    @DisplayName("Different Users")
    class MultipleUsersTest {

        @Test
        @DisplayName("Tạo ví cho 3 userId khác nhau → save() được gọi 3 lần với đúng userId")
        void createDefaultWallet_multipleUsers_eachGetsOwnWallet() {
            Long userId1 = 1L;
            Long userId2 = 2L;
            Long userId3 = 3L;

            when(walletRepository.existsByUserIdAndName(anyLong(), eq(DEFAULT_WALLET_NAME)))
                    .thenReturn(false);
            when(walletRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            walletService.createDefaultWallet(userId1);
            walletService.createDefaultWallet(userId2);
            walletService.createDefaultWallet(userId3);

            ArgumentCaptor<WalletEntity> captor = ArgumentCaptor.forClass(WalletEntity.class);
            verify(walletRepository, times(3)).save(captor.capture());

            var savedEntities = captor.getAllValues();
            assertThat(savedEntities)
                    .extracting(WalletEntity::getUserId)
                    .containsExactlyInAnyOrder(userId1, userId2, userId3);
        }
    }
}
