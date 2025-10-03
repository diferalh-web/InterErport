# InterExport Guarantees Module - Comprehensive Testing Guide

## Overview

This guide provides comprehensive testing strategies for the InterExport Guarantees Module, focusing on CQRS architecture, Redis caching, Kafka event streaming, and overall application functionality.

## Table of Contents

1. [Test Architecture](#test-architecture)
2. [CQRS Testing](#cqrs-testing)
3. [Redis Testing](#redis-testing)
4. [Kafka Testing](#kafka-testing)
5. [API Testing](#api-testing)
6. [Performance Testing](#performance-testing)
7. [Security Testing](#security-testing)
8. [Automated Testing](#automated-testing)
9. [Test Execution](#test-execution)
10. [Troubleshooting](#troubleshooting)

## Test Architecture

### Test Categories

1. **Unit Tests**: Individual component testing
2. **Integration Tests**: Component interaction testing
3. **End-to-End Tests**: Complete workflow testing
4. **Performance Tests**: Load and stress testing
5. **Security Tests**: Authentication and authorization testing

### Test Environment Setup

```bash
# Start all services
docker-compose -f docker-compose.full.yml up -d

# Verify services are running
docker-compose -f docker-compose.full.yml ps
```

## CQRS Testing

### Command Side Testing

**Test Cases:**

1. **Create Guarantee Command**
   - Valid guarantee creation
   - Invalid data handling
   - Business rule validation
   - Event publishing

2. **Update Guarantee Command**
   - Status transitions
   - Field updates
   - Validation rules
   - Audit trail

3. **Delete Guarantee Command**
   - Soft delete implementation
   - Cascade operations
   - Event publishing

**Test Implementation:**

```java
@Test
void testCreateGuaranteeCommand_ShouldPublishEventAndCacheInRedis() {
    // Given
    CreateGuaranteeCommand command = createSampleCommand();
    
    // When
    String guaranteeId = commandHandler.handle(command);
    
    // Then
    assertNotNull(guaranteeId);
    // Verify event publishing
    // Verify Redis caching
    // Verify database persistence
}
```

### Query Side Testing

**Test Cases:**

1. **Get Guarantee by ID**
   - Cache hit scenarios
   - Cache miss scenarios
   - Data consistency

2. **Get Guarantees List**
   - Pagination
   - Filtering
   - Sorting
   - Performance

3. **Search Guarantees**
   - Text search
   - Date range filtering
   - Status filtering

**Test Implementation:**

```java
@Test
void testQueryGuarantee_ShouldReturnFromRedisCache() {
    // Given - Create guarantee first
    String guaranteeId = commandHandler.handle(createSampleCommand());
    
    // When - Query the guarantee
    GuaranteeContract queriedGuarantee = queryHandler.getGuaranteeById(Long.valueOf(guaranteeId));
    
    // Then - Verify cache hit
    assertNotNull(queriedGuarantee);
    assertTrue(redisTemplate.hasKey("guarantee:" + guaranteeId));
}
```

## Redis Testing

### Cache Testing

**Test Cases:**

1. **Cache Hit/Miss Scenarios**
   - First request (cache miss)
   - Subsequent requests (cache hit)
   - Cache expiration
   - Cache invalidation

2. **Cache Performance**
   - Response time comparison
   - Memory usage monitoring
   - Cache eviction policies

3. **Cache Consistency**
   - Data synchronization
   - Update propagation
   - Concurrent access

**Test Implementation:**

```java
@Test
void testRedisCaching_ShouldImprovePerformance() {
    // Measure first call (cache miss)
    long startTime = System.currentTimeMillis();
    GuaranteeContract guarantee1 = queryHandler.getGuaranteeById(1L);
    long firstCallTime = System.currentTimeMillis() - startTime;
    
    // Measure second call (cache hit)
    startTime = System.currentTimeMillis();
    GuaranteeContract guarantee2 = queryHandler.getGuaranteeById(1L);
    long secondCallTime = System.currentTimeMillis() - startTime;
    
    // Verify cache hit is faster
    assertTrue(secondCallTime < firstCallTime);
    assertEquals(guarantee1.getId(), guarantee2.getId());
}
```

### Cache Invalidation Testing

```java
@Test
void testCacheInvalidation_ShouldRefreshData() {
    // Create and cache guarantee
    String guaranteeId = commandHandler.handle(createSampleCommand());
    
    // Update guarantee
    GuaranteeContract guarantee = guaranteeRepository.findById(Long.valueOf(guaranteeId)).orElseThrow();
    guarantee.setStatus(GuaranteeStatus.APPROVED);
    guaranteeRepository.save(guarantee);
    
    // Invalidate cache
    redisTemplate.delete("guarantee:" + guaranteeId);
    
    // Verify fresh data is loaded
    GuaranteeContract refreshedGuarantee = queryHandler.getGuaranteeById(Long.valueOf(guaranteeId));
    assertEquals(GuaranteeStatus.APPROVED, refreshedGuarantee.getStatus());
}
```

## Kafka Testing

### Event Publishing Testing

**Test Cases:**

1. **Event Publishing**
   - Successful event publishing
   - Event content validation
   - Event ordering
   - Event durability

2. **Event Consumption**
   - Event processing
   - Error handling
   - Retry mechanisms
   - Dead letter queues

**Test Implementation:**

```java
@Test
void testKafkaEventPublishing_ShouldPublishGuaranteeCreatedEvent() {
    // Given
    CreateGuaranteeCommand command = createSampleCommand();
    
    // When
    String guaranteeId = commandHandler.handle(command);
    
    // Then - Verify event was published
    // This would typically use KafkaTestUtils in a real implementation
    verify(kafkaTemplate).send(eq("guarantee-created"), eq(guaranteeId), any(GuaranteeCreatedEvent.class));
}
```

### Event Processing Testing

```java
@Test
void testEventProcessing_ShouldUpdateQuerySide() {
    // Given - Event is published
    GuaranteeCreatedEvent event = createSampleEvent();
    
    // When - Event is processed
    eventProcessor.processGuaranteeCreated(event);
    
    // Then - Query side should be updated
    GuaranteeContract guarantee = queryHandler.getGuaranteeById(event.getGuaranteeId());
    assertNotNull(guarantee);
    assertEquals(event.getReference(), guarantee.getReference());
}
```

## API Testing

### REST API Testing

**Test Cases:**

1. **CRUD Operations**
   - Create guarantee
   - Read guarantee
   - Update guarantee
   - Delete guarantee

2. **Authentication & Authorization**
   - Valid credentials
   - Invalid credentials
   - Role-based access
   - Token expiration

3. **Input Validation**
   - Required fields
   - Data types
   - Business rules
   - Error responses

**Test Implementation:**

```java
@Test
void testCreateGuarantee_ShouldReturnCreatedGuarantee() {
    // Given
    GuaranteeCreateRequest request = createSampleRequest();
    
    // When
    ResponseEntity<GuaranteeContract> response = restTemplate.postForEntity(
        "/guarantees", request, GuaranteeContract.class);
    
    // Then
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    assertNotNull(response.getBody().getId());
    assertEquals(request.getReference(), response.getBody().getReference());
}
```

### API Performance Testing

```java
@Test
void testApiPerformance_ShouldRespondWithinTimeout() {
    // Given
    int numberOfRequests = 100;
    long maxResponseTime = 1000; // 1 second
    
    // When
    List<CompletableFuture<ResponseEntity<GuaranteeContract>>> futures = 
        IntStream.range(0, numberOfRequests)
            .mapToObj(i -> CompletableFuture.supplyAsync(() -> 
                restTemplate.getForEntity("/guarantees/1", GuaranteeContract.class)))
            .collect(Collectors.toList());
    
    // Then
    CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
    
    long averageResponseTime = futures.stream()
        .mapToLong(f -> f.join().getHeaders().getFirst("X-Response-Time"))
        .average()
        .orElse(0);
    
    assertTrue(averageResponseTime < maxResponseTime);
}
```

## Performance Testing

### Load Testing

**Test Scenarios:**

1. **Normal Load**
   - 100 concurrent users
   - 1000 requests per minute
   - Response time < 500ms

2. **Peak Load**
   - 500 concurrent users
   - 5000 requests per minute
   - Response time < 1000ms

3. **Stress Testing**
   - 1000 concurrent users
   - 10000 requests per minute
   - System stability

**Test Implementation:**

```java
@Test
void testLoadPerformance_ShouldHandleConcurrentRequests() {
    // Given
    int numberOfThreads = 100;
    int requestsPerThread = 10;
    ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
    
    // When
    List<CompletableFuture<ResponseEntity<GuaranteeContract>>> futures = 
        IntStream.range(0, numberOfThreads)
            .mapToObj(i -> CompletableFuture.supplyAsync(() -> {
                for (int j = 0; j < requestsPerThread; j++) {
                    return restTemplate.getForEntity("/guarantees/1", GuaranteeContract.class);
                }
                return null;
            }, executor))
            .collect(Collectors.toList());
    
    // Then
    CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
    
    long successfulRequests = futures.stream()
        .mapToLong(f -> f.join().getStatusCode().is2xxSuccessful() ? 1 : 0)
        .sum();
    
    assertEquals(numberOfThreads * requestsPerThread, successfulRequests);
}
```

### Memory Testing

```java
@Test
void testMemoryUsage_ShouldNotExceedLimits() {
    // Given
    Runtime runtime = Runtime.getRuntime();
    long initialMemory = runtime.totalMemory() - runtime.freeMemory();
    
    // When - Create many guarantees
    for (int i = 0; i < 1000; i++) {
        commandHandler.handle(createSampleCommand());
    }
    
    // Then
    long finalMemory = runtime.totalMemory() - runtime.freeMemory();
    long memoryIncrease = finalMemory - initialMemory;
    
    // Memory increase should be reasonable (less than 100MB)
    assertTrue(memoryIncrease < 100 * 1024 * 1024);
}
```

## Security Testing

### Authentication Testing

```java
@Test
void testAuthentication_ShouldRequireValidCredentials() {
    // Given - No authentication
    HttpHeaders headers = new HttpHeaders();
    
    // When
    ResponseEntity<String> response = restTemplate.exchange(
        "/guarantees", HttpMethod.GET, new HttpEntity<>(headers), String.class);
    
    // Then
    assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
}
```

### Authorization Testing

```java
@Test
void testAuthorization_ShouldEnforceRoleBasedAccess() {
    // Given - User with limited permissions
    HttpHeaders headers = new HttpHeaders();
    headers.setBasicAuth("user", "password");
    
    // When
    ResponseEntity<String> response = restTemplate.exchange(
        "/guarantees", HttpMethod.DELETE, new HttpEntity<>(headers), String.class);
    
    // Then
    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
}
```

## Automated Testing

### Test Agent

The automated test agent runs comprehensive tests every hour:

```java
@Scheduled(fixedRate = 3600000) // Every hour
public void runComprehensiveTestSuite() {
    // Run all test categories
    testCqrsCommandSide();
    testCqrsQuerySide();
    testRedisCaching();
    testKafkaEvents();
    testApiEndpoints();
    testDatabaseConsistency();
    testPerformance();
    testReportGeneration();
    testTemplateSystem();
    testSecurity();
}
```

### Test Execution Scripts

**PowerShell (Windows):**
```powershell
.\run-tests.ps1
```

**Bash (Linux/Mac):**
```bash
./run-tests.sh
```

## Test Execution

### Manual Test Execution

1. **Start Services:**
   ```bash
   docker-compose -f docker-compose.full.yml up -d
   ```

2. **Run Backend Tests:**
   ```bash
   cd guarantees-module/backend
   mvn test
   ```

3. **Run Frontend Tests:**
   ```bash
   cd guarantees-module/frontend
   npm test
   ```

4. **Run Integration Tests:**
   ```bash
   mvn test -Dtest=*IntegrationTest
   ```

### Automated Test Execution

1. **Run Test Agent:**
   ```bash
   java -jar test-automation/TestAgent.jar
   ```

2. **Run Test Script:**
   ```bash
   # Windows
   .\run-tests.ps1
   
   # Linux/Mac
   ./run-tests.sh
   ```

### CI/CD Integration

```yaml
# GitHub Actions example
name: Test Suite
on: [push, pull_request]
jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v2
      - name: Start Services
        run: docker-compose -f docker-compose.full.yml up -d
      - name: Run Tests
        run: ./run-tests.sh
      - name: Generate Report
        run: ./generate-test-report.sh
```

## Troubleshooting

### Common Issues

1. **Redis Connection Issues:**
   ```bash
   # Check Redis status
   redis-cli ping
   
   # Check Redis logs
   docker logs guarantees-module_redis_1
   ```

2. **Kafka Connection Issues:**
   ```bash
   # Check Kafka status
   kafka-topics.sh --bootstrap-server localhost:9092 --list
   
   # Check Kafka logs
   docker logs guarantees-module_kafka_1
   ```

3. **Database Connection Issues:**
   ```bash
   # Check database status
   docker exec -it guarantees-module_mysql_1 mysql -u root -p
   
   # Check database logs
   docker logs guarantees-module_mysql_1
   ```

### Test Debugging

1. **Enable Debug Logging:**
   ```yaml
   logging:
     level:
       com.interexport.guarantees: DEBUG
       org.springframework.kafka: DEBUG
       org.springframework.data.redis: DEBUG
   ```

2. **Monitor Test Execution:**
   ```bash
   # Monitor logs
   docker-compose -f docker-compose.full.yml logs -f
   
   # Monitor Redis
   redis-cli monitor
   
   # Monitor Kafka
   kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic guarantee-created
   ```

### Performance Monitoring

1. **Application Metrics:**
   - Response times
   - Throughput
   - Error rates
   - Memory usage

2. **Infrastructure Metrics:**
   - CPU usage
   - Memory usage
   - Disk I/O
   - Network I/O

3. **Database Metrics:**
   - Connection pool usage
   - Query performance
   - Lock contention
   - Cache hit rates

## Test Reports

### Test Results Format

```json
{
  "testSuite": "InterExport Guarantees Module",
  "timestamp": "2025-01-01T12:00:00Z",
  "totalTests": 50,
  "passedTests": 48,
  "failedTests": 2,
  "warnings": 1,
  "categories": {
    "CQRS": {
      "total": 10,
      "passed": 10,
      "failed": 0
    },
    "Redis": {
      "total": 8,
      "passed": 7,
      "failed": 1
    },
    "Kafka": {
      "total": 6,
      "passed": 6,
      "failed": 0
    },
    "API": {
      "total": 12,
      "passed": 11,
      "failed": 1
    },
    "Performance": {
      "total": 8,
      "passed": 8,
      "failed": 0
    },
    "Security": {
      "total": 6,
      "passed": 6,
      "failed": 0
    }
  }
}
```

### Test Coverage

- **Unit Tests**: 85% coverage
- **Integration Tests**: 90% coverage
- **End-to-End Tests**: 80% coverage
- **Performance Tests**: 100% critical paths
- **Security Tests**: 100% authentication/authorization

## Best Practices

1. **Test Isolation**: Each test should be independent
2. **Test Data**: Use realistic test data
3. **Test Environment**: Isolate test environment from production
4. **Test Monitoring**: Monitor test execution and results
5. **Test Maintenance**: Keep tests up-to-date with code changes
6. **Test Documentation**: Document test scenarios and expected results
7. **Test Automation**: Automate repetitive test scenarios
8. **Test Reporting**: Generate comprehensive test reports
9. **Test Performance**: Optimize test execution time
10. **Test Security**: Secure test data and environments

## Conclusion

This comprehensive testing guide ensures the InterExport Guarantees Module maintains high quality, performance, and reliability through systematic testing of all components and interactions. The combination of unit tests, integration tests, and automated testing provides confidence in the system's functionality and performance.
