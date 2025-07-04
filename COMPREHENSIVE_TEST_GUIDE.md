# Comprehensive Test Guide for Personal Hub Backend

## 🎯 Purpose
This guide serves as the definitive reference for testing the Personal Hub backend application. When agents need to understand what tests to run, how to run them, and what to test, this document provides all necessary information.

## 📋 Table of Contents
1. [Quick Test Commands](#quick-test-commands)
2. [Test Categories](#test-categories)
3. [What to Test for Each Feature](#what-to-test-for-each-feature)
4. [Test Execution Guide](#test-execution-guide)
5. [Test Writing Guidelines](#test-writing-guidelines)
6. [Troubleshooting](#troubleshooting)

## 🚀 Quick Test Commands

### Essential Commands
```bash
# Run all tests
./mvnw clean verify

# Run only unit tests
./mvnw test

# Run only integration tests
./mvnw verify -DskipUnitTests=true

# Run specific test class
./mvnw test -Dtest=TodoServiceTest

# Run with coverage report
./mvnw clean test jacoco:report

# Run tests in specific package
./mvnw test -Dtest="com.zametech.todoapp.application.service.*"
```

## 📂 Test Categories

### 1. Unit Tests
**Location**: `src/test/java/com/zametech/todoapp/*/`  
**Pattern**: `*Test.java` (excluding `*IntegrationTest.java`)  
**Purpose**: Test individual components in isolation

### 2. Integration Tests
**Location**: `src/test/java/com/zametech/todoapp/integration/`  
**Pattern**: `*IntegrationTest.java`  
**Purpose**: Test complete workflows with real database

### 3. Controller Integration Tests
**Location**: `src/test/java/com/zametech/todoapp/presentation/controller/`  
**Pattern**: `*ControllerIntegrationTest.java`  
**Purpose**: Test REST API endpoints with full Spring context

## 🧪 What to Test for Each Feature

### 1. Authentication & Authorization (`/api/v1/auth/*`)

#### Test Files:
- `AuthenticationControllerTest.java` (unit)
- `AuthenticationIntegrationTest.java` (integration)
- `JwtServiceTest.java` (unit)

#### Test Cases:
```
✅ User Registration
   - Valid registration with all required fields
   - Duplicate email rejection
   - Duplicate username rejection
   - Password validation (min 8 chars, complexity)
   - Email format validation

✅ User Login
   - Valid credentials return JWT token
   - Invalid email returns 401
   - Invalid password returns 401
   - Disabled account returns 403
   - Unverified email warning

✅ Token Refresh
   - Valid refresh token returns new access token
   - Expired refresh token returns 401
   - Invalid refresh token returns 401

✅ Password Reset
   - Request reset with valid email
   - Request reset with unknown email
   - Reset with valid token
   - Reset with expired token
   - Reset with invalid token

✅ Current User Info
   - Authenticated user can get their info
   - Unauthenticated request returns 401
```

### 2. Todo Management (`/api/v1/todos/*`)

#### Test Files:
- `TodoControllerTest.java` (unit)
- `TodoIntegrationTest.java` (integration)
- `TodoServiceTest.java` (unit)

#### Test Cases:
```
✅ Create Todo
   - Valid todo creation
   - Title required validation
   - User association
   - Parent todo validation
   - Due date validation

✅ List Todos
   - User sees only their todos
   - Pagination works correctly
   - Filtering by status
   - Filtering by date range
   - Sorting options

✅ Update Todo
   - Owner can update
   - Non-owner cannot update
   - Partial updates supported
   - Status transitions
   - Subtask updates

✅ Delete Todo
   - Owner can delete
   - Non-owner cannot delete
   - Cascade delete subtasks
   - Soft delete implementation

✅ Repeat Todos
   - Daily repeat creation
   - Weekly repeat creation
   - Monthly repeat creation
   - Custom repeat patterns
   - Repeat completion handling
```

### 3. Calendar Sync (`/api/v1/calendar/*`)

#### Test Files:
- `CalendarSyncControllerTest.java` (unit)
- `CalendarSyncIntegrationTest.java` (integration)
- `GoogleCalendarServiceTest.java` (unit)

#### Test Cases:
```
✅ OAuth Flow
   - Generate authorization URL
   - Handle callback with code
   - Token storage and encryption
   - Token refresh

✅ Calendar Operations
   - List user calendars
   - Select calendar for sync
   - Enable/disable sync
   - Manual sync trigger

✅ Event Sync
   - Todo to Google Calendar
   - Google Calendar to Todo
   - Update synchronization
   - Delete synchronization
   - Conflict resolution
```

### 4. OIDC Provider (`/.well-known/*`, `/auth/*`)

#### Test Files:
- `JwksControllerIntegrationTest.java`
- `OidcDiscoveryControllerIntegrationTest.java`
- `OidcAuthorizationControllerIntegrationTest.java`
- `OidcTokenControllerIntegrationTest.java`
- `OidcUserInfoControllerIntegrationTest.java`

#### Test Cases:
```
✅ Discovery Endpoint
   - Returns valid OpenID configuration
   - All required fields present
   - Endpoints URLs correct

✅ JWKS Endpoint
   - Returns valid JWK set
   - Only public keys exposed
   - Key rotation support

✅ Authorization Endpoint
   - Valid authorization request
   - PKCE support
   - State parameter validation
   - Redirect URI validation
   - Scope validation

✅ Token Endpoint
   - Authorization code exchange
   - Client authentication
   - Refresh token grant
   - Token revocation

✅ UserInfo Endpoint
   - Bearer token authentication
   - Scope-based claims
   - User data protection
```

### 5. Goals Management (`/api/v1/goals/*`)

#### Test Files:
- `GoalControllerTest.java` (unit)
- `GoalControllerIntegrationTest.java` (integration)
- `GoalServiceTest.java` (unit)

#### Test Cases:
```
✅ Goal CRUD
   - Create different goal types
   - Update goal progress
   - Delete goals
   - List user goals

✅ Goal Achievement
   - Toggle achievement status
   - Achievement date tracking
   - Progress calculation
   - Streak tracking

✅ Goal Types
   - SIMPLE goals
   - QUANTITATIVE goals
   - STREAK goals
   - Validation per type
```

### 6. Notes & Events (`/api/v1/notes/*`, `/api/v1/events/*`)

#### Test Files:
- `NoteIntegrationTest.java`
- `EventIntegrationTest.java`

#### Test Cases:
```
✅ Note Management
   - Create/Read/Update/Delete
   - Tag management
   - Search functionality
   - User isolation

✅ Event Management
   - Create with date/time
   - Recurring events
   - Event reminders
   - Calendar integration
```

## 📝 Test Execution Guide

### 1. Before Running Tests

```bash
# Ensure Docker is running (for Testcontainers)
docker ps

# Clean previous test artifacts
./mvnw clean

# Ensure test database is not running on conflict ports
docker ps | grep 5432
```

### 2. Running Specific Test Suites

#### Authentication Tests Only
```bash
./mvnw test -Dtest="*Authentication*,*Jwt*,*Security*"
```

#### Todo Feature Tests Only
```bash
./mvnw test -Dtest="*Todo*"
```

#### OIDC/OAuth Tests Only
```bash
./mvnw test -Dtest="*Oidc*,*OAuth*,*Jwks*"
```

#### Integration Tests Only
```bash
./mvnw verify -Dit.test="*IntegrationTest"
```

### 3. Test Reports

```bash
# View test results
cat target/surefire-reports/*.txt

# View coverage report
open target/site/jacoco/index.html

# Failed test details
find target/surefire-reports -name "*.txt" -exec grep -l "FAILURE" {} \;
```

## 🏗️ Test Writing Guidelines

### 1. Unit Test Template

```java
@ExtendWith(MockitoExtension.class)
class ServiceTest {
    
    @Mock
    private Repository repository;
    
    @InjectMocks
    private Service service;
    
    @Test
    void methodName_StateUnderTest_ExpectedBehavior() {
        // Given
        when(repository.findById(1L)).thenReturn(Optional.of(entity));
        
        // When
        Result result = service.method(1L);
        
        // Then
        assertThat(result).isNotNull();
        verify(repository).findById(1L);
    }
}
```

### 2. Integration Test Template

```java
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@Import(TestcontainersConfiguration.class)
class ControllerIntegrationTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    private String authToken;
    
    @BeforeEach
    void setUp() {
        // Setup test data and authentication
    }
    
    @Test
    void endpoint_WithValidData_ShouldReturnExpected() throws Exception {
        mockMvc.perform(post("/api/v1/resource")
                .header("Authorization", "Bearer " + authToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists());
    }
}
```

### 3. Test Data Builders

```java
// Use builders for test data
User testUser = User.builder()
    .email("test@example.com")
    .username("testuser")
    .password("encoded_password")
    .enabled(true)
    .build();

// Create minimal required data
Todo testTodo = Todo.builder()
    .title("Test Todo")
    .userId(testUser.getId())
    .build();
```

## 🔧 Troubleshooting

### Common Issues and Solutions

#### 1. Docker Connection Error
```
Error: Could not find a valid Docker environment
```
**Solution**: 
```bash
# Start Docker
sudo systemctl start docker
# or
sudo dockerd
```

#### 2. Port Already in Use
```
Error: Bind for 0.0.0.0:5432 failed: port is already allocated
```
**Solution**:
```bash
# Find and stop conflicting container
docker ps | grep 5432
docker stop <container_id>
```

#### 3. Test Context Failure
```
Error: Failed to load ApplicationContext
```
**Solution**:
- Check `@ActiveProfiles("test")`
- Verify test configuration files exist
- Check for missing beans or dependencies

#### 4. Authentication Failures in Tests
```
Error: 403 Forbidden
```
**Solution**:
- Ensure proper JWT token generation
- Check security configuration for test endpoints
- Verify user setup in @BeforeEach

#### 5. Transient Entity Errors
```
Error: TransientPropertyValueException
```
**Solution**:
- Save all entities before referencing
- Use cascade options appropriately
- Ensure proper transaction boundaries

### Test Debugging Commands

```bash
# Run with debug output
./mvnw test -Dtest=TodoServiceTest -X

# Run with specific Spring profiles
./mvnw test -Dspring.profiles.active=test,debug

# Skip specific tests
./mvnw test -Dtest=!SlowTest

# Run with system properties
./mvnw test -DargLine="-Xmx1024m -XX:MaxPermSize=256m"
```

## 📊 Test Coverage Requirements

### Minimum Coverage Targets
- **Overall**: 80%
- **Service Layer**: 90%
- **Controller Layer**: 85%
- **Repository Layer**: 70%
- **Utility Classes**: 95%

### Check Coverage
```bash
# Generate coverage report
./mvnw clean test jacoco:report

# Check coverage meets requirements
./mvnw clean verify jacoco:check
```

## 🔄 Continuous Integration

### GitHub Actions Test Execution
```yaml
- name: Run Unit Tests
  run: ./mvnw test
  
- name: Run Integration Tests
  run: ./mvnw verify -DskipUnitTests=true
  
- name: Generate Coverage Report
  run: ./mvnw jacoco:report
  
- name: Upload Coverage
  uses: codecov/codecov-action@v3
```

## 📚 Additional Resources

### Internal Documentation
- [E2E Testing Guide](./docs/E2E_TESTING_GUIDE.md)
- [API Documentation](./docs/API.md)
- [Security Testing](./docs/SECURITY_TESTING.md)

### External Resources
- [Spring Boot Testing](https://docs.spring.io/spring-boot/docs/current/reference/html/spring-boot-features.html#boot-features-testing)
- [Testcontainers](https://www.testcontainers.org/)
- [MockMvc](https://spring.io/guides/gs/testing-web/)
- [JUnit 5](https://junit.org/junit5/docs/current/user-guide/)

## 🎓 Test Philosophy

1. **Test Behavior, Not Implementation**: Focus on what the code does, not how
2. **Isolated Tests**: Each test should run independently
3. **Clear Names**: Test names should describe the scenario and expected outcome
4. **Fast Feedback**: Unit tests should run quickly, integration tests can be slower
5. **Maintainable**: Tests should be easy to understand and modify

---

**Remember**: Good tests are the foundation of reliable software. When in doubt, write a test!