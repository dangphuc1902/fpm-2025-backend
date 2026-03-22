package com.fpm_2025.wallet_service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fpm_2025.wallet_service.dto.payload.request.CreateWalletRequest;
import com.fpm_2025.wallet_service.dto.payload.response.WalletResponse;
import com.fpm_2025.wallet_service.entity.enums.WalletType;
import com.fpm_2025.wallet_service.security.JwtService;
import com.fpm_2025.wallet_service.service.WalletService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(WalletController.class)
@AutoConfigureMockMvc(addFilters = false)
public class WalletControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private WalletService walletService;

    @Autowired
    private JwtService jwtService;

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

        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(1L, null, Collections.emptyList());

        // Act & Assert
        mockMvc.perform(post("/api/v1/wallets")
                .principal(auth)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Wallet created successfully"))
                .andExpect(jsonPath("$.data.name").value("Savings Wallet"))
                .andExpect(jsonPath("$.data.balance").value(1000));
    }

    @Test
    public void testCreateWallet_InvalidRequest() throws Exception {
        // Arrange
        CreateWalletRequest request = CreateWalletRequest.builder()
                .name("") // Invalid: Blank name
                .type(WalletType.BANK)
                .build();

        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(1L, null, Collections.emptyList());

        // Act & Assert
        mockMvc.perform(post("/api/v1/wallets")
                .principal(auth)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
