package com.fpm2025.user_auth_service.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UserAuthGlobalExceptionHandlerTest {

    @RestController
    static class ThrowingController {
        @GetMapping("/refresh-invalid")
        void refreshInvalid() {
            throw new InvalidRefreshTokenException("Refresh token khong hop le");
        }

        @GetMapping("/missing")
        void missing() throws Exception {
            throw new NoResourceFoundException(HttpMethod.GET, "/missing");
        }
    }

    @RestController
    static class UploadController {
        @GetMapping("/too-large")
        void tooLarge() {
            throw new org.springframework.web.multipart.MaxUploadSizeExceededException(10);
        }
    }

    @Test
    void uploadTooLarge_returns413() throws Exception {
        MockMvcBuilders.standaloneSetup(new UploadController())
                .setControllerAdvice(new UserAuthGlobalExceptionHandler())
                .build()
                .perform(get("/too-large"))
                .andExpect(status().isPayloadTooLarge());
    }

    private final MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new ThrowingController())
            .setControllerAdvice(new UserAuthGlobalExceptionHandler())
            .build();

    @Test
    void invalidRefreshToken_returns401() throws Exception {
        mockMvc.perform(get("/refresh-invalid")).andExpect(status().isUnauthorized());
    }

    @Test
    void unknownRoute_returns404() throws Exception {
        mockMvc.perform(get("/missing")).andExpect(status().isNotFound());
    }
}
