package com.moviebooking.common.security;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

/**
 * Utility class for masking sensitive data in logs.
 * Provides various masking strategies for different types of sensitive information.
 */
public class DataMaskingUtil {
    
    private static final String MASK_CHAR = "*";
    
    /**
     * Mask user ID - shows user_ prefix and last 2 digits
     * Example: 12345 -> user_***45
     */
    public static String maskUserId(Long userId) {
        if (userId == null) {
            return "null";
        }
        String idStr = userId.toString();
        if (idStr.length() <= 2) {
            return "user_" + MASK_CHAR.repeat(idStr.length());
        }
        String lastTwo = idStr.substring(idStr.length() - 2);
        return "user_" + MASK_CHAR.repeat(3) + lastTwo;
    }
    
    /**
     * Mask session ID - shows first 8 chars only
     * Example: session-123456789abc -> session-1***
     */
    public static String maskSessionId(String sessionId) {
        if (sessionId == null || sessionId.isEmpty()) {
            return "null";
        }
        if (sessionId.length() <= 10) {
            return sessionId.substring(0, Math.min(4, sessionId.length())) + MASK_CHAR.repeat(4);
        }
        return sessionId.substring(0, 10) + MASK_CHAR.repeat(4);
    }
    
    /**
     * Mask transaction ID - shows hash prefix
     * Example: txn-abc123def456 -> txn_hash_a1b2
     */
    public static String maskTransactionId(String transactionId) {
        if (transactionId == null || transactionId.isEmpty()) {
            return "null";
        }
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(transactionId.getBytes());
            String hashStr = Base64.getEncoder().encodeToString(hash).substring(0, 8);
            return "txn_hash_" + hashStr;
        } catch (NoSuchAlgorithmException e) {
            return "txn_" + MASK_CHAR.repeat(8);
        }
    }
    
    /**
     * Mask email address - shows first char and domain
     * Example: john.doe@example.com -> j***@example.com
     */
    public static String maskEmail(String email) {
        if (email == null || email.isEmpty()) {
            return "null";
        }
        int atIndex = email.indexOf('@');
        if (atIndex <= 0) {
            return MASK_CHAR.repeat(5) + "@domain.com";
        }
        String local = email.substring(0, atIndex);
        String domain = email.substring(atIndex);
        
        if (local.length() == 1) {
            return local + MASK_CHAR.repeat(3) + domain;
        }
        return local.charAt(0) + MASK_CHAR.repeat(3) + domain;
    }
    
    /**
     * Mask phone number - shows country code and last 2 digits
     * Example: +1234567890 -> +12***90
     */
    public static String maskPhone(String phone) {
        if (phone == null || phone.isEmpty()) {
            return "null";
        }
        if (phone.length() <= 4) {
            return MASK_CHAR.repeat(phone.length());
        }
        String prefix = phone.substring(0, Math.min(3, phone.length()));
        String suffix = phone.substring(phone.length() - 2);
        return prefix + MASK_CHAR.repeat(3) + suffix;
    }
    
    /**
     * Mask gateway transaction ID - shows prefix only
     * Example: TXN_abc123def456 -> TXN_***456
     */
    public static String maskGatewayTxnId(String txnId) {
        if (txnId == null || txnId.isEmpty()) {
            return "null";
        }
        if (txnId.length() <= 7) {
            return txnId.substring(0, Math.min(4, txnId.length())) + MASK_CHAR.repeat(3);
        }
        String suffix = txnId.substring(txnId.length() - 3);
        return txnId.substring(0, 4) + MASK_CHAR.repeat(3) + suffix;
    }
    
    /**
     * Generic partial masking - shows first and last 2 chars
     * Example: sensitive123 -> se***23
     */
    public static String maskPartial(String value) {
        if (value == null || value.isEmpty()) {
            return "null";
        }
        if (value.length() <= 4) {
            return MASK_CHAR.repeat(value.length());
        }
        return value.substring(0, 2) + MASK_CHAR.repeat(3) + value.substring(value.length() - 2);
    }
    
    /**
     * Full masking - replaces entire value
     * Example: secret -> ******
     */
    public static String maskFull(String value) {
        if (value == null || value.isEmpty()) {
            return "null";
        }
        return MASK_CHAR.repeat(Math.min(value.length(), 8));
    }
}
