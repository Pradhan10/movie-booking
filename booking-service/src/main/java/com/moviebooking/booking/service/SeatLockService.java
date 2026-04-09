package com.moviebooking.booking.service;

import com.moviebooking.common.logging.CorrelationIdHolder;
import com.moviebooking.common.security.DataMaskingUtil;
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
        String correlationId = CorrelationIdHolder.get();
        try {
            Boolean acquired = redisTemplate.opsForValue()
                .setIfAbsent(lockKey, transactionId, LOCK_TTL);
            
            if (Boolean.TRUE.equals(acquired)) {
                if (log.isDebugEnabled()) {
                    log.debug("[{}] Lock acquired for seat: {} by txn: {}", 
                            correlationId, seatId, DataMaskingUtil.maskTransactionId(transactionId));
                }
                return true;
            } else {
                if (log.isDebugEnabled()) {
                    log.debug("[{}] Failed to acquire lock for seat: {}", 
                            correlationId, seatId);
                }
                return false;
            }
        } catch (Exception e) {
            log.error("[{}] Error acquiring lock for seat: {}", correlationId, seatId, e);
            return false;
        }
    }
    
    public void releaseLock(Long seatId, String transactionId) {
        String lockKey = generateLockKey(seatId);
        String correlationId = CorrelationIdHolder.get();
        try {
            Object currentHolder = redisTemplate.opsForValue().get(lockKey);
            if (transactionId.equals(currentHolder)) {
                redisTemplate.delete(lockKey);
                if (log.isDebugEnabled()) {
                    log.debug("[{}] Lock released for seat: {}", correlationId, seatId);
                }
            } else {
                if (log.isDebugEnabled()) {
                    log.debug("[{}] Cannot release lock for seat: {} - not owned", 
                            correlationId, seatId);
                }
            }
        } catch (Exception e) {
            log.error("[{}] Error releasing lock for seat: {}", correlationId, seatId, e);
        }
    }
    
    public void holdSeats(String sessionId, List<Long> seatIds, Long userId, Instant expiresAt) {
        String holdKey = generateHoldKey(sessionId);
        String correlationId = CorrelationIdHolder.get();
        try {
            Map<String, Object> holdData = new HashMap<>();
            holdData.put("seatIds", seatIds);
            holdData.put("userId", userId);
            holdData.put("expiresAt", expiresAt.toString());
            
            redisTemplate.opsForHash().putAll(holdKey, holdData);
            redisTemplate.expire(holdKey, HOLD_TTL);
            
            log.info("[{}] Seats held - user: {}, seatCount: {}, expires: {}", 
                    correlationId,
                    DataMaskingUtil.maskUserId(userId),
                    seatIds.size(), 
                    expiresAt);
        } catch (Exception e) {
            log.error("[{}] Error holding seats", correlationId, e);
        }
    }
    
    public void releaseHold(String sessionId) {
        String holdKey = generateHoldKey(sessionId);
        String correlationId = CorrelationIdHolder.get();
        try {
            redisTemplate.delete(holdKey);
            log.info("[{}] Hold released", correlationId);
        } catch (Exception e) {
            log.error("[{}] Error releasing hold", correlationId, e);
        }
    }
    
    @SuppressWarnings("unchecked")
    public Set<Long> getHeldSeats(String sessionId) {
        String holdKey = generateHoldKey(sessionId);
        String correlationId = CorrelationIdHolder.get();
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
            log.error("[{}] Error getting held seats", correlationId, e);
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
