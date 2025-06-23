# Architecture Design Document

## Overview
This project is a Personal Hub integrated application built with Spring Boot, adopting Hexagonal Architecture (Ports & Adapters pattern). It provides integrated management of TODO tasks, calendar, notes, and analytics features.

## Architecture Diagram
```
┌─────────────────────────────────────────────────────────┐
│                  Presentation Layer                     │
│  ┌─────────────┐ ┌─────────────┐ ┌─────────────────┐    │
│  │ Controller  │ │ Request DTO │ │ Response DTO    │    │
│  │             │ │             │ │                 │    │
│  └─────────────┘ └─────────────┘ └─────────────────┘    │
└─────────────────────┬───────────────────────────────────┘
                      │ ↕ HTTP/JWT
┌─────────────────────┴───────────────────────────────────┐
│                 Security Layer                          │
│  ┌─────────────┐ ┌─────────────┐ ┌─────────────────┐    │
│  │ Auth Filter │ │ JWT Service │ │ User Context    │    │
│  │             │ │             │ │                 │    │
│  └─────────────┘ └─────────────┘ └─────────────────┘    │
└─────────────────────┬───────────────────────────────────┘
                      │
┌─────────────────────┴───────────────────────────────────┐
│                Application Layer                        │
│  ┌─────────────┐ ┌─────────────┐ ┌─────────────────┐    │
│  │ Service     │ │ Service DTO │ │ Mapper          │    │
│  │             │ │             │ │                 │    │
│  └─────────────┘ └─────────────┘ └─────────────────┘    │
└─────────────────────┬───────────────────────────────────┘
                      │
┌─────────────────────┴───────────────────────────────────┐
│                 Domain Layer                            │
│  ┌─────────────┐ ┌─────────────┐ ┌─────────────────┐    │
│  │ Entity      │ │ Repository  │ │ Business Logic  │    │
│  │             │ │ Interface   │ │                 │    │
│  └─────────────┘ └─────────────┘ └─────────────────┘    │
└─────────────────────┬───────────────────────────────────┘
                      │
┌─────────────────────┴───────────────────────────────────┐
│              Infrastructure Layer                       │
│  ┌─────────────┐ ┌─────────────┐ ┌─────────────────┐    │
│  │ Repository  │ │ External    │ │ Security        │    │
│  │ Impl        │ │ Service     │ │ Impl            │    │
│  └─────────────┘ └─────────────┘ └─────────────────┘    │
└─────────────────────┬───────────────────────────────────┘
                      │
              ┌───────┴───────┐
              │   Database    │
              │   PostgreSQL  │
              └───────────────┘
```

## Core Design Principles

### 1. Hexagonal Architecture (Ports & Adapters)
- **Domain-Centric**: Business logic is at the center, isolated from external concerns
- **Dependency Inversion**: External layers depend on internal layers, not vice versa
- **Port-Adapter Pattern**: Clear interfaces (ports) with pluggable implementations (adapters)

### 2. Separation of Concerns
Each layer has distinct responsibilities:
- **Presentation**: HTTP/REST interface handling
- **Application**: Use case orchestration and business workflows
- **Domain**: Core business rules and entities
- **Infrastructure**: External system integration and technical implementation

### 3. Clean Dependency Rules
```
Presentation → Application → Domain ← Infrastructure
```
- Dependencies flow inward only
- Domain layer has no outward dependencies
- Infrastructure implements domain interfaces

## Layer Details

### Domain Layer (Core)
**Purpose**: Contains business entities, rules, and interfaces

**Components**:
- **Entities**: `User`, `Todo`, `CalendarEvent`, `Note`, `Analytics`
- **Repository Interfaces**: Data access contracts
- **Business Rules**: Domain validation and business logic

**Key Principles**:
- Framework-independent
- No external dependencies
- Pure business logic
- Immutable where possible

**Example**:
```java
// Domain Entity
public class Todo {
    private UUID id;
    private String title;
    private TodoStatus status;
    
    // Business logic methods
    public void markAsCompleted() {
        if (this.status == TodoStatus.DONE) {
            throw new IllegalStateException("Todo is already completed");
        }
        this.status = TodoStatus.DONE;
    }
}

// Repository Interface (Port)
public interface TodoRepository {
    Optional<Todo> findById(UUID id);
    List<Todo> findByUserId(UUID userId);
    Todo save(Todo todo);
}
```

### Application Layer (Use Cases)
**Purpose**: Orchestrates business operations and implements use cases

**Components**:
- **Services**: `TodoService`, `AuthenticationService`, `UserContextService`
- **DTOs**: Internal data transfer objects
- **Use Case Implementations**: Business workflow coordination

**Key Responsibilities**:
- Transaction management
- Use case orchestration
- Domain object coordination
- Business rule enforcement

**Example**:
```java
@Service
@Transactional
public class TodoService {
    private final TodoRepository todoRepository;
    private final UserContextService userContext;
    
    public TodoResponse createTodo(CreateTodoRequest request) {
        UUID currentUserId = userContext.getCurrentUserId();
        
        Todo todo = new Todo(
            currentUserId,
            request.title(),
            request.description(),
            TodoStatus.TODO
        );
        
        Todo saved = todoRepository.save(todo);
        return TodoResponse.from(saved);
    }
}
```

### Infrastructure Layer (Technical Implementation)
**Purpose**: Implements domain interfaces and handles external systems

**Components**:
- **Repository Implementations**: JPA-based data access
- **Security Implementation**: JWT processing, authentication
- **External Service Integration**: OAuth providers, calendar APIs

**Key Responsibilities**:
- Database access implementation
- External API integration
- Security mechanism implementation
- Configuration management

**Example**:
```java
// Repository Implementation (Adapter)
@Repository
public class JpaTodoRepository implements TodoRepository {
    private final JpaRepository<TodoEntity, UUID> jpaRepository;
    
    @Override
    public Optional<Todo> findById(UUID id) {
        return jpaRepository.findById(id)
            .map(TodoMapper::toDomain);
    }
}
```

### Presentation Layer (External Interface)
**Purpose**: Handles HTTP requests and responses

**Components**:
- **Controllers**: REST API endpoints
- **DTOs**: Request/Response data structures
- **Mappers**: DTO ↔ Domain conversion
- **Validation**: Input validation and sanitization

**Key Responsibilities**:
- HTTP request/response handling
- Input validation
- Output formatting
- Error response generation

**Example**:
```java
@RestController
@RequestMapping("/api/v1/todos")
public class TodoController {
    private final TodoService todoService;
    
    @PostMapping
    public ResponseEntity<TodoResponse> createTodo(
            @Valid @RequestBody CreateTodoRequest request) {
        TodoResponse response = todoService.createTodo(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
```

## Security Architecture

### Authentication & Authorization Flow
```
1. Client Request → 2. CORS Filter → 3. Rate Limiting → 4. JWT Filter
                                                            ↓
8. Response ← 7. Controller ← 6. Authorization ← 5. Security Context
```

### Security Components
- **JWT Authentication**: Stateless token-based authentication
- **Method-Level Security**: `@PreAuthorize` annotations for fine-grained access control
- **Rate Limiting**: Protection against brute force attacks
- **CORS Configuration**: Multi-environment development support

### Security Features
- **User Isolation**: Users can only access their own data
- **Role-Based Access**: Default `ROLE_USER` with extensible role system
- **OAuth2 Integration**: Google and GitHub OAuth providers
- **Token Management**: JWT with refresh token capability

## Data Flow

### Typical Request Flow
```
HTTP Request → Controller → Service → Repository → Database
     ↓              ↓           ↓          ↓          ↓
 Validation    Use Case    Business    Data       Storage
              Orchestration  Logic    Access
```

### Response Flow
```
Database → Repository → Service → Controller → HTTP Response
    ↓          ↓          ↓          ↓           ↓
  Storage   Domain     Business   Response    Client
           Objects     Processing  Formatting
```

## Database Design

### Entity Relationships
```
User (1) ──────── (N) Todo
              ╱       ╲
     CalendarEvent   Note
```

### Key Features
- **Flyway Migration**: Version-controlled database schema
- **JPA/Hibernate**: Object-relational mapping
- **PostgreSQL**: Production database
- **H2**: In-memory testing database

## Testing Strategy

### Testing Pyramid
```
                 ╭─────────╮
                ╱    E2E    ╲     ← Full application tests
               ╱_____________╲
              ╱               ╲
             ╱   Integration   ╲   ← Service + Repository tests
            ╱___________________╲
           ╱                     ╲
          ╱        Unit           ╲ ← Domain logic tests
         ╱_________________________╲
```

### Test Types
- **Unit Tests**: Domain logic and business rules
- **Integration Tests**: Service layer with database
- **API Tests**: Controller endpoints with MockMvc
- **Security Tests**: Authentication and authorization
- **E2E Tests**: Complete user workflows

## Configuration Management

### Environment-Based Configuration
- **Development**: Local PostgreSQL, extended JWT expiration, debug logging
- **Testing**: H2 in-memory database, test-specific configuration
- **Production**: Optimized security settings, monitoring enabled

### Key Configuration Areas
- **Database**: Connection pooling, migration settings
- **Security**: JWT expiration, rate limiting, CORS origins
- **Logging**: Level configuration for different packages
- **External Services**: OAuth provider settings

## Scalability Considerations

### Current Implementation
- **Stateless Design**: JWT-based authentication enables horizontal scaling
- **Clean Architecture**: Easy to extract microservices if needed
- **Repository Pattern**: Database abstraction for easy switching

### Future Scaling Options
- **Caching Layer**: Redis for session management and data caching
- **Message Queues**: Asynchronous processing for heavy operations
- **Microservices**: Split by domain boundaries (Todo, Calendar, Notes)
- **API Gateway**: Centralized routing and cross-cutting concerns

## Development Guidelines

### Adding New Features
1. **Domain First**: Start with entities and business rules
2. **Define Contracts**: Create repository interfaces
3. **Implement Use Cases**: Add application services
4. **Infrastructure**: Implement data access and external integration
5. **API Layer**: Create controllers and DTOs
6. **Testing**: Add comprehensive test coverage

### Code Quality Standards
- **Google Java Style**: Consistent code formatting
- **Conventional Commits**: Structured commit messages
- **Test Coverage**: Minimum 80% overall, 90% service layer
- **Documentation**: Comprehensive API and architecture documentation

This architecture ensures the application remains maintainable, testable, and adaptable to changing business requirements while following industry best practices for enterprise-grade applications.