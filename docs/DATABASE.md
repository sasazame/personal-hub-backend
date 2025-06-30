# Database Design Document

## Overview
Database design for Personal Hub integrated application using PostgreSQL 16
Comprehensive management of TODO, calendar, notes, goals, and authentication features

## ER Diagram
```
┌─────────────────────────────────────────────┐
│                   users                     │
├─────────────────────────────────────────────┤
│ id (UUID) PK                               │
│ username (VARCHAR(50)) NOT NULL UNIQUE     │
│ email (VARCHAR(255)) NOT NULL UNIQUE       │
│ password (VARCHAR(255)) NULLABLE           │
│ enabled (BOOLEAN) NOT NULL DEFAULT TRUE    │
│ email_verified (BOOLEAN) DEFAULT FALSE     │
│ given_name (VARCHAR(255))                  │
│ family_name (VARCHAR(255))                 │
│ locale (VARCHAR(10))                       │
│ profile_picture_url (TEXT)                 │
│ week_start_day (INTEGER) DEFAULT 1         │
│ created_at (TIMESTAMP) NOT NULL            │
│ updated_at (TIMESTAMP) NOT NULL            │
└─────────────────────────────────────────────┘
     │         │        │        │        │        │
┌────┴───┐ ┌──┴────┐ ┌─┴────┐ ┌┴───┐ ┌──┴────┐ ┌┴─────────┐
│ todos  │ │events │ │notes │ │goals│ │social │ │security  │
│        │ │       │ │      │ │    │ │accounts│ │events    │
└────────┘ └───────┘ └──────┘ └────┘ └────────┘ └──────────┘
```

## Table Specifications

### users Table
**Purpose**: User account management and profile information

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | UUID | PRIMARY KEY | Unique user identifier |
| username | VARCHAR(50) | NOT NULL, UNIQUE | User display name |
| email | VARCHAR(255) | NOT NULL, UNIQUE | Email address (login ID) |
| password | VARCHAR(255) | NULLABLE | BCrypt hashed password (NULL for OAuth users) |
| enabled | BOOLEAN | NOT NULL, DEFAULT TRUE | Account activation status |
| email_verified | BOOLEAN | DEFAULT FALSE | Email verification status |
| given_name | VARCHAR(255) | NULLABLE | First name (from OAuth) |
| family_name | VARCHAR(255) | NULLABLE | Last name (from OAuth) |
| locale | VARCHAR(10) | NULLABLE | User's locale preference |
| profile_picture_url | TEXT | NULLABLE | Profile image URL |
| week_start_day | INTEGER | DEFAULT 1, CHECK (0-6) | Week start preference (0=Sunday, 1=Monday) |
| created_at | TIMESTAMP | NOT NULL | Account creation timestamp |
| updated_at | TIMESTAMP | NOT NULL | Last update timestamp |

**Indexes**:
- Primary: `users_pkey` on `id`
- Unique: `users_username_key` on `username`
- Unique: `users_email_key` on `email`

### todos Table
**Purpose**: TODO task management with hierarchical structure

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | BIGSERIAL | PRIMARY KEY | Auto-incrementing task ID |
| user_id | UUID | NOT NULL, FK → users(id) | Task owner |
| title | VARCHAR(255) | NOT NULL | Task title |
| description | TEXT | NULLABLE | Detailed task description |
| status | todo_status | NOT NULL, DEFAULT 'TODO' | Task status (TODO/IN_PROGRESS/DONE) |
| priority | todo_priority | NOT NULL, DEFAULT 'MEDIUM' | Task priority (LOW/MEDIUM/HIGH) |
| due_date | DATE | NULLABLE | Task deadline |
| parent_id | BIGINT | NULLABLE, FK → todos(id) | Parent task for subtasks |
| is_repeatable | BOOLEAN | DEFAULT FALSE | Whether task repeats |
| repeat_type | repeat_type | NULLABLE | Repeat pattern type |
| repeat_interval | INTEGER | NULLABLE | Repeat interval |
| repeat_days_of_week | VARCHAR(20) | NULLABLE | Days of week for weekly repeat |
| repeat_day_of_month | INTEGER | NULLABLE | Day of month for monthly repeat |
| repeat_end_date | DATE | NULLABLE | When repetition ends |
| original_todo_id | BIGINT | NULLABLE, FK → todos(id) | Original task for repeated instances |
| created_at | TIMESTAMP | NOT NULL | Creation timestamp |
| updated_at | TIMESTAMP | NOT NULL | Last update timestamp |

**Indexes**:
- Primary: `todos_pkey` on `id`
- Index: `idx_todos_user_id` on `user_id`
- Index: `idx_todos_status` on `status`
- Index: `idx_todos_due_date` on `due_date`
- Index: `idx_todos_parent_id` on `parent_id`

### events Table (formerly calendar_events)
**Purpose**: Calendar event management with Google Calendar sync

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | BIGSERIAL | PRIMARY KEY | Auto-incrementing event ID |
| user_id | UUID | NOT NULL, FK → users(id) | Event owner |
| title | VARCHAR(255) | NOT NULL | Event title |
| description | TEXT | NULLABLE | Event description |
| start_time | TIMESTAMP | NOT NULL | Event start time |
| end_time | TIMESTAMP | NOT NULL | Event end time |
| location | VARCHAR(255) | NULLABLE | Event location |
| all_day | BOOLEAN | DEFAULT FALSE | All-day event flag |
| reminder_minutes | INTEGER | NULLABLE | Reminder time in minutes before event |
| google_calendar_id | VARCHAR(255) | NULLABLE | Google Calendar ID |
| google_event_id | VARCHAR(255) | NULLABLE | Google Event ID |
| last_synced_at | TIMESTAMP WITH TIME ZONE | NULLABLE | Last sync timestamp |
| sync_status | VARCHAR(50) | DEFAULT 'NONE' | Sync status |
| created_at | TIMESTAMP | NOT NULL | Creation timestamp |
| updated_at | TIMESTAMP | NOT NULL | Last update timestamp |

**Indexes**:
- Primary: `events_pkey` on `id`
- Index: `idx_events_user_id` on `user_id`
- Index: `idx_events_start_time` on `start_time`
- Index: `idx_events_google_event_id` on `google_event_id`
- Index: `idx_events_sync_status` on `sync_status`
- Index: `idx_events_last_synced_at` on `last_synced_at`

**Constraints**:
- Check: `start_time < end_time`
- Check: `reminder_minutes >= 0`
- Check: `sync_status IN ('NONE', 'SYNCED', 'SYNC_PENDING', 'SYNC_ERROR', 'SYNC_CONFLICT')`

### notes Table
**Purpose**: Note and documentation management

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | BIGSERIAL | PRIMARY KEY | Auto-incrementing note ID |
| user_id | UUID | NOT NULL, FK → users(id) | Note owner |
| title | VARCHAR(255) | NOT NULL | Note title |
| content | TEXT | NOT NULL | Note content (Markdown supported) |
| tags | TEXT[] | NULLABLE | Array of tags for categorization |
| is_public | BOOLEAN | DEFAULT FALSE | Public visibility flag |
| created_at | TIMESTAMP | NOT NULL | Creation timestamp |
| updated_at | TIMESTAMP | NOT NULL | Last update timestamp |

**Indexes**:
- Primary: `notes_pkey` on `id`
- Index: `idx_notes_user_id` on `user_id`
- Index: `idx_notes_tags` on `tags` (GIN index for array search)
- Index: `idx_notes_title` on `title` (for text search)

### goals Table
**Purpose**: Goal tracking and management

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | BIGSERIAL | PRIMARY KEY | Auto-incrementing goal ID |
| user_id | UUID | NOT NULL, FK → users(id) | Goal owner |
| title | VARCHAR(255) | NOT NULL | Goal title |
| description | TEXT | NULLABLE | Goal description |
| goal_type | VARCHAR(20) | NOT NULL | Goal period type |
| is_active | BOOLEAN | NOT NULL, DEFAULT TRUE | Active status |
| start_date | DATE | NOT NULL | Goal start date |
| end_date | DATE | NOT NULL | Goal end date |
| created_at | TIMESTAMP WITH TIME ZONE | DEFAULT CURRENT_TIMESTAMP | Creation timestamp |
| updated_at | TIMESTAMP WITH TIME ZONE | DEFAULT CURRENT_TIMESTAMP | Last update timestamp |

**Constraints**:
- Check: `goal_type IN ('ANNUAL', 'MONTHLY', 'WEEKLY', 'DAILY')`
- Foreign Key: `user_id` → `users(id)` ON DELETE CASCADE

### goal_achievement_history Table
**Purpose**: Track goal completion history

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | BIGSERIAL | PRIMARY KEY | Auto-incrementing ID |
| goal_id | BIGINT | NOT NULL, FK → goals(id) | Associated goal |
| achieved_date | DATE | NOT NULL | Achievement date |
| created_at | TIMESTAMP WITH TIME ZONE | DEFAULT CURRENT_TIMESTAMP | Creation timestamp |

**Constraints**:
- Unique: `(goal_id, achieved_date)`
- Foreign Key: `goal_id` → `goals(id)` ON DELETE CASCADE

### calendar_sync_settings Table
**Purpose**: Google Calendar synchronization settings

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | BIGSERIAL | PRIMARY KEY | Auto-incrementing ID |
| user_id | UUID | NOT NULL, FK → users(id) | User ID |
| google_calendar_id | VARCHAR(255) | NOT NULL | Google Calendar ID |
| calendar_name | VARCHAR(255) | NULLABLE | Calendar display name |
| sync_enabled | BOOLEAN | DEFAULT TRUE | Sync enabled flag |
| last_sync_at | TIMESTAMP WITH TIME ZONE | NULLABLE | Last sync timestamp |
| sync_direction | VARCHAR(20) | DEFAULT 'BIDIRECTIONAL' | Sync direction |
| auto_sync_enabled | BOOLEAN | DEFAULT TRUE | Auto-sync flag |
| sync_frequency_minutes | INTEGER | DEFAULT 30 | Sync frequency |
| created_at | TIMESTAMP WITH TIME ZONE | DEFAULT NOW() | Creation timestamp |
| updated_at | TIMESTAMP WITH TIME ZONE | DEFAULT NOW() | Last update timestamp |

**Constraints**:
- Unique: `(user_id, google_calendar_id)`
- Check: `sync_direction IN ('BIDIRECTIONAL', 'TO_GOOGLE', 'FROM_GOOGLE')`

### OAuth & Security Tables

#### oauth_applications Table
**Purpose**: OAuth application registration

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | UUID | PRIMARY KEY, DEFAULT gen_random_uuid() | Application ID |
| client_id | VARCHAR(255) | UNIQUE, NOT NULL | OAuth client ID |
| client_secret_hash | VARCHAR(255) | NULLABLE | Hashed client secret |
| redirect_uris | TEXT | NOT NULL | Allowed redirect URIs |
| scopes | VARCHAR(1000) | NOT NULL, DEFAULT 'openid,profile,email' | Allowed scopes |
| application_type | VARCHAR(50) | DEFAULT 'web' | Application type |
| grant_types | VARCHAR(500) | NOT NULL | Allowed grant types |
| response_types | VARCHAR(500) | NOT NULL | Allowed response types |
| token_endpoint_auth_method | VARCHAR(50) | DEFAULT 'client_secret_basic' | Auth method |
| application_name | VARCHAR(255) | NOT NULL | Application name |
| application_uri | VARCHAR(500) | NULLABLE | Application URL |
| contacts | VARCHAR(1000) | NULLABLE | Contact information |
| logo_uri | VARCHAR(500) | NULLABLE | Logo URL |
| tos_uri | VARCHAR(500) | NULLABLE | Terms of service URL |
| policy_uri | VARCHAR(500) | NULLABLE | Privacy policy URL |
| created_at | TIMESTAMP | DEFAULT NOW() | Creation timestamp |
| updated_at | TIMESTAMP | DEFAULT NOW() | Last update timestamp |

#### authorization_codes Table
**Purpose**: OAuth authorization code management

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| code | VARCHAR(255) | PRIMARY KEY | Authorization code |
| client_id | VARCHAR(255) | NOT NULL | OAuth client ID |
| user_id | UUID | NOT NULL, FK → users(id) | User ID |
| redirect_uri | TEXT | NOT NULL | Redirect URI |
| scopes | VARCHAR(1000) | NOT NULL | Granted scopes |
| code_challenge | VARCHAR(255) | NULLABLE | PKCE challenge |
| code_challenge_method | VARCHAR(10) | NULLABLE | PKCE method |
| nonce | VARCHAR(255) | NULLABLE | Nonce value |
| state | VARCHAR(255) | NULLABLE | State parameter |
| auth_time | TIMESTAMP | NOT NULL | Authentication time |
| expires_at | TIMESTAMP | NOT NULL | Expiration time |
| used | BOOLEAN | DEFAULT FALSE | Usage flag |
| created_at | TIMESTAMP | DEFAULT NOW() | Creation timestamp |

#### refresh_tokens Table
**Purpose**: OAuth refresh token management

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | UUID | PRIMARY KEY, DEFAULT gen_random_uuid() | Token ID |
| token_hash | VARCHAR(255) | UNIQUE, NOT NULL | Hashed token |
| user_id | UUID | NOT NULL, FK → users(id) | User ID |
| client_id | VARCHAR(255) | NOT NULL | OAuth client ID |
| scopes | VARCHAR(1000) | NOT NULL | Granted scopes |
| expires_at | TIMESTAMP | NOT NULL | Expiration time |
| revoked | BOOLEAN | DEFAULT FALSE | Revocation flag |
| revoked_at | TIMESTAMP | NULLABLE | Revocation timestamp |
| created_at | TIMESTAMP | DEFAULT NOW() | Creation timestamp |

#### user_social_accounts Table
**Purpose**: Social login provider accounts

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | UUID | PRIMARY KEY, DEFAULT gen_random_uuid() | Account ID |
| user_id | UUID | NOT NULL, FK → users(id) | User ID |
| provider | VARCHAR(50) | NOT NULL | Provider name (google, github) |
| provider_user_id | VARCHAR(255) | NOT NULL | Provider's user ID |
| email | VARCHAR(255) | NULLABLE | Provider email |
| email_verified | BOOLEAN | DEFAULT FALSE | Email verification status |
| name | VARCHAR(255) | NULLABLE | Full name |
| given_name | VARCHAR(255) | NULLABLE | First name |
| family_name | VARCHAR(255) | NULLABLE | Last name |
| picture | VARCHAR(500) | NULLABLE | Profile picture URL |
| locale | VARCHAR(10) | NULLABLE | Locale preference |
| profile_data | JSONB | NULLABLE | Additional profile data |
| access_token_encrypted | TEXT | NULLABLE | Encrypted access token |
| refresh_token_encrypted | TEXT | NULLABLE | Encrypted refresh token |
| token_expires_at | TIMESTAMP | NULLABLE | Token expiration |
| created_at | TIMESTAMP | DEFAULT NOW() | Creation timestamp |
| updated_at | TIMESTAMP | DEFAULT NOW() | Last update timestamp |

**Constraints**:
- Unique: `(provider, provider_user_id)`

#### security_events Table
**Purpose**: Security event logging and auditing

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | UUID | PRIMARY KEY, DEFAULT gen_random_uuid() | Event ID |
| event_type | VARCHAR(50) | NOT NULL | Event type |
| user_id | UUID | NULLABLE, FK → users(id) | Associated user |
| client_id | VARCHAR(255) | NULLABLE | OAuth client ID |
| ip_address | VARCHAR(255) | NOT NULL | Client IP address |
| user_agent | TEXT | NULLABLE | User agent string |
| success | BOOLEAN | NOT NULL | Success flag |
| error_code | VARCHAR(50) | NULLABLE | Error code |
| error_description | TEXT | NULLABLE | Error description |
| metadata | JSONB | NULLABLE | Additional metadata |
| created_at | TIMESTAMP | DEFAULT NOW() | Event timestamp |

**Event Types**:
- Authentication: LOGIN_SUCCESS, LOGIN_FAILURE, LOGOUT
- Token: TOKEN_REFRESH, TOKEN_REVOKE
- Authorization: AUTHORIZATION_CODE_ISSUED, AUTHORIZATION_CODE_USED, AUTHORIZATION_CODE_EXPIRED
- Account: PASSWORD_CHANGE, ACCOUNT_LOCKED, ACCOUNT_UNLOCKED
- Social: SOCIAL_ACCOUNT_LINKED, SOCIAL_ACCOUNT_UNLINKED
- Security: SUSPICIOUS_ACTIVITY

#### password_reset_tokens Table
**Purpose**: Password reset token management

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | UUID | PRIMARY KEY, DEFAULT gen_random_uuid() | Token ID |
| token | VARCHAR(255) | NOT NULL, UNIQUE | Reset token |
| user_id | UUID | NOT NULL, FK → users(id) | User ID |
| expires_at | TIMESTAMP | NOT NULL | Expiration timestamp |
| used | BOOLEAN | DEFAULT FALSE | Usage flag |
| created_at | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | Creation timestamp |

## Custom Types (Enums)

### todo_status
```sql
CREATE TYPE todo_status AS ENUM ('TODO', 'IN_PROGRESS', 'DONE');
```

### todo_priority
```sql
CREATE TYPE todo_priority AS ENUM ('LOW', 'MEDIUM', 'HIGH');
```

### repeat_type
```sql
CREATE TYPE repeat_type AS ENUM ('DAILY', 'WEEKLY', 'MONTHLY', 'YEARLY');
```

## Key Relationships

### User-centric Design
All major entities have a foreign key to the users table:
- todos → users (Many-to-One)
- events → users (Many-to-One)
- notes → users (Many-to-One)
- goals → users (Many-to-One)
- user_social_accounts → users (Many-to-One)
- calendar_sync_settings → users (Many-to-One)
- password_reset_tokens → users (Many-to-One)
- refresh_tokens → users (Many-to-One)
- authorization_codes → users (Many-to-One)
- security_events → users (Many-to-One, nullable)

### Hierarchical Relationships
- todos → todos (parent_id for subtasks)
- todos → todos (original_todo_id for recurring tasks)
- goals → goal_achievement_history (One-to-Many)

## Security Design

### Data Isolation
- All tables include `user_id` foreign key for row-level security
- Application-level enforcement ensures users can only access their own data
- UUID for user IDs prevents enumeration attacks

### Password Security
- BCrypt hashing for password storage
- Nullable password column supports OAuth users without passwords
- Password field never exposed in API responses
- Password reset tokens with expiration

### OAuth Security
- Client secrets hashed before storage
- Authorization codes with short expiration
- Refresh token rotation support
- Token encryption for social account tokens

### Audit Trail
- Security events table for comprehensive logging
- All authentication attempts tracked
- OAuth flow events recorded
- Suspicious activity detection support

## Migration Strategy

### Flyway Integration
- Version-controlled schema migrations in `src/main/resources/db/migration/`
- Naming convention: `V{version}__{description}.sql`
- Automatic migration execution on application startup

### Key Migrations
- V1-V8: Initial schema and basic features
- V15: UUID migration for better security
- V17: OAuth tables for social login
- V18: Nullable password for OAuth users
- V20-V22: Goal management system
- V23: Password reset functionality

## Performance Considerations

### Indexing Strategy
- Primary keys for all tables
- Foreign key indexes for join performance
- Composite indexes for common query patterns
- GIN indexes for array and full-text search
- Time-based indexes for event queries

### Query Optimization
- Pagination support for large datasets
- Efficient user-based filtering
- Proper use of LIMIT and OFFSET
- Connection pooling with HikariCP

### Scalability
- UUID primary keys for distributed systems
- Proper normalization to 3NF
- JSONB for flexible schema extension
- Support for read replicas through JPA configuration

## Data Integrity

### Referential Integrity
- Foreign key constraints with appropriate cascade rules
- Check constraints for business rules
- NOT NULL constraints for required fields
- Unique constraints for natural keys

### Business Rules
- Start time must be before end time for events
- Reminder minutes must be non-negative
- Todo status transitions follow business logic
- Parent-child relationships prevent circular dependencies
- Goal types restricted to valid periods
- Sync directions properly constrained

### Audit Trail
- `created_at` and `updated_at` timestamps on all entities
- Automatic timestamp updates via JPA annotations
- Security event logging for all authentication actions
- Token usage tracking

## Development and Testing

### Test Database
- H2 in-memory database for unit tests
- Separate Flyway migrations for test environment
- Test data fixtures for consistent testing

### Local Development
- PostgreSQL Docker containers for development
- Database seeding scripts for sample data
- Environment-specific configuration

This database design supports the Personal Hub application's comprehensive feature set including authentication, OAuth integration, goal tracking, calendar synchronization, and security auditing while maintaining good performance, security, and scalability characteristics.