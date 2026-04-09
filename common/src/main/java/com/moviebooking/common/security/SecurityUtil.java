package com.moviebooking.common.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Utility to extract authenticated user information from Security Context
 */
@Component
public class SecurityUtil {
    
    /**
     * Get currently authenticated user ID
     */
    public static Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal) {
            return ((UserPrincipal) authentication.getPrincipal()).getUserId();
        }
        throw new SecurityException("No authenticated user found");
    }
    
    /**
     * Get currently authenticated user principal
     */
    public static UserPrincipal getCurrentUserPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal) {
            return (UserPrincipal) authentication.getPrincipal();
        }
        throw new SecurityException("No authenticated user found");
    }
    
    /**
     * Get currently authenticated username
     */
    public static String getCurrentUsername() {
        return getCurrentUserPrincipal().getUsername();
    }
    
    /**
     * Check if user is authenticated
     */
    public static boolean isAuthenticated() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && 
               authentication.isAuthenticated() && 
               authentication.getPrincipal() instanceof UserPrincipal;
    }
    
    /**
     * Check if current user has specific role
     */
    public static boolean hasRole(String role) {
        UserPrincipal principal = getCurrentUserPrincipal();
        return principal.getRoles().contains(role);
    }
}
