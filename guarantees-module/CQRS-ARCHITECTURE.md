# CQRS Architecture for Guarantees Module

This document describes the CQRS (Command Query Responsibility Segregation) implementation for the Guarantees Module using Kafka and dual MySQL databases.

## 🏗️ Architecture Overview

```
┌─────────────┐    Commands    ┌─────────────┐    Events    ┌─────────────┐
│   Client    │ ──────────────►│ Command Side│ ────────────►│    Kafka    │
│             │                │             │              │             │
│             │ ◄──────────────│             │              │             │
└─────────────┘    Queries     └─────────────┘              └─────────────┘
                                      │                              │
                                      ▼                              ▼
                               ┌─────────────┐              ┌─────────────┐
                               │ Command DB  │              │ Query Side  │
                               │ (MySQL-1)   │              │             │
                               │             │              └─────────────┘
                               └─────────────┘                      │
                                                                    ▼
                                                             ┌─────────────┐
                                                             │  Query DB   │
                                                             │ (MySQL-2)   │
                                                             │             │
                                                             └─────────────┘
```

## 🎯 Key Components

### **Command Side (Write)**
- **Database**: `guarantees_command_db` (MySQL on port 3306)
- **Purpose**: Handles all write operations (create, update, delete)
- **Characteristics**: Normalized, ACID-compliant, optimized for consistency
- **Components**:
  - `GuaranteeCommand` - Command objects
  - `GuaranteeCommandHandler` - Business logic and validation
  - `GuaranteeCommandController` - REST endpoints for commands

### **Query Side (Read)**
- **Database**: `guarantees_query_db` (MySQL on port 3307)
- **Purpose**: Handles all read operations (queries, reports, analytics)
- **Characteristics**: Denormalized, optimized for performance
- **Components**:
  - `GuaranteeSummaryView` - Denormalized view models
  - `GuaranteeQueryHandler` - Event processing and query logic
  - `GuaranteeQueryController` - REST endpoints for queries

### **Event Streaming**
- **Kafka**: Event bus for eventual consistency
- **Topics**:
  - `guarantee-created` - When a guarantee is created
  - `guarantee-updated` - When a guarantee is updated
  - `guarantee-approved` - When a guarantee is approved
  - `claim-submitted` - When a claim is submitted
  - `amendment-created` - When an amendment is created

## 🚀 Getting Started

### **1. Start CQRS Infrastructure**

```bash
cd /Users/dherrera/idea/InterErport/guarantees-module
./start-cqrs.sh
```

This will start:
- MySQL Command DB (port 3306)
- MySQL Query DB (port 3307)
- Kafka (port 9092)
- Kafka UI (port 8080)
- Redis (port 6379)

### **2. Configure Application**

Update your `application.yml` to use the CQRS profile:

```yaml
spring:
  profiles:
    active: cqrs
```

### **3. Start Application**

```bash
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=cqrs
```

## 📊 Database Schemas

### **Command Database (guarantees_command_db)**
- Contains normalized entities from your existing schema
- Optimized for write operations and data integrity
- Uses existing JPA entities

### **Query Database (guarantees_query_db)**
- Contains denormalized views optimized for queries
- `guarantee_summary_view` - Main view for guarantee summaries
- Additional indexes for common query patterns
- Pre-calculated fields for better performance

## 🔄 Event Flow

### **1. Command Processing**
```
Client → Command Controller → Command Handler → Command DB → Kafka Event
```

### **2. Query Processing**
```
Kafka Event → Query Handler → Query DB → Query Controller → Client
```

### **3. Eventual Consistency**
- Events are processed asynchronously
- Query side is eventually consistent with command side
- Typically 1-5 seconds delay for consistency

## 🛠️ API Endpoints

### **Command Endpoints (Write)**
- `POST /api/v1/cqrs/commands/guarantees` - Create guarantee
- `PUT /api/v1/cqrs/commands/guarantees/{id}` - Update guarantee
- `POST /api/v1/cqrs/commands/guarantees/{id}/approve` - Approve guarantee

### **Query Endpoints (Read)**
- `GET /api/v1/cqrs/queries/guarantees` - Get all guarantees
- `GET /api/v1/cqrs/queries/guarantees/status/{status}` - Get by status
- `GET /api/v1/cqrs/queries/guarantees/expiring` - Get expiring guarantees
- `GET /api/v1/cqrs/queries/guarantees/currency/{currency}` - Get by currency
- `GET /api/v1/cqrs/queries/guarantees/dashboard/summary` - Dashboard summary

## 📈 Performance Benefits

### **Command Side**
- Optimized for write operations
- ACID compliance for data integrity
- Complex business logic validation
- Audit trail and compliance

### **Query Side**
- Denormalized data for fast queries
- Pre-calculated fields (risk level, days to expiry)
- Optimized indexes for common patterns
- Aggregated views for dashboards

### **Scalability**
- Independent scaling of read/write sides
- Kafka partitions for parallel processing
- Different hardware for different workloads
- Caching strategies per side

## 🔧 Configuration

### **Environment Variables**
```bash
# Command Database
COMMAND_DB_USERNAME=guarantees_command_user
COMMAND_DB_PASSWORD=guarantees_command_pass

# Query Database
QUERY_DB_USERNAME=guarantees_query_user
QUERY_DB_PASSWORD=guarantees_query_pass

# Kafka
KAFKA_BOOTSTRAP_SERVERS=localhost:9092

# Redis
REDIS_HOST=localhost
REDIS_PORT=6379
```

### **Application Profiles**
- `cqrs` - Full CQRS with Kafka and dual databases
- `default` - Traditional single database approach

## 🧪 Testing

### **1. Test Command Side**
```bash
curl -X POST http://localhost:8080/api/v1/cqrs/commands/guarantees \
  -H "Content-Type: application/json" \
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

### **2. Test Query Side**
```bash
curl http://localhost:8080/api/v1/cqrs/queries/guarantees
```

### **3. Monitor Events**
- Kafka UI: http://localhost:8080
- Check topics and message flow
- Monitor consumer lag

## 🚨 Monitoring and Troubleshooting

### **Health Checks**
- Command DB: `mysql://localhost:3306/guarantees_command_db`
- Query DB: `mysql://localhost:3307/guarantees_query_db`
- Kafka: `localhost:9092`
- Redis: `redis://localhost:6379`

### **Common Issues**
1. **Event Processing Delays**: Check Kafka consumer lag
2. **Data Inconsistency**: Verify event processing in Kafka UI
3. **Performance Issues**: Check database indexes and query patterns
4. **Connection Issues**: Verify all services are running

### **Logs**
```bash
# Application logs
tail -f backend.log

# Kafka logs
docker-compose -f docker-compose.cqrs.yml logs -f kafka

# Database logs
docker-compose -f docker-compose.cqrs.yml logs -f mysql-command
docker-compose -f docker-compose.cqrs.yml logs -f mysql-query
```

## 🔄 Migration Strategy

### **Phase 1: Parallel Running**
- Run both traditional and CQRS approaches
- Gradually migrate read operations to query side
- Monitor performance and consistency

### **Phase 2: Event Processing**
- Implement event handlers for existing data
- Backfill query database with historical data
- Test event processing thoroughly

### **Phase 3: Full Migration**
- Switch all read operations to query side
- Remove traditional read endpoints
- Optimize based on usage patterns

## 📚 Additional Resources

- [CQRS Pattern Documentation](https://docs.microsoft.com/en-us/azure/architecture/patterns/cqrs)
- [Kafka Documentation](https://kafka.apache.org/documentation/)
- [Spring Kafka Documentation](https://docs.spring.io/spring-kafka/docs/current/reference/html/)
- [MySQL Documentation](https://dev.mysql.com/doc/)

## 🤝 Contributing

When adding new features:
1. Add command handlers for write operations
2. Add query handlers for read operations
3. Define events for data changes
4. Update both database schemas
5. Add appropriate tests
6. Update documentation
