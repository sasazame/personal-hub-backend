# Google OAuth Setup Guide

## Overview
Setup instructions for using Google OAuth authentication in the Personal Hub backend.

## Prerequisites
- Google Cloud Console account
- Google Cloud project

## Setup Instructions

### 1. Google Cloud Console Configuration
1. Access [Google Cloud Console](https://console.cloud.google.com/)
2. Select or create a project
3. Navigate to "APIs & Services" → "Credentials"
4. Click "Create Credentials" → "OAuth client ID"
5. Select "Web application" as the application type
6. Configure the following settings:
   - **Name**: Personal Hub Backend
   - **Authorized JavaScript origins**: 
     - `http://localhost:8080`
     - `http://localhost:3000` (for frontend)
   - **Authorized redirect URIs**:
     - `http://localhost:8080/api/v1/auth/oidc/google/callback`

### 2. Environment Variable Configuration
Create a `.env` file in the project root with the following content:

```bash
# Google OAuth Configuration
GOOGLE_OIDC_CLIENT_ID=your_client_id_here
GOOGLE_OIDC_CLIENT_SECRET=your_client_secret_here
GOOGLE_OIDC_REDIRECT_URI=http://localhost:8080/api/v1/auth/oidc/google/callback
```

### 3. Start Application
```bash
# Load environment variables and start
source .env && mvn spring-boot:run
```

Or when using an IDE, configure environment variables before starting.

## Authentication Flow

### 1. Initiate Authentication
```
GET /api/v1/auth/oidc/google/authorize
```

Response:
```json
{
  "authorizationUrl": "https://accounts.google.com/o/oauth2/v2/auth?...",
  "state": "random-state-value"
}
```

### 2. Google Login
Redirect user to authorizationUrl to log in with Google account

### 3. Callback Processing
After Google redirect:
```
POST /api/v1/auth/oidc/google/callback
Content-Type: application/json

{
  "code": "authorization-code-from-google",
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
    "username": "user@example.com",
    "email": "user@example.com",
    "createdAt": "2023-01-01T00:00:00",
    "updatedAt": "2023-01-01T00:00:00"
  }
}
```

## Information Retrieved from Google OAuth

### Basic User Information
- Google user ID (sub)
- Email address
- Email verification status
- Full name
- Given name (first name)
- Family name (last name)
- Profile picture URL
- Locale preference

### Scopes Requested
- `openid`: OpenID Connect authentication
- `email`: Email address access
- `profile`: Basic profile information

## Troubleshooting

### Error: redirect_uri_mismatch
- Verify that the redirect URI configured in Google Cloud Console matches exactly with the application URI
- Protocol (http/https), port number, and path must match precisely

### Error: invalid_client
- Verify CLIENT_ID and CLIENT_SECRET are configured correctly
- Verify environment variables are loaded correctly

### Error: access_denied
- User denied permission during OAuth consent
- Check OAuth consent screen configuration

### Error: invalid_grant
- Authorization code expired (typically 10 minutes)
- Authorization code already used
- Time synchronization issues

## Security Considerations
- **Never commit** the `.env` file to Git
- **Always use HTTPS** in production environments
- Keep CLIENT_SECRET secure and **never expose** to frontend
- Configure OAuth consent screen appropriately
- Use minimal required scopes

## OAuth Consent Screen Configuration

### Development Environment
- User Type: External (for testing)
- Test users: Add specific Google accounts for testing
- Publishing status: Testing

### Production Environment
- User Type: External
- Publishing status: Published (after verification)
- Privacy policy and terms of service URLs required

## Development vs Production Environment Configuration

### Development Environment
```bash
GOOGLE_OIDC_REDIRECT_URI=http://localhost:8080/api/v1/auth/oidc/google/callback
```

### Production Environment
```bash
GOOGLE_OIDC_REDIRECT_URI=https://yourdomain.com/api/v1/auth/oidc/google/callback
```

### Google Cloud Console Settings

#### Development
- Authorized JavaScript origins: `http://localhost:8080`, `http://localhost:3000`
- Authorized redirect URIs: `http://localhost:8080/api/v1/auth/oidc/google/callback`

#### Production
- Authorized JavaScript origins: `https://yourdomain.com`, `https://app.yourdomain.com`
- Authorized redirect URIs: `https://yourdomain.com/api/v1/auth/oidc/google/callback`

## Testing OAuth Integration

### 1. Manual Testing
1. Start the application
2. Navigate to `/api/v1/auth/oidc/google/authorize`
3. Follow the redirect to Google
4. Complete authentication
5. Verify callback processing

### 2. Frontend Integration Testing
1. Implement frontend OAuth flow
2. Test state parameter validation
3. Verify token storage and usage
4. Test error handling scenarios

## Common Issues and Solutions

### Issue: "This app isn't verified"
- Expected during development with external user type
- Add test users to avoid this warning
- For production, complete app verification process

### Issue: Token refresh not working
- Google OAuth tokens expire in 1 hour
- Implement token refresh logic in frontend
- Use refresh tokens for long-lived sessions

### Issue: CORS errors
- Ensure frontend origin is in authorized JavaScript origins
- Check CORS configuration in backend
- Verify exact URL matching (including protocol and port)

For multiple environments, it's recommended to create separate Google Cloud projects or OAuth clients for each environment to maintain proper isolation.