# Personal Hub Backend

[![CI Pipeline](https://github.com/sasazame/personal-hub-backend/actions/workflows/ci.yml/badge.svg)](https://github.com/sasazame/personal-hub-backend/actions/workflows/ci.yml)
[![codecov](https://codecov.io/gh/sasazame/personal-hub-backend/branch/main/graph/badge.svg)](https://codecov.io/gh/sasazame/personal-hub-backend)

A comprehensive personal management system backend API built with Spring Boot + PostgreSQL

## 🚀 Project Overview

### Technology Stack
- **Java**: 21 (OpenJDK)
- **Framework**: Spring Boot 3.3.7
- **Database**: PostgreSQL 16+
- **Build Tool**: Maven 3.8+
- **Architecture**: Hexagonal Architecture (Ports & Adapters)

### Key Features
- ✅ **Authentication & Authorization**: JWT-based authentication system with OAuth2 support
- ✅ **User Management**: User registration, login, and profile management
- ✅ **TODO Management**: Full CRUD operations, parent-child task relationships, status & priority management
- ✅ **Calendar Features**: Event management with reminder settings
- ✅ **Note Management**: Markdown-supported notes with tag management
- ✅ **Analytics**: Productivity dashboard and activity statistics
- ✅ **Access Control**: Users can only access their own data
- ✅ **Pagination**: Efficient handling of large datasets
- ✅ **Security**: Endpoint-based access control and CORS configuration
- ✅ **RESTful API**: Standard HTTP methods and status codes
- ✅ **Validation**: Comprehensive input data validation
- ✅ **Global Exception Handling**: Unified error response format
- ✅ **Multi-Environment Support**: CORS configured for various development setups

## 📋 Table of Contents
1. [Quick Start](#quick-start)
2. [Project Structure](#project-structure)
3. [Environment Setup](#environment-setup)
4. [API Specification](#api-specification)
5. [Development Guide](#development-guide)
6. [Testing](#testing)
7. [Documentation](#documentation)
8. [Recent Security Fixes](#recent-security-fixes)

## ⚡ Quick Start

### Prerequisites
- Java 21+
- Maven 3.8+
- PostgreSQL 16+

### Setup Instructions
```bash
# 1. Clone the repository
git clone https://github.com/sasazame/personal-hub-backend.git
cd personal-hub-backend

# 2. Database setup
sudo -u postgres psql -c "CREATE DATABASE personalhub;"
sudo -u postgres psql -c "CREATE USER personalhub WITH ENCRYPTED PASSWORD 'personalhub';"
sudo -u postgres psql -c "GRANT ALL PRIVILEGES ON DATABASE personalhub TO personalhub;"

# 3. Environment configuration (copy .env.example and edit)
cp .env.example .env
# Edit .env file to configure Google/GitHub OAuth credentials

# 4. Run the application
# Linux/Mac
./run.sh

# Windows
run.bat

# Or directly with Maven
mvn spring-boot:run

# 5. Verify installation (user registration)
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "SecurePass123!",
    "username": "testuser"
  }'

# 6. Login to get JWT token
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "SecurePass123!"
  }'
```

The application will start at http://localhost:8080

## 🏗️ Project Structure

This project follows **Hexagonal Architecture** principles for clean separation of concerns.

```
src/main/java/com/zametech/todoapp/
├── common/           # Shared components
├── domain/           # Domain layer (business rules)
├── application/      # Application layer (use cases)
├── infrastructure/   # Infrastructure layer (external system integration)
└── presentation/     # Presentation layer (API)
```

### 📚 Detailed Documentation
- **[📁 Folder Structure](docs/FOLDER_STRUCTURE.md)** - Purpose and usage of each folder
- **[🏛️ Architecture](docs/ARCHITECTURE.md)** - Design philosophy and layer structure
- **[📖 API Specification](docs/API.md)** - Detailed REST API documentation

### Key Characteristics
- **Dependency Control**: Clear separation of responsibilities across layers
- **Extensibility**: Minimal impact when adding new features
- **Testability**: Each layer can be tested independently
- **Maintainability**: Separation of business logic and infrastructure

## 🛠️ Environment Setup

### Database Setup
```bash
# Install PostgreSQL (Ubuntu/Debian)
sudo apt update
sudo apt install postgresql postgresql-contrib

# Create database and user
sudo -u postgres psql << EOF
CREATE DATABASE personalhub;
CREATE USER personalhub WITH ENCRYPTED PASSWORD 'personalhub';
GRANT ALL PRIVILEGES ON DATABASE personalhub TO personalhub;
ALTER DATABASE personalhub OWNER TO personalhub;
\q
EOF
```

### Configuration Files
Main configuration is in `src/main/resources/application.yml`:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/personalhub
    username: personalhub
    password: personalhub
  jpa:
    hibernate:
      ddl-auto: validate
  flyway:
    enabled: true
```

## 📡 API Specification

### Base URL
```
http://localhost:8080/api/v1
```

### Main Endpoints

#### Authentication Endpoints (No Authentication Required)
| Method | Endpoint | Description |
|---------|----------|-------------|
| POST | `/auth/register` | User registration |
| POST | `/auth/login` | User login |
| POST | `/auth/refresh` | Refresh JWT token |

#### TODO Endpoints (Authentication Required)
| Method | Endpoint | Description |
|---------|----------|-------------|
| POST | `/todos` | Create TODO |
| GET | `/todos` | Get TODO list (paginated) |
| GET | `/todos/{id}` | Get TODO by ID |
| GET | `/todos/status/{status}` | Get TODOs by status |
| PUT | `/todos/{id}` | Update TODO |
| DELETE | `/todos/{id}` | Delete TODO |
| GET | `/todos/{id}/children` | Get child tasks |
| POST | `/todos/{id}/toggle-status` | Toggle TODO status |

#### Calendar Endpoints (Authentication Required)
| Method | Endpoint | Description |
|---------|----------|-------------|
| POST | `/calendar/events` | Create event |
| GET | `/calendar/events` | Get events list |
| GET | `/calendar/events/{id}` | Get event |
| PUT | `/calendar/events/{id}` | Update event |
| DELETE | `/calendar/events/{id}` | Delete event |

#### Note Endpoints (Authentication Required)
| Method | Endpoint | Description |
|---------|----------|-------------|
| POST | `/notes` | Create note |
| GET | `/notes` | Get notes list |
| GET | `/notes/{id}` | Get note |
| PUT | `/notes/{id}` | Update note |
| DELETE | `/notes/{id}` | Delete note |
| GET | `/notes/search` | Search notes |

#### Analytics Endpoints (Authentication Required)
| Method | Endpoint | Description |
|---------|----------|-------------|
| GET | `/analytics/dashboard` | Productivity dashboard |
| GET | `/analytics/todos/activity` | TODO activity statistics |

**Note**: Authentication-required endpoints need a JWT token in the Authorization header as `Bearer {token}`.

### Request Examples
```bash
# 1. User registration
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@example.com",
    "password": "SecurePass123!",
    "username": "john_doe"
  }'

# 2. Login (get accessToken from response)
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@example.com",
    "password": "SecurePass123!"
  }'

# 3. Create TODO (authentication required)
curl -X POST http://localhost:8080/api/v1/todos \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d '{
    "title": "Complete project",
    "description": "Final review and submission",
    "priority": "HIGH",
    "dueDate": "2024-12-31"
  }'

# 4. Get TODO list (authentication required)
curl "http://localhost:8080/api/v1/todos?page=0&size=10&sort=createdAt,desc" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

For detailed API specifications, see [docs/API.md](docs/API.md).

## 👨‍💻 Development Guide

### Branch Strategy
```bash
# New feature development
git checkout -b feat/feature-name
# Implementation, testing, commit
git commit -m "feat: description of new feature"
# Create pull request
git push origin feat/feature-name
gh pr create --assignee sasazame
```

### Coding Standards
- **Java**: Google Java Style Guide
- **Commit Messages**: Conventional Commits
- **Testing**: JUnit 5 + Mockito

### Project Structure
```
src/main/java/com/zametech/todoapp/
├── common/              # Shared components
│   ├── config/         # Configuration classes (SecurityConfig, etc.)
│   ├── exception/      # Exception handling
│   ├── util/           # Utilities
│   └── validation/     # Validation logic
├── domain/              # Domain layer
│   ├── model/          # Domain models (Todo, User, etc.)
│   └── repository/     # Repository interfaces
├── application/         # Application layer
│   ├── dto/            # Application layer DTOs
│   └── service/        # Business logic
├── infrastructure/      # Infrastructure layer
│   ├── persistence/    # Data access (JPA implementation)
│   └── security/       # Security-related (JWT processing, etc.)
└── presentation/        # Presentation layer
    ├── controller/     # REST controllers
    ├── dto/            # API request/response DTOs
    └── mapper/         # DTO ↔ Domain model conversion
```

## 🧪 Testing

### Running Tests
```bash
# Run all tests
mvn test

# Run integration tests
mvn verify

# Generate coverage report
mvn test jacoco:report
```

### Test Configuration
- **Unit Tests**: Service layer business logic (JUnit 5 + Mockito)
- **Integration Tests**: E2E tests including authentication and authorization (SpringBootTest)
- **API Tests**: Controller layer endpoints (MockMvc)
- **Security Tests**: JWT authentication and access control tests

### Test Environment
- **Database**: H2 In-Memory (test-specific)
- **Configuration**: Dedicated settings in `application-test.yml`
- **Migration**: Test-specific Flyway scripts

## 📚 Documentation

### Detailed Documentation
- **[🏛️ Architecture Design](docs/ARCHITECTURE.md)** - System design philosophy and layer structure
- **[📁 Folder Structure](docs/FOLDER_STRUCTURE.md)** - Project structure and folder purposes
- **[📖 API Specification](docs/API.md)** - Detailed REST API documentation
- **[🗄️ Database Design](docs/DATABASE.md)** - DB schema and design principles
- **[🔐 Security Fixes (June 2025)](docs/SECURITY_FIXES_2025_06.md)** - Recent security improvements

### OAuth Integration Guides
- **[🔑 Google OAuth Setup](docs/GOOGLE_OAUTH_SETUP.md)** - Google OAuth configuration
- **[🐙 GitHub OAuth Setup](docs/GITHUB_OAUTH_SETUP.md)** - GitHub OAuth configuration
- **[🔄 Frontend OAuth Implementation](docs/OAUTH_FRONTEND_REDIRECT_IMPLEMENTATION.md)** - Frontend integration guide

### Architecture Overview
```
┌─────────────────┐
│ Presentation    │ ← REST API, DTO
├─────────────────┤
│ Application     │ ← Business logic
├─────────────────┤
│ Domain          │ ← Domain models
├─────────────────┤
│ Infrastructure  │ ← Data access, external integration
└─────────────────┘
```

## 🚨 Recent Security Fixes

### June 2025 Critical Fixes
We recently resolved critical 403 authorization errors that were blocking frontend E2E testing:

1. **Session Management Conflicts**: Removed conflicting session limits that prevented consecutive JWT requests
2. **CORS Configuration Issues**: Fixed hardcoded CORS annotations that blocked multi-environment development
3. **Missing User Authorities**: Implemented proper `ROLE_USER` assignment for method-level security
4. **OAuth2 Endpoint Issues**: Fixed userinfo endpoint paths for frontend compatibility

**Result**: All consecutive CRUD operations now work correctly, and the application supports multiple development environments.

For detailed information, see [Security Fixes Documentation](docs/SECURITY_FIXES_2025_06.md).

## 🔧 Tools & Libraries

### Main Dependencies
- **Spring Boot Starter Web** - REST API
- **Spring Boot Starter Data JPA** - Data access
- **Spring Boot Starter Security** - Security and authentication
- **Spring Boot Starter Validation** - Validation
- **PostgreSQL Driver** - Database connection
- **Flyway** - Database migration
- **JJWT** - JWT token processing
- **Lombok** - Boilerplate code reduction
- **H2 Database** - In-memory DB for testing

### Development Tools
- **Spring Boot DevTools** - Hot reload
- **Spring Boot Actuator** - Monitoring and metrics

## 🚧 Future Development Plans

### Near-term Plans
- [ ] Complete implementation of event management features
- [ ] Complete implementation of note features
- [ ] Complete implementation of analytics features
- [ ] Integrated search functionality
- [ ] File attachment features

### Medium to Long-term Plans
- [ ] Notification and reminder features
- [ ] Caching functionality (Redis)
- [ ] Batch operation APIs
- [ ] Automatic OpenAPI documentation generation
- [ ] Performance monitoring and metrics
- [ ] Collaboration features
- [ ] AI-powered suggestion features

## 📝 License

This project is published under the MIT License.

## 🤝 Contributing

1. Fork this repository
2. Create a feature branch (`git checkout -b feat/amazing-feature`)
3. Commit your changes (`git commit -m 'feat: add amazing feature'`)
4. Push to the branch (`git push origin feat/amazing-feature`)
5. Create a Pull Request

## 📞 Support

If you have questions or issues, please create an [Issue](https://github.com/sasazame/personal-hub-backend/issues).

---

**Developer**: sasazame  
**Last Updated**: June 2025