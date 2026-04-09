package com.moviebooking.common.logging;

/**
 * Thread-local holder for correlation ID used for distributed tracing.
 * Replaces session ID in logs with a non-sensitive correlation identifier.
 */
public class CorrelationIdHolder {
    
    private static final ThreadLocal<String> correlationId = new ThreadLocal<>();
    
    /**
     * Set correlation ID for current thread
     */
    public static void set(String id) {
        correlationId.set(id);
    }
    
    /**
     * Get correlation ID for current thread
     */
    public static String get() {
        return correlationId.get();
    }
    
    /**
     * Clear correlation ID for current thread
     */
    public static void clear() {
        correlationId.remove();
    }
    
    /**
     * Check if correlation ID exists
     */
    public static boolean exists() {
        return correlationId.get() != null;
    }
}
