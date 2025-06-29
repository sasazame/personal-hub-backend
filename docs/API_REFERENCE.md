# API Reference

## Base URL
```
http://localhost:8080/api/v1
```

## Authentication
Most endpoints require JWT authentication. Include in headers:
```
Authorization: Bearer YOUR_JWT_TOKEN
```

## Quick Reference

### 🔐 Authentication
| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| POST | `/auth/register` | Create new account | ❌ |
| POST | `/auth/login` | Login to get JWT | ❌ |
| POST | `/auth/refresh` | Refresh JWT token | ❌ |
| POST | `/auth/logout` | Logout user | ✅ |
| POST | `/auth/forgot-password` | Request password reset | ❌ |
| POST | `/auth/reset-password` | Reset password | ❌ |

### ✅ TODOs
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/todos` | List todos (paginated) |
| POST | `/todos` | Create new todo |
| GET | `/todos/{id}` | Get specific todo |
| PUT | `/todos/{id}` | Update todo |
| DELETE | `/todos/{id}` | Delete todo |
| POST | `/todos/{id}/toggle-status` | Toggle completion |
| GET | `/todos/{id}/children` | Get subtasks |

### 🎯 Goals
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/goals` | List goals |
| POST | `/goals` | Create goal |
| PUT | `/goals/{id}` | Update goal |
| DELETE | `/goals/{id}` | Delete goal |
| POST | `/goals/{id}/progress` | Update progress |

### 📅 Calendar
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/calendar/events` | List events |
| POST | `/calendar/events` | Create event |
| GET | `/calendar/events/{id}` | Get event |
| PUT | `/calendar/events/{id}` | Update event |
| DELETE | `/calendar/events/{id}` | Delete event |

### 📝 Notes
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/notes` | List notes |
| POST | `/notes` | Create note |
| GET | `/notes/{id}` | Get note |
| PUT | `/notes/{id}` | Update note |
| DELETE | `/notes/{id}` | Delete note |
| GET | `/notes/search` | Search notes |

### 📊 Analytics
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/analytics/dashboard` | Get dashboard data |
| GET | `/analytics/todos/activity` | Todo activity stats |

### 🔗 OAuth2
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/auth/oidc/google/authorize` | Start Google OAuth |
| POST | `/auth/oidc/google/callback` | Google callback |
| GET | `/auth/oidc/github/authorize` | Start GitHub OAuth |
| POST | `/auth/oidc/github/callback` | GitHub callback |

## Common Request Examples

### Register & Login
```bash
# Register
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@example.com",
    "password": "SecurePass123!",
    "username": "johndoe"
  }'

# Login
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@example.com",
    "password": "SecurePass123!"
  }'
```

### Create TODO
```bash
curl -X POST http://localhost:8080/api/v1/todos \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Complete project documentation",
    "description": "Write comprehensive API docs",
    "priority": "HIGH",
    "dueDate": "2025-07-01"
  }'
```

### Create Goal
```bash
curl -X POST http://localhost:8080/api/v1/goals \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Read 12 books",
    "description": "Read one book per month",
    "targetValue": 12,
    "currentValue": 0,
    "unit": "books",
    "period": "ANNUAL"
  }'
```

## Response Formats

### Success Response
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "title": "Complete project",
  "status": "TODO",
  "createdAt": "2025-06-29T10:00:00Z",
  "updatedAt": "2025-06-29T10:00:00Z"
}
```

### Error Response
```json
{
  "timestamp": "2025-06-29T10:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "path": "/api/v1/todos"
}
```

### Paginated Response
```json
{
  "content": [...],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 20,
    "sort": {
      "sorted": true,
      "descending": true
    }
  },
  "totalElements": 100,
  "totalPages": 5,
  "first": true,
  "last": false
}
```

## Query Parameters

### Pagination
```
?page=0&size=20&sort=createdAt,desc
```

### Filtering (TODOs)
```
?status=TODO&priority=HIGH
```

### Date Range
```
?startDate=2025-06-01&endDate=2025-06-30
```

## Data Types

### TODO
```json
{
  "title": "string (required)",
  "description": "string",
  "status": "TODO | IN_PROGRESS | DONE",
  "priority": "LOW | MEDIUM | HIGH",
  "dueDate": "2025-07-01",
  "parentId": "uuid (optional)"
}
```

### Goal
```json
{
  "title": "string (required)",
  "description": "string",
  "targetValue": "number",
  "currentValue": "number",
  "unit": "string",
  "period": "DAILY | WEEKLY | MONTHLY | ANNUAL",
  "startDate": "2025-01-01",
  "endDate": "2025-12-31"
}
```

### Calendar Event
```json
{
  "title": "string (required)",
  "description": "string",
  "startDateTime": "2025-07-01T10:00:00Z",
  "endDateTime": "2025-07-01T11:00:00Z",
  "location": "string",
  "reminderMinutes": 15
}
```

### Note
```json
{
  "title": "string (required)",
  "content": "string (markdown supported)",
  "tags": ["tag1", "tag2"]
}
```

## Rate Limiting

- **Authentication endpoints**: 10 requests/minute per IP
- **General API**: 100 requests/minute per user

## Status Codes

- `200` OK - Request successful
- `201` Created - Resource created
- `204` No Content - Deletion successful
- `400` Bad Request - Invalid input
- `401` Unauthorized - Auth required
- `403` Forbidden - Access denied
- `404` Not Found - Resource not found
- `429` Too Many Requests - Rate limit exceeded
- `500` Internal Server Error

---

📚 For detailed API documentation with all request/response schemas, see [Full API Documentation](API.md)