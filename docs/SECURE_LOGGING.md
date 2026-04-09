# Secure Logging Implementation

## Overview
This document describes the comprehensive security logging implementation for the Movie Booking Platform. All sensitive data in logs is now masked, replaced with correlation IDs, and controlled through environment-specific log levels.

## Security Features Implemented

### 1. Data Masking Utilities

**Location**: `common/src/main/java/com/moviebooking/common/security/DataMaskingUtil.java`

Provides specialized masking for different data types:

- **User ID Masking**: `12345` → `user_***45`
- **Session ID Masking**: `session-123456789abc` → `session-1***`
- **Transaction ID Masking**: `txn-abc123def456` → `txn_hash_a1b2`
- **Email Masking**: `john.doe@example.com` → `j***@example.com`
- **Phone Masking**: `+1234567890` → `+12***90`
- **Gateway Transaction ID**: `TXN_abc123def456` → `TXN_***456`

**Usage Example**:
```java
log.info("User: {} created booking", DataMaskingUtil.maskUserId(userId));
```

### 2. @Sensitive Annotation

**Location**: `common/src/main/java/com/moviebooking/common/security/Sensitive.java`

Mark fields in DTOs for automatic masking:

```java
@Sensitive(Sensitive.MaskType.PARTIAL)
private Long userId;

@Sensitive(Sensitive.MaskType.HASH)
private String sessionId;

@Sensitive(Sensitive.MaskType.FULL)
private String creditCard;
```

**Mask Types**:
- `PARTIAL`: Shows first and last 2 chars (default)
- `HASH`: Shows SHA-256 hash prefix
- `FULL`: Complete masking

### 3. Correlation ID System

**Location**: `booking-service/src/main/java/com/moviebooking/booking/config/CorrelationIdFilter.java`

Replaces session IDs with non-sensitive correlation IDs for request tracing.

**Format**: `CID-{timestamp}-{random8chars}`
**Example**: `CID-1712345678901-a1b2c3d4`

**Features**:
- Accepts `X-Correlation-ID` header from clients
- Auto-generates if not provided
- Stored in MDC (Mapped Diagnostic Context) for automatic inclusion in all logs
- Returned in response headers for client-side tracing
- Thread-safe via ThreadLocal storage

**Example Log Output**:
```
2024-03-28 10:15:23.456 [http-nio-8082-exec-1] [CID-1712345678901-a1b2c3d4] INFO  BookingController - Creating booking for user: user_***45
```

### 4. Automatic Logging Aspect

**Location**: `booking-service/src/main/java/com/moviebooking/booking/aspect/SensitiveDataLoggingAspect.java`

Intercepts all controller and service methods to automatically mask sensitive parameters.

**Features**:
- Detects `@Sensitive` annotations on parameters and fields
- Automatically masks complex objects
- Handles collections intelligently (limits output to 10 items)
- Debug-level logging with full context

**Example**:
```java
// Automatic masking happens in the aspect
[CID-123] Entering BookingService.createBooking with args: [{userId=user_***45, showId=100, seatIds=[1,2,3]}]
```

### 5. Environment-Specific Log Levels

**Location**: `booking-service/src/main/resources/application.yml`

#### Default Profile (Local Development)
```yaml
logging:
  level:
    root: INFO
    com.moviebooking.booking: INFO
```

#### Dev Profile
```yaml
logging:
  level:
    root: INFO
    com.moviebooking.booking: DEBUG
    org.hibernate.SQL: DEBUG
```

#### Production Profile
```yaml
logging:
  level:
    root: WARN
    com.moviebooking.booking: INFO
    com.moviebooking.booking.aspect: ERROR  # Disable DEBUG logs with transactionId
```

**Running with profiles**:
```bash
# Development
java -jar booking-service.jar --spring.profiles.active=dev

# Production
java -jar booking-service.jar --spring.profiles.active=prod
```

### 6. Structured Logging with MDC

**Location**: `booking-service/src/main/resources/logback-spring.xml`

**Features**:
- Correlation ID automatically injected via MDC
- Time-based log rotation (daily)
- Size-based rotation (100MB per file)
- 30-day retention
- Async appender for production performance
- Separate file logging for production

**Log Pattern**:
```
%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] [%X{correlationId:-NO-CID}] %-5level %logger{36} - %msg%n
```

## Updated Files

### Services with Masked Logging

1. **BookingController.java**
   - User IDs masked in all endpoints
   - Correlation ID in every log statement
   - Session IDs removed from logs

2. **BookingService.java**
   - User IDs masked: `user_***45`
   - Session ID references removed
   - Correlation ID tracking throughout

3. **SeatLockService.java**
   - Transaction IDs masked at DEBUG level: `txn_hash_a1b2`
   - Session IDs replaced with correlation IDs
   - User IDs masked in Redis hold operations

4. **PaymentService.java**
   - Gateway transaction IDs masked: `TXN_***456`
   - Correlation ID for payment tracing

### DTOs with @Sensitive Annotations

1. **HoldSeatsRequest.java**
   - `sessionId` marked as `@Sensitive`
   - `userId` marked as `@Sensitive`

2. **BookingRequest.java**
   - `userId` marked as `@Sensitive`
   - `sessionId` marked as `@Sensitive`
   - `contactEmail` marked as `@Sensitive`
   - `contactPhone` marked as `@Sensitive`

## Security Benefits

### Before Implementation
```log
2024-03-28 10:15:23 INFO BookingController - POST /bookings - user: 12345, session: session-abc123def456
2024-03-28 10:15:24 INFO SeatLockService - Lock acquired for seat: 1 by transaction: txn-abc123def456
```

**Risks**: Exposed user IDs, session IDs, transaction IDs

### After Implementation
```log
2024-03-28 10:15:23 [CID-1712345678901-a1b2c3d4] INFO BookingController - POST /bookings - user: user_***45, show: 100
2024-03-28 10:15:24 [CID-1712345678901-a1b2c3d4] DEBUG SeatLockService - Lock acquired for seat: 1
```

**Benefits**:
- User IDs masked
- Session IDs eliminated from logs
- Transaction IDs only in DEBUG (disabled in prod)
- Non-sensitive correlation ID for tracing
- Full request lifecycle trackable via CID

## Compliance

### GDPR Compliance
✅ Personal identifiers masked  
✅ Email addresses masked  
✅ Phone numbers masked  
✅ Right to be forgotten supported (no plain-text user data in logs)

### PCI-DSS Compliance
✅ No card data logged  
✅ Gateway transaction IDs masked  
✅ Payment details excluded from logs

### OWASP Logging Best Practices
✅ No sensitive data in plain text  
✅ Correlation IDs for tracing  
✅ Environment-specific log levels  
✅ Log rotation and retention policies

## Testing the Implementation

### 1. Test Correlation ID
```bash
# Request with correlation ID
curl -H "X-Correlation-ID: TEST-123" http://localhost:8082/api/v1/bookings/hold

# Check logs for: [TEST-123]
```

### 2. Test Data Masking
```bash
# Create booking
curl -X POST http://localhost:8082/api/v1/bookings \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 12345,
    "showId": 1,
    "seatIds": [1,2,3],
    "sessionId": "session-abc123"
  }'

# Check logs for masked values:
# - user_***45 (not 12345)
# - Correlation ID (not session-abc123)
```

### 3. Test Environment-Specific Logging
```bash
# Run in dev mode (DEBUG enabled)
java -jar booking-service.jar --spring.profiles.active=dev

# Run in prod mode (DEBUG disabled)
java -jar booking-service.jar --spring.profiles.active=prod
```

### 4. Test Aspect-Based Masking
Enable DEBUG logging and check for automatic parameter masking:
```bash
# Set log level to DEBUG
# Check logs for automatic masking in method entry logs
```

## Production Deployment Checklist

- [ ] Set `spring.profiles.active=prod`
- [ ] Verify log level is INFO or WARN
- [ ] Confirm DEBUG logs are disabled
- [ ] Test correlation ID propagation
- [ ] Verify no sensitive data in logs
- [ ] Configure log aggregation (ELK Stack)
- [ ] Set up log monitoring and alerts
- [ ] Test log rotation and retention
- [ ] Document correlation ID usage for support teams
- [ ] Train operations team on masked data interpretation

## Future Enhancements

1. **JSON-Structured Logging**: Add Logstash encoder for structured JSON logs
2. **Sensitive Data Detection**: ML-based detection of accidental PII logging
3. **Audit Trail**: Separate audit log with encrypted sensitive data
4. **Log Anonymization**: Periodic batch anonymization of historical logs
5. **Compliance Reporting**: Automated compliance reports from log analysis

## Support

For questions or issues related to secure logging:
- Review DataMaskingUtil for masking methods
- Check CorrelationIdFilter for tracing issues
- Consult logback-spring.xml for log configuration
- Reference this document for implementation details

---

**Last Updated**: 2024-03-28  
**Version**: 1.0  
**Status**: Production-Ready
