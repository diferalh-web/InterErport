# Redis Setup for Guarantees Module

This guide explains how to set up and use Redis with the Guarantees Module for caching and session management.

## 🚀 Quick Start

### Option 1: Using Docker (Recommended)

1. **Start Redis with Docker Compose:**
   ```bash
   cd guarantees-module
   ./start-redis.sh
   ```

2. **Or manually with Docker Compose:**
   ```bash
   docker-compose -f docker-compose.redis.yml up -d
   ```

### Option 2: Local Installation

1. **Install Redis locally:**
   ```bash
   # macOS with Homebrew
   brew install redis
   
   # Ubuntu/Debian
   sudo apt-get install redis-server
   
   # CentOS/RHEL
   sudo yum install redis
   ```

2. **Start Redis:**
   ```bash
   redis-server
   ```

## 🔧 Configuration

### Environment Variables

You can configure Redis using environment variables:

```bash
export REDIS_HOST=localhost
export REDIS_PORT=6379
export REDIS_PASSWORD=your_password
export REDIS_DATABASE=0
```

### Application Configuration

Redis is configured in `application.yml`:

```yaml
spring:
  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}
      password: ${REDIS_PASSWORD:}
      database: ${REDIS_DATABASE:0}
      timeout: 2000ms
      lettuce:
        pool:
          max-active: 20
          max-idle: 10
          min-idle: 5
          max-wait: 2000ms
```

## 📊 Cache Configuration

### Cache Types and TTL

| Cache Type | TTL | Description |
|------------|-----|-------------|
| `fxRates` | 60 minutes | Foreign exchange rates |
| `dashboard` | 5 minutes | Dashboard analytics |
| `guarantees:session` | Session-based | User sessions |

### Cache Keys Pattern

- **FX Rates**: `fxRates:{fromCurrency}_{toCurrency}`
- **Dashboard**: `dashboard:summary`, `dashboard:monthly_{months}`
- **Sessions**: `guarantees:session:{sessionId}`

## 🛠️ Management

### Redis Management Endpoints

The application provides REST endpoints for cache management:

```bash
# Get cache statistics
GET /api/v1/redis/stats

# Clear dashboard cache
POST /api/v1/redis/clear/dashboard

# Clear FX rates cache
POST /api/v1/redis/clear/fx-rates

# Clear all cache
POST /api/v1/redis/clear/all

# Check if key exists
GET /api/v1/redis/exists/{key}

# Delete specific key
DELETE /api/v1/redis/key/{key}
```

### Redis Commander (Web UI)

Access the Redis web interface at: http://localhost:8081

## 🔍 Monitoring

### Check Redis Status

```bash
# Test Redis connection
docker exec guarantees-redis redis-cli ping

# Get Redis info
docker exec guarantees-redis redis-cli info

# Monitor Redis commands
docker exec guarantees-redis redis-cli monitor
```

### Cache Statistics

```bash
# Get cache statistics via API
curl -u admin:admin123 http://localhost:8080/api/v1/redis/stats
```

## 🚨 Troubleshooting

### Common Issues

1. **Redis Connection Failed**
   - Check if Redis is running: `docker ps | grep redis`
   - Verify port 6379 is available: `lsof -i :6379`
   - Check Redis logs: `docker logs guarantees-redis`

2. **Cache Not Working**
   - Verify Redis configuration in `application.yml`
   - Check if `@EnableCaching` is present in main application class
   - Ensure Redis dependencies are in `pom.xml`

3. **Performance Issues**
   - Monitor Redis memory usage: `docker exec guarantees-redis redis-cli info memory`
   - Check connection pool settings
   - Review TTL configurations

### Logs

```bash
# Redis logs
docker logs guarantees-redis

# Application logs
tail -f backend/backend.log
```

## 🔒 Security

### Production Considerations

1. **Set Redis Password:**
   ```bash
   export REDIS_PASSWORD=your_secure_password
   ```

2. **Use Redis AUTH:**
   ```yaml
   spring:
     data:
       redis:
         password: ${REDIS_PASSWORD}
   ```

3. **Network Security:**
   - Use Redis in private networks
   - Configure firewall rules
   - Use TLS for encrypted connections

## 📈 Performance Tuning

### Redis Configuration

```bash
# Increase memory limit
docker exec guarantees-redis redis-cli config set maxmemory 512mb

# Set eviction policy
docker exec guarantees-redis redis-cli config set maxmemory-policy allkeys-lru
```

### Application Configuration

```yaml
spring:
  data:
    redis:
      lettuce:
        pool:
          max-active: 50    # Increase for high load
          max-idle: 20      # Increase for better performance
          min-idle: 10      # Keep connections alive
          max-wait: 5000ms  # Increase timeout
```

## 🧪 Testing

### Test Redis Integration

```bash
# Start the application
cd guarantees-module
./start-backend.sh

# Test cache operations
curl -u admin:admin123 http://localhost:8080/api/v1/redis/stats
curl -u admin:admin123 http://localhost:8080/api/v1/dashboard/summary
curl -u admin:admin123 http://localhost:8080/api/v1/redis/stats
```

## 📚 Additional Resources

- [Redis Documentation](https://redis.io/documentation)
- [Spring Data Redis](https://spring.io/projects/spring-data-redis)
- [Redis Commander](https://github.com/joeferner/redis-commander)
- [Docker Redis](https://hub.docker.com/_/redis)

---

**Note**: This Redis setup is optimized for development and testing. For production deployment, consider using managed Redis services like AWS ElastiCache, Azure Cache for Redis, or Google Cloud Memorystore.

