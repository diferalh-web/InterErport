package com.interexport.guarantees.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Redis utility service for manual cache operations.
 * 
 * Provides methods for:
 * - Manual cache operations
 * - Cache invalidation
 * - Cache statistics
 * - Key management
 * 
 * @author InterExport Development Team
 * @version 1.0.0-POC
 */
@Service
public class RedisService {

    private final RedisTemplate<String, Object> redisTemplate;

    @Autowired
    public RedisService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Store a value in Redis with TTL
     */
    public void set(String key, Object value, Duration ttl) {
        redisTemplate.opsForValue().set(key, value, ttl);
    }

    /**
     * Store a value in Redis with TTL in seconds
     */
    public void set(String key, Object value, long ttlSeconds) {
        redisTemplate.opsForValue().set(key, value, ttlSeconds, TimeUnit.SECONDS);
    }

    /**
     * Get a value from Redis
     */
    public Object get(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    /**
     * Delete a key from Redis
     */
    public void delete(String key) {
        redisTemplate.delete(key);
    }

    /**
     * Delete multiple keys from Redis
     */
    public void delete(Set<String> keys) {
        redisTemplate.delete(keys);
    }

    /**
     * Check if a key exists in Redis
     */
    public boolean exists(String key) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    /**
     * Get TTL for a key in seconds
     */
    public long getTtl(String key) {
        return redisTemplate.getExpire(key, TimeUnit.SECONDS);
    }

    /**
     * Set TTL for an existing key
     */
    public boolean setTtl(String key, long ttlSeconds) {
        return Boolean.TRUE.equals(redisTemplate.expire(key, ttlSeconds, TimeUnit.SECONDS));
    }

    /**
     * Clear all cache entries for a specific pattern
     */
    public void clearCacheByPattern(String pattern) {
        Set<String> keys = redisTemplate.keys(pattern);
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }

    /**
     * Clear all dashboard cache entries
     */
    public void clearDashboardCache() {
        clearCacheByPattern("dashboard:*");
    }

    /**
     * Clear all FX rates cache entries
     */
    public void clearFxRatesCache() {
        clearCacheByPattern("fxRates:*");
    }

    /**
     * Clear all cache entries
     */
    public void clearAllCache() {
        clearCacheByPattern("*");
    }

    /**
     * Get cache statistics
     */
    public String getCacheStats() {
        Set<String> allKeys = redisTemplate.keys("*");
        if (allKeys == null) {
            return "No keys found";
        }

        long dashboardKeys = allKeys.stream().filter(key -> key.startsWith("dashboard:")).count();
        long fxRateKeys = allKeys.stream().filter(key -> key.startsWith("fxRates:")).count();
        long sessionKeys = allKeys.stream().filter(key -> key.startsWith("guarantees:session:")).count();

        return String.format("Cache Statistics:\n" +
                "Total Keys: %d\n" +
                "Dashboard Keys: %d\n" +
                "FX Rate Keys: %d\n" +
                "Session Keys: %d",
                allKeys.size(), dashboardKeys, fxRateKeys, sessionKeys);
    }
}

