# Security Setup & Deployment Guide

## Quick Start (Local Development)

### 1. Clone & Setup
```bash
git clone <repository-url>
cd psharma32

# Copy environment template
cp .env.example .env
```

### 2. Configure Environment Variables
Edit `.env` file:
```bash
# Minimum required for local development
DB_PASSWORD=your_local_password
REDIS_PASSWORD=your_local_password
RABBITMQ_PASSWORD=your_local_password
JWT_SECRET=generate-a-secure-32-character-minimum-secret-key-here
```

### 3. Start Infrastructure
```bash
# Start PostgreSQL, Redis, RabbitMQ
docker-compose up -d postgres redis rabbitmq

# Wait for services to be healthy
docker-compose ps
```

### 4. Run Services
```bash
# Option A: Using Maven (Development)
cd booking-service
mvn spring-boot:run -Dspring-boot.run.profiles=dev

cd ../catalog-service
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# Option B: Using Docker (Production-like)
docker-compose up -d
```

### 5. Access Services
- **Booking Service**: http://localhost:8082/api/v1
- **Catalog Service**: http://localhost:8081/api/v1
- **Swagger UI** (dev only): http://localhost:8082/swagger-ui.html
- **RabbitMQ Management**: http://localhost:15672

---

## Authentication Setup

### Generate JWT Token (Mock - Implement Real Auth Service)

For testing, you can generate a JWT token using the `JwtTokenUtil`:

```java
// Example JWT generation (add to a test/dev endpoint)
@RestController
@RequestMapping("/dev/auth")
@Profile("dev")
public class DevAuthController {
    
    @Autowired
    private JwtTokenUtil jwtTokenUtil;
    
    @PostMapping("/token")
    public String generateToken() {
        return jwtTokenUtil.generateToken(
            123L,                              // userId
            "testuser",                        // username
            "test@example.com",                // email
            List.of("ROLE_USER", "ROLE_ADMIN") // roles
        );
    }
}
```

### Using JWT Token
```bash
# Get token (implement this endpoint)
TOKEN=$(curl -X POST http://localhost:8082/dev/auth/token)

# Use token in requests
curl -H "Authorization: Bearer $TOKEN" \
  http://localhost:8082/api/v1/bookings/hold \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 123,
    "showId": 1,
    "seatIds": [1,2,3],
    "sessionId": "session-12345"
  }'
```

---

## Production Deployment

### 1. Generate Secure Secrets
```bash
# Generate JWT secret (256-bit)
openssl rand -base64 32

# Generate database password
openssl rand -base64 24

# Generate Redis password
openssl rand -base64 24

# Generate RabbitMQ password
openssl rand -base64 24
```

### 2. Configure Production Environment
Create `.env` file with production values:
```bash
# Database
DB_NAME=moviebooking_prod
DB_USERNAME=movieapp_user
DB_PASSWORD=<generated-secure-password>

# Redis
REDIS_PASSWORD=<generated-secure-password>

# RabbitMQ
RABBITMQ_USERNAME=movieapp_mq
RABBITMQ_PASSWORD=<generated-secure-password>

# JWT (CRITICAL - Never reuse across environments)
JWT_SECRET=<generated-256-bit-secret>
JWT_EXPIRATION=86400000

# Payment Gateway
PAYMENT_GATEWAY_URL=https://api.razorpay.com
PAYMENT_WEBHOOK_SECRET=<secret-from-razorpay>

# CORS
CORS_ALLOWED_ORIGINS=https://yourdomain.com,https://www.yourdomain.com

# Security
SWAGGER_ENABLED=false
SPRING_PROFILES_ACTIVE=prod
```

### 3. Deploy with Docker Compose
```bash
# Build images
docker-compose build

# Start services in production mode
SPRING_PROFILES_ACTIVE=prod docker-compose up -d

# View logs
docker-compose logs -f booking-service
docker-compose logs -f catalog-service
```

### 4. Verify Deployment
```bash
# Check health endpoints
curl http://your-domain.com/api/v1/actuator/health
# Should return: {"status":"UP"}

# Verify Swagger is disabled
curl http://your-domain.com/api/v1/swagger-ui.html
# Should return: 404 Not Found

# Verify authentication required
curl http://your-domain.com/api/v1/bookings
# Should return: 401 Unauthorized

# Verify CORS
curl -H "Origin: https://attacker.com" http://your-domain.com/api/v1/shows
# Should be blocked by CORS
```

---

## Security Checklist

### Pre-Deployment
- [ ] All environment variables set in `.env`
- [ ] JWT secret is 32+ characters and unique
- [ ] Database password is strong (16+ characters)
- [ ] Redis password is set
- [ ] RabbitMQ uses non-default credentials
- [ ] CORS origins set to production domains only
- [ ] Swagger UI disabled (`SWAGGER_ENABLED=false`)
- [ ] Spring profile set to `prod`

### Post-Deployment
- [ ] Health check endpoint accessible
- [ ] Swagger UI returns 404
- [ ] Unauthenticated requests return 401
- [ ] Authorization checks working (user can't access others' bookings)
- [ ] Mock payment endpoint not accessible
- [ ] Redis requires password
- [ ] RabbitMQ management console secured

### Monitoring
- [ ] Set up log aggregation (ELK Stack)
- [ ] Configure alerts for SecurityException
- [ ] Monitor correlation IDs for request tracing
- [ ] Set up JWT token expiration alerts
- [ ] Monitor failed authentication attempts

---

## Troubleshooting

### Issue: 401 Unauthorized on All Requests

**Cause**: JWT token missing or invalid

**Solution**:
```bash
# Verify JWT secret is set
echo $JWT_SECRET

# Generate new token with correct secret
# Ensure token hasn't expired (default: 24 hours)
```

### Issue: CORS Error in Browser

**Cause**: Frontend origin not in CORS_ALLOWED_ORIGINS

**Solution**:
```bash
# Update .env
CORS_ALLOWED_ORIGINS=https://your-frontend-domain.com

# Restart services
docker-compose restart booking-service catalog-service
```

### Issue: Redis Connection Failed

**Cause**: Missing REDIS_PASSWORD

**Solution**:
```bash
# Set in .env
REDIS_PASSWORD=your-redis-password

# Restart services
docker-compose restart redis booking-service catalog-service
```

### Issue: Mock Payment Endpoint Works in Production

**Cause**: Wrong Spring profile

**Solution**:
```bash
# Ensure prod profile is active
export SPRING_PROFILES_ACTIVE=prod

# Or in docker-compose.yml
environment:
  SPRING_PROFILES_ACTIVE: prod
```

---

## Development Profiles

### dev (Development)
- Swagger UI enabled
- DEBUG logging
- Mock payment endpoint available
- Relaxed CORS

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

### dev-no-auth (Testing without Security)
- All security disabled
- Useful for integration tests

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev-no-auth
```

### prod (Production)
- Swagger UI disabled
- INFO/WARN logging only
- Mock payment endpoint disabled
- Strict CORS

```bash
java -jar booking-service.jar --spring.profiles.active=prod
```

---

## Testing Endpoints

### With Authentication
```bash
# Set your JWT token
TOKEN="eyJhbGciOiJIUzUxMiJ9..."

# Hold seats
curl -H "Authorization: Bearer $TOKEN" \
  -X POST http://localhost:8082/api/v1/bookings/hold \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 123,
    "showId": 1,
    "seatIds": [1, 2],
    "sessionId": "session-abc123"
  }'

# Create booking
curl -H "Authorization: Bearer $TOKEN" \
  -X POST http://localhost:8082/api/v1/bookings \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 123,
    "showId": 1,
    "seatIds": [1, 2],
    "sessionId": "session-abc123",
    "contactEmail": "user@example.com",
    "contactPhone": "+1234567890"
  }'

# Cancel booking
curl -H "Authorization: Bearer $TOKEN" \
  -X POST http://localhost:8082/api/v1/bookings/1/cancel?reason=changed_plans
```

---

## Performance Tuning

### Database Connection Pool
```yaml
# application.yml
spring:
  datasource:
    hikari:
      maximum-pool-size: 30      # Increase for high load
      minimum-idle: 10
      connection-timeout: 30000
```

### Redis Connection Pool
```yaml
spring:
  data:
    redis:
      lettuce:
        pool:
          max-active: 20  # Increase for high load
          max-idle: 10
```

### JVM Options
```bash
# In Dockerfile or docker-compose.yml
JAVA_OPTS=-Xms512m -Xmx2g -XX:+UseG1GC
```

---

## Backup & Recovery

### Database Backup
```bash
# Backup
docker exec moviebooking-postgres pg_dump -U postgres moviebooking > backup.sql

# Restore
docker exec -i moviebooking-postgres psql -U postgres moviebooking < backup.sql
```

### Redis Backup
```bash
# Redis automatically persists to /data (AOF enabled)
# Backup Redis data
docker cp moviebooking-redis:/data/appendonly.aof ./redis-backup.aof
```

---

## Support & Documentation

- **Security Fixes**: `docs/SECURITY_FIXES_IMPLEMENTED.md`
- **Security Audit**: `docs/SECURITY_AUDIT_REPORT.md`
- **API Documentation**: http://localhost:8082/swagger-ui.html (dev only)
- **Architecture**: `docs/system-architecture-hld.drawio`

---

**Version**: 2.0  
**Last Updated**: 2024-03-28
