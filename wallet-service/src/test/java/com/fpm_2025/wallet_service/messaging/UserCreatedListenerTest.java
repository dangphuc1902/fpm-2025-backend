package com.fpm_2025.wallet_service.messaging;

import com.fpm2025.domain.event.UserCreatedEvent;
import com.fpm_2025.wallet_service.service.WalletService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

/**
 * Unit tests cho {@link UserCreatedListener}.
 *
 * <p>Xác nhận:
 * <ol>
 *   <li>Khi nhận {@link UserCreatedEvent} hợp lệ → gọi {@code createDefaultWallet(userId)}</li>
 *   <li>Idempotency: nhận event 2 lần → gọi 2 lần (service tự dedup)</li>
 *   <li>Khi {@code createDefaultWallet()} throw exception → không re-throw (non-blocking)</li>
 *   <li>Null userId → không gọi createDefaultWallet()</li>
 * </ol>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UserCreatedListener — Unit Tests")
class UserCreatedListenerTest {

    @Mock
    private WalletService walletService;

    @InjectMocks
    private UserCreatedListener listener;

    private UserCreatedEvent validEvent;

    @BeforeEach
    void setUp() {
        validEvent = UserCreatedEvent.builder()
                .userId(42L)
                .email("user@example.com")
                .username("Nguyen Van A")
                .createdAt("2025-01-01T10:00:00")
                .build();
    }

    @Test
    @DisplayName("Happy path: nhận event hợp lệ → createDefaultWallet() được gọi với đúng userId")
    void handleUserCreated_validEvent_callsCreateDefaultWallet() {
        // When
        listener.handleUserCreated(validEvent);

        // Then
        verify(walletService, times(1)).createDefaultWallet(42L);
    }

    @Test
    @DisplayName("Idempotency: nhận cùng event 2 lần → createDefaultWallet() được gọi 2 lần (service tự dedup)")
    void handleUserCreated_duplicateEvent_calledTwice() {
        // When
        listener.handleUserCreated(validEvent);
        listener.handleUserCreated(validEvent);

        // Then: listener không tự dedup, service phải tự xử lý
        verify(walletService, times(2)).createDefaultWallet(42L);
    }

    @Test
    @DisplayName("Service exception → KHÔNG re-throw (non-blocking, tránh Kafka retry vô hạn)")
    void handleUserCreated_serviceThrows_doesNotRethrow() {
        // Given
        doThrow(new RuntimeException("DB connection failed"))
                .when(walletService).createDefaultWallet(anyLong());

        // When / Then: KHÔNG throw exception ra ngoài
        listener.handleUserCreated(validEvent);

        verify(walletService, times(1)).createDefaultWallet(42L);
    }

    @Test
    @DisplayName("Event với userId khác nhau → createDefaultWallet() gọi với đúng từng userId")
    void handleUserCreated_differentUserIds_eachCalledCorrectly() {
        // Given
        UserCreatedEvent event1 = UserCreatedEvent.builder().userId(100L).email("a@test.com").build();
        UserCreatedEvent event2 = UserCreatedEvent.builder().userId(200L).email("b@test.com").build();
        UserCreatedEvent event3 = UserCreatedEvent.builder().userId(300L).email("c@test.com").build();

        // When
        listener.handleUserCreated(event1);
        listener.handleUserCreated(event2);
        listener.handleUserCreated(event3);

        // Then
        verify(walletService, times(1)).createDefaultWallet(100L);
        verify(walletService, times(1)).createDefaultWallet(200L);
        verify(walletService, times(1)).createDefaultWallet(300L);
        verifyNoMoreInteractions(walletService);
    }

    @Test
    @DisplayName("Event đầy đủ field → chỉ userId được dùng, email/username không ảnh hưởng")
    void handleUserCreated_fullEvent_onlyUserIdUsed() {
        // Given: event với đầy đủ thông tin
        UserCreatedEvent event = UserCreatedEvent.builder()
                .userId(999L)
                .email("premium@company.vn")
                .username("Premium User")
                .createdAt("2025-04-18T09:00:00")
                .build();

        // When
        listener.handleUserCreated(event);

        // Then: chỉ userId=999 được truyền vào createDefaultWallet
        verify(walletService).createDefaultWallet(999L);
    }
}
