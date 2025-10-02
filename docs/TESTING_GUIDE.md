# 🧪 Automated Testing Guide - InterExport Guarantees Module

## Overview

This guide provides comprehensive instructions for setting up and running automated tests across all components of the InterExport Guarantees Module.

## 🏗️ Testing Architecture

### Test Pyramid Structure

```
    ┌─────────────────┐
    │   E2E Tests     │  ← Cypress/Playwright (Few, Critical Paths)
    ├─────────────────┤
    │ Integration     │  ← API Tests, Database Tests (Some)
    ├─────────────────┤
    │ Unit Tests      │  ← Service Tests, Component Tests (Many)
    └─────────────────┘
```

### Test Types by Component

| Component | Unit Tests | Integration Tests | E2E Tests |
|-----------|------------|-------------------|-----------|
| Backend (Spring Boot) | ✅ JUnit 5 + Mockito | ✅ TestContainers | ❌ Not implemented |
| Frontend (React) | ❌ Missing | ❌ Not implemented | ❌ Not implemented |
| SWIFT Platform | ❌ Missing | ✅ Custom | ❌ Not implemented |

## 🚀 Quick Start

### 1. Backend Testing

#### Prerequisites
```bash
# Ensure Java 17+ is installed
java -version

# Ensure Maven is installed
mvn -version
```

#### Fix Compilation Issues First
The backend currently has compilation errors that need to be resolved:

1. **Missing Lombok annotations** - Add `@Data`, `@Getter`, `@Setter` to entity classes
2. **Missing methods** - Implement missing getter/setter methods
3. **Class naming issues** - Fix public class declarations

#### Run Backend Tests
```bash
cd guarantees-module/backend

# Clean and compile
mvn clean compile

# Run unit tests
mvn test

# Run integration tests
mvn verify

# Generate test coverage report
mvn jacoco:report
# Report available at: target/site/jacoco/index.html
```

#### Backend Test Structure
```
src/test/java/
├── com/interexport/guarantees/
│   ├── service/                    # Unit tests for services
│   │   ├── CommissionCalculationServiceTest.java ✅
│   │   ├── GuaranteeServiceTest.java ✅
│   │   └── FxRateServiceTest.java ❌ (needs creation)
│   ├── controller/                 # Unit tests for controllers
│   │   ├── GuaranteeControllerTest.java ❌ (needs creation)
│   │   └── FxRateControllerTest.java ❌ (needs creation)
│   ├── repository/                 # Repository tests
│   │   ├── GuaranteeContractRepositoryTest.java ❌ (needs creation)
│   │   └── FxRateRepositoryTest.java ❌ (needs creation)
│   └── integration/               # Integration tests
│       ├── GuaranteeIntegrationTest.java ❌ (needs creation)
│       └── DatabaseIntegrationTest.java ❌ (needs creation)
```

### 2. Frontend Testing

#### Setup React Testing
```bash
cd guarantees-module/frontend

# Install testing dependencies
npm install --save-dev @testing-library/jest-dom @testing-library/user-event

# Create test setup file
mkdir -p src/__tests__
```

#### Run Frontend Tests
```bash
# Run all tests
npm test

# Run tests with coverage
npm test -- --coverage

# Run tests in CI mode
npm test -- --watchAll=false

# Run specific test file
npm test -- --testNamePattern="GuaranteeList"
```

#### Frontend Test Structure (To Be Created)
```
src/
├── __tests__/
│   ├── components/
│   │   ├── Layout.test.tsx
│   │   ├── LanguageSwitcher.test.tsx
│   │   └── TemplateSelector.test.tsx
│   ├── pages/
│   │   ├── Dashboard.test.tsx
│   │   ├── GuaranteeList.test.tsx
│   │   ├── CreateGuarantee.test.tsx
│   │   └── EditGuarantee.test.tsx
│   ├── services/
│   │   └── api.test.ts
│   └── utils/
│       └── helpers.test.ts
├── setupTests.ts
└── test-utils.tsx
```

### 3. SWIFT Platform Testing

#### Setup Node.js Testing
```bash
cd swift-test-platform

# Install testing dependencies
npm install --save-dev jest supertest

# Update package.json with test script
```

#### Run SWIFT Platform Tests
```bash
# Run integration tests
npm test

# Run with verbose output
npm test -- --verbose

# Run specific test
npm test -- --testNamePattern="MessageSending"
```

## 🔧 Test Configuration

### Backend Test Configuration

#### application-test.yml
```yaml
spring:
  datasource:
    url: jdbc:h2:mem:testdb
    driver-class-name: org.h2.Driver
  jpa:
    hibernate:
      ddl-auto: create-drop
  h2:
    console:
      enabled: true

logging:
  level:
    com.interexport.guarantees: DEBUG
    org.springframework.test: DEBUG
```

#### TestContainers Configuration
```java
@Testcontainers
@SpringBootTest
class IntegrationTest {
    
    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");
    
    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
            .withExposedPorts(6379);
}
```

### Frontend Test Configuration

#### jest.config.js
```javascript
module.exports = {
  testEnvironment: 'jsdom',
  setupFilesAfterEnv: ['<rootDir>/src/setupTests.ts'],
  moduleNameMapping: {
    '^@/(.*)$': '<rootDir>/src/$1',
  },
  collectCoverageFrom: [
    'src/**/*.{js,jsx,ts,tsx}',
    '!src/**/*.d.ts',
    '!src/index.tsx',
    '!src/setupTests.ts',
  ],
  coverageThreshold: {
    global: {
      branches: 80,
      functions: 80,
      lines: 80,
      statements: 80,
    },
  },
};
```

#### setupTests.ts
```typescript
import '@testing-library/jest-dom';
import 'jest-canvas-mock';

// Mock window.matchMedia
Object.defineProperty(window, 'matchMedia', {
  writable: true,
  value: jest.fn().mockImplementation(query => ({
    matches: false,
    media: query,
    onchange: null,
    addListener: jest.fn(),
    removeListener: jest.fn(),
    addEventListener: jest.fn(),
    removeEventListener: jest.fn(),
    dispatchEvent: jest.fn(),
  })),
});
```

## 🚀 Automated Testing Commands

### Local Development

#### Run All Tests
```bash
# Backend tests
cd guarantees-module/backend && mvn test

# Frontend tests
cd guarantees-module/frontend && npm test -- --watchAll=false

# SWIFT platform tests
cd swift-test-platform && npm test
```

#### Run Tests with Coverage
```bash
# Backend coverage
cd guarantees-module/backend && mvn jacoco:report

# Frontend coverage
cd guarantees-module/frontend && npm test -- --coverage --watchAll=false
```

### CI/CD Pipeline

The GitHub Actions workflows automatically run:

1. **Unit Tests** - All components
2. **Integration Tests** - Backend with TestContainers
3. **Security Scanning** - Dependency and code analysis
4. **Code Coverage** - Generate and upload reports
5. **Linting** - Code quality checks

## 📊 Test Coverage Goals

| Component | Current | Target | Status |
|-----------|---------|--------|--------|
| Backend Services | ~60% | 85% | 🟡 Needs improvement |
| Backend Controllers | 0% | 80% | 🔴 Not implemented |
| Frontend Components | 0% | 75% | 🔴 Not implemented |
| SWIFT Platform | ~40% | 70% | 🟡 Needs improvement |

## 🛠️ Test Data Management

### Test Data Factory
```java
public class TestDataFactory {
    public static GuaranteeContract createTestGuarantee() {
        return GuaranteeContract.builder()
            .reference("TEST-001")
            .guaranteeType(GuaranteeType.PERFORMANCE)
            .amount(new BigDecimal("100000.00"))
            .currency("USD")
            .issueDate(LocalDate.now())
            .expiryDate(LocalDate.now().plusMonths(12))
            .build();
    }
}
```

### Database Seeding
```java
@Sql(scripts = "/test-data/guarantees.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = "/test-data/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
class GuaranteeIntegrationTest {
    // Test methods
}
```

## 🔍 Debugging Tests

### Backend Debugging
```bash
# Run specific test with debug output
mvn test -Dtest=GuaranteeServiceTest -Dmaven.surefire.debug

# Run with verbose logging
mvn test -Dlogging.level.com.interexport.guarantees=DEBUG
```

### Frontend Debugging
```bash
# Run tests in watch mode
npm test

# Run with debug output
npm test -- --verbose

# Run specific test file
npm test -- --testNamePattern="GuaranteeList"
```

## 📈 Performance Testing

### Load Testing with JMeter
```bash
# Install JMeter
brew install jmeter

# Run load test
jmeter -n -t tests/load/guarantees-load-test.jmx -l results.jtl
```

### API Performance Testing
```bash
# Using Apache Bench
ab -n 1000 -c 10 http://localhost:8080/api/v1/guarantees

# Using wrk
wrk -t12 -c400 -d30s http://localhost:8080/api/v1/guarantees
```

## 🚨 Common Issues & Solutions

### Backend Issues
1. **Compilation Errors**: Fix missing Lombok annotations
2. **Database Connection**: Ensure H2 is properly configured
3. **Test Data**: Use @Sql annotations for data setup

### Frontend Issues
1. **Module Resolution**: Check import paths and aliases
2. **Mocking**: Mock external dependencies properly
3. **Async Operations**: Use proper async/await patterns

### SWIFT Platform Issues
1. **Dependencies**: Ensure all npm packages are installed
2. **Port Conflicts**: Check for port 8081 availability
3. **API Endpoints**: Verify endpoint URLs and responses

## 📚 Best Practices

### Test Naming
- Use descriptive test names: `should_calculate_commission_when_amount_is_valid`
- Follow Given-When-Then pattern
- Include test case numbers from requirements

### Test Organization
- Group related tests in test classes
- Use `@DisplayName` for readable test descriptions
- Separate unit, integration, and e2e tests

### Test Data
- Use factories for test data creation
- Clean up test data after each test
- Use realistic test data that matches production

### Assertions
- Use specific assertions: `assertThat(result).isEqualTo(expected)`
- Test both positive and negative scenarios
- Verify side effects and interactions

## 🎯 Next Steps

1. **Fix Backend Compilation Issues** - Resolve Lombok and missing method errors
2. **Create Frontend Tests** - Set up React Testing Library tests
3. **Enhance SWIFT Platform Tests** - Add Jest configuration and more test cases
4. **Add E2E Tests** - Implement Cypress or Playwright tests
5. **Performance Testing** - Add load and stress tests
6. **Test Data Management** - Implement proper test data factories

---

**Last Updated**: December 2024
**Maintainer**: InterExport Development Team
