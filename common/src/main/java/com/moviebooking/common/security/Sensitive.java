package com.moviebooking.common.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to mark fields as sensitive for automatic masking in logs.
 * Fields annotated with @Sensitive will be automatically masked when logged.
 */
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface Sensitive {
    
    /**
     * Type of masking to apply
     */
    MaskType value() default MaskType.PARTIAL;
    
    enum MaskType {
        PARTIAL,    // Show first and last 2 chars: 12345 -> 12***45
        HASH,       // Show hash: 12345 -> hash_a1b2c3
        FULL        // Complete mask: 12345 -> *****
    }
}
