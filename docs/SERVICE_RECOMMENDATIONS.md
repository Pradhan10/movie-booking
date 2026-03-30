# Service Implementation Recommendations

## Executive Summary

For this interview exercise, I recommend implementing **TWO core services** that demonstrate the most critical technical challenges and architectural decisions:

1. **Show Catalog Service** (Read Scenario)
2. **Booking Service** (Write Scenario)

---

## Recommendation 1: Show Catalog Service (Read)

### What It Does
Enables users to browse movies, theatres, and show timings across cities with applied offers.

### Why This Service?

#### Demonstrates Core Competencies:
1. **Caching Strategy**
   - Redis cache-aside pattern
   - TTL-based invalidation
   - Cache warming strategies

2. **Query Optimization**
   - Complex joins across multiple tables
   - Index usage for performance
   - Pagination for large result sets

3. **Read Scalability**
   - Read replicas utilization
   - Database connection pooling
   - Query result caching

4. **Search Capabilities**
   - Filter by city, language, genre, date
   - Elasticsearch integration for full-text search
   - Autocomplete functionality

#### Technical Complexity: **Medium**
- Multiple data sources (DB + Cache)
- Complex business logic (offer calculation)
- Performance optimization critical (high read volume)

#### Business Value: **High**
- Primary entry point for users
- Drives conversion (browse → book)
- Showcase theatre inventory

---

## Recommendation 2: Booking Service (Write)

### What It Does
Handles the complete ticket booking flow: seat selection, hold, payment, and confirmation.

### Why This Service?

#### Demonstrates Core Competencies:
1. **Concurrency Control**
   - **Optimistic locking** for seat updates (version column)
   - **Pessimistic locking** for critical sections (SELECT FOR UPDATE)
   - **Distributed locking** using Redis (prevent duplicate bookings)
   - Race condition handling

2. **Transaction Management**
   - ACID transactions for booking creation
   - Saga pattern for distributed transactions
   - Rollback and compensation logic

3. **Event-Driven Architecture**
   - Publish events for async processing
   - Decouple booking from notification
   - Handle partial failures gracefully

4. **State Machine Management**
   - Booking lifecycle: PENDING → CONFIRMED → CANCELLED
   - Seat status transitions: AVAILABLE → HELD → BOOKED
   - Idempotency for payment webhooks

5. **Integration Patterns**
   - Payment gateway integration
   - Webhook handling and validation
   - Retry mechanisms with exponential backoff

#### Technical Complexity: **High**
- Critical path for revenue
- Multiple failure scenarios
- Distributed system challenges (CAP theorem trade-offs)

#### Business Value: **Critical**
- Direct revenue generation
- Customer satisfaction depends on reliability
- Must prevent double-booking at all costs

---

## Why NOT Other Services?

### User Service
- **Complexity**: Low (standard CRUD with JWT auth)
- **Uniqueness**: Nothing special to demonstrate
- **Decision**: Too common, not differentiated

### Notification Service
- **Complexity**: Low (async worker consuming queue)
- **Decision**: Important but straightforward, can mock for demo

### Theatre Management Service
- **Complexity**: Medium (CRUD with validation)
- **Decision**: Less critical than booking flow, similar patterns to booking

### Analytics Service
- **Complexity**: Medium (data aggregation)
- **Decision**: Not core to booking flow, can be added later

### Search Service (Elasticsearch)
- **Complexity**: Medium (indexing and search)
- **Decision**: Can be embedded in Catalog Service

---

## Combined Coverage: Catalog + Booking Services

### Together They Demonstrate:

#### 1. Full User Journey
```
Browse (Catalog) → Select (Catalog) → Book (Booking) → Pay (Booking) → Confirm (Booking)
```

#### 2. Read vs Write Patterns
- **Read**: Cache-heavy, eventual consistency acceptable
- **Write**: Strong consistency, ACID transactions critical

#### 3. CAP Theorem Trade-offs
- **Catalog**: Favor Availability (AP) - Stale cache acceptable
- **Booking**: Favor Consistency (CP) - No double bookings

#### 4. Scalability Patterns
- **Catalog**: Horizontal scaling + caching (stateless)
- **Booking**: Distributed locks + idempotency (stateful operations)

#### 5. Failure Handling
- **Catalog**: Graceful degradation (show cached data)
- **Booking**: Saga pattern (compensating transactions)

---

## Implementation Scope

### Service 1: Show Catalog Service

#### Features to Implement:
1. ✅ Browse shows by movie and date
2. ✅ Browse shows by theatre and date
3. ✅ Get show details with seat availability
4. ✅ Get applicable offers (50% on 3rd ticket, 20% afternoon discount)
5. ✅ Search and filter (city, language, genre)

#### Tech Stack:
- **Framework**: Spring Boot 3.2
- **Database**: PostgreSQL 15
- **Cache**: Redis 7.0
- **API**: REST with OpenAPI/Swagger
- **Testing**: JUnit 5, Testcontainers

#### Key Classes:
- `ShowCatalogController`
- `ShowCatalogService`
- `ShowRepository`, `TheatreRepository`, `MovieRepository`
- `RedisCacheService`
- `OfferService`

---

### Service 2: Booking Service

#### Features to Implement:
1. ✅ Check seat availability
2. ✅ Hold seats with distributed lock (10-minute TTL)
3. ✅ Create booking with offer calculation
4. ✅ Integrate with payment gateway (Razorpay mock)
5. ✅ Confirm booking on payment success
6. ✅ Cancel booking and release seats
7. ✅ Publish events for notification service

#### Tech Stack:
- **Framework**: Spring Boot 3.2
- **Database**: PostgreSQL 15 (with optimistic locking)
- **Cache/Lock**: Redis 7.0 (distributed locks, seat holds)
- **Messaging**: RabbitMQ (event publishing)
- **API**: REST with OpenAPI/Swagger
- **Testing**: JUnit 5, Testcontainers, WireMock

#### Key Classes:
- `BookingController`
- `BookingService`
- `SeatLockService` (Redis-based)
- `BookingRepository`, `SeatRepository`
- `PaymentService`
- `EventPublisher`

---

## Design Decisions Deep-Dive

### Decision 1: PostgreSQL vs NoSQL

#### Choice: **PostgreSQL**

**Why PostgreSQL?**
- **ACID transactions required** - Booking is a financial transaction
- **Complex relationships** - Shows, seats, bookings, payments are highly relational
- **Strong consistency needed** - No double bookings tolerated
- **Rich querying** - Joins across theatres, shows, movies
- **Mature ecosystem** - Connection pooling, replication, backup

**Why NOT NoSQL (MongoDB/Cassandra)?**
- **No transactions** (or limited in MongoDB)
- **Eventual consistency** - Unacceptable for bookings
- **Complex joins** - Application-level joins are inefficient
- **Schema rigidity needed** - Financial data requires strict schema

**When would NoSQL be considered?**
- **Session storage** - Fast key-value lookups (Redis used)
- **Analytics/logs** - Cassandra for time-series data
- **Product catalog** - If no relational queries needed

---

### Decision 2: Redis vs In-Memory Cache

#### Choice: **Redis**

**Why Redis?**
1. **Distributed** - Shared across multiple service instances
2. **Persistence** - RDB + AOF for durability
3. **Atomic operations** - SETNX for locks, EXPIRE for TTL
4. **Data structures** - Hashes, Sets, Sorted Sets
5. **Pub/Sub** - Real-time notifications
6. **Cluster mode** - Horizontal scaling with sharding

**Why NOT In-Memory (Caffeine/Guava)?**
1. **Not distributed** - Each instance has separate cache
   - User hits Instance A → Cache miss
   - User hits Instance B → Cache miss again (wasted DB query)
2. **No shared locks** - Cannot prevent double booking across instances
3. **Lost on restart** - Seat holds disappear
4. **Manual TTL** - Need background threads for cleanup

**Trade-offs:**
- **In-memory is faster** - No network latency (~1-2ms vs 0.01ms)
- **Redis adds complexity** - Network calls, serialization overhead
- **When to use in-memory** - Non-critical, instance-local data only

**Hybrid Approach (Future Optimization):**
```
L1 Cache (In-Memory) → L2 Cache (Redis) → Database
- L1: Immutable data (movie details)
- L2: Frequently changing data (seat availability)
- DB: Source of truth
```

---

### Decision 3: Optimistic vs Pessimistic Locking

#### Choice: **Both** (Hybrid approach)

**Optimistic Locking (Primary)**
- **When**: Final seat booking update
- **How**: Version column check
- **Code**:
  ```sql
  UPDATE seat SET status = 'BOOKED', version = version + 1
  WHERE id = ? AND version = ? AND status = 'HELD';
  ```
- **Pros**: Low overhead, no lock contention, scales well
- **Cons**: Retry logic needed on version mismatch

**Pessimistic Locking (Secondary)**
- **When**: Initial seat availability check
- **How**: SELECT FOR UPDATE SKIP LOCKED
- **Code**:
  ```sql
  SELECT * FROM seat WHERE id IN (?, ?, ?) AND status = 'AVAILABLE'
  FOR UPDATE SKIP LOCKED;
  ```
- **Pros**: Guaranteed lock, no version conflicts
- **Cons**: Lock contention, reduced throughput

**Why Hybrid?**
1. **First check**: Pessimistic (short-lived, ensures availability)
2. **User holds seat**: Redis lock (10 min, distributed)
3. **Final booking**: Optimistic (handles race conditions gracefully)

**Alternative Considered: Only Pessimistic**
- **Rejected**: Holding DB locks for 10 minutes would kill throughput
- **Why**: 100 users browsing = 100 active DB connections with locks

---

### Decision 4: Avoiding Duplicate Seat Booking

#### Multi-Layer Defense Strategy:

**Layer 1: Database Unique Constraint**
```sql
UNIQUE (show_id, seat_number)  -- Prevents DB-level duplicates
```

**Layer 2: Optimistic Locking (Version Column)**
```java
@Version
private Integer version;

// Hibernate will auto-check version on update
// Throws OptimisticLockException if version mismatch
```

**Layer 3: Distributed Lock (Redis)**
```java
public boolean acquireLock(Long seatId, String transactionId) {
    String lockKey = "lock:seat:" + seatId;
    Boolean acquired = redisTemplate.opsForValue()
        .setIfAbsent(lockKey, transactionId, Duration.ofSeconds(30));
    return Boolean.TRUE.equals(acquired);
}

// SETNX ensures atomic lock acquisition
```

**Layer 4: Application-Level Validation**
```java
if (seat.getStatus() != SeatStatus.AVAILABLE) {
    throw new SeatUnavailableException("Seat already booked");
}
```

**Layer 5: Idempotency Key**
```java
// Prevent duplicate requests from retry/refresh
@PostMapping("/bookings")
public BookingResponse createBooking(
    @RequestHeader("Idempotency-Key") String key,
    @RequestBody BookingRequest request
) {
    // Check if booking already exists for this key
}
```

#### How They Work Together:
```
Request 1 (User A):               Request 2 (User B):
1. Acquire Redis lock ✓           1. Acquire Redis lock ✗ (BLOCKED)
2. Check seat.status = AVAILABLE   2. Wait for lock...
3. Read seat.version = 5           3. Lock acquired after A releases
4. Update with version = 6 ✓       4. Check seat.status = BOOKED
5. Release lock                    5. Throw SeatUnavailableException ✗

Result: User A gets seat, User B sees error immediately
```

---

### Decision 5: Event-Driven vs Synchronous Notification

#### Choice: **Event-Driven (Async)**

**Why Async?**
1. **Payment success is critical** - Must return immediately
2. **Notification is non-critical** - Retry later if fails
3. **Decoupling** - Booking service doesn't wait for SMTP
4. **Resilience** - Notification failure doesn't block booking

**Flow:**
```
Payment Success → Booking Confirmed → Publish Event → Return Response (200ms)
                                    ↓
                            Notification Service (async)
                                    ↓
                            Send Email/SMS (2-5 seconds)
                                    ↓
                            If fails → Retry queue
```

**Edge Case Handling (req2 requirement):**
```
Payment Success + Notification Failed:
1. Booking status = CONFIRMED (in DB)
2. Payment status = SUCCESS (in DB)
3. Notification status = FAILED (in DB)
4. User redirected to Booking Confirmation page
5. Show banner: "Confirmation email pending, check your Message Center"
6. Store notification in user's inbox
7. Background worker retries (exponential backoff: 1m, 5m, 15m)
```

**Alternative Considered: Synchronous**
- **Rejected**: SMTP timeout (10s) would block booking response
- **Why**: Poor UX, payment already succeeded but user sees loading spinner

---

## Implementation Priority

### Phase 1: Core Entities & Repositories
- Database schema creation
- JPA entities with relationships
- Repository interfaces

### Phase 2: Show Catalog Service
- REST APIs for browsing
- Redis caching layer
- Offer calculation logic

### Phase 3: Booking Service
- Seat hold mechanism (Redis locks)
- Booking creation with transactions
- Payment gateway integration (mock)
- Event publishing

### Phase 4: Integration Testing
- Testcontainers for PostgreSQL + Redis
- Concurrent booking scenarios
- Payment webhook handling

---

## Technology Stack (Final)

### Backend
- **Language**: Java 17
- **Framework**: Spring Boot 3.2.3
- **API**: REST with Spring Web
- **Documentation**: SpringDoc OpenAPI (Swagger UI)

### Database
- **Primary**: PostgreSQL 15
- **ORM**: Spring Data JPA (Hibernate)
- **Migrations**: Flyway

### Cache & Locks
- **Cache**: Redis 7.2 (Lettuce client)
- **Locks**: Redis with Spring Data Redis

### Messaging
- **Queue**: RabbitMQ 3.12
- **Client**: Spring AMQP

### Testing
- **Unit**: JUnit 5, Mockito
- **Integration**: Testcontainers
- **API**: RestAssured

### Build & Deployment
- **Build**: Maven
- **Containerization**: Docker + Docker Compose
- **CI/CD**: GitHub Actions (optional)

---

## What Will NOT Be Implemented (Out of Scope)

To keep the exercise focused, the following are **documented but not coded**:

### 1. UI Layer
- No React/Angular frontend
- APIs will be tested via Swagger UI / Postman

### 2. Other Microservices
- User Service (assume JWT token provided)
- Notification Service (event published, consumer not built)
- Theatre Management Service (theatre data pre-seeded)
- Analytics Service (future scope)

### 3. Production Features
- OAuth2 / API Gateway (use simple JWT)
- Service mesh / Istio
- Comprehensive monitoring (Prometheus metrics added, but no Grafana)
- CI/CD pipelines (Dockerfile provided)

### 4. Advanced Features
- Bulk booking
- Seat layout visualization
- QR code generation
- PDF ticket generation
- Real payment gateway (use mock)

---

## Interview Defense Preparation

### Expected Questions & Answers

#### Q1: Why did you choose PostgreSQL over MongoDB?
**Answer:**
> Booking is a financial transaction requiring ACID compliance. PostgreSQL provides:
> - Multi-row transactions with rollback
> - Foreign key constraints preventing orphaned records
> - Proven reliability for payment systems (Stripe, PayPal use PostgreSQL)
> - Complex queries (joins across shows, theatres, seats) that are inefficient in MongoDB
>
> MongoDB would be suitable for: event logs, product catalogs with flexible schemas, or analytics data.

#### Q2: Why Redis for distributed locks instead of database locks?
**Answer:**
> Database locks are precious resources. Holding a lock for 10 minutes (seat hold duration) would:
> - Exhaust connection pool (max 100 connections)
> - Block other queries on locked rows
> - Cause timeout issues
>
> Redis locks are:
> - TTL-based (auto-expire)
> - No impact on database performance
> - Sub-millisecond latency
> - Designed for high-throughput locking

#### Q3: How do you prevent duplicate bookings across multiple instances?
**Answer:**
> Multi-layer defense:
> 1. **Redis distributed lock** (lock:seat:{id}) - Prevents concurrent access
> 2. **Optimistic locking** (version column) - Detects concurrent updates
> 3. **Database unique constraint** (show_id, seat_number) - Last line of defense
> 4. **Idempotency key** - Prevents duplicate API calls
>
> Even if Redis fails, optimistic locking + DB constraint prevent duplicates.

#### Q4: What happens if payment succeeds but notification fails?
**Answer:**
> This is explicitly handled in req2:
> 1. Booking status = CONFIRMED (payment succeeded)
> 2. User redirected to confirmation page (shows ticket)
> 3. Display warning banner: "Confirmation email pending"
> 4. Store notification in user's Message Center (in-app inbox)
> 5. Background retry worker picks up FAILED notifications
> 6. Exponential backoff: 1 min, 5 min, 15 min (max 3 retries)
> 7. If all fail: Manual investigation, but booking is valid
>
> **Key principle**: Payment success is immutable. Notification is best-effort.

#### Q5: Why not in-memory cache (Caffeine)?
**Answer:**
> In-memory cache is not shared across service instances:
> - Load balancer routes User A to Instance 1 (cache miss → DB query)
> - Next request routes User A to Instance 2 (cache miss again)
> - Result: 50% cache hit rate with 2 instances
>
> Redis provides:
> - **Shared cache** - All instances see same data
> - **Distributed locks** - Critical for booking
> - **Persistence** - Survives restarts
>
> Trade-off: 1-2ms network latency vs 0.01ms in-memory, but worth it for consistency.

#### Q6: How do you scale to 99.99% availability?
**Answer:**
> **Database:**
> - Multi-AZ deployment (AWS RDS)
> - Read replicas in each region (3-5)
> - Automatic failover (30-60 seconds)
>
> **Services:**
> - Kubernetes with auto-scaling (HPA)
> - Min 3 replicas per service
> - Health checks and rolling updates
>
> **Redis:**
> - Cluster mode with replication (3 master + 3 replica)
> - Automatic failover (Sentinel)
>
> **Region Strategy:**
> - Multi-region deployment (us-east-1, eu-west-1, ap-south-1)
> - Route53 geo-routing
> - Active-Active architecture
>
> **Calculation:**
> - 99.99% = 52.6 minutes downtime/year
> - Achieve via: Redundancy + Health checks + Fast failover

#### Q7: What's your caching strategy?
**Answer:**
> **Cache-Aside Pattern:**
> ```
> 1. Check Redis cache
> 2. If hit → Return (fast path)
> 3. If miss → Query database
> 4. Store in Redis with TTL
> 5. Return result
> ```
>
> **TTL Strategy:**
> - Show details: 5 minutes (rarely change)
> - Seat availability: 1 minute (frequently change)
> - Offers: 10 minutes (static during day)
> - Theatre info: 30 minutes (very static)
>
> **Cache Invalidation:**
> - Write-through on updates (update DB + delete cache)
> - Event-driven: ShowUpdated event → Invalidate cache
> - Expiry: TTL handles stale data gracefully

#### Q8: How do you handle payment gateway failures?
**Answer:**
> **Retry with Idempotency:**
> 1. Payment initiated with unique `idempotency_key`
> 2. Gateway returns 5xx error
> 3. Retry same request with same key (3 attempts)
> 4. Gateway recognizes key, returns original response
>
> **Circuit Breaker:**
> - Resilience4j circuit breaker on payment gateway calls
> - After 5 failures: Open circuit (fail fast)
> - Half-open after 60s (try one request)
>
> **Webhook for Async Confirmation:**
> - Don't rely on synchronous response
> - Gateway sends webhook on final status
> - Validate webhook signature
> - Update booking status

---

## Project Structure

```
movie-booking-platform/
├── catalog-service/
│   ├── src/main/java/com/moviebooking/catalog/
│   │   ├── controller/
│   │   ├── service/
│   │   ├── repository/
│   │   ├── model/
│   │   ├── dto/
│   │   ├── config/
│   │   └── exception/
│   ├── src/main/resources/
│   │   ├── application.yml
│   │   └── db/migration/
│   ├── src/test/java/
│   ├── Dockerfile
│   └── pom.xml
│
├── booking-service/
│   ├── src/main/java/com/moviebooking/booking/
│   │   ├── controller/
│   │   ├── service/
│   │   ├── repository/
│   │   ├── model/
│   │   ├── dto/
│   │   ├── config/
│   │   ├── event/
│   │   └── exception/
│   ├── src/main/resources/
│   │   ├── application.yml
│   │   └── db/migration/
│   ├── src/test/java/
│   ├── Dockerfile
│   └── pom.xml
│
├── common/
│   └── shared DTOs, enums, utilities
│
├── docker-compose.yml
├── schema.sql
├── seed-data.sql
├── README.md
└── docs/
    ├── API.md
    ├── ARCHITECTURE.md
    └── DEPLOYMENT.md
```

---

## Success Criteria

### Functional:
✅ All APIs return correct responses
✅ Seat booking prevents duplicates
✅ Offers calculate correctly
✅ Payment flow handles success/failure
✅ Events published on state changes

### Non-Functional:
✅ API response time < 500ms (P99)
✅ Concurrent booking test passes (10 users, same seat)
✅ Redis failover handled gracefully
✅ Database transactions rollback on error

### Code Quality:
✅ Clean architecture (Controller → Service → Repository)
✅ Proper exception handling
✅ Unit test coverage > 70%
✅ Integration tests for critical paths
✅ API documentation (Swagger)

---

## Timeline Estimate (Not Required, but for Context)

### Day 1-2: Foundation
- Database schema + migrations
- JPA entities
- Repository layer
- Basic CRUD APIs

### Day 3-4: Catalog Service
- Browse APIs
- Redis caching
- Offer calculation
- Integration tests

### Day 5-7: Booking Service
- Seat hold mechanism
- Booking creation
- Payment integration (mock)
- Saga pattern implementation
- Concurrency tests

### Day 8: Polish
- Documentation
- Docker Compose setup
- Seed data
- README with setup instructions

---

## Deliverable Checklist

- [x] HLD diagram (system architecture)
- [x] LLD diagram (class diagram)
- [x] User journey flows (success + failure)
- [x] Database schema with justifications
- [ ] Running code for 2 services ← **NEXT**
- [ ] API documentation (Swagger)
- [ ] Integration tests with Testcontainers
- [ ] Docker Compose for local setup
- [ ] GitHub repository with README
- [ ] Design decision documentation

---

**RECOMMENDATION APPROVED**: Proceeding with implementation of Catalog Service + Booking Service.
