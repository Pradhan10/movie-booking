# ARCHITECTURE DEEP DIVE

## System Design Justifications for Interview

---

## Table of Contents
1. [Database Design](#database-design)
2. [Caching Strategy](#caching-strategy)
3. [Concurrency Control](#concurrency-control)
4. [Event-Driven Architecture](#event-driven-architecture)
5. [Scalability & High Availability](#scalability--high-availability)
6. [Security](#security)
7. [Monitoring & Observability](#monitoring--observability)

---

## 1. Database Design

### PostgreSQL vs NoSQL Decision Matrix

| Requirement | PostgreSQL | MongoDB | Cassandra | Winner |
|-------------|------------|---------|-----------|--------|
| **ACID Transactions** | ✅ Full support | ⚠️ Limited (single doc) | ❌ No | **PostgreSQL** |
| **Strong Consistency** | ✅ Yes | ⚠️ Eventual | ❌ Eventual | **PostgreSQL** |
| **Complex Joins** | ✅ Optimized | ❌ App-level | ❌ No joins | **PostgreSQL** |
| **Schema Enforcement** | ✅ Strict | ⚠️ Flexible | ⚠️ Flexible | **PostgreSQL** |
| **Write Throughput** | Good (10K/s) | Excellent (50K/s) | Excellent (100K/s) | NoSQL |
| **Read Scalability** | Good (replicas) | Excellent (sharding) | Excellent (P2P) | NoSQL |

**Decision**: PostgreSQL wins because:
- Financial transactions cannot tolerate eventual consistency
- One duplicate booking = lost customer trust + refund overhead
- Complex queries (browse shows across cities/theatres) are inefficient in NoSQL

**When would we use NoSQL?**
- **Analytics**: Cassandra for time-series booking data
- **Session Store**: Redis for user sessions (already using)
- **Logs**: Elasticsearch for application logs

---

### Schema Design Highlights

#### Normalized Design (3NF)
```
theatre (master data)
  ↓
show (transactional)
  ↓
seat (transactional, high write volume)
  ↓
booking (transactional, audit required)
```

**Why normalized?**
- Eliminates data redundancy
- Easier to maintain consistency
- Atomic updates

**Trade-off**: More joins required

**Mitigation**: JPA fetch strategies, caching

---

#### Optimistic Locking (Version Column)

**Why not pessimistic locks everywhere?**

Pessimistic locks (SELECT FOR UPDATE) hold database connections:
- User selects seat → Lock acquired
- User enters payment details (5 minutes) → Lock held
- 100 concurrent users → 100 active connections with locks
- Connection pool exhausted (max 30-50 connections)

**Solution**: Optimistic locking

```java
@Version
private Integer version;

// Hibernate automatically adds: WHERE version = ?
// If another transaction updated it, OptimisticLockException thrown
```

**Flow**:
1. Read seat (version = 5)
2. User holds seat for 10 minutes (no DB lock)
3. Final booking: UPDATE where version = 5
4. If version changed → Retry

**Trade-off**: Higher retry rate on contention

**Mitigation**: Combine with distributed locks (Redis)

---

#### Indexes Strategy

**High-traffic queries**:
```sql
-- Browse shows by movie and date (most common)
CREATE INDEX idx_show_movie_date ON show(movie_id, show_date);

-- Find available seats (booking flow)
CREATE INDEX idx_seat_show_status ON seat(show_id, status);

-- Cleanup expired holds (background job)
CREATE INDEX idx_seat_held_until ON seat(held_until);
```

**Index selectivity**:
- High cardinality columns first (e.g., movie_id before show_date)
- Composite indexes for common query patterns

**Cost**: Write performance impact (~10-15% slower inserts)
**Benefit**: Read performance gain (~100x faster queries)

---

## 2. Caching Strategy

### Cache-Aside Pattern

```
┌─────────┐   ┌─────────┐   ┌──────────────┐
│  Client │──▶│ Service │──▶│ Redis Cache  │
└─────────┘   └────┬────┘   └──────┬───────┘
                   │                │
                   │         Cache miss
                   ▼                │
              ┌──────────┐          │
              │   DB     │◀─────────┘
              └──────────┘
                   │
                   │ Fetch data
                   ▼
              Update cache + Return
```

**Implementation**:
```java
public List<ShowDTO> findShowsByMovie(Long movieId, LocalDate date, String city) {
    String cacheKey = "shows:movie:" + movieId + ":date:" + date + ":city:" + city;
    
    // Try cache first
    List<ShowDTO> cached = cacheService.get(cacheKey, List.class).orElse(null);
    if (cached != null) {
        return cached;  // Fast path: 3-5ms
    }
    
    // Cache miss: Query database
    List<Show> shows = showRepository.findByMovieAndDateAndCity(...);  // 100-150ms
    
    // Update cache
    cacheService.set(cacheKey, shows, Duration.ofSeconds(60));
    
    return shows;
}
```

---

### TTL Strategy

| Data Type | TTL | Rationale |
|-----------|-----|-----------|
| Show list | 60s | Seat availability changes frequently |
| Show details | 300s (5min) | Show info rarely changes |
| Theatre info | 1800s (30min) | Master data, very static |
| Offers | 600s (10min) | Updated daily, low change rate |
| Seat availability | 10s | Real-time critical |

**Why different TTLs?**
- Balance between **freshness** and **DB load**
- Stale data acceptable for static info
- Real-time critical for booking flow

---

### Cache Invalidation

**Write-through on updates**:
```java
public void updateShow(Show show) {
    showRepository.save(show);
    
    // Invalidate all related cache keys
    cacheService.delete("show:details:" + show.getId());
    cacheService.delete("shows:theatre:" + show.getTheatreId() + ":*");
    cacheService.delete("shows:movie:" + show.getMovieId() + ":*");
}
```

**Event-driven invalidation**:
```java
@EventListener
public void onShowUpdated(ShowUpdatedEvent event) {
    cacheService.delete("show:details:" + event.getShowId());
}
```

---

### Redis vs In-Memory Cache Comparison

**Scenario**: 2 service instances with load balancing

**With In-Memory Cache (Caffeine)**:
```
Request 1 → Instance A → Cache miss → DB query → Cache A updated
Request 2 → Instance B → Cache miss → DB query → Cache B updated
Request 3 → Instance A → Cache hit (from Request 1)
Request 4 → Instance B → Cache hit (from Request 2)

Cache hit rate: 50% (with 2 instances)
Cache hit rate: 33% (with 3 instances)
```

**With Redis**:
```
Request 1 → Instance A → Redis miss → DB query → Redis updated
Request 2 → Instance B → Redis hit (from Request 1)
Request 3 → Instance A → Redis hit
Request 4 → Instance B → Redis hit

Cache hit rate: 75%+ (regardless of instance count)
```

**Verdict**: Redis provides consistent cache hit rate across scaled instances

---

## 3. Concurrency Control

### The Duplicate Booking Problem

**Scenario**: 2 users click "Book" for seat A1 at exactly the same time

```
Time    User 1                          User 2
────────────────────────────────────────────────────────────
T0      Read seat A1 (AVAILABLE)        Read seat A1 (AVAILABLE)
T1      Decide to book                  Decide to book
T2      UPDATE seat SET status=BOOKED   UPDATE seat SET status=BOOKED
T3      ✅ Success                       ✅ Success ❌ DUPLICATE!
```

---

### Solution: Multi-Layer Defense

#### Layer 1: Distributed Lock (Redis)
```java
public boolean acquireLock(Long seatId, String txnId) {
    return redisTemplate.opsForValue()
        .setIfAbsent("lock:seat:" + seatId, txnId, Duration.ofSeconds(30));
}
```

**How it works**:
```
Time    User 1                          User 2
────────────────────────────────────────────────────────────
T0      SETNX lock:seat:A1 txn1 → ✅    SETNX lock:seat:A1 txn2 → ❌ (blocked)
T1      Proceed to book                 Wait for lock or fail
T2      UPDATE seat status=BOOKED       
T3      DELETE lock:seat:A1             Lock available (but seat now BOOKED)
T4      ✅ Success                       Query finds seat BOOKED → ❌ Failure
```

**Why SETNX?**
- Atomic operation (no race condition)
- Returns false if key exists (another transaction holds lock)
- TTL prevents deadlocks (auto-release after 30s)

---

#### Layer 2: Pessimistic Lock (Initial Check)
```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
List<Seat> findByIdsAndStatusForUpdate(List<Long> seatIds, SeatStatus status);
```

**SQL Generated**:
```sql
SELECT * FROM seat 
WHERE id IN (1, 2, 3) AND status = 'AVAILABLE'
FOR UPDATE SKIP LOCKED;
```

**Purpose**:
- Ensure seat is available at transaction start
- Short-lived lock (milliseconds)
- SKIP LOCKED avoids waiting on already-locked rows

---

#### Layer 3: Optimistic Lock (Final Update)
```java
@Version
private Integer version;

int updated = seatRepository.updateStatusWithOptimisticLock(
    seatId, SeatStatus.HELD, SeatStatus.BOOKED, expectedVersion
);

if (updated == 0) {
    throw new OptimisticLockException("Concurrent modification detected");
}
```

**SQL Generated**:
```sql
UPDATE seat 
SET status = 'BOOKED', version = version + 1, updated_at = NOW()
WHERE id = ? AND status = 'HELD' AND version = ?;

-- If rows affected = 0, another transaction updated it
```

---

#### Layer 4: Database Constraint
```sql
CONSTRAINT unique_seat_per_show UNIQUE (show_id, seat_number)
```

**Last line of defense**: Even if all locks fail, database prevents duplicate

---

### Why This Hybrid Approach?

| Approach | Pros | Cons | Our Usage |
|----------|------|------|-----------|
| **Only Pessimistic** | Simple, guaranteed | Connection pool exhaustion | ❌ No (holds too long) |
| **Only Optimistic** | High throughput | High retry rate on contention | ❌ No (poor UX) |
| **Only Distributed** | Scales well | No DB-level guarantee | ❌ No (risky) |
| **Hybrid (All 3)** | Best of all worlds | More complex | ✅ **YES** |

---

## 4. Event-Driven Architecture

### Why Async Notifications?

**Problem**: Synchronous notification blocks booking response

```
Synchronous Flow:
Payment Success → Update Booking → Send Email (2-5s) → Return Response
                                    ↑
                              If SMTP timeout?
                              User waits 30s → ERROR
```

**Solution**: Async with events

```
Async Flow:
Payment Success → Update Booking → Publish Event → Return Response (200ms)
                                         ↓
                              Notification Service (background)
                                         ↓
                              Send Email (2-5s, user doesn't wait)
                                         ↓
                              If fails → Retry queue
```

---

### Saga Pattern for Distributed Transactions

**Booking Flow as Saga**:

```
┌─────────────┐    ┌─────────────┐    ┌─────────────┐
│   Booking   │───▶│   Payment   │───▶│Notification │
│   Service   │    │   Service   │    │   Service   │
└─────────────┘    └─────────────┘    └─────────────┘
      │                   │                   │
      ▼                   ▼                   ▼
   Hold Seats        Process Pay         Send Email
      │                   │                   │
      ├─Success───────────┼─Success───────────┼─Success─▶ ✅ Complete
      │                   │                   │
      │                   ├─Failure───────────┼──────────▶ ❌ Rollback
      │                   │                               (Cancel booking)
      │                   │                               (Release seats)
```

**Compensating Transactions**:
```java
// If payment fails after booking created
public void handlePaymentFailure(Long bookingId) {
    cancelBooking(bookingId, "Payment failed");  // Compensating action
    releaseSeats(bookingId);                     // Rollback seat status
    publishBookingCancelledEvent();              // Notify downstream
}
```

---

### Event Schema

**BookingConfirmedEvent**:
```json
{
  "bookingId": 1,
  "bookingReference": "BK1711638000-A1B2C3D4",
  "userId": 1,
  "showId": 1,
  "seatIds": [1, 2, 3],
  "finalAmount": 625.00,
  "confirmedAt": "2026-03-28T15:40:00Z"
}
```

**Consumers**:
- Notification Service (send email/SMS)
- Analytics Service (update KPIs)
- Audit Service (log for compliance)

---

## 5. Scalability & High Availability

### Horizontal Scaling Strategy

**Services**:
```
        Load Balancer (NGINX)
               │
    ┌──────────┼──────────┐
    ▼          ▼          ▼
  Instance1  Instance2  Instance3  (Auto-scaled)
```

**Why stateless services?**
- No session affinity needed
- Any instance can handle any request
- Easy to add/remove instances

**State management**:
- Session: Redis (shared across instances)
- Database: PostgreSQL (single source of truth)

---

### Database Scaling

**Read Replicas**:
```
┌──────────┐
│  Master  │ (Writes only)
└────┬─────┘
     │
     ├─────────┬─────────┬─────────┐
     ▼         ▼         ▼         ▼
  Replica1  Replica2  Replica3  Replica4
  (Reads)   (Reads)   (Reads)   (Reads)
```

**Routing Strategy**:
- Write operations → Master
- Browse/search (90% of traffic) → Replicas (round-robin)
- Booking read (verify) → Master (avoid replication lag)

**Replication Lag**: < 1 second (acceptable for browse, not for booking)

---

### Redis Cluster

**Setup**:
```
┌─────────┐  ┌─────────┐  ┌─────────┐
│Master 1 │  │Master 2 │  │Master 3 │  (Hash slots 0-16383)
└────┬────┘  └────┬────┘  └────┬────┘
     │            │            │
     ▼            ▼            ▼
  Replica1     Replica2     Replica3
```

**Benefits**:
- Automatic sharding (16K hash slots)
- High availability (master fails → replica promoted)
- Linear scalability (add more masters)

**Trade-off**: Cluster management overhead

---

### Multi-Region Deployment

```
          Route53 (Geo-routing)
                 │
     ┌───────────┼───────────┐
     ▼           ▼           ▼
  US-East    EU-West    AP-South
    │           │           │
  [Full]     [Full]     [Full]
  Stack      Stack      Stack
```

**Active-Active Architecture**:
- Each region has complete stack
- Database replication across regions (async)
- Users routed to nearest region (latency optimization)

**Data Consistency**:
- Master in each region
- Cross-region replication (eventual consistency)
- Conflict resolution: Last-write-wins (LWW) with timestamp

**Trade-off**: Complexity vs availability

---

### 99.99% Availability Calculation

**Target**: 52.6 minutes downtime per year

**Single Component Availability**:
- Database: 99.95% (AWS RDS Multi-AZ)
- Service: 99.9% (3 instances with health checks)
- Redis: 99.9% (Cluster with replication)

**Combined**:
- With redundancy: 1 - (0.0005 × 0.001 × 0.001) = 99.9999%
- With buffer for deployments: 99.99%

**Deployment Strategy**:
- Rolling updates (1 instance at a time)
- Blue-green deployment for zero downtime
- Canary releases (1% traffic first)

---

## 6. Security

### OWASP Top 10 Mitigations

#### 1. SQL Injection
**Threat**: Malicious SQL in user input
```sql
-- Vulnerable
"SELECT * FROM show WHERE city = '" + userInput + "'"

-- If userInput = "Bangalore' OR '1'='1"
-- Returns all shows regardless of city
```

**Mitigation**: Prepared statements (JPA)
```java
@Query("SELECT s FROM Show s WHERE s.city = :city")
List<Show> findByCity(@Param("city") String city);

// Hibernate uses PreparedStatement with bound parameters
```

---

#### 2. Broken Authentication
**Threat**: Weak authentication allows unauthorized access

**Mitigation**:
- JWT with short expiry (15 minutes)
- Refresh tokens (7 days, stored in HttpOnly cookie)
- Password hashing (BCrypt with salt)
```java
String hash = BCrypt.hashpw(password, BCrypt.gensalt(12));
```

---

#### 3. Sensitive Data Exposure
**Threat**: Payment card data leaked

**Mitigation**:
- **Never store card numbers** - Gateway handles it
- Store only `gateway_txn_id` for reconciliation
- TLS 1.3 for data in transit
- PostgreSQL encryption at rest (AES-256)

---

#### 4. API Security
**Rate Limiting**:
```java
@RateLimiter(name = "bookingAPI", fallbackMethod = "rateLimitFallback")
public BookingResponse createBooking(BookingRequest request) {
    // Limited to 5 bookings per minute per user
}
```

**API Key for Partners**:
```java
@PreAuthorize("hasRole('THEATRE_PARTNER')")
public void createShow(ShowRequest request) {
    // Only theatre partners can create shows
}
```

---

## 7. Monitoring & Observability

### Three Pillars

#### 1. Metrics (Prometheus)
```java
@Timed(value = "booking.create", histogram = true)
public Booking createBooking(BookingRequest request) {
    // Automatically records duration, count, percentiles
}
```

**Key Metrics**:
- `booking_create_duration_seconds` (histogram)
- `seat_lock_acquisition_failures_total` (counter)
- `cache_hit_rate` (gauge)
- `db_connection_pool_active` (gauge)

---

#### 2. Logging (ELK Stack)
```java
log.info("Booking created - ref: {}, user: {}, amount: {}, seats: {}",
    booking.getReference(), userId, amount, seatIds);

// JSON output:
// {
//   "timestamp": "2026-03-28T15:40:00Z",
//   "level": "INFO",
//   "service": "booking-service",
//   "correlationId": "abc-123",
//   "message": "Booking created",
//   "bookingRef": "BK1711638000-A1B2C3D4",
//   "userId": 1,
//   "amount": 625.00
// }
```

---

#### 3. Tracing (Jaeger)
```
User Request → API Gateway → Catalog Service → Database
                                    ↓
                          Booking Service → Redis
                                    ↓
                          Payment Service → Gateway
                                    ↓
                          Notification Service → SMTP

Trace ID: abc-123 (propagated across all services)
```

**Identifies Bottlenecks**:
- Which service is slow?
- Where is time spent? (DB query vs network vs processing)

---

### Alerting Rules

```yaml
alerts:
  - name: HighErrorRate
    condition: error_rate > 1%
    severity: critical
    action: Page on-call engineer
  
  - name: HighLatency
    condition: p99_latency > 1000ms
    severity: warning
    action: Create Jira ticket
  
  - name: DatabaseDown
    condition: db_connection_failures > 5
    severity: critical
    action: Auto-failover + Page SRE team
```

---

## Deployment Architecture

### Kubernetes Manifests

**Service Deployment**:
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: booking-service
spec:
  replicas: 3
  strategy:
    type: RollingUpdate
    rollingUpdate:
      maxSurge: 1
      maxUnavailable: 0
  template:
    spec:
      containers:
      - name: booking-service
        image: booking-service:1.0.0
        resources:
          requests:
            memory: "512Mi"
            cpu: "500m"
          limits:
            memory: "1Gi"
            cpu: "1000m"
        livenessProbe:
          httpGet:
            path: /actuator/health
            port: 8082
          initialDelaySeconds: 30
          periodSeconds: 10
        readinessProbe:
          httpGet:
            path: /actuator/health/readiness
            port: 8082
          initialDelaySeconds: 10
          periodSeconds: 5
```

**Auto-scaling**:
```yaml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: booking-service-hpa
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: booking-service
  minReplicas: 3
  maxReplicas: 20
  metrics:
  - type: Resource
    resource:
      name: cpu
      target:
        type: Utilization
        averageUtilization: 70
  - type: Pods
    pods:
      metric:
        name: http_requests_per_second
      target:
        type: AverageValue
        averageValue: "1000"
```

---

## Cost Optimization

### Cloud Cost Breakdown (AWS)

| Component | Instance Type | Monthly Cost |
|-----------|---------------|--------------|
| **Database** | RDS db.r6g.xlarge (Multi-AZ) | $450 |
| **Redis** | ElastiCache cache.r6g.large (Cluster) | $280 |
| **Services** | EKS (3 nodes, t3.large) | $220 |
| **Load Balancer** | ALB | $25 |
| **Data Transfer** | 1 TB/month | $90 |
| **RabbitMQ** | EC2 t3.medium | $35 |
| **Total** | | **~$1,100/month** |

**Scaling Cost**:
- 10x traffic → Add 2 DB replicas + 10 service instances = +$600/month
- Still < $2K/month for 100K concurrent users

---

## Summary: Key Takeaways for Interview

### Technical Depth Demonstrated

1. ✅ **Concurrency Control**: 4-layer defense against duplicate bookings
2. ✅ **Distributed Systems**: Redis locks, event-driven, stateless services
3. ✅ **Performance**: Caching strategy reduces DB load by 80%
4. ✅ **Reliability**: Optimistic locking, retry mechanisms, circuit breakers
5. ✅ **Scalability**: Horizontal scaling, read replicas, multi-region
6. ✅ **Security**: OWASP mitigation, PCI-DSS compliance
7. ✅ **Observability**: Metrics, logs, tracing, alerting

### Design Trade-offs Made

| Decision | Trade-off | Rationale |
|----------|-----------|-----------|
| Redis over in-memory | +2ms latency | Consistency across instances |
| Async notifications | Eventual delivery | Don't block booking response |
| Optimistic locking | Retry overhead | Better throughput than pessimistic |
| Normalized schema | More joins | Data consistency critical |
| PostgreSQL | Lower write throughput | ACID compliance required |

---

**This document should be used as reference during the interview discussion to demonstrate architectural thinking and design maturity.**
