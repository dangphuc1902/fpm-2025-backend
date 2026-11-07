package com.fpm2025.user_auth_service.controller;

import com.fpm2025.domain.common.BaseResponse;
import com.fpm2025.user_auth_service.entity.UserEntity;
import com.fpm2025.user_auth_service.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Android doc ho so tu truong "data" (ApiResponse<UserDto>), body tran lam app hien sai ten nguoi dung. */
class UserControllerTest {

    private UserController controller;

    @BeforeEach
    void setUp() {
        UserRepository repository = mock(UserRepository.class);
        UserEntity user = new UserEntity();
        user.setId(1L);
        user.setEmail("a@test.local");
        user.setUsername("Tester");
        when(repository.findById(1)).thenReturn(Optional.of(user));
        controller = new UserController(repository);
    }

    private Object dataOf(ResponseEntity<?> response) {
        return assertInstanceOf(BaseResponse.class, response.getBody()).getData();
    }

    @Test
    void getCurrentUser_wrapsProfileInData() {
        Map<?, ?> data = assertInstanceOf(Map.class, dataOf(controller.getCurrentUser("1")));

        assertEquals("a@test.local", data.get("email"));
        assertEquals("Tester", data.get("username"));
    }

    @Test
    void updateProfile_wrapsProfileInData() {
        Map<?, ?> data = assertInstanceOf(Map.class,
                dataOf(controller.updateProfile("1", Map.of("username", "Ten moi"))));

        assertEquals("Ten moi", data.get("username"));
    }
}
