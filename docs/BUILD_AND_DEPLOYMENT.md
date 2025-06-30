# Build and Deployment Guide

## Build Configuration

### Local Build

```bash
# Clean build
mvn clean package

# Skip tests for faster build
mvn clean package -DskipTests

# Build with specific profile
mvn clean package -Pproduction
```

### Build Requirements
- **Java**: 21 (OpenJDK/Temurin)
- **Maven**: 3.8+
- **Memory**: Minimum 2GB RAM for build

### Build Output
- **JAR Location**: `target/personal-hub-0.0.1-SNAPSHOT.jar`
- **Type**: Executable Spring Boot JAR
- **Size**: ~70-80MB (includes dependencies)

## CI/CD Pipeline

### GitHub Actions Workflow
The project uses GitHub Actions for CI/CD with the following stages:

1. **Test Stage**
   - Runs all unit and integration tests
   - Generates test reports
   - Creates coverage reports
   - ⚠️ Currently using `-Dmaven.test.failure.ignore=true`

2. **Build Stage**
   - Creates executable JAR
   - Archives artifacts

3. **Security Scan**
   - OWASP dependency check
   - Vulnerability scanning

4. **Code Quality**
   - SpotBugs analysis
   - PMD checks

### Pipeline Status
- **Badge**: [![CI Pipeline](https://github.com/sasazame/personal-hub-backend/actions/workflows/ci.yml/badge.svg)](https://github.com/sasazame/personal-hub-backend/actions/workflows/ci.yml)
- **Coverage**: [![codecov](https://codecov.io/gh/sasazame/personal-hub-backend/branch/main/graph/badge.svg)](https://codecov.io/gh/sasazame/personal-hub-backend)

## Deployment Options

### 1. Direct JAR Deployment

```bash
# Run with default settings
java -jar target/personal-hub-0.0.1-SNAPSHOT.jar

# Run with custom port
java -jar target/personal-hub-0.0.1-SNAPSHOT.jar --server.port=8081

# Run with environment variables
export DB_HOST=production-db.example.com
export JWT_SECRET_KEY=your-production-secret
java -jar target/personal-hub-0.0.1-SNAPSHOT.jar
```

### 2. Docker Deployment

Create `Dockerfile`:
```dockerfile
FROM eclipse-temurin:21-jre-alpine
VOLUME /tmp
COPY target/*.jar app.jar
ENTRYPOINT ["java","-jar","/app.jar"]
```

Build and run:
```bash
docker build -t personal-hub-backend .
docker run -p 8080:8080 --env-file .env personal-hub-backend
```

### 3. Docker Compose

Create `docker-compose.yml`:
```yaml
version: '3.8'
services:
  app:
    build: .
    ports:
      - "8080:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=production
      - DB_HOST=db
      - DB_PORT=5432
      - DB_NAME=personalhub
      - DB_USERNAME=personalhub
      - DB_PASSWORD=${DB_PASSWORD}
      - JWT_SECRET_KEY=${JWT_SECRET_KEY}
    depends_on:
      - db
  
  db:
    image: postgres:16-alpine
    environment:
      - POSTGRES_DB=personalhub
      - POSTGRES_USER=personalhub
      - POSTGRES_PASSWORD=${DB_PASSWORD}
    volumes:
      - postgres_data:/var/lib/postgresql/data

volumes:
  postgres_data:
```

### 4. Cloud Deployment

#### AWS Elastic Beanstalk
```bash
# Install EB CLI
pip install awsebcli

# Initialize
eb init -p java-21 personal-hub-backend

# Deploy
eb create personal-hub-env
eb deploy
```

#### Heroku
```bash
# Create app
heroku create personal-hub-backend

# Add PostgreSQL
heroku addons:create heroku-postgresql:mini

# Deploy
git push heroku main
```

#### Railway/Render
- Connect GitHub repository
- Set environment variables
- Auto-deploy on push

## Production Configuration

### Environment Variables (Required)
```bash
# Database
DB_HOST=your-db-host
DB_PORT=5432
DB_NAME=personalhub
DB_USERNAME=personalhub
DB_PASSWORD=secure-password

# Security
JWT_SECRET_KEY=your-256-bit-secret-key
JWT_EXPIRATION=900000

# Application
APP_BASE_URL=https://api.yourdomain.com
APP_FRONTEND_URL=https://yourdomain.com

# Email (Production)
APP_EMAIL_PROVIDER=brevo
BREVO_API_KEY=your-api-key
```

### JVM Options
```bash
# Memory settings
-Xms512m -Xmx1024m

# Garbage collection
-XX:+UseG1GC

# Monitoring
-Dcom.sun.management.jmxremote
-Dcom.sun.management.jmxremote.port=9090
```

### Nginx Configuration
```nginx
server {
    listen 80;
    server_name api.yourdomain.com;
    
    location / {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

## Health Monitoring

### Health Check Endpoint
```bash
curl http://localhost:8080/actuator/health
```

### Metrics
- Available at `/actuator/metrics`
- JVM metrics
- HTTP metrics
- Database connection pool

## Deployment Checklist

### Pre-deployment
- [ ] All tests passing
- [ ] Security audit completed
- [ ] Environment variables configured
- [ ] Database migrations tested
- [ ] SSL certificates ready

### Deployment
- [ ] Deploy application
- [ ] Run database migrations
- [ ] Verify health endpoint
- [ ] Test critical endpoints
- [ ] Monitor logs

### Post-deployment
- [ ] Monitor error rates
- [ ] Check performance metrics
- [ ] Verify email functionality
- [ ] Test OAuth flows
- [ ] Update DNS if needed

## Rollback Strategy

1. **Keep previous JAR**
   ```bash
   cp app.jar app-backup.jar
   ```

2. **Database migrations**
   - Always test rollback scripts
   - Keep migration reversible

3. **Quick rollback**
   ```bash
   # Stop current
   systemctl stop personal-hub
   
   # Restore previous
   cp app-backup.jar app.jar
   
   # Start
   systemctl start personal-hub
   ```

## Performance Tuning

### Database
- Connection pool: 10-20 connections
- Statement cache: Enabled
- Slow query logging: Enabled

### Application
- Tomcat threads: 200
- Max connections: 10000
- Compression: Enabled

### Caching
- Consider Redis for:
  - Session storage
  - Rate limiting
  - Frequently accessed data

## Troubleshooting

### Common Issues

1. **Out of Memory**
   - Increase heap size
   - Check for memory leaks
   - Enable heap dumps

2. **Database Connection Issues**
   - Check connection pool settings
   - Verify network connectivity
   - Review PostgreSQL logs

3. **Slow Performance**
   - Enable SQL logging
   - Check N+1 queries
   - Review indexes

### Debug Mode
```bash
java -jar app.jar \
  --logging.level.com.zametech=DEBUG \
  --logging.level.org.springframework.security=DEBUG
```

## Security Considerations

1. **Never expose**
   - Database credentials
   - JWT secret keys
   - API keys

2. **Always use**
   - HTTPS in production
   - Strong passwords
   - Environment variables

3. **Regular updates**
   - Dependencies
   - JVM
   - Operating system