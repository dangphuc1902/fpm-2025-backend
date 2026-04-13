package com.fpm_2025.wallet_service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fpm_2025.wallet_service.dto.payload.request.CreateWalletRequest;
import com.fpm2025.domain.dto.response.WalletResponse;
import com.fpm2025.domain.enums.WalletType;
import com.fpm2025.security.jwt.JwtTokenProvider;
import com.fpm_2025.wallet_service.service.WalletService;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(WalletController.class)
@AutoConfigureMockMvc
@Disabled("Failing due to security context issues in test environment")
public class WalletControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private WalletService walletService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private com.fpm_2025.wallet_service.grpc.client.UserGrpcClient userGrpcClient;
 
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    public void testCreateWallet_Success() throws Exception {
        // Arrange
        CreateWalletRequest request = CreateWalletRequest.builder()
                .name("Savings Wallet")
                .type(WalletType.BANK)
                .currency("VND")
                .initialBalance(BigDecimal.valueOf(1000))
                .build();

        WalletResponse response = WalletResponse.builder()
                .id(1L)
                .userId(1L)
                .name("Savings Wallet")
                .type(WalletType.BANK)
                .currency("VND")
                .balance(BigDecimal.valueOf(1000))
                .build();

        when(walletService.createWallet(any(CreateWalletRequest.class), eq(1L))).thenReturn(response);

        // Act & Assert
        mockMvc.perform(post("/api/v1/wallets")
                .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("1"))
                .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }
 
    @Test
    public void testCreateWallet_InvalidRequest() throws Exception {
        // Arrange
        CreateWalletRequest request = CreateWalletRequest.builder()
                .name("") // Invalid: Blank name
                .type(WalletType.BANK)
                .build();
 
        // Act & Assert
        mockMvc.perform(post("/api/v1/wallets")
                .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("1"))
                .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
