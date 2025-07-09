# Personal Hub Backend

[![CI Pipeline](https://github.com/sasazame/personal-hub-backend/actions/workflows/ci.yml/badge.svg)](https://github.com/sasazame/personal-hub-backend/actions/workflows/ci.yml)
[![codecov](https://codecov.io/gh/sasazame/personal-hub-backend/branch/main/graph/badge.svg)](https://codecov.io/gh/sasazame/personal-hub-backend)

A comprehensive personal management system backend API built with Spring Boot + PostgreSQL

## 🚀 Quick Start

### Prerequisites
- Java 21+
- PostgreSQL 16+
- Maven 3.8+

### Setup (5 minutes)
```bash
# 1. Clone & setup
git clone https://github.com/sasazame/personal-hub-backend.git
cd personal-hub-backend
cp .env.example .env

# 2. Create database
sudo -u postgres psql -c "CREATE DATABASE personalhub;"
sudo -u postgres psql -c "CREATE USER personalhub WITH ENCRYPTED PASSWORD 'personalhub';"
sudo -u postgres psql -c "GRANT ALL PRIVILEGES ON DATABASE personalhub TO personalhub;"

# 3. Run
mvn spring-boot:run
```

The API will be available at `http://localhost:8080`

📚 **[Detailed Setup Guide](QUICKSTART.md)**

## 🎯 Key Features

- **🔐 Authentication**: JWT + OAuth2 (Google/GitHub)
- **✅ TODO Management**: Hierarchical tasks with priorities
- **📅 Calendar**: Event management with reminders
- **📝 Notes**: Markdown support with tagging
- **⏰ Moments**: Timeline-based thought capture with tagging
- **📊 Analytics**: Productivity insights
- **🛡️ Security**: User data isolation, rate limiting

## 📖 API Overview

### Base URL
```
http://localhost:8080/api/v1
```

### Authentication
```bash
# Register
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email": "user@example.com", "password": "Pass123!", "username": "user"}'

# Login (get JWT token)
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email": "user@example.com", "password": "Pass123!"}'
```

### Example: Create TODO
```bash
curl -X POST http://localhost:8080/api/v1/todos \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"title": "My Task", "priority": "HIGH"}'
```

📚 **[API Quick Reference](docs/API_REFERENCE.md)** | **[Full API Documentation](docs/API.md)**

## 🏗️ Architecture

Built with **Hexagonal Architecture** for clean separation of concerns:

```
┌─────────────────┐
│ Presentation    │ ← REST Controllers
├─────────────────┤
│ Application     │ ← Business Logic
├─────────────────┤
│ Domain          │ ← Core Models
├─────────────────┤
│ Infrastructure  │ ← Database, External APIs
└─────────────────┘
```

📚 **[Architecture Guide](docs/ARCHITECTURE.md)** | **[Folder Structure](docs/FOLDER_STRUCTURE.md)**

## 👨‍💻 Development

### Tech Stack
- **Java 21** + **Spring Boot 3.3.7**
- **PostgreSQL 16+** + **Flyway migrations**
- **JWT** authentication
- **JUnit 5** + **Mockito** for testing

### Development Workflow
```bash
# Create feature branch
git checkout -b feat/new-feature

# Make changes & test
mvn test

# Commit & push
git commit -m "feat: add new feature"
git push origin feat/new-feature
```

### Testing
```bash
mvn test          # Run all tests
mvn verify        # Run integration tests
mvn jacoco:report # Generate coverage report
```

## 📚 Documentation

- **[API Quick Reference](docs/API_REFERENCE.md)** - Concise endpoint overview
- **[API Full Documentation](docs/API.md)** - Complete endpoint details
- **[Architecture](docs/ARCHITECTURE.md)** - System design details
- **[Database Schema](docs/DATABASE.md)** - Data model documentation
- **[OAuth Setup](docs/OAUTH_SETUP.md)** - Google/GitHub integration
- **[Frontend Examples](docs/FRONTEND_API_EXAMPLES.md)** - Integration code samples

## 🚧 Roadmap

- ✅ Core authentication & authorization
- ✅ TODO management with hierarchy
- ✅ Goal tracking system
- ✅ OAuth2 integration
- 🚧 Calendar sync (Google Calendar)
- 🚧 Advanced analytics
- 📋 File attachments
- 📋 Real-time notifications

## 🤝 Contributing

1. Fork the repository
2. Create your feature branch (`git checkout -b feat/amazing-feature`)
3. Commit changes (`git commit -m 'feat: add amazing feature'`)
4. Push to branch (`git push origin feat/amazing-feature`)
5. Open a Pull Request

## 📝 License

This project is licensed under the MIT License.

## 📞 Support

- 📖 [Documentation](docs/)
- 🐛 [Report Issues](https://github.com/sasazame/personal-hub-backend/issues)
- 💬 [Discussions](https://github.com/sasazame/personal-hub-backend/discussions)

---

**Maintained by**: [@sasazame](https://github.com/sasazame)