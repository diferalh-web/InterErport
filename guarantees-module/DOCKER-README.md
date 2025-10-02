# 🐳 Docker Setup for CQRS Guarantees Module

This document provides complete instructions for running the CQRS Guarantees Module using Docker. This setup includes all services: Backend, Frontend, MySQL (Command & Query), Kafka, Redis, and management UIs.

## 🏗️ Architecture Overview

```
┌─────────────┐    ┌─────────────┐    ┌─────────────┐    ┌─────────────┐
│  Frontend   │    │   Backend   │    │ Command DB  │    │  Query DB   │
│  (React)    │◄──►│ (Spring)    │◄──►│  (MySQL)    │    │  (MySQL)    │
│  Port 3000  │    │  Port 8082  │    │  Port 3306  │    │  Port 3307  │
└─────────────┘    └─────────────┘    └─────────────┘    └─────────────┘
                           │                   │
                           ▼                   ▼
                    ┌─────────────┐    ┌─────────────┐
                    │    Kafka    │    │    Redis    │
                    │  Port 9092  │    │  Port 6379  │
                    └─────────────┘    └─────────────┘
```

## 🚀 Quick Start

### **Prerequisites**
- Docker Desktop (Windows/Mac) or Docker Engine (Linux)
- Docker Compose
- At least 4GB RAM available for Docker
- Ports 3000, 8080, 8081, 8082, 8083, 3306, 3307, 6379, 9092, 2181 available

### **1. Start Everything**
```bash
cd /Users/dherrera/idea/InterErport/guarantees-module
./start-docker-full.sh
```

### **2. Test the Application**
```bash
./test-docker-application.sh
```

### **3. Access the Application**
- **Frontend**: http://localhost:3000
- **Backend API**: http://localhost:8082/api/v1
- **Kafka UI**: http://localhost:8080
- **Redis Commander**: http://localhost:8081
- **phpMyAdmin**: http://localhost:8083

## 📊 Services Included

### **Application Services**
| Service | Port | Description | URL |
|---------|------|-------------|-----|
| Frontend | 3000 | React application | http://localhost:3000 |
| Backend | 8082 | Spring Boot API | http://localhost:8082/api/v1 |

### **Database Services**
| Service | Port | Description | Credentials |
|---------|------|-------------|-------------|
| MySQL Command | 3306 | Write database | guarantees_command_user / guarantees_command_pass |
| MySQL Query | 3307 | Read database | guarantees_query_user / guarantees_query_pass |

### **Infrastructure Services**
| Service | Port | Description | URL |
|---------|------|-------------|-----|
| Kafka | 9092 | Event streaming | localhost:9092 |
| Redis | 6379 | Caching & sessions | redis://localhost:6379 |
| Zookeeper | 2181 | Kafka coordination | localhost:2181 |

### **Management UIs**
| Service | Port | Description | URL |
|---------|------|-------------|-----|
| Kafka UI | 8080 | Kafka management | http://localhost:8080 |
| Redis Commander | 8081 | Redis management | http://localhost:8081 |
| phpMyAdmin | 8083 | Database management | http://localhost:8083 |

## 🔧 Configuration

### **Environment Variables**
The application uses the following environment variables (defined in `docker.env`):

```bash
# Database Configuration
COMMAND_DB_USERNAME=guarantees_command_user
COMMAND_DB_PASSWORD=guarantees_command_pass
QUERY_DB_USERNAME=guarantees_query_user
QUERY_DB_PASSWORD=guarantees_query_pass

# Kafka Configuration
KAFKA_BOOTSTRAP_SERVERS=kafka:29092

# Redis Configuration
REDIS_HOST=redis
REDIS_PORT=6379
REDIS_PASSWORD=redispassword

# Spring Profile
SPRING_PROFILES_ACTIVE=cqrs
```

### **Application Profiles**
- `cqrs` - CQRS with dual databases and Kafka
- `docker` - Docker-specific configuration
- `default` - Traditional single database approach

## 🧪 Testing the Application

### **1. Health Checks**
```bash
# Backend health
curl http://localhost:8082/api/v1/actuator/health

# Frontend
curl http://localhost:3000
```

### **2. CQRS Commands (Write Operations)**
```bash
# Create a guarantee
curl -X POST http://localhost:8082/api/v1/cqrs/commands/guarantees \
  -H 'Content-Type: application/json' \
  -u admin:admin123 \
  -d '{
    "reference": "GAR-2024-001",
    "guaranteeType": "BID_BOND",
    "amount": 100000.00,
    "currency": "USD",
    "issueDate": "2024-01-01",
    "expiryDate": "2024-12-31",
    "beneficiaryName": "Test Beneficiary",
    "applicantId": 1,
    "guaranteeText": "Test guarantee text",
    "language": "en"
  }'
```

### **3. CQRS Queries (Read Operations)**
```bash
# Get all guarantees
curl http://localhost:8082/api/v1/cqrs/queries/guarantees

# Get guarantees by status
curl http://localhost:8082/api/v1/cqrs/queries/guarantees/status/ACTIVE

# Get expiring guarantees
curl http://localhost:8082/api/v1/cqrs/queries/guarantees/expiring

# Get dashboard summary
curl http://localhost:8082/api/v1/cqrs/queries/guarantees/dashboard/summary
```

### **4. Redis Cache Management**
```bash
# Get cache statistics
curl -u admin:admin123 http://localhost:8082/api/v1/redis/stats

# Clear all caches
curl -u admin:admin123 -X POST http://localhost:8082/api/v1/redis/clear/all

# Clear specific cache
curl -u admin:admin123 -X POST http://localhost:8082/api/v1/redis/clear/dashboard
```

## 🛠️ Management Commands

### **Start Services**
```bash
# Start all services
docker-compose -f docker-compose.full.yml up -d

# Start with logs
docker-compose -f docker-compose.full.yml up

# Start specific service
docker-compose -f docker-compose.full.yml up -d backend
```

### **Stop Services**
```bash
# Stop all services
docker-compose -f docker-compose.full.yml down

# Stop and remove volumes
docker-compose -f docker-compose.full.yml down -v

# Stop specific service
docker-compose -f docker-compose.full.yml stop backend
```

### **View Logs**
```bash
# View all logs
docker-compose -f docker-compose.full.yml logs -f

# View specific service logs
docker-compose -f docker-compose.full.yml logs -f backend

# View last 100 lines
docker-compose -f docker-compose.full.yml logs --tail=100 backend
```

### **Restart Services**
```bash
# Restart all services
docker-compose -f docker-compose.full.yml restart

# Restart specific service
docker-compose -f docker-compose.full.yml restart backend
```

### **Scale Services**
```bash
# Scale backend to 2 instances
docker-compose -f docker-compose.full.yml up -d --scale backend=2

# Scale frontend to 3 instances
docker-compose -f docker-compose.full.yml up -d --scale frontend=3
```

## 🔍 Monitoring and Debugging

### **Container Status**
```bash
# View running containers
docker-compose -f docker-compose.full.yml ps

# View container details
docker inspect guarantees-backend

# View container logs
docker logs guarantees-backend
```

### **Database Access**
```bash
# Access Command DB
docker exec -it guarantees-mysql-command mysql -u guarantees_command_user -pguarantees_command_pass

# Access Query DB
docker exec -it guarantees-mysql-query mysql -u guarantees_query_user -pguarantees_query_pass

# Access Redis
docker exec -it guarantees-redis redis-cli -a redispassword
```

### **Kafka Management**
```bash
# List topics
docker exec guarantees-kafka kafka-topics --list --bootstrap-server localhost:9092

# Create topic
docker exec guarantees-kafka kafka-topics --create --topic test-topic --bootstrap-server localhost:9092 --partitions 3 --replication-factor 1

# View topic details
docker exec guarantees-kafka kafka-topics --describe --topic guarantee-created --bootstrap-server localhost:9092
```

## 🚨 Troubleshooting

### **Common Issues**

#### **1. Port Already in Use**
```bash
# Check what's using the port
lsof -i :8082

# Kill the process
kill -9 <PID>
```

#### **2. Container Won't Start**
```bash
# Check container logs
docker logs guarantees-backend

# Check container status
docker ps -a

# Restart container
docker restart guarantees-backend
```

#### **3. Database Connection Issues**
```bash
# Check database logs
docker logs guarantees-mysql-command

# Test database connection
docker exec guarantees-mysql-command mysqladmin ping -h localhost -u root -prootpassword
```

#### **4. Kafka Issues**
```bash
# Check Kafka logs
docker logs guarantees-kafka

# Check Zookeeper
docker logs guarantees-zookeeper

# Test Kafka connection
docker exec guarantees-kafka kafka-broker-api-versions --bootstrap-server localhost:9092
```

#### **5. Redis Issues**
```bash
# Check Redis logs
docker logs guarantees-redis

# Test Redis connection
docker exec guarantees-redis redis-cli -a redispassword ping
```

### **Performance Issues**

#### **1. Memory Issues**
```bash
# Check Docker memory usage
docker stats

# Increase Docker memory limit in Docker Desktop settings
```

#### **2. Slow Startup**
```bash
# Check container startup order
docker-compose -f docker-compose.full.yml ps

# Wait for dependencies
docker-compose -f docker-compose.full.yml up -d mysql-command mysql-query
sleep 30
docker-compose -f docker-compose.full.yml up -d kafka redis
sleep 30
docker-compose -f docker-compose.full.yml up -d backend frontend
```

## 📈 Performance Optimization

### **1. Resource Allocation**
```bash
# Allocate more memory to Docker Desktop
# Windows/Mac: Docker Desktop Settings > Resources > Memory > 4GB+
# Linux: Increase Docker daemon memory limit
```

### **2. Database Optimization**
```bash
# Increase MySQL buffer pool size
# Edit docker-compose.full.yml and add:
# command: --innodb_buffer_pool_size=256M
```

### **3. Kafka Optimization**
```bash
# Increase Kafka heap size
# Edit docker-compose.full.yml and add:
# environment:
#   KAFKA_HEAP_OPTS: -Xmx512M -Xms512M
```

## 🔄 Development Workflow

### **1. Code Changes**
```bash
# Rebuild and restart backend
docker-compose -f docker-compose.full.yml up -d --build backend

# Rebuild and restart frontend
docker-compose -f docker-compose.full.yml up -d --build frontend
```

### **2. Database Changes**
```bash
# Apply database migrations
docker exec guarantees-mysql-command mysql -u guarantees_command_user -pguarantees_command_pass < migration.sql
```

### **3. Configuration Changes**
```bash
# Update environment variables
# Edit docker.env file
# Restart services
docker-compose -f docker-compose.full.yml restart
```

## 📚 Additional Resources

- [Docker Documentation](https://docs.docker.com/)
- [Docker Compose Documentation](https://docs.docker.com/compose/)
- [Kafka Documentation](https://kafka.apache.org/documentation/)
- [Redis Documentation](https://redis.io/documentation)
- [MySQL Documentation](https://dev.mysql.com/doc/)
- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [React Documentation](https://reactjs.org/docs/)

## 🤝 Contributing

When making changes to the Docker setup:

1. Test locally with `./test-docker-application.sh`
2. Update documentation if needed
3. Test on different operating systems
4. Verify all services start correctly
5. Check performance and resource usage

## 📞 Support

If you encounter issues:

1. Check the troubleshooting section above
2. View container logs for error details
3. Verify all prerequisites are met
4. Check port availability
5. Ensure sufficient system resources

---

**Happy Dockerizing! 🐳✨**
