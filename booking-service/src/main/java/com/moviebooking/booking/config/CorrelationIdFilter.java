package com.moviebooking.booking.config;

import com.moviebooking.common.logging.CorrelationIdHolder;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;

/**
 * Filter to generate and manage correlation IDs for request tracing.
 * Replaces session ID with non-sensitive correlation ID in logs.
 */
@Component
@Order(1)
public class CorrelationIdFilter implements Filter {
    
    private static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
    private static final String CORRELATION_ID_MDC_KEY = "correlationId";
    
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        
        try {
            // Get or generate correlation ID
            String correlationId = httpRequest.getHeader(CORRELATION_ID_HEADER);
            if (correlationId == null || correlationId.trim().isEmpty()) {
                correlationId = generateCorrelationId();
            }
            
            // Store in thread-local and MDC for logging
            CorrelationIdHolder.set(correlationId);
            MDC.put(CORRELATION_ID_MDC_KEY, correlationId);
            
            // Add to response header for client tracking
            httpResponse.setHeader(CORRELATION_ID_HEADER, correlationId);
            
            chain.doFilter(request, response);
            
        } finally {
            // Clean up thread-local storage
            CorrelationIdHolder.clear();
            MDC.remove(CORRELATION_ID_MDC_KEY);
        }
    }
    
    /**
     * Generate a new correlation ID
     * Format: CID-{timestamp}-{random}
     */
    private String generateCorrelationId() {
        return "CID-" + System.currentTimeMillis() + "-" + 
               UUID.randomUUID().toString().substring(0, 8);
    }
}
