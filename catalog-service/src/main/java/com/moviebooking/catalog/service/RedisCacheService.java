package com.moviebooking.catalog.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class RedisCacheService {
    
    private final RedisTemplate<String, Object> redisTemplate;
    
    public <T> Optional<T> get(String key, Class<T> type) {
        try {
            Object value = redisTemplate.opsForValue().get(key);
            if (value != null && type.isInstance(value)) {
                log.debug("Cache HIT for key: {}", key);
                return Optional.of(type.cast(value));
            }
            log.debug("Cache MISS for key: {}", key);
            return Optional.empty();
        } catch (Exception e) {
            log.error("Redis GET error for key: {}", key, e);
            return Optional.empty();
        }
    }
    
    public void set(String key, Object value, Duration ttl) {
        try {
            redisTemplate.opsForValue().set(key, value, ttl);
            log.debug("Cache SET for key: {} with TTL: {}", key, ttl);
        } catch (Exception e) {
            log.error("Redis SET error for key: {}", key, e);
        }
    }
    
    public void delete(String key) {
        try {
            redisTemplate.delete(key);
            log.debug("Cache DELETE for key: {}", key);
        } catch (Exception e) {
            log.error("Redis DELETE error for key: {}", key, e);
        }
    }
    
    public boolean exists(String key) {
        try {
            Boolean exists = redisTemplate.hasKey(key);
            return Boolean.TRUE.equals(exists);
        } catch (Exception e) {
            log.error("Redis EXISTS error for key: {}", key, e);
            return false;
        }
    }
}
