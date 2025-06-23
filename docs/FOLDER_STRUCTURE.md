# Folder Structure and Design Intent

## Overview
This project is designed based on **Hexagonal Architecture (Ports & Adapters)**. Each folder has clear responsibilities and controlled dependency directions.

## Overall Structure

```
src/main/java/com/zametech/todoapp/
├── TodoAppApplication.java          # Application entry point
├── common/                         # Shared components
│   ├── config/                     # Configuration classes (Security, etc.)
│   ├── exception/                  # Global exception handling
│   ├── util/                       # Common utilities (currently empty)
│   └── validation/                 # Custom validation
├── domain/                         # Domain layer (dependency center)
│   ├── model/                      # Domain models & entities
│   └── repository/                 # Repository interfaces
├── application/                    # Application layer
│   ├── dto/                        # Application layer DTOs (currently empty)
│   └── service/                    # Business logic & use cases
├── infrastructure/                 # Infrastructure layer
│   ├── persistence/                # Data access implementation
│   │   ├── entity/                 # JPA entities
│   │   └── repository/             # Repository implementations
│   └── security/                   # Security implementation
└── presentation/                   # Presentation layer
    ├── controller/                 # REST API controllers
    ├── dto/                        # API DTOs
    │   ├── request/                # Request DTOs
    │   └── response/               # Response DTOs
    └── mapper/                     # DTO ↔ Domain conversion
```

## Layer Descriptions

### 1. Domain Layer (`domain/`)
**Purpose**: Core business logic and rules
**Dependencies**: None (independent)

#### `domain/model/`
- Business entities representing core concepts
- Examples: `User`, `Todo`, `CalendarEvent`, `Note`
- Contains business logic and validation rules
- **Independence**: No dependencies on external frameworks

#### `domain/repository/`
- Defines data access contracts (interfaces)
- Specifies required operations for domain models
- **Dependency Inversion**: Implementation is in infrastructure layer

**Design Principle**: Domain layer is the center of dependencies; all other layers depend on it, but it depends on nothing.

### 2. Application Layer (`application/`)
**Purpose**: Orchestrates business operations and use cases
**Dependencies**: Domain layer only

#### `application/service/`
- Implements business use cases
- Coordinates between domain models
- Handles transactions and business workflow
- Examples: `TodoService`, `AuthenticationService`, `UserContextService`

#### `application/dto/` (currently empty)
- Application-specific data transfer objects
- Used for internal service communication
- Independent of external representation formats

**Design Principle**: Translates external requests into domain operations.

### 3. Infrastructure Layer (`infrastructure/`)
**Purpose**: External system integration and technical implementation
**Dependencies**: Domain layer (implements interfaces)

#### `infrastructure/persistence/`
**Data Access Implementation**

- `entity/`: JPA entities for database mapping
- `repository/`: Repository interface implementations
- Converts between domain models and database entities
- **Dependency Direction**: Implements domain repository interfaces

#### `infrastructure/security/`
**Security Implementation**

- JWT token processing and validation
- Authentication filters and security configuration
- User details service implementation
- **Examples**: `JwtService`, `JwtAuthenticationFilter`, `CustomUserDetailsService`

**Design Principle**: Provides concrete implementations for domain abstractions.

### 4. Presentation Layer (`presentation/`)
**Purpose**: External interface (REST API)
**Dependencies**: Application layer

#### `presentation/controller/`
- REST API endpoints
- HTTP request/response handling
- Input validation and error handling
- Examples: `TodoController`, `AuthenticationController`

#### `presentation/dto/`
- API-specific data transfer objects
- `request/`: Input data validation and structure
- `response/`: Output data format for clients
- **Separation**: Different from domain models for API versioning

#### `presentation/mapper/`
- Converts between API DTOs and domain models
- Handles format transformations
- Maintains clear separation between API and business logic

**Design Principle**: Adapts external communication to internal business operations.

### 5. Common Layer (`common/`)
**Purpose**: Shared components across all layers
**Dependencies**: Minimal, mostly technical infrastructure

#### `common/config/`
- Application configuration classes
- Spring Boot configuration beans
- **Examples**: `SecurityConfig`, database configuration

#### `common/exception/`
- Global exception handling
- Custom exception definitions
- Error response formatting
- **Example**: `GlobalExceptionHandler`

#### `common/validation/`
- Custom validation logic
- Reusable validation annotations
- Business rule validation

#### `common/util/`
- General utility functions
- Helper methods for common operations
- Framework-independent utilities

## Dependency Rules

### Hexagonal Architecture Principles

```
┌─────────────────┐
│ Presentation    │ ───┐
├─────────────────┤    │
│ Infrastructure  │ ───┼──► Domain ◄─── Application
├─────────────────┤    │
│ Common          │ ───┘
└─────────────────┘
```

### Key Rules
1. **Domain Independence**: Domain layer has no outward dependencies
2. **Dependency Inversion**: Infrastructure implements domain interfaces
3. **Single Direction**: Each layer only depends on inner layers
4. **Interface Segregation**: Clear contracts between layers

## Package Organization Benefits

### 1. Maintainability
- Clear separation of concerns
- Easy to locate and modify specific functionality
- Minimal impact when making changes

### 2. Testability
- Each layer can be tested independently
- Easy mocking of dependencies
- Clear test boundaries

### 3. Scalability
- New features follow established patterns
- Infrastructure changes don't affect business logic
- Easy to add new adapters (REST, GraphQL, etc.)

### 4. Team Development
- Different teams can work on different layers
- Clear interfaces reduce coordination overhead
- Consistent development patterns

## Implementation Guidelines

### When Adding New Features
1. **Start with Domain**: Define entities and business rules
2. **Define Contracts**: Create repository interfaces
3. **Implement Use Cases**: Add application services
4. **Add Infrastructure**: Implement data access and external integration
5. **Expose via API**: Create controllers and DTOs

### Testing Strategy
- **Domain**: Unit tests for business logic
- **Application**: Integration tests for use cases
- **Infrastructure**: Integration tests with real dependencies
- **Presentation**: API tests with MockMvc

### Common Patterns
- **Repository Pattern**: Data access abstraction
- **Service Pattern**: Business logic encapsulation
- **DTO Pattern**: Data transfer and transformation
- **Factory Pattern**: Object creation and configuration

This structure ensures the application remains maintainable, testable, and adaptable to changing requirements while following clean architecture principles.