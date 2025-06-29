# Quick Start Guide

Get Personal Hub Backend running in 5 minutes!

## Prerequisites

- Java 21+
- PostgreSQL 16+
- Maven 3.8+

## 1. Clone & Setup

```bash
# Clone the repository
git clone https://github.com/sasazame/personal-hub-backend.git
cd personal-hub-backend

# Copy environment variables
cp .env.example .env
```

## 2. Configure Database

Edit `.env` file with your PostgreSQL credentials (or run `./scripts/check-env.sh` to validate):

```env
DB_HOST=localhost
DB_PORT=5432
DB_NAME=personalhub
DB_USERNAME=personalhub
DB_PASSWORD=personalhub
```

## 3. Run the Application

```bash
# Install dependencies & run
mvn clean install
mvn spring-boot:run
```

The API will be available at `http://localhost:8080`

## 4. Test the API

### Register a new user:
```bash
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@example.com",
    "password": "Password123!",
    "username": "testuser"
  }'
```

### Login:
```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@example.com",
    "password": "Password123!"
  }'
```

Save the `accessToken` from the response for authenticated requests.

### Create a TODO:
```bash
curl -X POST http://localhost:8080/api/v1/todos \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN" \
  -d '{
    "title": "My first todo",
    "description": "Testing the API"
  }'
```

## Next Steps

- Check the full [API Documentation](docs/API.md)
- Read the [Architecture Guide](docs/ARCHITECTURE.md)
- Configure [OAuth providers](docs/OAUTH_SETUP.md) (optional)

## Troubleshooting

### Database connection issues?
```bash
# Check PostgreSQL is running
sudo systemctl status postgresql

# Create database if needed
createdb personalhub
```

### Port already in use?
```bash
# Change port in application.yml
server:
  port: 8081
```

## Need Help?

- 📖 [Full Documentation](README.md)
- 🐛 [Report Issues](https://github.com/sasazame/personal-hub-backend/issues)
- 💬 [Discussions](https://github.com/sasazame/personal-hub-backend/discussions)