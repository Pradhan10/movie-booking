# Security Issues - Quick Reference

## 🔴 CRITICAL (5 issues) - FIX IMMEDIATELY

1. **No Authentication System**
   - Anyone can create bookings with any userId
   - No Spring Security dependency
   - **Fix**: Add Spring Security + JWT

2. **No Authorization Checks**
   - Users can cancel ANY booking
   - No ownership verification
   - **Fix**: Verify booking.userId == authenticatedUserId

3. **Hardcoded Database Credentials**
   - `username: postgres, password: postgres` in application.yml
   - Same creds in docker-compose.yml
   - **Fix**: Use environment variables

4. **Hardcoded RabbitMQ Credentials**
   - `username: guest, password: guest`
   - Default credentials unchanged
   - **Fix**: Use strong passwords via env vars

5. **Public Mock Payment Endpoint**
   - `/api/v1/bookings/payment/mock-success/{paymentId}`
   - Works in production - FREE TICKETS FOR ANYONE!
   - **Fix**: Add `@Profile("dev")` or environment check

## 🟠 HIGH (8 issues) - FIX WITHIN 2 WEEKS

6. **Missing Amount Validation** - Negative payments accepted
7. **No Rate Limiting** - DoS vulnerability
8. **No CORS Configuration** - Cross-origin attacks possible
9. **No CSRF Protection** - State-changing operations vulnerable
10. **Seat Hold Ownership** - Users can book others' held seats
11. **Missing Input Size Limits** - Can hold 10,000 seats at once
12. **Missing Email Validation** - Invalid emails stored
13. **Exposed Webhook Secret** - Payment forgery possible

## 🟡 MEDIUM (12 issues) - FIX WITHIN 1 MONTH

14. No Redis authentication
15. Swagger UI enabled in production
16. Sequential database IDs (predictable)
17. User IDs stored plaintext in Redis
18. Debug logging in catalog service production
19. No path traversal protection
20. Detailed error messages (mitigated but risky)
21. Race condition in booking confirmation
22. No database SSL connection
23. Price manipulation risk (low but exists)
24. No audit trail for price calculations
25. Session management issues

## 🟢 LOW (3 issues) - ONGOING HARDENING

26. No dependency vulnerability scanning
27. No health check authentication plan
28. Missing security headers

---

## Top 3 Must-Fix NOW

### 1. Mock Payment Endpoint 🚨
**File**: `BookingController.java:121`
```java
// THIS ALLOWS FREE TICKETS IN PRODUCTION!
@PostMapping("/payment/mock-success/{paymentId}")
public ResponseEntity<PaymentResponse> mockPaymentSuccess(...) {
```
**Quick Fix**:
```java
@PostMapping("/payment/mock-success/{paymentId}")
@Profile("dev")  // Add this line
public ResponseEntity<PaymentResponse> mockPaymentSuccess(...) {
```

### 2. Authentication 🚨
**Files**: All controllers
**Quick Fix**: Add to pom.xml:
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>
```

### 3. Hardcoded Credentials 🚨
**Files**: `application.yml`, `docker-compose.yml`
**Quick Fix**: 
```yaml
# application.yml
datasource:
  password: ${DB_PASSWORD:postgres}  # Default only for local dev

# .env file (add to .gitignore)
DB_PASSWORD=strong_random_password_here
```

---

## Attack Scenarios

### Scenario 1: Free Tickets
1. Create booking → Get payment ID
2. Call mock endpoint → Payment marked success
3. Booking confirmed → Free tickets!

### Scenario 2: Cancel Anyone's Booking
1. Guess booking IDs (sequential: 1, 2, 3...)
2. Call `/bookings/{id}/cancel`
3. Anyone's booking cancelled

### Scenario 3: Impersonate Any User
1. Send booking request with `userId: 99999`
2. System accepts it (no auth check)
3. Create bookings as any user

---

## Files Requiring Immediate Attention

```
CRITICAL:
├── BookingController.java (lines 121-130) - Mock endpoint
├── BookingController.java (lines 47-53) - No auth
├── BookingController.java (lines 108-119) - No authz
├── application.yml (lines 6-8, 42-43) - Hardcoded creds
├── docker-compose.yml (lines 9-10, 41-42) - Hardcoded creds
└── pom.xml - Missing Spring Security

HIGH:
├── BookingRequest.java - Missing validations
├── PaymentRequest.java - Missing amount validation
├── HoldSeatsRequest.java - Missing size limit
└── BookingService.java (line 106) - No seat hold ownership check
```

---

## Testing Commands to Verify Vulnerabilities

### Test 1: Mock Payment Bypass
```bash
# Create booking
curl -X POST http://localhost:8082/api/v1/bookings \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 123,
    "showId": 1,
    "seatIds": [1],
    "sessionId": "test"
  }'

# Response will include paymentId
# Then bypass payment:
curl -X POST http://localhost:8082/api/v1/bookings/payment/mock-success/{paymentId}

# Result: Free tickets! 🎫💸
```

### Test 2: Authorization Bypass
```bash
# Cancel someone else's booking
curl -X POST http://localhost:8082/api/v1/bookings/1/cancel?reason=test

# Result: Anyone's booking cancelled!
```

### Test 3: Negative Payment
```bash
curl -X POST http://localhost:8082/api/v1/bookings \
  -H "Content-Type: application/json" \
  -d '{
    "amount": -1000.00,
    "userId": 123,
    "showId": 1,
    "seatIds": [1]
  }'

# May be accepted (no validation)
```

---

## Compliance Status

| Requirement | Status | Risk |
|------------|--------|------|
| PCI-DSS | ❌ FAIL | Can't process payments |
| OWASP Top 10 | ❌ FAIL | A01, A02, A05, A07 violated |
| GDPR | ⚠️ PARTIAL | Logging fixed, but access control missing |
| SOC 2 | ❌ FAIL | No access controls |

---

## Estimated Fix Time

- **Phase 1 (CRITICAL)**: 1-2 weeks
- **Phase 2 (HIGH)**: 2-3 weeks
- **Phase 3 (MEDIUM)**: 3-4 weeks
- **Total**: 6-9 weeks for full remediation

---

**RECOMMENDATION**: Do NOT deploy to production until at least Phase 1 (CRITICAL issues) are fixed.

**See full report**: `SECURITY_AUDIT_REPORT.md`
