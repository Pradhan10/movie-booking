package com.moviebooking.booking.aspect;

import com.moviebooking.common.logging.CorrelationIdHolder;
import com.moviebooking.common.security.DataMaskingUtil;
import com.moviebooking.common.security.Sensitive;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.lang.reflect.Parameter;
import java.util.*;

/**
 * Aspect for automatic masking of sensitive data in logs.
 * Intercepts method calls and masks parameters annotated with @Sensitive.
 */
@Aspect
@Component
public class SensitiveDataLoggingAspect {
    
    private static final Logger log = LoggerFactory.getLogger(SensitiveDataLoggingAspect.class);
    
    /**
     * Intercept methods in controller and service packages
     */
    @Around("execution(* com.moviebooking.booking.controller..*(..)) || " +
            "execution(* com.moviebooking.booking.service..*(..))")
    public Object maskSensitiveData(ProceedingJoinPoint joinPoint) throws Throwable {
        
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Object[] args = joinPoint.getArgs();
        Parameter[] parameters = signature.getMethod().getParameters();
        
        // Log method entry with masked parameters
        if (log.isDebugEnabled() && args.length > 0) {
            String maskedArgs = maskMethodArguments(parameters, args);
            String correlationId = CorrelationIdHolder.get();
            log.debug("[{}] Entering {}.{} with args: {}", 
                     correlationId != null ? correlationId : "NO-CID",
                     signature.getDeclaringTypeName(), 
                     signature.getName(), 
                     maskedArgs);
        }
        
        return joinPoint.proceed();
    }
    
    /**
     * Mask sensitive parameters before logging
     */
    private String maskMethodArguments(Parameter[] parameters, Object[] args) {
        StringBuilder result = new StringBuilder("[");
        
        for (int i = 0; i < args.length; i++) {
            if (i > 0) {
                result.append(", ");
            }
            
            if (args[i] == null) {
                result.append("null");
                continue;
            }
            
            // Check if parameter is annotated with @Sensitive
            if (parameters[i].isAnnotationPresent(Sensitive.class)) {
                result.append(maskValue(args[i].toString()));
            } else {
                // Check if object has @Sensitive fields
                result.append(maskObjectFields(args[i]));
            }
        }
        
        result.append("]");
        return result.toString();
    }
    
    /**
     * Mask object fields annotated with @Sensitive
     */
    private String maskObjectFields(Object obj) {
        if (obj == null) {
            return "null";
        }
        
        // For primitive types, just return as string
        if (obj.getClass().isPrimitive() || obj instanceof String || 
            obj instanceof Number || obj instanceof Boolean) {
            return obj.toString();
        }
        
        // For collections, mask each element
        if (obj instanceof Collection) {
            return maskCollection((Collection<?>) obj);
        }
        
        // For complex objects, check for @Sensitive fields
        try {
            Map<String, Object> fieldMap = new HashMap<>();
            Field[] fields = obj.getClass().getDeclaredFields();
            
            for (Field field : fields) {
                field.setAccessible(true);
                Object value = field.get(obj);
                
                if (field.isAnnotationPresent(Sensitive.class)) {
                    fieldMap.put(field.getName(), maskValue(value != null ? value.toString() : "null"));
                } else {
                    fieldMap.put(field.getName(), value);
                }
            }
            
            return fieldMap.toString();
        } catch (Exception e) {
            return obj.getClass().getSimpleName() + "@" + Integer.toHexString(obj.hashCode());
        }
    }
    
    /**
     * Mask collection elements
     */
    private String maskCollection(Collection<?> collection) {
        if (collection.isEmpty()) {
            return "[]";
        }
        
        StringBuilder sb = new StringBuilder("[");
        int count = 0;
        for (Object item : collection) {
            if (count > 0) {
                sb.append(", ");
            }
            if (count >= 10) {
                sb.append("... (").append(collection.size() - 10).append(" more)");
                break;
            }
            sb.append(item);
            count++;
        }
        sb.append("]");
        return sb.toString();
    }
    
    /**
     * Apply appropriate masking based on value type
     */
    private String maskValue(String value) {
        if (value == null || value.equals("null")) {
            return "null";
        }
        
        // Try to detect type and apply appropriate masking
        if (value.contains("@")) {
            return DataMaskingUtil.maskEmail(value);
        } else if (value.matches("^\\+?[0-9]{10,}$")) {
            return DataMaskingUtil.maskPhone(value);
        } else if (value.matches("^[0-9]+$")) {
            return DataMaskingUtil.maskUserId(Long.parseLong(value));
        } else {
            return DataMaskingUtil.maskPartial(value);
        }
    }
}
