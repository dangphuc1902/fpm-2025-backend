package com.fpm_2025.wallet_service.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class CacheService {
	private Logger logger  = LoggerFactory.getLogger(CacheService.class);
    private final RedisTemplate<String, Object> redisTemplate;
    private final CacheManager cacheManager;
    // ========== Wallet Balance Cache ==========
    
    public void cacheWalletBalance(Long walletId, Object balance) {
        String key = "wallet:balance:" + walletId;
        redisTemplate.opsForValue().set(key, balance, 5, TimeUnit.MINUTES);
        logger.debug("Cached wallet balance: walletId={}", walletId);
    }

    public Object getWalletBalance(Long walletId) {
        String key = "wallet:balance:" + walletId;
        return redisTemplate.opsForValue().get(key);
    }

    public void evictWalletBalance(Long walletId) {
        String key = "wallet:balance:" + walletId;
        redisTemplate.delete(key);
        logger.debug("Evicted wallet balance cache: walletId={}", walletId);
    }

    // ========== User Wallets Cache ==========
    
    public void cacheUserWallets(Long userId, Object wallets) {
        String key = "user:wallets:" + userId;
        redisTemplate.opsForValue().set(key, wallets, 10, TimeUnit.MINUTES);
        logger.debug("Cached user wallets: userId={}", userId);
    }

    public Object getUserWallets(Long userId) {
        String key = "user:wallets:" + userId;
        return redisTemplate.opsForValue().get(key);
    }

    public void evictUserWallets(Long userId) {
        String key = "user:wallets:" + userId;
        redisTemplate.delete(key);
        
        // Also evict from Spring Cache
        Objects.requireNonNull(cacheManager.getCache("user:wallets"))
                .evict(userId);
        logger.debug("Evicted user wallets cache: userId={}", userId);
    }
}