# Environment Configuration Guide

## Overview
This guide explains all environment variables and configuration options for Personal Hub Backend.

## Quick Setup

1. Copy the example environment file:
```bash
cp .env.example .env
```

2. Edit `.env` with your configuration
3. Start the application

## Environment Variables

### 🔐 Authentication & Security

#### JWT Configuration
```bash
# JWT secret key (REQUIRED - must be at least 32 bytes)
# Generate with: openssl rand -base64 32
JWT_SECRET_KEY=your_256_bit_jwt_secret_key_here

# Token expiration time in milliseconds
# Default: 900000 (15 minutes)
JWT_EXPIRATION=900000

# JWT Key ID (for key rotation)
JWT_KEY_ID=default-key
```

#### OAuth2 Configuration

##### Google OAuth
```bash
# Google OAuth Client credentials
GOOGLE_OIDC_CLIENT_ID=your_google_client_id
GOOGLE_OIDC_CLIENT_SECRET=your_google_client_secret
GOOGLE_OIDC_REDIRECT_URI=http://localhost:3000/auth/callback
```

##### GitHub OAuth
```bash
# GitHub OAuth App credentials
GITHUB_CLIENT_ID=your_github_client_id
GITHUB_CLIENT_SECRET=your_github_client_secret
GITHUB_REDIRECT_URI=http://localhost:3000/auth/callback
```

### 🗄️ Database Configuration

```bash
# PostgreSQL connection settings
DB_HOST=localhost
DB_PORT=5432
DB_NAME=personalhub
DB_USERNAME=personalhub
DB_PASSWORD=personalhub

# Connection pool settings (optional)
# DB_MAX_POOL_SIZE=10
# DB_MIN_IDLE=5
# DB_CONNECTION_TIMEOUT=30000
```

### 📧 Email Configuration

#### Email Provider Selection
```bash
# Provider options: mock, smtp, sendgrid, brevo
# Default: mock (for development)
APP_EMAIL_PROVIDER=mock

# Email sender information
APP_EMAIL_FROM=noreply@yourdomain.com
APP_EMAIL_FROM_NAME=Personal Hub
```

#### Provider-Specific Settings

##### Brevo (Recommended for Production)
```bash
APP_EMAIL_PROVIDER=brevo
BREVO_API_KEY=your-brevo-api-key
# Optional: Custom API URL
# BREVO_API_URL=https://api.brevo.com/v3
```

##### SendGrid
```bash
APP_EMAIL_PROVIDER=sendgrid
SENDGRID_API_KEY=your-sendgrid-api-key
```

##### SMTP (Gmail Example)
```bash
APP_EMAIL_PROVIDER=smtp
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=your-email@gmail.com
MAIL_PASSWORD=your-app-specific-password
```

### 🌐 Application URLs

```bash
# Backend base URL
APP_BASE_URL=http://localhost:8080

# Frontend URL (for CORS and redirects)
APP_FRONTEND_URL=http://localhost:3000

# OIDC Configuration
OIDC_ISSUER=http://localhost:8080
OIDC_BASE_URL=http://localhost:8080
```

### ⚡ Rate Limiting

```bash
# Authentication endpoints rate limiting
RATE_LIMIT_AUTH_CAPACITY=10        # Max requests
RATE_LIMIT_AUTH_REFILL=10         # Refill rate
RATE_LIMIT_AUTH_PERIOD=1          # Period in minutes

# General API rate limiting
RATE_LIMIT_GENERAL_CAPACITY=100   # Max requests
RATE_LIMIT_GENERAL_REFILL=100     # Refill rate
RATE_LIMIT_GENERAL_PERIOD=1       # Period in minutes
```

### 📅 Google Calendar Integration

```bash
# Google Calendar API credentials
GOOGLE_CLIENT_ID=your_google_calendar_client_id
GOOGLE_CLIENT_SECRET=your_google_calendar_client_secret
GOOGLE_REDIRECT_URI=http://localhost:8080/api/v1/calendar/sync/auth/callback

# Optional: Custom paths
# GOOGLE_CREDENTIALS_FILE=credentials.json
# GOOGLE_TOKENS_DIR=tokens
```

### 🔧 Advanced Configuration

#### Token Lifetimes
```bash
# OAuth2/OIDC token configuration (in seconds)
OIDC_AUTH_CODE_TTL=600      # Authorization code: 10 minutes
OIDC_ACCESS_TOKEN_TTL=900   # Access token: 15 minutes
OIDC_REFRESH_TOKEN_TTL=2592000  # Refresh token: 30 days
OIDC_ID_TOKEN_TTL=3600      # ID token: 1 hour
```

#### Logging
```bash
# Log level (TRACE, DEBUG, INFO, WARN, ERROR)
LOGGING_LEVEL_ROOT=INFO
LOGGING_LEVEL_APP=DEBUG
```

## Configuration by Environment

### Development
```bash
# .env.development
JWT_EXPIRATION=3600000  # 1 hour for easier development
APP_EMAIL_PROVIDER=mock
RATE_LIMIT_AUTH_CAPACITY=1000  # Higher limits for testing
```

### Production
```bash
# .env.production
JWT_EXPIRATION=900000   # 15 minutes for security
APP_EMAIL_PROVIDER=brevo
RATE_LIMIT_AUTH_CAPACITY=10    # Strict limits
```

## Security Best Practices

1. **Never commit `.env` files** - Only commit `.env.example`
2. **Use strong JWT secrets** - Generate with `openssl rand -base64 32`
3. **Rotate secrets regularly** - Update JWT_KEY_ID when rotating
4. **Use environment-specific files** - Separate dev/prod configurations
5. **Secure database credentials** - Use strong passwords in production

## Troubleshooting

### Common Issues

1. **JWT errors**
   - Ensure JWT_SECRET_KEY is at least 32 bytes
   - Check token expiration settings

2. **Database connection fails**
   - Verify PostgreSQL is running
   - Check credentials and network access

3. **OAuth redirect issues**
   - Ensure redirect URIs match OAuth provider settings
   - Check CORS configuration

4. **Email not sending**
   - Verify email provider credentials
   - Check provider-specific settings

## Environment Variable Priority

Variables are loaded in this order (later overrides earlier):
1. Default values in `application.yml`
2. `.env` file
3. System environment variables
4. Command line arguments

Example:
```bash
# Override via command line
java -jar app.jar --JWT_EXPIRATION=1800000
```

## Validation

The application validates critical environment variables on startup:
- JWT_SECRET_KEY must be present and valid
- Database connection must be successful
- OAuth credentials format validation

Missing required variables will prevent application startup with clear error messages.