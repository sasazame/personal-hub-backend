# Database Design Document

## Overview
Database design for Personal Hub integrated application using PostgreSQL 16
Comprehensive management of TODO, calendar, and note features

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
│ given_name (VARCHAR(100))                  │
│ family_name (VARCHAR(100))                 │
│ locale (VARCHAR(10))                       │
│ profile_picture_url (TEXT)                 │
│ created_at (TIMESTAMP) NOT NULL            │
│ updated_at (TIMESTAMP) NOT NULL            │
└─────────────────────────────────────────────┘
         │              │              │
         │              │              │
┌────────┴────────┐ ┌───┴─────────┐ ┌──┴───────────┐
│      todos      │ │ calendar_   │ │    notes     │
│                 │ │   events    │ │              │
├─────────────────┤ ├─────────────┤ ├──────────────┤
│ id (BIGSERIAL)  │ │ id (BIGINT) │ │ id (BIGINT)  │
│ user_id (UUID)  │ │ user_id     │ │ user_id      │
│ title           │ │ title       │ │ title        │
│ description     │ │ description │ │ content      │
│ status          │ │ start_time  │ │ tags         │
│ priority        │ │ end_time    │ │ created_at   │
│ due_date        │ │ location    │ │ updated_at   │
│ parent_id       │ │ created_at  │ │              │
│ created_at      │ │ updated_at  │ │              │
│ updated_at      │ │             │ │              │
└─────────────────┘ └─────────────┘ └──────────────┘
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
| given_name | VARCHAR(100) | NULLABLE | First name (from OAuth) |
| family_name | VARCHAR(100) | NULLABLE | Last name (from OAuth) |
| locale | VARCHAR(10) | NULLABLE | User's locale preference |
| profile_picture_url | TEXT | NULLABLE | Profile image URL |
| created_at | TIMESTAMP | NOT NULL | Account creation timestamp |
| updated_at | TIMESTAMP | NOT NULL | Last update timestamp |

**Indexes**:
- Primary: `users_pkey` on `id`
- Unique: `users_username_key` on `username`
- Unique: `users_email_key` on `email`

**Special Considerations**:
- Password is nullable for OAuth/social login users
- UUID used for better security and distribution
- Support for OAuth user profile information

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

**Foreign Keys**:
- `fk_todos_user_id`: `user_id` → `users(id)` ON DELETE CASCADE
- `fk_todos_parent_id`: `parent_id` → `todos(id)` ON DELETE SET NULL
- `fk_todos_original_todo_id`: `original_todo_id` → `todos(id)` ON DELETE SET NULL

### calendar_events Table
**Purpose**: Calendar event management

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
| created_at | TIMESTAMP | NOT NULL | Creation timestamp |
| updated_at | TIMESTAMP | NOT NULL | Last update timestamp |

**Indexes**:
- Primary: `calendar_events_pkey` on `id`
- Index: `idx_calendar_events_user_id` on `user_id`
- Index: `idx_calendar_events_start_time` on `start_time`

**Constraints**:
- Check: `start_time < end_time`
- Check: `reminder_minutes >= 0`

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

## Security Design

### Data Isolation
- All tables include `user_id` foreign key for row-level security
- Application-level enforcement ensures users can only access their own data
- UUID for user IDs prevents enumeration attacks

### Password Security
- BCrypt hashing for password storage
- Nullable password column supports OAuth users without passwords
- Password field never exposed in API responses

### Access Control
- Foreign key constraints ensure data integrity
- Cascade deletion for user data cleanup
- Proper indexing for efficient user-based queries

## Migration Strategy

### Flyway Integration
- Version-controlled schema migrations in `src/main/resources/db/migration/`
- Naming convention: `V{version}__{description}.sql`
- Automatic migration execution on application startup

### Key Migrations
- `V1__Initial_schema.sql`: Initial table creation
- `V18__make_password_nullable_for_oauth_users.sql`: OAuth support
- Additional migrations for repeatable todos and calendar features

## Performance Considerations

### Indexing Strategy
- Primary keys for all tables
- Foreign key indexes for join performance
- Composite indexes for common query patterns
- GIN indexes for array and full-text search

### Query Optimization
- Pagination support for large datasets
- Efficient user-based filtering
- Proper use of LIMIT and OFFSET
- Connection pooling with HikariCP

### Scalability
- UUID primary keys for distributed systems
- Proper normalization to 3NF
- Efficient date-based queries with proper indexing
- Support for read replicas through JPA configuration

## Data Integrity

### Referential Integrity
- Foreign key constraints with appropriate cascade rules
- Check constraints for business rules
- NOT NULL constraints for required fields

### Business Rules
- Start time must be before end time for events
- Reminder minutes must be non-negative
- Todo status transitions follow business logic
- Parent-child relationships prevent circular dependencies

### Audit Trail
- `created_at` and `updated_at` timestamps on all entities
- Automatic timestamp updates via JPA annotations
- Soft deletes possible through status fields where needed

## Development and Testing

### Test Database
- H2 in-memory database for unit tests
- Separate Flyway migrations for test environment
- Test data fixtures for consistent testing

### Local Development
- PostgreSQL Docker containers for development
- Database seeding scripts for sample data
- Environment-specific configuration

This database design supports the Personal Hub application's requirements while maintaining good performance, security, and scalability characteristics.