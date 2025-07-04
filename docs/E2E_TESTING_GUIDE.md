# E2E Testing Guide for Personal Hub Backend

## Overview

This guide provides comprehensive documentation for end-to-end (E2E) testing in the Personal Hub backend application. Our E2E testing strategy focuses on API-level integration tests that verify complete user workflows and system behavior.

## Testing Architecture

### 1. Test Types

#### Integration Tests
- **Purpose**: Test complete API workflows with real database and full Spring context
- **Location**: `src/test/java/com/zametech/todoapp/integration/`
- **Naming Convention**: `*IntegrationTest.java`
- **Framework**: Spring Boot Test with MockMvc

#### Controller Integration Tests
- **Purpose**: Test individual controller endpoints with full integration
- **Location**: `src/test/java/com/zametech/todoapp/presentation/controller/`
- **Naming Convention**: `*ControllerIntegrationTest.java`
- **Framework**: Spring Boot Test with MockMvc

### 2. Technology Stack

- **Spring Boot Test**: Full application context testing
- **MockMvc**: HTTP request/response testing without starting a server
- **Testcontainers**: PostgreSQL database isolation
- **JUnit 5**: Test framework
- **AssertJ**: Fluent assertions
- **Maven Failsafe**: Integration test execution

## Test Configuration

### 1. Base Configuration

```java
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@Import(TestcontainersConfiguration.class)
class MyIntegrationTest {
    // Test implementation
}
```

### 2. Testcontainers Setup

```java
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {
    @Bean
    @ServiceConnection
    PostgreSQLContainer<?> postgresContainer() {
        return new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine"))
                .withDatabaseName("testdb")
                .withUsername("test")
                .withPassword("test");
    }
}
```

### 3. Test Profiles

- **test**: Basic test configuration
- **ci**: Continuous integration optimized configuration

## Common Test Patterns

### 1. Authentication Testing

```java
@BeforeEach
void setUp() {
    // Create test user
    testUser = new UserEntity();
    testUser.setEmail("test@example.com");
    testUser = userRepository.save(testUser);
    
    // Generate JWT token
    UserDetails userDetails = User.builder()
            .username(testUser.getEmail())
            .password(testUser.getPassword())
            .authorities(new ArrayList<>())
            .build();
    authToken = jwtService.generateToken(userDetails);
}

@Test
void authenticatedEndpoint_ShouldWork() throws Exception {
    mockMvc.perform(get("/api/v1/todos")
            .header("Authorization", "Bearer " + authToken))
            .andExpect(status().isOk());
}
```

### 2. OAuth Application Testing

```java
@BeforeEach
void setUp() {
    // Create OAuth application
    testApplication = OAuthApplication.builder()
            .clientId("test-client")
            .clientSecretHash(passwordEncoder.encode("test-secret"))
            .redirectUris(Arrays.asList("http://localhost:8080/callback"))
            .scopes(Arrays.asList("openid", "profile", "email"))
            .build();
    testApplication = oAuthApplicationRepository.save(testApplication);
}
```

### 3. Request/Response Testing

```java
@Test
void createResource_ShouldReturnCreated() throws Exception {
    CreateRequest request = new CreateRequest();
    request.setName("Test");
    
    mockMvc.perform(post("/api/v1/resource")
            .header("Authorization", "Bearer " + authToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").exists())
            .andExpect(jsonPath("$.name").value("Test"));
}
```

## Best Practices

### 1. Test Isolation
- Use `@Transactional` to rollback database changes after each test
- Create fresh test data in `@BeforeEach` methods
- Don't rely on test execution order

### 2. Test Data Management
- Use builder patterns for creating test entities
- Create minimal required data for each test
- Use descriptive variable names for test data

### 3. Assertion Guidelines
- Test both success and failure scenarios
- Verify response status codes
- Check response body structure and content
- Validate error messages and formats

### 4. Performance Considerations
- Share expensive resources (like Testcontainers) across tests
- Use `@DirtiesContext` sparingly
- Parallelize test execution where possible

## Test Coverage Areas

### 1. Authentication & Authorization
- User registration and login
- JWT token generation and validation
- Password reset flows
- OAuth2/OIDC authentication

### 2. CRUD Operations
- Create, Read, Update, Delete for all entities
- Pagination and filtering
- Validation error handling
- Concurrent modification handling

### 3. Business Logic
- Todo management with subtasks
- Calendar synchronization
- Goal tracking and achievements
- Event and note management

### 4. OAuth/OIDC Provider
- Authorization endpoint
- Token endpoint
- UserInfo endpoint
- Discovery and JWKS endpoints
- PKCE support

### 5. External Integrations
- Google Calendar sync
- Email notifications
- OAuth client flows (Google, GitHub)

## Running Tests

### 1. Unit Tests Only
```bash
./mvnw test
```

### 2. Integration Tests Only
```bash
./mvnw verify -DskipUnitTests=true
```

### 3. All Tests
```bash
./mvnw verify
```

### 4. Specific Test Class
```bash
./mvnw test -Dtest=JwksControllerIntegrationTest
```

### 5. With Coverage Report
```bash
./mvnw clean test jacoco:report
```

## Troubleshooting

### Common Issues

1. **Docker not running**
   - Error: "Could not find a valid Docker environment"
   - Solution: Start Docker daemon

2. **Port conflicts**
   - Error: "Bind for 0.0.0.0:5432 failed: port is already allocated"
   - Solution: Stop conflicting containers or change test ports

3. **Test context failures**
   - Error: "ApplicationContext failure threshold exceeded"
   - Solution: Check test configuration and dependencies

4. **Authentication failures**
   - Error: "403 Forbidden"
   - Solution: Verify security configuration permits test endpoints

## CI/CD Integration

### GitHub Actions Configuration
```yaml
- name: Run tests
  run: ./mvnw verify
  env:
    SPRING_PROFILES_ACTIVE: ci
```

### Test Reports
- Surefire reports: `target/surefire-reports/`
- Failsafe reports: `target/failsafe-reports/`
- JaCoCo coverage: `target/site/jacoco/`

## Future Enhancements

1. **Performance Testing**
   - Add JMeter or Gatling for load testing
   - Implement response time assertions
   - Monitor resource usage during tests

2. **Contract Testing**
   - Add Spring Cloud Contract for API contracts
   - Generate client stubs from contracts
   - Validate backward compatibility

3. **Security Testing**
   - Add OWASP dependency checks
   - Implement penetration test scenarios
   - Validate security headers

4. **Monitoring Integration**
   - Add test metrics to monitoring systems
   - Track test execution times
   - Alert on test failures

## Contributing

When adding new integration tests:

1. Follow existing patterns and conventions
2. Ensure tests are isolated and repeatable
3. Document any special setup requirements
4. Add appropriate test categories/tags
5. Update this documentation as needed

## References

- [Spring Boot Testing Documentation](https://docs.spring.io/spring-boot/docs/current/reference/html/spring-boot-features.html#boot-features-testing)
- [Testcontainers Documentation](https://www.testcontainers.org/)
- [MockMvc Documentation](https://docs.spring.io/spring-framework/docs/current/reference/html/testing.html#spring-mvc-test-framework)
- [Maven Failsafe Plugin](https://maven.apache.org/surefire/maven-failsafe-plugin/)