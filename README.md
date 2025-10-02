# InterExport - Banking Guarantees Management System

[![Java](https://img.shields.io/badge/Java-17-orange.svg)](https://openjdk.java.net/projects/jdk/17/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.0-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![React](https://img.shields.io/badge/React-18.2.0-blue.svg)](https://reactjs.org/)
[![TypeScript](https://img.shields.io/badge/TypeScript-4.9.5-blue.svg)](https://www.typescriptlang.org/)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

A comprehensive banking guarantees management system built with modern microservices architecture, featuring CQRS pattern, multi-language support, and SWIFT message processing capabilities.

## 🏗️ Architecture Overview

This project implements a complete banking guarantees management solution with the following components:

### Core Modules

1. **Guarantees Module** - Main business logic for guarantee lifecycle management
2. **Swift Test Platform** - SWIFT message processing and validation system

### Technology Stack

- **Backend**: Spring Boot 3.2.0 with Java 17
- **Frontend**: React 18 with TypeScript
- **Database**: MySQL with Redis caching
- **Message Queue**: Apache Kafka for CQRS events
- **Containerization**: Docker & Docker Compose
- **API Documentation**: OpenAPI 3 (Swagger)

## 📁 Project Structure

```
InterErport/
├── guarantees-module/              # Main guarantees management system
│   ├── backend/                    # Spring Boot microservice
│   │   ├── src/main/java/         # Java source code
│   │   │   └── com/interexport/guarantees/
│   │   │       ├── controller/    # REST API controllers
│   │   │       ├── service/       # Business logic services
│   │   │       ├── repository/    # Data access layer
│   │   │       ├── entity/        # JPA entities
│   │   │       ├── cqrs/          # CQRS command/query handlers
│   │   │       ├── config/        # Configuration classes
│   │   │       └── exception/     # Custom exceptions
│   │   ├── src/main/resources/    # Configuration files
│   │   └── src/test/java/         # Unit and integration tests
│   ├── frontend/                   # React TypeScript application
│   │   ├── src/
│   │   │   ├── components/        # Reusable UI components
│   │   │   ├── pages/            # Page components
│   │   │   ├── services/         # API service layer
│   │   │   ├── types/            # TypeScript definitions
│   │   │   └── i18n/             # Internationalization
│   │   └── public/               # Static assets
│   ├── infrastructure/            # Infrastructure as Code
│   ├── docs/                     # Documentation
│   └── tests/                    # Integration tests
├── swift-test-platform/          # SWIFT message processing platform
│   ├── services/                 # SWIFT processing services
│   ├── public/                   # Web interface
│   └── integration-test.js       # Integration tests
└── docs/                         # Project documentation
```

## 🚀 Quick Start

### Prerequisites

- **Java 17+** - [Download](https://openjdk.java.net/projects/jdk/17/)
- **Node.js 18+** - [Download](https://nodejs.org/)
- **Maven 3.8+** - [Download](https://maven.apache.org/)
- **Docker & Docker Compose** - [Download](https://www.docker.com/)
- **MySQL 8.0+** - [Download](https://dev.mysql.com/downloads/)
- **Redis 6.0+** - [Download](https://redis.io/download)

### 1. Clone the Repository

```bash
git clone https://github.com/diferalh-web/InterErport.git
cd InterErport
```

### 2. Start Infrastructure Services

```bash
# Start MySQL, Redis, and Kafka using Docker Compose
cd guarantees-module
docker-compose -f docker-compose.full.yml up -d
```

### 3. Setup Database

```bash
# Run database setup scripts
mysql -u root -p < setup-mysql.sql
mysql -u root -p < setup-cqrs-databases.sql
```

### 4. Start Backend Services

```bash
# Start the Spring Boot application
cd guarantees-module/backend
mvn clean install
mvn spring-boot:run
```

### 5. Start Frontend

```bash
# In a new terminal
cd guarantees-module/frontend
npm install
npm start
```

### 6. Access the Application

- **Frontend**: http://localhost:3000
- **Backend API**: http://localhost:8080/api/v1
- **API Documentation**: http://localhost:8080/swagger-ui.html
- **Database Console**: http://localhost:8080/h2-console (development)

## 🎯 Key Features

### ✅ Implemented Features

#### Core Guarantee Management
- **F1**: Complete guarantee lifecycle management (Create, Read, Update, Delete)
- **F3**: Advanced commission and exchange rate calculations
- **F15**: Multi-currency support with real-time FX conversion
- **F16**: Currency rate loading (manual and API integration ready)

#### Technical Features
- **CQRS Architecture**: Command Query Responsibility Segregation pattern
- **Event Sourcing**: Complete audit trail of all guarantee operations
- **Multi-language Support**: English, Spanish, and German interfaces
- **RESTful API**: Comprehensive REST API with OpenAPI documentation
- **Real-time Updates**: WebSocket support for live data updates
- **Security**: JWT-based authentication and authorization

#### SWIFT Integration
- **SWIFT Message Processing**: Complete MT799 and MT760 message handling
- **Message Validation**: Comprehensive SWIFT message validation
- **Integration Testing**: Automated SWIFT message testing platform

### 📋 Pending Features

- **F4**: Liability Accounts and Accounting Integration
- **F5**: Advanced Amendment Management
- **F6**: Claims Processing and Settlement
- **F10**: Parameter Management (Clients, Banks, Accounts)
- **F11**: Workflow Engine Integration
- **F12**: Advanced Reporting and Analytics

## 🧪 Testing

### Backend Testing

```bash
cd guarantees-module/backend

# Run unit tests
mvn test

# Run integration tests
mvn verify

# Generate test coverage report
mvn jacoco:report
# Report available at: target/site/jacoco/index.html
```

### Frontend Testing

```bash
cd guarantees-module/frontend

# Run unit tests
npm test

# Run tests with coverage
npm test -- --coverage

# Run linting
npm run lint
```

### Integration Testing

```bash
# Test SWIFT message processing
cd swift-test-platform
npm test
```

## 🔧 Configuration

### Environment Variables

Create a `.env` file in the `guarantees-module` directory:

```env
# Database Configuration
DB_HOST=localhost
DB_PORT=3306
DB_NAME=guarantees_db
DB_USERNAME=root
DB_PASSWORD=your_password

# Redis Configuration
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=

# Kafka Configuration
KAFKA_BOOTSTRAP_SERVERS=localhost:9092

# Security
JWT_SECRET=your_jwt_secret_key
JWT_EXPIRATION=86400000

# External APIs
FX_RATE_API_KEY=your_fx_api_key
SWIFT_API_ENDPOINT=https://api.swift.com
```

### Application Profiles

- **Development**: `application.yml` (H2 database, debug logging)
- **Docker**: `application-docker.yml` (containerized environment)
- **CQRS**: `application-cqrs.yml` (event sourcing configuration)

## 🐳 Docker Deployment

### Full Stack Deployment

```bash
# Build and start all services
docker-compose -f docker-compose.full.yml up -d

# View logs
docker-compose -f docker-compose.full.yml logs -f

# Stop all services
docker-compose -f docker-compose.full.yml down
```

### Individual Services

```bash
# Backend only
docker-compose -f docker-compose.backend.yml up -d

# Frontend only
docker-compose -f docker-compose.frontend.yml up -d

# CQRS services
docker-compose -f docker-compose.cqrs.yml up -d
```

## 📊 API Documentation

### Core Endpoints

#### Guarantees Management
- `POST /api/v1/guarantees` - Create new guarantee
- `GET /api/v1/guarantees` - Search guarantees with filters
- `GET /api/v1/guarantees/{id}` - Get guarantee details
- `PUT /api/v1/guarantees/{id}` - Update guarantee
- `POST /api/v1/guarantees/{id}/submit` - Submit for approval
- `POST /api/v1/guarantees/{id}/approve` - Approve guarantee
- `POST /api/v1/guarantees/{id}/reject` - Reject guarantee

#### FX Rate Management
- `GET /api/v1/fx-rates` - Get all exchange rates
- `POST /api/v1/fx-rates` - Create manual exchange rate
- `GET /api/v1/fx-rates/current` - Get current rate for currency pair

#### Client Management
- `GET /api/v1/clients` - List all clients
- `POST /api/v1/clients` - Create new client
- `PUT /api/v1/clients/{id}` - Update client information

### CQRS Endpoints

#### Commands
- `POST /api/v1/commands/guarantees` - Create guarantee command
- `PUT /api/v1/commands/guarantees/{id}` - Update guarantee command

#### Queries
- `GET /api/v1/queries/guarantees` - Query guarantees
- `GET /api/v1/queries/guarantees/{id}` - Get guarantee details

## 🔒 Security

### Authentication & Authorization
- JWT-based authentication
- Role-based access control (RBAC)
- API key authentication for external services
- CORS configuration for cross-origin requests

### Data Protection
- Input validation and sanitization
- SQL injection prevention
- XSS protection
- CSRF protection
- Data encryption at rest and in transit

## 📈 Monitoring & Observability

### Health Checks
- Spring Boot Actuator endpoints
- Database connectivity monitoring
- Redis cache health checks
- Kafka broker connectivity

### Metrics
- Prometheus metrics integration
- Custom business metrics
- Performance monitoring
- Error rate tracking

### Logging
- Structured JSON logging
- Log aggregation with ELK stack
- Audit trail for all operations
- Security event logging

## 🌍 Internationalization

The application supports multiple languages:

- **English** (en) - Default
- **Spanish** (es) - Español
- **German** (de) - Deutsch

Language switching is available in the frontend interface, and all user-facing text is externalized to translation files.

## 🚀 Deployment

### Production Deployment

1. **Prerequisites**:
   - Kubernetes cluster or Docker Swarm
   - MySQL 8.0+ database
   - Redis 6.0+ cache
   - Apache Kafka cluster

2. **Build Images**:
   ```bash
   docker build -t guarantees-backend ./guarantees-module/backend
   docker build -t guarantees-frontend ./guarantees-module/frontend
   ```

3. **Deploy**:
   ```bash
   kubectl apply -f k8s/
   # or
   docker stack deploy -c docker-compose.prod.yml guarantees
   ```

### CI/CD Pipeline

The project includes GitHub Actions workflows for:
- Automated testing
- Code quality checks
- Security scanning
- Docker image building
- Deployment to staging/production

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add some amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

### Development Guidelines

- Follow the existing code style
- Write comprehensive tests for new features
- Update documentation for API changes
- Ensure all tests pass before submitting PR

## 📞 Support

For support and questions:

- **Documentation**: Check the `/docs` directory
- **Issues**: Create an issue on GitHub
- **Email**: Contact the development team

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🙏 Acknowledgments

- Spring Boot team for the excellent framework
- React team for the powerful frontend library
- Apache Kafka for event streaming capabilities
- All contributors and testers

---

**Built with ❤️ for InterExport Banking Solutions**

*Last updated: December 2024*
