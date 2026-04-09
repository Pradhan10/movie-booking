# Quick Start Guide

## Getting Started in 5 Minutes

---

## Prerequisites

- Docker Desktop installed and running
- Git installed

---

## Step 1: Clone Repository

```bash
git clone <your-repo-url>
cd movie-booking-platform
```

---

## Step 2: Start All Services

```bash
docker-compose up -d
```

**This starts**:
- PostgreSQL (port 5432)
- Redis (port 6379)
- RabbitMQ (port 5672, management UI 15672)
- Catalog Service (port 8081)
- Booking Service (port 8082)

**Wait 30 seconds** for services to initialize.

---

## Step 3: Verify Services

```bash
docker-compose ps
```

**Expected output**:
```
NAME                  STATUS    PORTS
catalog-service       Up        0.0.0.0:8081->8081/tcp
booking-service       Up        0.0.0.0:8082->8082/tcp
moviebooking-postgres Up        0.0.0.0:5432->5432/tcp
moviebooking-redis    Up        0.0.0.0:6379->6379/tcp
moviebooking-rabbitmq Up        0.0.0.0:5672->5672/tcp, 0.0.0.0:15672->15672/tcp
```

---

## Step 4: Test APIs

### Option A: Swagger UI (Recommended)

Open in browser:
- **Catalog Service**: http://localhost:8081/api/v1/swagger-ui.html
- **Booking Service**: http://localhost:8082/api/v1/swagger-ui.html

Click "Try it out" on any endpoint to test interactively.

---

### Option B: cURL Commands

**Test 1: Browse Shows**
```bash
curl "http://localhost:8081/api/v1/shows/movie/1?date=2026-03-29&city=Bangalore"
```

**Test 2: Hold Seats**
```bash
curl -X POST "http://localhost:8082/api/v1/bookings/hold" \
  -H "Content-Type: application/json" \
  -d '{"sessionId":"s1","showId":1,"seatIds":[1,2,3],"userId":1}'
```

**Test 3: Create Booking**
```bash
curl -X POST "http://localhost:8082/api/v1/bookings" \
  -H "Content-Type: application/json" \
  -d '{"userId":1,"showId":1,"seatIds":[1,2,3],"sessionId":"s1"}'
```

**Test 4: Complete Payment (Mock)**
```bash
# Use paymentId from Test 3 response
curl -X POST "http://localhost:8082/api/v1/bookings/payment/mock-success/1"
```

---

## Step 5: View Data

### PostgreSQL
```bash
docker exec -it moviebooking-postgres psql -U postgres -d moviebooking

# Sample queries
\dt                              # List tables
SELECT * FROM show LIMIT 5;      # View shows
SELECT * FROM seat WHERE show_id = 1 LIMIT 10;  # View seats
SELECT * FROM booking;           # View bookings
```

### Redis
```bash
docker exec -it moviebooking-redis redis-cli

# Sample commands
KEYS *                           # List all keys
GET show:details:1               # Get cached show
HGETALL seat:hold:s1             # View seat hold
```

### RabbitMQ Management UI
- URL: http://localhost:15672
- Username: `guest`
- Password: `guest`
- Navigate to "Queues" to see events

---

## Troubleshooting

### Services not starting?

**Check logs**:
```bash
docker-compose logs -f catalog-service
docker-compose logs -f booking-service
```

**Common issues**:
1. **Port already in use**: Stop conflicting service or change port in docker-compose.yml
2. **Database not ready**: Wait 30 seconds for init scripts to complete
3. **Out of memory**: Increase Docker Desktop memory to 4GB+

---

### Database connection error?

**Check PostgreSQL**:
```bash
docker exec -it moviebooking-postgres pg_isready -U postgres
```

**Reinitialize database**:
```bash
docker-compose down -v  # Remove volumes
docker-compose up -d    # Recreate with seed data
```

---

### Redis connection error?

**Check Redis**:
```bash
docker exec -it moviebooking-redis redis-cli ping
# Expected: PONG
```

---

## Clean Up

**Stop all services**:
```bash
docker-compose down
```

**Remove all data** (reset to fresh state):
```bash
docker-compose down -v
```

---

## Next Steps

1. Read `docs/SERVICE_RECOMMENDATIONS.md` for architecture decisions
2. Explore draw.io diagrams in `docs/` folder
3. Review `docs/API_DOCUMENTATION.md` for complete API reference
4. Check `docs/ARCHITECTURE_DEEPDIVE.md` for interview defense prep

---

## Running Tests Locally

```bash
# Catalog Service tests
cd catalog-service
mvn test

# Booking Service tests (includes concurrency test)
cd booking-service
mvn test

# Both services
mvn test  # From root directory
```

---

## Development Mode (Without Docker)

**Step 1**: Start dependencies only
```bash
docker-compose up -d postgres redis rabbitmq
```

**Step 2**: Run services from IDE
- Open `catalog-service` in IntelliJ/Eclipse
- Run `CatalogServiceApplication.java`
- Open `booking-service` in another window
- Run `BookingServiceApplication.java`

**Benefit**: Hot reload during development

---

**Ready to go!** Open Swagger UI and start testing: http://localhost:8081/api/v1/swagger-ui.html
