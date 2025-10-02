# 🧪 Automated Testing Setup - InterExport Guarantees Module

## Quick Start

### Run All Tests
```bash
# Make the script executable (if not already done)
chmod +x run-tests.sh

# Run all automated tests
./run-tests.sh
```

### Run Individual Test Suites
```bash
# Backend tests (Spring Boot)
cd guarantees-module/backend
mvn test

# Frontend tests (React)
cd guarantees-module/frontend
npm test -- --watchAll=false

# SWIFT platform tests (Node.js)
cd swift-test-platform
npm test
```

## 🏗️ Testing Architecture

### Test Types by Component

| Component | Unit Tests | Integration Tests | E2E Tests | Status |
|-----------|------------|-------------------|-----------|--------|
| **Backend (Spring Boot)** | ✅ JUnit 5 + Mockito | ✅ TestContainers | ❌ Not implemented | 🟡 Partial |
| **Frontend (React)** | ✅ React Testing Library | ❌ Not implemented | ❌ Not implemented | 🟡 Partial |
| **SWIFT Platform** | ✅ Jest | ✅ Custom Integration | ❌ Not implemented | 🟡 Partial |

## 📊 Current Test Coverage

### Backend Tests
- **CommissionCalculationServiceTest**: ✅ Complete (270 lines)
- **GuaranteeServiceTest**: ✅ Complete (289 lines)
- **Missing Tests**: Controllers, Repositories, Integration tests

### Frontend Tests
- **Layout.test.tsx**: ✅ Basic component test
- **Dashboard.test.tsx**: ✅ Page component test
- **api.test.ts**: ✅ Service layer test
- **Missing Tests**: All other components and pages

### SWIFT Platform Tests
- **SwiftMessageProcessor.test.js**: ✅ Complete message processing tests
- **SwiftValidationService.test.js**: ✅ Complete validation tests
- **integration-test.js**: ✅ End-to-end integration tests

## 🚀 How to Run Automated Testing

### 1. **Complete Test Suite**
```bash
# Run all tests with detailed output
./run-tests.sh

# This will:
# - Run backend unit tests
# - Run frontend component tests
# - Run SWIFT platform tests
# - Run security scans
# - Generate coverage reports
# - Create test report
```

### 2. **Backend Testing**
```bash
cd guarantees-module/backend

# Fix compilation issues first (if any)
mvn clean compile

# Run unit tests
mvn test

# Run with coverage
mvn jacoco:report

# View coverage report
open target/site/jacoco/index.html
```

### 3. **Frontend Testing**
```bash
cd guarantees-module/frontend

# Install dependencies
npm install

# Run tests
npm test -- --watchAll=false

# Run with coverage
npm test -- --coverage --watchAll=false

# View coverage report
open coverage/lcov-report/index.html
```

### 4. **SWIFT Platform Testing**
```bash
cd swift-test-platform

# Install dependencies
npm install

# Run unit tests
npm test

# Run integration tests
npm run test:integration

# Run with coverage
npm run test:coverage
```

## 🔧 Test Configuration

### Backend Test Configuration

#### Maven Configuration (pom.xml)
```xml
<plugin>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
    <version>0.8.8</version>
    <executions>
        <execution>
            <goals>
                <goal>prepare-agent</goal>
            </goals>
        </execution>
        <execution>
            <id>report</id>
            <phase>test</phase>
            <goals>
                <goal>report</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

#### Test Application Properties
```yaml
# src/test/resources/application-test.yml
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
```

### Frontend Test Configuration

#### Jest Configuration
```javascript
// jest.config.js
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

#### Test Setup
```typescript
// src/setupTests.ts
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

### SWIFT Platform Test Configuration

#### Jest Configuration
```javascript
// jest.config.js
module.exports = {
  testEnvironment: 'node',
  testMatch: [
    '**/__tests__/**/*.test.js',
    '**/?(*.)+(spec|test).js'
  ],
  collectCoverageFrom: [
    'services/**/*.js',
    'server.js',
    '!**/node_modules/**',
    '!**/coverage/**'
  ],
  coverageDirectory: 'coverage',
  coverageReporters: ['text', 'lcov', 'html'],
  setupFilesAfterEnv: ['<rootDir>/jest.setup.js'],
  testTimeout: 10000,
  verbose: true
};
```

## 📈 Test Coverage Goals

| Component | Current | Target | Status |
|-----------|---------|--------|--------|
| Backend Services | ~60% | 85% | 🟡 Needs improvement |
| Backend Controllers | 0% | 80% | 🔴 Not implemented |
| Frontend Components | ~20% | 75% | 🔴 Needs implementation |
| SWIFT Platform | ~70% | 85% | 🟡 Good progress |

## 🛠️ Test Data Management

### Test Data Factory (Backend)
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

### Test Data Factory (Frontend)
```typescript
// src/test-utils.tsx
export const mockGuarantee = {
  id: 1,
  reference: 'GT-20240101-000001',
  guaranteeType: 'PERFORMANCE',
  amount: 100000.00,
  currency: 'USD',
  issueDate: '2024-01-01',
  expiryDate: '2024-12-31',
  status: 'DRAFT',
  beneficiaryName: 'Test Beneficiary',
  applicantName: 'Test Applicant',
};
```

## 🚨 Common Issues & Solutions

### Backend Issues
1. **Compilation Errors**
   - **Problem**: Missing Lombok annotations
   - **Solution**: Add `@Data`, `@Getter`, `@Setter` to entity classes
   
2. **Missing Methods**
   - **Problem**: Entity classes missing getter/setter methods
   - **Solution**: Implement missing methods or use Lombok

3. **Test Database Issues**
   - **Problem**: H2 database connection issues
   - **Solution**: Check `application-test.yml` configuration

### Frontend Issues
1. **Module Resolution**
   - **Problem**: Import path errors
   - **Solution**: Check `jest.config.js` moduleNameMapping

2. **Mocking Issues**
   - **Problem**: External dependencies not mocked
   - **Solution**: Add proper mocks in `setupTests.ts`

3. **Async Operations**
   - **Problem**: Tests not waiting for async operations
   - **Solution**: Use `waitFor` from React Testing Library

### SWIFT Platform Issues
1. **Dependencies**
   - **Problem**: Missing npm packages
   - **Solution**: Run `npm install`

2. **Port Conflicts**
   - **Problem**: Port 8081 already in use
   - **Solution**: Change port in configuration

## 📚 Best Practices

### Test Naming
- Use descriptive names: `should_calculate_commission_when_amount_is_valid`
- Follow Given-When-Then pattern
- Include test case numbers from requirements

### Test Organization
- Group related tests in test classes
- Use `@DisplayName` for readable descriptions
- Separate unit, integration, and e2e tests

### Test Data
- Use factories for test data creation
- Clean up test data after each test
- Use realistic test data

### Assertions
- Use specific assertions: `assertThat(result).isEqualTo(expected)`
- Test both positive and negative scenarios
- Verify side effects and interactions

## 🎯 Next Steps

### Immediate Actions
1. **Fix Backend Compilation Issues**
   - Add missing Lombok annotations
   - Implement missing getter/setter methods
   - Fix class naming issues

2. **Enhance Frontend Tests**
   - Add tests for all components
   - Add integration tests
   - Improve test coverage

3. **Complete SWIFT Platform Tests**
   - Add more comprehensive test cases
   - Add performance tests
   - Add error handling tests

### Long-term Goals
1. **Add E2E Tests**
   - Implement Cypress or Playwright
   - Test complete user workflows
   - Add visual regression testing

2. **Performance Testing**
   - Add JMeter load tests
   - Add API performance tests
   - Add frontend performance tests

3. **Security Testing**
   - Add OWASP ZAP integration
   - Add dependency vulnerability scanning
   - Add penetration testing

## 📊 Monitoring & Reporting

### Coverage Reports
- **Backend**: `target/site/jacoco/index.html`
- **Frontend**: `coverage/lcov-report/index.html`
- **SWIFT**: `coverage/lcov-report/index.html`

### Test Reports
- **Generated**: `test-report-YYYYMMDD-HHMMSS.md`
- **Logs**: `logs/` directory
- **CI/CD**: GitHub Actions workflows

---

**Last Updated**: December 2024
**Maintainer**: InterExport Development Team
