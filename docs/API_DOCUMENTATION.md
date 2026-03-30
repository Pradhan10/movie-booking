# API Documentation

## Movie Ticket Booking Platform - REST APIs

---

## Base URLs

- **Catalog Service**: `http://localhost:8081/api/v1`
- **Booking Service**: `http://localhost:8082/api/v1`

---

## Catalog Service APIs

### 1. Browse Shows by Movie

**Endpoint**: `GET /shows/movie/{movieId}`

**Description**: Get all shows for a specific movie on a given date in a city

**Query Parameters**:
- `date` (required): Show date in YYYY-MM-DD format
- `city` (required): City name (e.g., "Bangalore")

**Example Request**:
```bash
GET /api/v1/shows/movie/1?date=2026-03-29&city=Bangalore
```

**Example Response** (200 OK):
```json
[
  {
    "showId": 1,
    "movieId": 1,
    "movieTitle": "Inception",
    "theatreId": 1,
    "theatreName": "PVR Phoenix Marketcity",
    "screenName": "Screen 1",
    "showDate": "2026-03-29",
    "showTime": "10:00:00",
    "availableSeats": 200,
    "basePrice": 350.00,
    "language": "English",
    "genre": "Sci-Fi, Thriller",
    "city": "Bangalore"
  }
]
```

**Caching**: Results cached for 60 seconds

---

### 2. Browse Shows by Theatre

**Endpoint**: `GET /shows/theatre/{theatreId}`

**Description**: Get all shows at a specific theatre on a given date

**Query Parameters**:
- `date` (required): Show date in YYYY-MM-DD format

**Example Request**:
```bash
GET /api/v1/shows/theatre/1?date=2026-03-29
```

**Example Response** (200 OK):
```json
[
  {
    "showId": 1,
    "movieTitle": "Inception",
    "showDate": "2026-03-29",
    "showTime": "10:00:00",
    "availableSeats": 200,
    "basePrice": 350.00
  },
  {
    "showId": 2,
    "movieTitle": "Inception",
    "showDate": "2026-03-29",
    "showTime": "13:30:00",
    "availableSeats": 200,
    "basePrice": 300.00
  }
]
```

---

### 3. Get Show Details

**Endpoint**: `GET /shows/{showId}`

**Description**: Get detailed information about a specific show

**Example Request**:
```bash
GET /api/v1/shows/1
```

**Example Response** (200 OK):
```json
{
  "showId": 1,
  "movieId": 1,
  "movieTitle": "Inception",
  "theatreId": 1,
  "theatreName": "PVR Phoenix Marketcity",
  "showDate": "2026-03-29",
  "showTime": "10:00:00",
  "availableSeats": 200,
  "basePrice": 350.00,
  "language": "English",
  "genre": "Sci-Fi, Thriller",
  "city": "Bangalore"
}
```

**Caching**: Results cached for 300 seconds (5 minutes)

---

### 4. Search Shows

**Endpoint**: `GET /shows/search`

**Description**: Search shows by city and date range

**Query Parameters**:
- `city` (required): City name
- `startDate` (required): Start date (YYYY-MM-DD)
- `endDate` (required): End date (YYYY-MM-DD)

**Example Request**:
```bash
GET /api/v1/shows/search?city=Bangalore&startDate=2026-03-29&endDate=2026-04-05
```

---

### 5. Get Applicable Offers

**Endpoint**: `GET /shows/offers`

**Description**: Get all active offers for a city

**Query Parameters**:
- `city` (required): City name
- `theatreId` (optional): Theatre ID for theatre-specific offers

**Example Request**:
```bash
GET /api/v1/shows/offers?city=Bangalore
```

**Example Response** (200 OK):
```json
[
  {
    "offerId": 1,
    "code": "THIRD50",
    "description": "50% discount on the third ticket",
    "discountType": "PERCENTAGE",
    "discountValue": 50.00,
    "conditions": "Third ticket discount"
  },
  {
    "offerId": 2,
    "code": "AFTERNOON20",
    "description": "20% discount on afternoon shows (12 PM - 4 PM)",
    "discountType": "PERCENTAGE",
    "discountValue": 20.00,
    "conditions": "Afternoon show discount"
  }
]
```

---

## Booking Service APIs

### 1. Hold Seats

**Endpoint**: `POST /bookings/hold`

**Description**: Temporarily hold seats for 10 minutes before creating booking

**Request Body**:
```json
{
  "sessionId": "session-abc123",
  "showId": 1,
  "seatIds": [1, 2, 3],
  "userId": 1
}
```

**Response** (200 OK):
```json
{
  "sessionId": "session-abc123",
  "seatIds": [1, 2, 3],
  "expiresAt": "2026-03-28T15:45:00Z",
  "success": true,
  "message": null
}
```

**Error Response** (409 Conflict):
```json
{
  "timestamp": "2026-03-28T15:35:12Z",
  "status": 409,
  "error": "Conflict",
  "errorCode": "SEAT_UNAVAILABLE",
  "message": "Seats not available: [2, 3]"
}
```

**How It Works**:
1. Acquires distributed lock (Redis) for each seat
2. Checks seat availability with pessimistic lock (DB)
3. Updates seat status to HELD
4. Creates Redis hold entry with 10-minute TTL
5. Releases locks
6. Returns hold expiry time

---

### 2. Create Booking

**Endpoint**: `POST /bookings`

**Description**: Create a booking for previously held seats

**Request Body**:
```json
{
  "userId": 1,
  "showId": 1,
  "seatIds": [1, 2, 3],
  "sessionId": "session-abc123",
  "offerCode": "THIRD50",
  "contactEmail": "john.doe@example.com",
  "contactPhone": "+91-9876543210"
}
```

**Response** (201 Created):
```json
{
  "bookingId": 1,
  "bookingReference": "BK1711638000-A1B2C3D4",
  "status": "PENDING",
  "totalAmount": 750.00,
  "discountAmount": 125.00,
  "finalAmount": 625.00,
  "seats": [
    {"seatId": 1, "price": 250.00},
    {"seatId": 2, "price": 250.00},
    {"seatId": 3, "price": 250.00}
  ],
  "paymentUrl": "https://api.razorpay.com/checkout/1",
  "paymentId": 1,
  "expiresAt": null,
  "qrCode": null
}
```

**Offer Calculation**:
- 3 tickets × 250 = 750.00
- 3rd ticket discount (50%): 250 × 0.5 = 125.00
- Final amount: 750 - 125 = 625.00

**Error Response** (410 Gone):
```json
{
  "timestamp": "2026-03-28T15:50:00Z",
  "status": 410,
  "error": "Gone",
  "errorCode": "HOLD_EXPIRED",
  "message": "Seat hold expired or invalid session"
}
```

---

### 3. Confirm Booking

**Endpoint**: `POST /bookings/{bookingId}/confirm`

**Description**: Confirm booking after successful payment (called by payment webhook or mock)

**Query Parameters**:
- `paymentId` (required): Payment ID

**Example Request**:
```bash
POST /api/v1/bookings/1/confirm?paymentId=1
```

**Response** (200 OK):
```json
{
  "bookingId": 1,
  "bookingReference": "BK1711638000-A1B2C3D4",
  "status": "CONFIRMED",
  "finalAmount": 625.00,
  "qrCode": "QR_BK1711638000-A1B2C3D4"
}
```

**What Happens**:
1. Validates payment is successful
2. Acquires distributed lock for seats
3. Updates seat status: HELD → BOOKED (with optimistic lock)
4. Updates booking status: PENDING → CONFIRMED
5. Releases Redis hold
6. Publishes `BookingConfirmedEvent` to RabbitMQ
7. Notification service (async) sends email/SMS

**Edge Case: Notification Fails**
- Booking status remains CONFIRMED
- User sees confirmation page with warning banner
- Notification stored in Message Center
- Background worker retries (1min, 5min, 15min)

---

### 4. Cancel Booking

**Endpoint**: `POST /bookings/{bookingId}/cancel`

**Description**: Cancel an existing booking

**Query Parameters**:
- `reason` (optional): Cancellation reason

**Example Request**:
```bash
POST /api/v1/bookings/1/cancel?reason=User%20requested
```

**Response** (204 No Content)

**What Happens**:
1. Updates booking status to CANCELLED
2. If booking was CONFIRMED, releases seats (BOOKED → AVAILABLE)
3. Publishes `BookingCancelledEvent`
4. Triggers refund process (if applicable)

---

### 5. Mock Payment Success (Testing Only)

**Endpoint**: `POST /bookings/payment/mock-success/{paymentId}`

**Description**: Simulate successful payment for testing purposes

**Example Request**:
```bash
POST /api/v1/bookings/payment/mock-success/1
```

**Response** (200 OK):
```json
{
  "paymentId": 1,
  "bookingId": 1,
  "amount": 625.00,
  "status": "SUCCESS",
  "gatewayTxnId": "TXN_mock-abc123"
}
```

**Note**: In production, this would be replaced by actual payment gateway webhook handler

---

## Complete Booking Flow

### Happy Path

```
1. User browses shows
   GET /api/v1/shows/movie/1?date=2026-03-29&city=Bangalore

2. User selects show and seats
   POST /api/v1/bookings/hold
   Body: {"sessionId": "s1", "showId": 1, "seatIds": [1,2,3], "userId": 1}
   
   Response: {"success": true, "expiresAt": "15:45:00Z"}

3. User creates booking
   POST /api/v1/bookings
   Body: {"userId": 1, "showId": 1, "seatIds": [1,2,3], "sessionId": "s1"}
   
   Response: {"bookingId": 1, "paymentUrl": "...", "paymentId": 1}

4. User redirected to payment gateway
   External: Payment gateway UI

5. Payment success webhook received
   POST /api/v1/bookings/payment/mock-success/1  (mock)
   
   Internally calls: POST /bookings/1/confirm?paymentId=1

6. Booking confirmed
   Response: {"status": "CONFIRMED", "qrCode": "QR_..."}

7. Notification sent (async)
   Event published → Notification service → Email/SMS
```

### Failure Scenarios

#### Scenario 1: Seats Already Booked
```
POST /bookings/hold → 409 Conflict
{
  "errorCode": "SEAT_UNAVAILABLE",
  "message": "Seats not available: [2]"
}

Redirect: Back to seat selection page
```

#### Scenario 2: Hold Expired
```
POST /bookings → 410 Gone
{
  "errorCode": "HOLD_EXPIRED",
  "message": "Seat hold expired or invalid session"
}

Redirect: Back to show selection page
```

#### Scenario 3: Payment Failed
```
Payment gateway webhook → status: FAILED

Internally:
- Payment status → FAILED
- Booking status → CANCELLED
- Seats released (HELD → AVAILABLE)
- Event published

Redirect: Back to payment page with error message
```

#### Scenario 4: Payment Success, Notification Failed
```
Payment webhook → status: SUCCESS

Internally:
- Payment status → SUCCESS
- Booking status → CONFIRMED
- Seats booked (HELD → BOOKED)
- Event published
- Notification service fails

User Experience:
- Redirect to confirmation page (shows ticket + QR code)
- Warning banner: "Email confirmation pending"
- Notification stored in Message Center
- Background retry (max 3 attempts)

Key: Booking is valid even if notification fails
```

---

## Error Codes

| Code | HTTP Status | Description | User Action |
|------|-------------|-------------|-------------|
| `SEAT_UNAVAILABLE` | 409 Conflict | Seat already booked or held | Select different seats |
| `HOLD_EXPIRED` | 410 Gone | Seat hold expired (> 10 min) | Start booking again |
| `LOCK_FAILED` | 409 Conflict | Failed to acquire distributed lock | Retry after few seconds |
| `CONCURRENT_MODIFICATION` | 409 Conflict | Optimistic lock failure | Retry booking |
| `PAYMENT_FAILED` | 400 Bad Request | Payment gateway rejected | Retry with different payment method |
| `INTERNAL_ERROR` | 500 Internal Server Error | Unexpected error | Contact support |

---

## Rate Limiting

| Endpoint | Limit | Window |
|----------|-------|--------|
| Browse APIs | 100 requests | per minute per user |
| Hold Seats | 10 requests | per minute per user |
| Create Booking | 5 requests | per minute per user |

---

## Idempotency

### Booking Creation
- Use `Idempotency-Key` header to prevent duplicate bookings from retries
- Same key returns same booking (if already exists)

**Example**:
```bash
curl -X POST http://localhost:8082/api/v1/bookings \
  -H "Idempotency-Key: user1-show1-seats123-timestamp" \
  -H "Content-Type: application/json" \
  -d '{"userId": 1, "showId": 1, "seatIds": [1,2,3], "sessionId": "s1"}'
```

---

## Swagger UI

Interactive API documentation available at:

- **Catalog**: http://localhost:8081/api/v1/swagger-ui.html
- **Booking**: http://localhost:8082/api/v1/swagger-ui.html

Test APIs directly from browser without curl/Postman.

---

## Testing Scenarios

### Test 1: Browse Shows
```bash
curl -X GET "http://localhost:8081/api/v1/shows/movie/1?date=2026-03-29&city=Bangalore"
```

### Test 2: Hold Seats
```bash
curl -X POST "http://localhost:8082/api/v1/bookings/hold" \
  -H "Content-Type: application/json" \
  -d '{
    "sessionId": "test-session-1",
    "showId": 1,
    "seatIds": [1, 2, 3],
    "userId": 1
  }'
```

### Test 3: Create Booking
```bash
curl -X POST "http://localhost:8082/api/v1/bookings" \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 1,
    "showId": 1,
    "seatIds": [1, 2, 3],
    "sessionId": "test-session-1",
    "contactEmail": "test@example.com"
  }'
```

### Test 4: Mock Payment Success
```bash
# Replace {paymentId} with actual ID from step 3
curl -X POST "http://localhost:8082/api/v1/bookings/payment/mock-success/1"
```

### Test 5: Concurrent Booking (Bash Script)
```bash
#!/bin/bash
# Test duplicate booking prevention

for i in {1..10}; do
  curl -X POST "http://localhost:8082/api/v1/bookings/hold" \
    -H "Content-Type: application/json" \
    -d "{\"sessionId\": \"concurrent-$i\", \"showId\": 1, \"seatIds\": [10], \"userId\": $i}" &
done
wait

# Expected: 1 success, 9 failures (409 Conflict)
```

---

## Postman Collection

Import this collection for easy testing:

```json
{
  "info": {
    "name": "Movie Booking Platform",
    "schema": "https://schema.getpostman.com/json/collection/v2.1.0/collection.json"
  },
  "item": [
    {
      "name": "Catalog Service",
      "item": [
        {
          "name": "Browse Shows by Movie",
          "request": {
            "method": "GET",
            "url": "http://localhost:8081/api/v1/shows/movie/1?date=2026-03-29&city=Bangalore"
          }
        }
      ]
    },
    {
      "name": "Booking Service",
      "item": [
        {
          "name": "Hold Seats",
          "request": {
            "method": "POST",
            "url": "http://localhost:8082/api/v1/bookings/hold",
            "body": {
              "mode": "raw",
              "raw": "{\n  \"sessionId\": \"{{$guid}}\",\n  \"showId\": 1,\n  \"seatIds\": [1, 2, 3],\n  \"userId\": 1\n}"
            }
          }
        }
      ]
    }
  ]
}
```

---

## Response Times (Benchmarks)

| Endpoint | Cache Miss | Cache Hit | Target |
|----------|------------|-----------|--------|
| Browse shows | 150ms | 5ms | < 500ms |
| Show details | 100ms | 3ms | < 200ms |
| Hold seats | 200ms | N/A | < 500ms |
| Create booking | 150ms | N/A | < 500ms |
| Confirm booking | 250ms | N/A | < 1000ms |

---

## Headers

### Required Headers
- `Content-Type: application/json` (for POST requests)

### Optional Headers
- `Idempotency-Key: <unique-key>` (for booking creation)
- `X-Request-ID: <uuid>` (for distributed tracing)
- `Authorization: Bearer <jwt>` (when auth is enabled)

---

## Webhooks

### Payment Gateway Webhook (Production)

**Endpoint**: `POST /bookings/payment/webhook`

**Signature Validation**:
```java
String signature = request.getHeader("X-Razorpay-Signature");
boolean valid = validateSignature(payload, signature, webhookSecret);
```

**Payload**:
```json
{
  "event": "payment.captured",
  "payload": {
    "payment": {
      "id": "pay_abc123",
      "amount": 62500,
      "status": "captured"
    }
  }
}
```

**Idempotency**: Webhook may be sent multiple times. Payment status update is idempotent.

---

## Additional Resources

- **Swagger UI**: http://localhost:8081/api/v1/swagger-ui.html
- **API Docs JSON**: http://localhost:8081/api/v1/api-docs
- **RabbitMQ Management**: http://localhost:15672 (guest/guest)
- **Design Documents**: See `docs/` folder
