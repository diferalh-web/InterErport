# Guarantees Module POC - InterExport

A comprehensive proof of concept for a banking guarantees management system built with Spring Boot and React.

## 📋 Overview

This POC implements core functionality for managing banking guarantees based on the technical specifications and requirements documents. It includes features for guarantee lifecycle management, commission calculations, FX rate handling, amendments, claims processing, and multi-currency support.

## 🏗️ Architecture

### Backend (Spring Boot 3.2.0)
- **Framework**: Spring Boot with Spring Data JPA
- **Database**: H2 (development) / PostgreSQL (production)
- **Security**: Spring Security with basic authentication
- **API Documentation**: OpenAPI 3 (Swagger)
- **Testing**: JUnit 5, Mockito, TestContainers

### Frontend (React 18)
- **Framework**: React with TypeScript
- **UI Library**: Ant Design
- **State Management**: React Query for server state
- **Routing**: React Router v6
- **Build Tool**: Create React App

## 🚀 Features Implemented

### ✅ Core Features (Completed)
- **F1**: Guarantees CRUD with full lifecycle management
- **F3**: Commission and Exchange Rate Calculation
- **F16**: Currency Rate Loading (manual and API-ready)
- **F15**: Multi-currency Capability
- **REST API**: Complete CRUD operations with proper error handling
- **Frontend Interface**: Modern React UI for testing functionality
- **Unit Tests**: Comprehensive test coverage for services

### 📝 Additional Features (Pending)
- **F4**: Liability Accounts and Accounting
- **F5**: Amendments (immediate and with consent)
- **F6**: Claims (request, payment, rejection)
- **F10**: Parameters (clients, banks, accounts, commissions)
- Integration tests and end-to-end testing

## 📁 Project Structure

```
guarantees-module/
├── backend/                          # Spring Boot application
│   ├── src/main/java/com/interexport/guarantees/
│   │   ├── controller/              # REST controllers
│   │   ├── service/                 # Business logic services
│   │   ├── repository/              # Data access layer
│   │   ├── entity/                  # JPA entities
│   │   ├── exception/               # Custom exceptions
│   │   └── GuaranteesModuleApplication.java
│   ├── src/main/resources/
│   │   └── application.yml          # Application configuration
│   ├── src/test/java/               # Unit and integration tests
│   └── pom.xml                      # Maven dependencies
├── frontend/                        # React application
│   ├── src/
│   │   ├── components/              # Reusable UI components
│   │   ├── pages/                   # Page components
│   │   ├── services/                # API service layer
│   │   ├── types/                   # TypeScript type definitions
│   │   └── App.tsx                  # Main application component
│   ├── public/
│   └── package.json                 # npm dependencies
├── docs/                            # Documentation
└── README.md                        # This file
```

## 🛠️ Setup & Installation

### Prerequisites
- Java 17+
- Node.js 18+
- Maven 3.8+
- npm/yarn

### Backend Setup

1. Navigate to the backend directory:
   ```bash
   cd backend
   ```

2. Install dependencies and run:
   ```bash
   mvn clean install
   mvn spring-boot:run
   ```

3. Access the API:
   - REST API: http://localhost:8080/api/v1
   - Swagger UI: http://localhost:8080/swagger-ui.html
   - H2 Console: http://localhost:8080/h2-console

### Frontend Setup

1. Navigate to the frontend directory:
   ```bash
   cd frontend
   ```

2. Install dependencies:
   ```bash
   npm install
   ```

3. Start the development server:
   ```bash
   npm start
   ```

4. Access the application:
   - Frontend: http://localhost:3000

## 🧪 Testing

### Unit Tests (Backend)
```bash
cd backend
mvn test
```

### Test Coverage Report
```bash
cd backend
mvn jacoco:report
# Report available at: target/site/jacoco/index.html
```

### Frontend Tests
```bash
cd frontend
npm test
```

## 🔑 API Endpoints

### Guarantees
- `POST /api/v1/guarantees` - Create guarantee
- `GET /api/v1/guarantees` - Search guarantees (with filters)
- `GET /api/v1/guarantees/{id}` - Get guarantee by ID
- `PUT /api/v1/guarantees/{id}` - Update guarantee
- `POST /api/v1/guarantees/{id}/submit` - Submit for approval
- `POST /api/v1/guarantees/{id}/approve` - Approve guarantee
- `POST /api/v1/guarantees/{id}/reject` - Reject guarantee
- `POST /api/v1/guarantees/{id}/cancel` - Cancel guarantee

### FX Rates
- `GET /api/v1/fx-rates` - Get all FX rates
- `POST /api/v1/fx-rates` - Create manual FX rate
- `GET /api/v1/fx-rates/current` - Get current rate for currency pair

### Health Check
- `GET /api/v1/guarantees/health` - Service health status

## 📊 Database Schema

### Key Entities

1. **GuaranteeContract** - Core guarantee entity
2. **Amendment** - Guarantee amendments
3. **Claim** - Claims against guarantees
4. **FeeItem** - Commission and fee calculations
5. **Client** - Client/applicant information
6. **FxRate** - Foreign exchange rates

## 🧩 Business Rules Implemented

### Guarantee Validation
- Amount must be greater than zero
- Expiry date must be after issue date
- Status transitions follow business rules
- Reference uniqueness enforced

### Commission Calculation
- Percentage-based calculation with minimum thresholds
- Multi-currency support with FX conversion
- Installment distribution (default 4 quarterly payments)
- Banking rounding (HALF_UP)

### FX Rate Management
- Manual rate entry with effective dates
- Provider-based rate sources (ECB, Bloomberg ready)
- Cache management with configurable TTL
- Cross-rate calculation through base currency

## 🔒 Security

- Basic authentication for POC (extensible to OAuth2/OIDC)
- CORS configuration for development
- Input validation and sanitization
- SQL injection prevention via JPA

## ⚡ Performance

- Database indexing on key lookup fields
- Connection pooling
- Lazy loading for related entities
- Caching for frequently accessed data (FX rates)

## 🌍 Internationalization

- Multi-language support framework in place
- Currency formatting with locale support
- Date/time formatting with timezone handling

## 📈 Monitoring & Observability

- Spring Boot Actuator endpoints
- Health checks and metrics
- Structured logging
- OpenAPI documentation

## 🔄 Deployment

### Development
- H2 in-memory database
- Embedded server
- Hot reload enabled

### Production Ready
- PostgreSQL configuration
- Connection pooling
- Environment-specific profiles
- Docker containerization ready

## 🤝 Testing the POC

1. Start both backend and frontend services
2. Access the web interface at http://localhost:3000
3. Navigate through the features:
   - Dashboard: Overview of guarantees portfolio
   - Create Guarantee: Full form with validation
   - Guarantee List: Search, filter, and manage guarantees
   - FX Rates: View and manage exchange rates
   - Clients: Manage client information

### Sample Test Cases

1. **Create Guarantee** (T1.1)
   - Create a performance guarantee for $100,000
   - Verify 201 response with Location header
   - Check commission calculation

2. **Business Validation** (T1.2)
   - Try creating guarantee with zero amount
   - Verify proper validation error

3. **Commission Calculation** (T3.1-T3.6)
   - Test percentage-based calculation
   - Verify minimum commission application
   - Test installment distribution
   - FX conversion for foreign currencies

## 🐛 Known Limitations

- Authentication is basic (suitable for POC)
- Some advanced features are placeholder implementations
- SWIFT message integration is simulated
- Real-time FX rate APIs not fully integrated

## 🚀 Next Steps for Production

1. Implement OAuth2/OIDC authentication
2. Add comprehensive audit logging
3. Implement SWIFT message processing
4. Add real-time notifications
5. Complete workflow engine integration
6. Add comprehensive integration tests
7. Implement CI/CD pipeline
8. Add performance testing
9. Security penetration testing
10. Load balancing and clustering

## 📞 Support

For questions or issues regarding this POC, please refer to the technical documentation or contact the development team.

---

**Built with ❤️ for InterExport Banking Solutions**





