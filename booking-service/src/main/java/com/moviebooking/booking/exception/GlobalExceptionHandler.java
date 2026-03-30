package com.moviebooking.booking.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    
    @ExceptionHandler(SeatUnavailableException.class)
    public ResponseEntity<Map<String, Object>> handleSeatUnavailable(SeatUnavailableException ex) {
        log.error("Seat unavailable: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.CONFLICT, "SEAT_UNAVAILABLE", ex.getMessage());
    }
    
    @ExceptionHandler(HoldExpiredException.class)
    public ResponseEntity<Map<String, Object>> handleHoldExpired(HoldExpiredException ex) {
        log.error("Hold expired: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.GONE, "HOLD_EXPIRED", ex.getMessage());
    }
    
    @ExceptionHandler(LockAcquisitionException.class)
    public ResponseEntity<Map<String, Object>> handleLockAcquisition(LockAcquisitionException ex) {
        log.error("Lock acquisition failed: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.CONFLICT, "LOCK_FAILED", ex.getMessage());
    }
    
    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<Map<String, Object>> handleOptimisticLock(ObjectOptimisticLockingFailureException ex) {
        log.error("Optimistic lock failure: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.CONFLICT, "CONCURRENT_MODIFICATION", 
            "Seat was modified by another user. Please try again.");
    }
    
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleGenericError(RuntimeException ex) {
        log.error("Unexpected error: {}", ex.getMessage(), ex);
        return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", 
            "An unexpected error occurred");
    }
    
    private ResponseEntity<Map<String, Object>> buildErrorResponse(
        HttpStatus status, String errorCode, String message
    ) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("timestamp", Instant.now().toString());
        errorResponse.put("status", status.value());
        errorResponse.put("error", status.getReasonPhrase());
        errorResponse.put("errorCode", errorCode);
        errorResponse.put("message", message);
        
        return ResponseEntity.status(status).body(errorResponse);
    }
}
