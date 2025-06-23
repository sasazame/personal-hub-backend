# GitHub OAuth Setup Guide

## Overview
Setup instructions for using GitHub OAuth authentication in the Personal Hub backend.

## Prerequisites
- GitHub account
- GitHub OAuth application

## Setup Instructions

### 1. Create GitHub OAuth Application
1. Log in to GitHub
2. Go to Settings → Developer settings → OAuth Apps
3. Click "New OAuth App" or "Register a new application"
4. Enter the following information:
   - **Application name**: Personal Hub Backend
   - **Homepage URL**: http://localhost:8080
   - **Application description**: Personal Hub OAuth authentication
   - **Authorization callback URL**: http://localhost:8080/api/v1/auth/oidc/github/callback
5. Click "Register application"
6. Note the Client ID and Client Secret (generate Client Secret with "Generate a new client secret")

### 2. Environment Variable Configuration
Add the following to the `.env` file in the project root:

```bash
# GitHub OAuth Configuration
GITHUB_CLIENT_ID=your_github_client_id_here
GITHUB_CLIENT_SECRET=your_github_client_secret_here
GITHUB_REDIRECT_URI=http://localhost:8080/api/v1/auth/oidc/github/callback
```

### 3. Start Application
```bash
# Load environment variables and start
source .env && mvn spring-boot:run
```

## Authentication Flow

### 1. Initiate Authentication
```
GET /api/v1/auth/oidc/github/authorize
```

Response:
```json
{
  "authorizationUrl": "https://github.com/login/oauth/authorize?...",
  "state": "random-state-value",
  "provider": "github"
}
```

### 2. GitHub Login
Redirect user to authorizationUrl to log in with GitHub account

### 3. Callback Processing
After GitHub redirect:
```
POST /api/v1/auth/oidc/github/callback
Content-Type: application/json

{
  "code": "authorization-code-from-github",
  "state": "state-from-step-1"
}
```

Response:
```json
{
  "accessToken": "jwt-token",
  "refreshToken": null,
  "user": {
    "id": "user-uuid",
    "username": "github-username",
    "email": "user@example.com",
    "createdAt": "2023-01-01T00:00:00",
    "updatedAt": "2023-01-01T00:00:00"
  }
}
```

## Information Retrieved from GitHub OAuth

### Basic User Information
- GitHub user ID
- Username (login)
- Full name
- Email address (primary + verified)
- Avatar image URL

### Additional Profile Information (stored only)
- Company name
- Blog URL
- Location
- Bio
- Public repository count
- Follower count
- Following count

## Troubleshooting

### Error: redirect_uri_mismatch
- Verify that the "Authorization callback URL" in GitHub application settings matches exactly
- Protocol (http/https), port number, and path must match completely

### Error: bad_verification_code
- Authorization code expired (10 minutes)
- Authorization code already used
- Invalid authorization code

### Error: incorrect_client_credentials
- Verify CLIENT_ID and CLIENT_SECRET are configured correctly
- Verify environment variables are loaded correctly

## Security Considerations
- **Never commit** the `.env` file to Git
- **Always use HTTPS** in production environments
- Keep CLIENT_SECRET secure and **never expose** to frontend
- Request only necessary scopes in GitHub application settings

## Development vs Production Environment Configuration

### Development Environment
```
Authorization callback URL: http://localhost:8080/api/v1/auth/oidc/github/callback
```

### Production Environment
```
Authorization callback URL: https://yourdomain.com/api/v1/auth/oidc/github/callback
```

For multiple environments, it's recommended to create separate GitHub OAuth applications for each environment.