# OAuth2 Google Calendar Sync Documentation

## Overview

The Personal Hub backend now supports bidirectional synchronization with Google Calendar using OAuth2 authentication. This feature allows users to:
- View their Google Calendar events in Personal Hub
- Create events in Personal Hub that sync to Google Calendar
- Update events in either system and have changes propagate

## Architecture

### Authentication Flow

1. **Initial OAuth2 Login**
   - User initiates login via `/api/v1/auth/oidc/google/authorize`
   - System includes calendar scope: `https://www.googleapis.com/auth/calendar`
   - After successful authentication, access and refresh tokens are encrypted and stored

2. **Token Management**
   - Access tokens expire after 1 hour
   - Refresh tokens are used automatically when access tokens expire
   - Token encryption uses AES-GCM for secure storage

### Key Components

#### Services

1. **GoogleOidcService** (`/application/service/GoogleOidcService.java`)
   - Handles OAuth2 authentication flow
   - Manages token refresh
   - Stores encrypted tokens in database

2. **GoogleCalendarOAuth2Service** (`/application/service/GoogleCalendarOAuth2Service.java`)
   - Uses OAuth2 tokens to access Google Calendar API
   - Implements calendar operations (list, create, update events)
   - Handles automatic token refresh

3. **CalendarSyncService** (`/application/service/CalendarSyncService.java`)
   - Orchestrates bidirectional sync
   - Manages sync settings and status
   - Handles conflict resolution

4. **TokenEncryptionService** (`/infrastructure/security/TokenEncryptionService.java`)
   - Provides AES-GCM encryption for tokens
   - Ensures secure token storage

#### Database Schema

1. **user_social_accounts** table
   - Stores OAuth2 tokens (encrypted)
   - Links users to their Google accounts
   - Tracks token expiration times

2. **calendar_sync_settings** table
   - Stores per-calendar sync configuration
   - Supports multiple Google calendars per user
   - Configurable sync direction (bidirectional, to Google, from Google)

## API Endpoints

### Authentication
- `GET /api/v1/auth/oidc/google/authorize` - Initiate Google OAuth2 login
- `GET /api/v1/auth/oidc/google/callback` - OAuth2 callback endpoint

### Calendar Sync
- `POST /api/v1/calendar/sync/connect` - Connect Google Calendar
- `GET /api/v1/calendar/sync/status` - Get sync status
- `POST /api/v1/calendar/sync` - Trigger manual sync
- `GET /api/v1/calendar/sync/settings` - Get sync settings
- `PUT /api/v1/calendar/sync/settings` - Update sync settings

## Usage Flow

### For New Users

1. **Register via Google OAuth2**
   ```bash
   # Get authorization URL
   GET /api/v1/auth/oidc/google/authorize
   
   # User is redirected to Google, approves access
   # Callback returns JWT token
   ```

2. **Connect Calendar**
   ```bash
   POST /api/v1/calendar/sync/connect
   Authorization: Bearer {jwt_token}
   
   # Returns list of available calendars
   ```

3. **Configure Sync Settings**
   ```bash
   PUT /api/v1/calendar/sync/settings
   {
     "calendarId": "primary",
     "enabled": true,
     "syncDirection": "BIDIRECTIONAL",
     "autoSync": true,
     "syncInterval": 30
   }
   ```

4. **Trigger Sync**
   ```bash
   POST /api/v1/calendar/sync
   Authorization: Bearer {jwt_token}
   ```

### For Existing Users (Username/Password)

Users who registered with username/password must:
1. Login via Google OAuth2 to link their account
2. Then follow the calendar connection flow above

## Error Handling

### OAuth2 Required
- **Status**: 403 Forbidden
- **Code**: `OAUTH2_REQUIRED`
- **Message**: "Please login with Google OAuth2 first to connect calendars"
- **When**: User attempts calendar operations without OAuth2 authentication

### Token Expired
- System automatically attempts refresh using refresh token
- If refresh fails, user must re-authenticate

### Sync Errors
- Individual event sync failures don't stop the entire sync
- Errors are logged and reported in sync status
- Events marked with `SYNC_ERROR` status

## Security Considerations

1. **Token Storage**
   - All tokens encrypted using AES-GCM
   - Encryption key should be configured via environment variable
   - Never store plain text tokens

2. **Scope Management**
   - Only request necessary scopes (calendar access)
   - Users must explicitly approve calendar access

3. **Access Control**
   - Users can only sync their own calendars
   - Calendar data isolated per user

## Configuration

### Environment Variables
```properties
# Google OAuth2 credentials
GOOGLE_OIDC_CLIENT_ID=your-client-id
GOOGLE_OIDC_CLIENT_SECRET=your-client-secret
GOOGLE_OIDC_REDIRECT_URI=http://localhost:8080/api/v1/auth/oidc/google/callback

# Token encryption key (base64 encoded)
APP_SECURITY_TOKEN_ENCRYPTION_KEY=your-base64-encoded-key
```

### Application Properties
```yaml
google:
  client:
    id: ${GOOGLE_OIDC_CLIENT_ID}
    secret: ${GOOGLE_OIDC_CLIENT_SECRET}
  redirect:
    uri: ${GOOGLE_OIDC_REDIRECT_URI}

app:
  security:
    token-encryption-key: ${APP_SECURITY_TOKEN_ENCRYPTION_KEY}
```

## Limitations

1. **Authentication Requirement**
   - Calendar sync only available for OAuth2 authenticated users
   - Username/password users must link Google account

2. **Token Expiration**
   - Access tokens expire after 1 hour
   - Automatic refresh implemented but may fail if refresh token expires

3. **Rate Limits**
   - Subject to Google Calendar API quotas
   - Implement appropriate rate limiting for sync operations

## Future Enhancements

1. **Multiple Provider Support**
   - Add support for Outlook, iCal, etc.

2. **Selective Sync**
   - Allow users to filter which events to sync

3. **Conflict Resolution**
   - Implement sophisticated conflict resolution strategies

4. **Real-time Sync**
   - Use webhooks for instant synchronization