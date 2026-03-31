package com.hdbank.attendance.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * Caffeine L1 (local) cache configuration for attendance-service.
 *
 * Cached data:
 * - bssid-cache: WiFi access point lookups by BSSID (5min TTL, max 1000 entries)
 * - shift-cache: Current shift lookups (5min TTL, max 500 entries)
 * - org-tree-cache: Organization tree structure (10min TTL, max 200 entries)
 *
 * Note: Check-in records are NOT cached per architecture requirements.
 */
@Configuration
@EnableCaching
public class CacheConfig {

    public static final String BSSID_CACHE = "bssid-cache";
    public static final String SHIFT_CACHE = "shift-cache";
    public static final String ORG_TREE_CACHE = "org-tree-cache";

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        cacheManager.setCacheNames(java.util.List.of(BSSID_CACHE, SHIFT_CACHE, ORG_TREE_CACHE));
        cacheManager.setCaffeine(defaultCaffeineSpec());
        // Register individual cache specs
        cacheManager.registerCustomCache(BSSID_CACHE, bssidCache());
        cacheManager.registerCustomCache(SHIFT_CACHE, shiftCache());
        cacheManager.registerCustomCache(ORG_TREE_CACHE, orgTreeCache());
        return cacheManager;
    }

    private Caffeine<Object, Object> defaultCaffeineSpec() {
        return Caffeine.newBuilder()
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .maximumSize(500);
    }

    private com.github.benmanes.caffeine.cache.Cache<Object, Object> bssidCache() {
        return Caffeine.newBuilder()
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .maximumSize(1000)
                .recordStats()
                .build();
    }

    private com.github.benmanes.caffeine.cache.Cache<Object, Object> shiftCache() {
        return Caffeine.newBuilder()
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .maximumSize(500)
                .recordStats()
                .build();
    }

    private com.github.benmanes.caffeine.cache.Cache<Object, Object> orgTreeCache() {
        return Caffeine.newBuilder()
                .expireAfterWrite(10, TimeUnit.MINUTES)
                .maximumSize(200)
                .recordStats()
                .build();
    }
}
