package com.moviebooking.booking.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SeatLockService {
    
    private final RedisTemplate<String, Object> redisTemplate;
    
    private static final Duration LOCK_TTL = Duration.ofSeconds(30);
    private static final Duration HOLD_TTL = Duration.ofMinutes(10);
    
    public boolean acquireLock(Long seatId, String transactionId) {
        String lockKey = generateLockKey(seatId);
        try {
            Boolean acquired = redisTemplate.opsForValue()
                .setIfAbsent(lockKey, transactionId, LOCK_TTL);
            
            if (Boolean.TRUE.equals(acquired)) {
                log.debug("Lock acquired for seat: {} by transaction: {}", seatId, transactionId);
                return true;
            } else {
                log.warn("Failed to acquire lock for seat: {} by transaction: {}", seatId, transactionId);
                return false;
            }
        } catch (Exception e) {
            log.error("Error acquiring lock for seat: {}", seatId, e);
            return false;
        }
    }
    
    public void releaseLock(Long seatId, String transactionId) {
        String lockKey = generateLockKey(seatId);
        try {
            Object currentHolder = redisTemplate.opsForValue().get(lockKey);
            if (transactionId.equals(currentHolder)) {
                redisTemplate.delete(lockKey);
                log.debug("Lock released for seat: {} by transaction: {}", seatId, transactionId);
            } else {
                log.warn("Cannot release lock for seat: {} - not owned by transaction: {}", seatId, transactionId);
            }
        } catch (Exception e) {
            log.error("Error releasing lock for seat: {}", seatId, e);
        }
    }
    
    public void holdSeats(String sessionId, List<Long> seatIds, Long userId, Instant expiresAt) {
        String holdKey = generateHoldKey(sessionId);
        try {
            Map<String, Object> holdData = new HashMap<>();
            holdData.put("seatIds", seatIds);
            holdData.put("userId", userId);
            holdData.put("expiresAt", expiresAt.toString());
            
            redisTemplate.opsForHash().putAll(holdKey, holdData);
            redisTemplate.expire(holdKey, HOLD_TTL);
            
            log.info("Seats held for session: {} - seats: {}, expires: {}", sessionId, seatIds, expiresAt);
        } catch (Exception e) {
            log.error("Error holding seats for session: {}", sessionId, e);
        }
    }
    
    public void releaseHold(String sessionId) {
        String holdKey = generateHoldKey(sessionId);
        try {
            redisTemplate.delete(holdKey);
            log.info("Hold released for session: {}", sessionId);
        } catch (Exception e) {
            log.error("Error releasing hold for session: {}", sessionId, e);
        }
    }
    
    @SuppressWarnings("unchecked")
    public Set<Long> getHeldSeats(String sessionId) {
        String holdKey = generateHoldKey(sessionId);
        try {
            Object seatIdsObj = redisTemplate.opsForHash().get(holdKey, "seatIds");
            if (seatIdsObj instanceof List) {
                return ((List<?>) seatIdsObj).stream()
                    .filter(obj -> obj instanceof Number)
                    .map(obj -> ((Number) obj).longValue())
                    .collect(Collectors.toSet());
            }
            return Set.of();
        } catch (Exception e) {
            log.error("Error getting held seats for session: {}", sessionId, e);
            return Set.of();
        }
    }
    
    public boolean isHeldBySession(Long seatId, String sessionId) {
        Set<Long> heldSeats = getHeldSeats(sessionId);
        return heldSeats.contains(seatId);
    }
    
    private String generateLockKey(Long seatId) {
        return "lock:seat:" + seatId;
    }
    
    private String generateHoldKey(String sessionId) {
        return "seat:hold:" + sessionId;
    }
}
