# Database Schema Design
## Movie Ticket Booking Platform

---

## Database Choice: **PostgreSQL (Primary) + Redis (Cache/Locks)**

### Why PostgreSQL?
- **ACID compliance** critical for booking transactions
- Complex queries for browse/search scenarios
- Mature tooling and wide adoption
- Support for JSON columns for flexible attributes
- Strong consistency guarantees

### Why Redis?
- **Distributed locking** for seat reservation
- **Session management** for temporary seat holds (TTL)
- **Caching layer** for frequently accessed data (theatres, shows)
- **High throughput** for read-heavy browse operations

### Why NOT In-Memory Cache?
- **Not distributed** - won't scale across multiple service instances
- **Lost on restart** - no persistence for critical locks
- **No TTL management** - manual expiry handling needed
- **Single point of failure** - no replication

---

## Entity Relationship Diagram

```
┌─────────────────┐         ┌──────────────────┐         ┌─────────────────┐
│     THEATRE     │         │      MOVIE       │         │      USER       │
├─────────────────┤         ├──────────────────┤         ├─────────────────┤
│ id (PK)         │         │ id (PK)          │         │ id (PK)         │
│ name            │         │ title            │         │ email           │
│ location        │         │ language         │         │ phone           │
│ city            │         │ genre            │         │ name            │
│ address         │         │ duration_minutes │         │ created_at      │
│ partner_id (FK) │         │ release_date     │         └─────────────────┘
│ total_screens   │         │ rating           │                 │
│ status          │         │ poster_url       │                 │
│ created_at      │         │ description      │                 │
└────────┬────────┘         └──────────────────┘                 │
         │                           │                            │
         │                           │                            │
         │    ┌──────────────────────┘                            │
         │    │                                                   │
         │    │         ┌─────────────────┐                      │
         │    │         │     SCREEN      │                      │
         │    │         ├─────────────────┤                      │
         │    │         │ id (PK)         │                      │
         │    │         │ theatre_id (FK) │◄─────────────────────┘
         │    │         │ name            │
         │    │         │ total_seats     │
         │    │         │ seat_layout     │ (JSON)
         │    │         └────────┬────────┘
         │    │                  │
         │    │                  │
         ▼    ▼                  ▼
    ┌─────────────────────────────────────┐
    │             SHOW                    │
    ├─────────────────────────────────────┤
    │ id (PK)                             │
    │ movie_id (FK)                       │
    │ theatre_id (FK)                     │
    │ screen_id (FK)                      │
    │ show_date                           │
    │ show_time                           │
    │ base_price                          │
    │ available_seats                     │
    │ status (ACTIVE/CANCELLED/COMPLETED) │
    │ version (Optimistic Lock)           │
    │ created_at                          │
    │ updated_at                          │
    └──────────────┬──────────────────────┘
                   │
                   │
                   ▼
         ┌─────────────────────┐
         │    SEAT_PRICING     │
         ├─────────────────────┤
         │ id (PK)             │
         │ show_id (FK)        │
         │ category            │ (PREMIUM/NORMAL/VIP)
         │ price               │
         │ available_count     │
         └─────────────────────┘
                   │
                   │
                   ▼
         ┌─────────────────────┐
         │        SEAT         │
         ├─────────────────────┤
         │ id (PK)             │
         │ show_id (FK)        │
         │ seat_number         │
         │ row_label           │
         │ category            │
         │ status              │ (AVAILABLE/HELD/BOOKED/BLOCKED)
         │ version             │ (Optimistic Lock)
         └──────────┬──────────┘
                    │
                    │
                    ▼
         ┌─────────────────────┐
         │   SEAT_HOLD         │  (Redis: TTL 10 min)
         ├─────────────────────┤
         │ session_id (PK)     │
         │ seat_ids[]          │
         │ user_id             │
         │ expires_at          │
         │ created_at          │
         └─────────────────────┘
                    │
                    │
                    ▼
         ┌─────────────────────────────┐
         │          BOOKING            │
         ├─────────────────────────────┤
         │ id (PK)                     │
         │ user_id (FK)                │
         │ show_id (FK)                │
         │ booking_date                │
         │ total_amount                │
         │ discount_amount             │
         │ final_amount                │
         │ status                      │ (PENDING/CONFIRMED/CANCELLED)
         │ payment_id (FK)             │
         │ booking_reference           │ (Unique)
         │ created_at                  │
         │ updated_at                  │
         └──────────┬──────────────────┘
                    │
                    │
         ┌──────────┴──────────┐
         │                     │
         ▼                     ▼
┌──────────────────┐  ┌──────────────────────┐
│  BOOKING_SEAT    │  │      PAYMENT         │
├──────────────────┤  ├──────────────────────┤
│ id (PK)          │  │ id (PK)              │
│ booking_id (FK)  │  │ booking_id (FK)      │
│ seat_id (FK)     │  │ amount               │
│ price_paid       │  │ payment_method       │
└──────────────────┘  │ gateway_txn_id       │
                      │ status               │ (INITIATED/SUCCESS/FAILED)
                      │ gateway_response     │ (JSON)
                      │ initiated_at         │
                      │ completed_at         │
                      └──────────────────────┘
                                │
                                │
                                ▼
                      ┌──────────────────────┐
                      │    NOTIFICATION      │
                      ├──────────────────────┤
                      │ id (PK)              │
                      │ booking_id (FK)      │
                      │ user_id (FK)         │
                      │ type                 │ (EMAIL/SMS/PUSH)
                      │ status               │ (PENDING/SENT/FAILED)
                      │ retry_count          │
                      │ sent_at              │
                      │ failed_reason        │
                      │ created_at           │
                      └──────────────────────┘

         ┌─────────────────────┐
         │   THEATRE_PARTNER   │
         ├─────────────────────┤
         │ id (PK)             │
         │ company_name        │
         │ email               │
         │ phone               │
         │ status              │ (ACTIVE/INACTIVE/PENDING)
         │ commission_rate     │
         │ onboarding_date     │
         │ created_at          │
         └─────────────────────┘
                  │
                  └──────────► theatre.partner_id

         ┌─────────────────────┐
         │       OFFER         │
         ├─────────────────────┤
         │ id (PK)             │
         │ code                │
         │ description         │
         │ discount_type       │ (PERCENTAGE/FIXED)
         │ discount_value      │
         │ conditions          │ (JSON: {ticket_position:3, show_time:"afternoon"})
         │ applicable_cities   │ (Array)
         │ valid_from          │
         │ valid_to            │
         │ status              │
         └─────────────────────┘
```

---

## Table Definitions

### 1. THEATRE
```sql
CREATE TABLE theatre (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    location VARCHAR(255) NOT NULL,
    city VARCHAR(100) NOT NULL,
    state VARCHAR(100),
    country VARCHAR(100) DEFAULT 'India',
    address TEXT,
    partner_id BIGINT NOT NULL,
    total_screens INT NOT NULL,
    facilities JSONB,  -- {parking: true, food_court: true, wheelchair_access: true}
    status VARCHAR(20) DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (partner_id) REFERENCES theatre_partner(id),
    INDEX idx_city (city),
    INDEX idx_partner (partner_id)
);
```

### 2. MOVIE
```sql
CREATE TABLE movie (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    language VARCHAR(50) NOT NULL,
    genre VARCHAR(100),  -- JSON array: ["Action", "Thriller"]
    duration_minutes INT NOT NULL,
    release_date DATE NOT NULL,
    rating VARCHAR(10),  -- U, UA, A
    poster_url VARCHAR(500),
    description TEXT,
    cast JSONB,  -- [{name: "Actor", role: "Lead"}]
    director VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_language (language),
    INDEX idx_genre (genre),
    INDEX idx_release_date (release_date)
);
```

### 3. SCREEN
```sql
CREATE TABLE screen (
    id BIGSERIAL PRIMARY KEY,
    theatre_id BIGINT NOT NULL,
    name VARCHAR(50) NOT NULL,  -- "Screen 1", "Audi 2"
    total_seats INT NOT NULL,
    seat_layout JSONB NOT NULL,  -- [{row: "A", seats: [1,2,3], category: "PREMIUM"}]
    screen_type VARCHAR(50),  -- IMAX, 4DX, Standard
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (theatre_id) REFERENCES theatre(id),
    INDEX idx_theatre (theatre_id)
);
```

### 4. SHOW
```sql
CREATE TABLE show (
    id BIGSERIAL PRIMARY KEY,
    movie_id BIGINT NOT NULL,
    theatre_id BIGINT NOT NULL,
    screen_id BIGINT NOT NULL,
    show_date DATE NOT NULL,
    show_time TIME NOT NULL,
    base_price DECIMAL(10,2) NOT NULL,
    available_seats INT NOT NULL,
    status VARCHAR(20) DEFAULT 'ACTIVE',  -- ACTIVE, CANCELLED, COMPLETED
    version INT DEFAULT 0,  -- Optimistic locking
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (movie_id) REFERENCES movie(id),
    FOREIGN KEY (theatre_id) REFERENCES theatre(id),
    FOREIGN KEY (screen_id) REFERENCES screen(id),
    UNIQUE (screen_id, show_date, show_time),  -- Prevent double booking
    INDEX idx_movie_date (movie_id, show_date),
    INDEX idx_theatre_date (theatre_id, show_date),
    INDEX idx_show_datetime (show_date, show_time)
);
```

### 5. SEAT
```sql
CREATE TABLE seat (
    id BIGSERIAL PRIMARY KEY,
    show_id BIGINT NOT NULL,
    seat_number VARCHAR(10) NOT NULL,  -- "A1", "B12"
    row_label VARCHAR(5) NOT NULL,
    category VARCHAR(20) NOT NULL,  -- PREMIUM, NORMAL, VIP
    status VARCHAR(20) DEFAULT 'AVAILABLE',  -- AVAILABLE, HELD, BOOKED, BLOCKED
    version INT DEFAULT 0,  -- Optimistic locking
    held_until TIMESTAMP,  -- Expiry for HELD status
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (show_id) REFERENCES show(id),
    UNIQUE (show_id, seat_number),
    INDEX idx_show_status (show_id, status),
    INDEX idx_held_until (held_until)
);
```

### 6. BOOKING
```sql
CREATE TABLE booking (
    id BIGSERIAL PRIMARY KEY,
    booking_reference VARCHAR(20) UNIQUE NOT NULL,  -- "BK20260328ABC123"
    user_id BIGINT NOT NULL,
    show_id BIGINT NOT NULL,
    booking_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    total_amount DECIMAL(10,2) NOT NULL,
    discount_amount DECIMAL(10,2) DEFAULT 0,
    final_amount DECIMAL(10,2) NOT NULL,
    offer_applied VARCHAR(100),  -- Which offer was used
    status VARCHAR(20) DEFAULT 'PENDING',  -- PENDING, CONFIRMED, CANCELLED, REFUNDED
    payment_id BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES user_account(id),
    FOREIGN KEY (show_id) REFERENCES show(id),
    FOREIGN KEY (payment_id) REFERENCES payment(id),
    INDEX idx_user (user_id),
    INDEX idx_show (show_id),
    INDEX idx_booking_ref (booking_reference),
    INDEX idx_status_date (status, booking_date)
);
```

### 7. BOOKING_SEAT
```sql
CREATE TABLE booking_seat (
    id BIGSERIAL PRIMARY KEY,
    booking_id BIGINT NOT NULL,
    seat_id BIGINT NOT NULL,
    price_paid DECIMAL(10,2) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (booking_id) REFERENCES booking(id),
    FOREIGN KEY (seat_id) REFERENCES seat(id),
    UNIQUE (booking_id, seat_id),
    INDEX idx_booking (booking_id),
    INDEX idx_seat (seat_id)
);
```

### 8. PAYMENT
```sql
CREATE TABLE payment (
    id BIGSERIAL PRIMARY KEY,
    booking_id BIGINT NOT NULL,
    amount DECIMAL(10,2) NOT NULL,
    payment_method VARCHAR(50),  -- CARD, UPI, NET_BANKING, WALLET
    gateway_name VARCHAR(50),  -- RAZORPAY, STRIPE, PAYTM
    gateway_txn_id VARCHAR(255),
    status VARCHAR(20) DEFAULT 'INITIATED',  -- INITIATED, SUCCESS, FAILED, REFUNDED
    gateway_response JSONB,  -- Store full gateway response
    initiated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP,
    failed_reason TEXT,
    FOREIGN KEY (booking_id) REFERENCES booking(id),
    INDEX idx_booking (booking_id),
    INDEX idx_gateway_txn (gateway_txn_id),
    INDEX idx_status (status)
);
```

### 9. NOTIFICATION
```sql
CREATE TABLE notification (
    id BIGSERIAL PRIMARY KEY,
    booking_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    type VARCHAR(20) NOT NULL,  -- EMAIL, SMS, PUSH
    recipient VARCHAR(255) NOT NULL,  -- email or phone
    subject VARCHAR(255),
    message TEXT,
    status VARCHAR(20) DEFAULT 'PENDING',  -- PENDING, SENT, FAILED
    retry_count INT DEFAULT 0,
    max_retries INT DEFAULT 3,
    sent_at TIMESTAMP,
    failed_reason TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (booking_id) REFERENCES booking(id),
    FOREIGN KEY (user_id) REFERENCES user_account(id),
    INDEX idx_booking (booking_id),
    INDEX idx_status_retry (status, retry_count),
    INDEX idx_created_at (created_at)
);
```

### 10. USER_ACCOUNT
```sql
CREATE TABLE user_account (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) UNIQUE NOT NULL,
    phone VARCHAR(20),
    name VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    preferred_language VARCHAR(50),
    preferred_city VARCHAR(100),
    status VARCHAR(20) DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_login TIMESTAMP,
    INDEX idx_email (email),
    INDEX idx_phone (phone)
);
```

### 11. THEATRE_PARTNER
```sql
CREATE TABLE theatre_partner (
    id BIGSERIAL PRIMARY KEY,
    company_name VARCHAR(255) NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    phone VARCHAR(20),
    contact_person VARCHAR(255),
    status VARCHAR(20) DEFAULT 'PENDING',  -- PENDING, ACTIVE, INACTIVE, SUSPENDED
    commission_rate DECIMAL(5,2) DEFAULT 10.00,  -- Platform commission %
    onboarding_date DATE,
    contract_start DATE,
    contract_end DATE,
    api_key VARCHAR(255) UNIQUE,  -- For integration
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_email (email),
    INDEX idx_status (status)
);
```

### 12. OFFER
```sql
CREATE TABLE offer (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(50) UNIQUE NOT NULL,  -- "THIRD50", "AFTERNOON20"
    description TEXT,
    discount_type VARCHAR(20) NOT NULL,  -- PERCENTAGE, FIXED
    discount_value DECIMAL(10,2) NOT NULL,
    conditions JSONB,  -- {ticket_position: 3} or {show_time_start: "12:00", show_time_end: "16:00"}
    applicable_cities TEXT[],  -- Array of cities
    applicable_theatres BIGINT[],  -- Array of theatre IDs
    valid_from DATE NOT NULL,
    valid_to DATE NOT NULL,
    status VARCHAR(20) DEFAULT 'ACTIVE',
    max_usage_per_user INT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_code (code),
    INDEX idx_validity (valid_from, valid_to),
    INDEX idx_status (status)
);
```

### 13. BOOKING_AUDIT
```sql
CREATE TABLE booking_audit (
    id BIGSERIAL PRIMARY KEY,
    booking_id BIGINT NOT NULL,
    action VARCHAR(50) NOT NULL,  -- CREATED, PAYMENT_SUCCESS, PAYMENT_FAILED, CANCELLED
    old_status VARCHAR(20),
    new_status VARCHAR(20),
    metadata JSONB,  -- Store relevant context
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (booking_id) REFERENCES booking(id),
    INDEX idx_booking (booking_id),
    INDEX idx_action (action),
    INDEX idx_created_at (created_at)
);
```

---

## Redis Data Structures

### 1. Seat Hold (Temporary Lock)
```
Key: seat:hold:{sessionId}
Type: Hash
TTL: 600 seconds (10 minutes)
Value: {
    "seatIds": ["123", "124"],
    "showId": "456",
    "userId": "789",
    "expiresAt": "2026-03-28T14:45:00Z"
}
```

### 2. Show Cache
```
Key: show:details:{showId}
Type: Hash
TTL: 300 seconds (5 minutes)
Value: {
    "movieTitle": "Inception",
    "theatreName": "PVR Phoenix",
    "showTime": "18:30",
    "availableSeats": 85,
    "basePrice": 250
}
```

### 3. Seat Availability Cache
```
Key: show:seats:{showId}
Type: Hash
TTL: 60 seconds (1 minute)
Value: {
    "A1": "AVAILABLE",
    "A2": "BOOKED",
    "A3": "HELD"
}
```

### 4. Distributed Lock for Seat Booking
```
Key: lock:seat:{seatId}
Type: String
TTL: 30 seconds
Value: {transactionId}
```

---

## Data Model Decisions & Justifications

### 1. **Optimistic Locking (Version Column)**
**Why?**
- Prevents lost updates in concurrent booking scenarios
- Lower overhead than pessimistic locks
- Better for high-read, moderate-write workloads

**How it works:**
```sql
UPDATE seat 
SET status = 'BOOKED', version = version + 1 
WHERE id = ? AND version = ? AND status = 'AVAILABLE';

-- If rows_affected = 0, another user already booked it
```

### 2. **Redis for Seat Holds (Not PostgreSQL)**
**Why Redis?**
- **Automatic expiry (TTL)** - No cleanup job needed
- **High throughput** - Handles thousands of concurrent holds
- **Distributed** - Works across multiple service instances
- **Atomic operations** - SETNX for lock acquisition

**Why NOT in-memory cache?**
- Lost on service restart
- Not visible across instances
- Manual TTL management
- No atomic operations

### 3. **Separate SEAT table (Not embedded in SHOW)**
**Why?**
- **Granular locking** - Lock individual seats, not entire show
- **Audit trail** - Track seat status changes
- **Performance** - Index on seat availability for fast queries
- **Flexibility** - Different seat categories and pricing

### 4. **JSONB for Flexible Attributes**
**Where used:**
- `screen.seat_layout` - Theatre-specific layouts
- `payment.gateway_response` - Varying gateway responses
- `offer.conditions` - Complex offer rules

**Why?**
- Schema flexibility without migrations
- Queryable (PostgreSQL JSONB indexes)
- Backward compatibility for partner integrations

### 5. **Separate NOTIFICATION table**
**Why?**
- **Retry mechanism** - Track failed notifications
- **Async processing** - Decouple from payment flow
- **Observability** - Monitor delivery rates
- **Multiple channels** - Email, SMS, Push

### 6. **BOOKING_AUDIT for Compliance**
**Why?**
- **Financial audit trail** - Required for disputes
- **Debugging** - Trace booking lifecycle
- **Analytics** - Understand failure patterns
- **Compliance** - PCI-DSS, data regulations

---

## Indexes Strategy

### High-Traffic Queries:
1. **Browse shows by movie and date**
   - `INDEX idx_movie_date ON show(movie_id, show_date)`

2. **Browse shows by theatre and date**
   - `INDEX idx_theatre_date ON show(theatre_id, show_date)`

3. **Find available seats for show**
   - `INDEX idx_show_status ON seat(show_id, status)`

4. **Lookup booking by reference**
   - `UNIQUE INDEX on booking_reference`

5. **Expired seat holds cleanup**
   - `INDEX idx_held_until ON seat(held_until)`

---

## Scaling Considerations

### Partitioning Strategy:
1. **BOOKING table** - Partition by `booking_date` (monthly)
   - Old bookings archived, queries faster
   
2. **SHOW table** - Partition by `show_date` (monthly)
   - Historical shows can be archived

3. **BOOKING_AUDIT** - Partition by `created_at` (monthly)
   - Compliance retention, archival

### Replication:
- **Master-Slave replication** for read scalability
- **Read replicas** for browse/search queries
- **Master** for booking/payment writes

---

## Data Consistency Patterns

### 1. Seat Booking Flow (ACID Transaction)
```sql
BEGIN TRANSACTION;

-- 1. Check and lock seats (Optimistic Lock)
UPDATE seat SET status = 'HELD', version = version + 1, held_until = NOW() + INTERVAL '10 minutes'
WHERE id IN (123, 124) AND version = 5 AND status = 'AVAILABLE';

-- 2. If rows_affected = expected, proceed
-- 3. Update show available_seats count
UPDATE show SET available_seats = available_seats - 2 WHERE id = 456;

-- 4. Create seat hold in Redis
-- 5. Return success

COMMIT;
```

### 2. Payment Success Flow (Saga Pattern)
```
1. Payment Success Received
2. Update Payment Status (DB Write)
3. Update Booking Status to CONFIRMED (DB Write)
4. Update Seat Status from HELD to BOOKED (DB Write)
5. Remove Redis hold
6. Publish "BookingConfirmed" event
7. Async: Notification Service listens and sends email/SMS
```

### 3. Notification Failure Handling
```
- Notification service subscribes to BookingConfirmed events
- On failure:
  * Insert NOTIFICATION record with status=FAILED
  * Retry worker picks up FAILED notifications (exponential backoff)
  * Max 3 retries
  * Booking still CONFIRMED (payment already succeeded)
```

---

## Query Patterns

### Q1: Browse Theatres Running a Movie
```sql
SELECT DISTINCT 
    t.id, t.name, t.location, t.city,
    s.show_date, s.show_time, s.available_seats, s.base_price
FROM show s
JOIN theatre t ON s.theatre_id = t.id
JOIN movie m ON s.movie_id = m.id
WHERE m.id = ?
  AND s.show_date = ?
  AND s.status = 'ACTIVE'
  AND s.available_seats > 0
ORDER BY s.show_time;

-- Cached in Redis for 5 minutes
```

### Q2: Check Seat Availability with Lock
```sql
SELECT id, seat_number, category, status, version
FROM seat
WHERE show_id = ?
  AND id IN (?, ?, ?)
  AND status = 'AVAILABLE'
FOR UPDATE SKIP LOCKED;  -- Skip already locked seats

-- Then apply optimistic lock on update
```

### Q3: Calculate Offer Discount
```sql
SELECT o.id, o.discount_type, o.discount_value, o.conditions
FROM offer o
WHERE o.status = 'ACTIVE'
  AND CURRENT_DATE BETWEEN o.valid_from AND o.valid_to
  AND (o.applicable_cities IS NULL OR ? = ANY(o.applicable_cities))
  AND (o.applicable_theatres IS NULL OR ? = ANY(o.applicable_theatres));

-- Apply business logic in service layer
```

---

## Data Seeding Strategy

### Master Data:
- Movies: Load from external API (TMDB/IMDB)
- Cities: Predefined list
- Offers: Admin-configured

### Transactional Data:
- Users: Sign-up driven
- Bookings: User-driven
- Shows: Theatre partner-driven

---

## Backup & Recovery

### PostgreSQL:
- **WAL (Write-Ahead Logging)** enabled
- **Point-in-time recovery** for financial data
- **Daily full backups** + continuous archiving
- **Cross-region replication** for disaster recovery

### Redis:
- **RDB snapshots** every 5 minutes
- **AOF (Append-Only File)** for durability
- **Sentinel/Cluster** for high availability
- Not critical (cache) - can rebuild from PostgreSQL

---

## Compliance & Audit

### PCI-DSS:
- **No card data stored** - Gateway handles it
- Only store `gateway_txn_id` for reference
- Encrypted at rest (PostgreSQL TDE)

### GDPR/Data Privacy:
- **User consent** for notifications
- **Right to deletion** - Anonymize after 7 years
- **Audit trail** - booking_audit table

### Financial Audit:
- Immutable booking records (soft deletes only)
- Audit trail for all status changes
- Reconciliation reports with payment gateway

