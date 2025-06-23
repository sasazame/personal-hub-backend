# API Specification

## Overview
RESTful API specification for Personal Hub application
Integrated personal management system providing TODO, calendar, note, and analytics features

## Base Information
- **Base URL**: `http://localhost:8080/api/v1`
- **Data Format**: JSON
- **Character Encoding**: UTF-8
- **Authentication**: JWT Bearer Token (except for certain endpoints)

## Common Specifications

### HTTP Status Codes
| Code | Description |
|------|-------------|
| 200 | OK - Success |
| 201 | Created - Creation successful |
| 204 | No Content - Deletion successful |
| 400 | Bad Request - Request error |
| 401 | Unauthorized - Authentication required |
| 403 | Forbidden - Access denied |
| 404 | Not Found - Resource not found |
| 409 | Conflict - Data conflict error |
| 500 | Internal Server Error - Server error |

### Authentication Header
For endpoints requiring authentication, include the following header:
```
Authorization: Bearer {JWT_TOKEN}
```

### Error Response Format
```json
{
  "timestamp": "2025-06-23T12:34:56.789Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "path": "/api/v1/todos"
}
```

### Pagination Parameters
For paginated endpoints:
```
?page=0&size=20&sort=createdAt,desc
```

## Authentication Endpoints

### User Registration
**Endpoint**: `POST /auth/register`
**Authentication**: Not required

**Request Body**:
```json
{
  "username": "john_doe",
  "email": "john@example.com",
  "password": "SecurePass123!"
}
```

**Response** (201 Created):
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "user": {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "username": "john_doe",
    "email": "john@example.com",
    "createdAt": "2025-06-23T12:34:56.789Z",
    "updatedAt": "2025-06-23T12:34:56.789Z"
  }
}
```

### User Login
**Endpoint**: `POST /auth/login`
**Authentication**: Not required

**Request Body**:
```json
{
  "email": "john@example.com",
  "password": "SecurePass123!"
}
```

**Response** (200 OK):
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "user": {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "username": "john_doe",
    "email": "john@example.com",
    "createdAt": "2025-06-23T12:34:56.789Z",
    "updatedAt": "2025-06-23T12:34:56.789Z"
  }
}
```

### Token Refresh
**Endpoint**: `POST /auth/refresh`
**Authentication**: Not required

**Request Body**:
```json
{
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

**Response** (200 OK):
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "user": {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "username": "john_doe",
    "email": "john@example.com",
    "createdAt": "2025-06-23T12:34:56.789Z",
    "updatedAt": "2025-06-23T12:34:56.789Z"
  }
}
```

### Get Current User
**Endpoint**: `GET /auth/me`
**Authentication**: Required

**Response** (200 OK):
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "username": "john_doe",
  "email": "john@example.com",
  "createdAt": "2025-06-23T12:34:56.789Z",
  "updatedAt": "2025-06-23T12:34:56.789Z"
}
```

## OAuth2 Endpoints

### OAuth2 UserInfo
**Endpoint**: `GET /oauth2/userinfo`
**Authentication**: Required

**Response** (200 OK):
```json
{
  "sub": "550e8400-e29b-41d4-a716-446655440000",
  "email": "john@example.com",
  "email_verified": true,
  "name": "John Doe",
  "given_name": "John",
  "family_name": "Doe",
  "picture": "https://example.com/avatar.jpg",
  "locale": "en"
}
```

## TODO Endpoints

### Create TODO
**Endpoint**: `POST /todos`
**Authentication**: Required

**Request Body**:
```json
{
  "title": "Complete project documentation",
  "description": "Write comprehensive API documentation",
  "priority": "HIGH",
  "dueDate": "2025-06-30",
  "parentId": null,
  "isRepeatable": false,
  "repeatConfig": null
}
```

**Response** (201 Created):
```json
{
  "id": 123,
  "title": "Complete project documentation",
  "description": "Write comprehensive API documentation",
  "status": "TODO",
  "priority": "HIGH",
  "dueDate": "2025-06-30",
  "parentId": null,
  "isRepeatable": false,
  "repeatConfig": null,
  "originalTodoId": null,
  "createdAt": "2025-06-23T12:34:56.789Z",
  "updatedAt": "2025-06-23T12:34:56.789Z"
}
```

### Get TODO List (Paginated)
**Endpoint**: `GET /todos`
**Authentication**: Required

**Query Parameters**:
- `page` (optional): Page number (default: 0)
- `size` (optional): Page size (default: 20)
- `sort` (optional): Sort criteria (default: createdAt,desc)

**Response** (200 OK):
```json
{
  "content": [
    {
      "id": 123,
      "title": "Complete project documentation",
      "description": "Write comprehensive API documentation",
      "status": "TODO",
      "priority": "HIGH",
      "dueDate": "2025-06-30",
      "parentId": null,
      "isRepeatable": false,
      "repeatConfig": null,
      "originalTodoId": null,
      "createdAt": "2025-06-23T12:34:56.789Z",
      "updatedAt": "2025-06-23T12:34:56.789Z"
    }
  ],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 20,
    "sort": {
      "sorted": true,
      "unsorted": false,
      "empty": false
    }
  },
  "totalPages": 1,
  "totalElements": 1,
  "last": true,
  "first": true,
  "size": 20,
  "number": 0,
  "numberOfElements": 1,
  "empty": false
}
```

### Get TODO by ID
**Endpoint**: `GET /todos/{id}`
**Authentication**: Required

**Response** (200 OK):
```json
{
  "id": 123,
  "title": "Complete project documentation",
  "description": "Write comprehensive API documentation",
  "status": "TODO",
  "priority": "HIGH",
  "dueDate": "2025-06-30",
  "parentId": null,
  "isRepeatable": false,
  "repeatConfig": null,
  "originalTodoId": null,
  "createdAt": "2025-06-23T12:34:56.789Z",
  "updatedAt": "2025-06-23T12:34:56.789Z"
}
```

### Update TODO
**Endpoint**: `PUT /todos/{id}`
**Authentication**: Required

**Request Body**:
```json
{
  "title": "Complete project documentation (Updated)",
  "description": "Write comprehensive API documentation with examples",
  "status": "IN_PROGRESS",
  "priority": "HIGH",
  "dueDate": "2025-06-30",
  "parentId": null,
  "isRepeatable": false,
  "repeatConfig": null
}
```

**Response** (200 OK):
```json
{
  "id": 123,
  "title": "Complete project documentation (Updated)",
  "description": "Write comprehensive API documentation with examples",
  "status": "IN_PROGRESS",
  "priority": "HIGH",
  "dueDate": "2025-06-30",
  "parentId": null,
  "isRepeatable": false,
  "repeatConfig": null,
  "originalTodoId": null,
  "createdAt": "2025-06-23T12:34:56.789Z",
  "updatedAt": "2025-06-23T13:45:12.456Z"
}
```

### Delete TODO
**Endpoint**: `DELETE /todos/{id}`
**Authentication**: Required

**Response** (204 No Content)

### Get TODOs by Status
**Endpoint**: `GET /todos/status/{status}`
**Authentication**: Required

**Path Parameters**:
- `status`: TODO, IN_PROGRESS, or DONE

**Response** (200 OK):
```json
[
  {
    "id": 123,
    "title": "Complete project documentation",
    "description": "Write comprehensive API documentation",
    "status": "TODO",
    "priority": "HIGH",
    "dueDate": "2025-06-30",
    "parentId": null,
    "isRepeatable": false,
    "repeatConfig": null,
    "originalTodoId": null,
    "createdAt": "2025-06-23T12:34:56.789Z",
    "updatedAt": "2025-06-23T12:34:56.789Z"
  }
]
```

### Get Child Tasks
**Endpoint**: `GET /todos/{parentId}/children`
**Authentication**: Required

**Response** (200 OK):
```json
[
  {
    "id": 124,
    "title": "Write API endpoint documentation",
    "description": "Document all REST endpoints",
    "status": "TODO",
    "priority": "MEDIUM",
    "dueDate": "2025-06-28",
    "parentId": 123,
    "isRepeatable": false,
    "repeatConfig": null,
    "originalTodoId": null,
    "createdAt": "2025-06-23T12:34:56.789Z",
    "updatedAt": "2025-06-23T12:34:56.789Z"
  }
]
```

### Toggle TODO Status
**Endpoint**: `POST /todos/{id}/toggle-status`
**Authentication**: Required

**Response** (200 OK):
```json
{
  "id": 123,
  "title": "Complete project documentation",
  "description": "Write comprehensive API documentation",
  "status": "DONE",
  "priority": "HIGH",
  "dueDate": "2025-06-30",
  "parentId": null,
  "isRepeatable": false,
  "repeatConfig": null,
  "originalTodoId": null,
  "createdAt": "2025-06-23T12:34:56.789Z",
  "updatedAt": "2025-06-23T13:45:12.456Z"
}
```

### Get Repeatable TODOs
**Endpoint**: `GET /todos/repeatable`
**Authentication**: Required

**Response** (200 OK):
```json
[
  {
    "id": 125,
    "title": "Daily standup meeting",
    "description": "Team synchronization meeting",
    "status": "TODO",
    "priority": "MEDIUM",
    "dueDate": null,
    "parentId": null,
    "isRepeatable": true,
    "repeatConfig": {
      "repeatType": "DAILY",
      "interval": 1,
      "daysOfWeek": null,
      "dayOfMonth": null,
      "endDate": null
    },
    "originalTodoId": null,
    "createdAt": "2025-06-23T12:34:56.789Z",
    "updatedAt": "2025-06-23T12:34:56.789Z"
  }
]
```

### Generate Pending Repeat Instances
**Endpoint**: `POST /todos/repeat/generate`
**Authentication**: Required

**Response** (201 Created):
```json
[
  {
    "id": 126,
    "title": "Daily standup meeting",
    "description": "Team synchronization meeting",
    "status": "TODO",
    "priority": "MEDIUM",
    "dueDate": "2025-06-24",
    "parentId": null,
    "isRepeatable": false,
    "repeatConfig": null,
    "originalTodoId": 125,
    "createdAt": "2025-06-23T12:34:56.789Z",
    "updatedAt": "2025-06-23T12:34:56.789Z"
  }
]
```

## Calendar Endpoints

### Create Event
**Endpoint**: `POST /calendar/events`
**Authentication**: Required

**Request Body**:
```json
{
  "title": "Team Meeting",
  "description": "Weekly team synchronization",
  "startTime": "2025-06-24T10:00:00Z",
  "endTime": "2025-06-24T11:00:00Z",
  "location": "Conference Room A",
  "allDay": false,
  "reminderMinutes": 15
}
```

**Response** (201 Created):
```json
{
  "id": 456,
  "title": "Team Meeting",
  "description": "Weekly team synchronization",
  "startTime": "2025-06-24T10:00:00Z",
  "endTime": "2025-06-24T11:00:00Z",
  "location": "Conference Room A",
  "allDay": false,
  "reminderMinutes": 15,
  "createdAt": "2025-06-23T12:34:56.789Z",
  "updatedAt": "2025-06-23T12:34:56.789Z"
}
```

### Get Events
**Endpoint**: `GET /calendar/events`
**Authentication**: Required

**Query Parameters**:
- `start` (optional): Start date (ISO 8601)
- `end` (optional): End date (ISO 8601)
- `page` (optional): Page number
- `size` (optional): Page size

**Response** (200 OK):
```json
{
  "content": [
    {
      "id": 456,
      "title": "Team Meeting",
      "description": "Weekly team synchronization",
      "startTime": "2025-06-24T10:00:00Z",
      "endTime": "2025-06-24T11:00:00Z",
      "location": "Conference Room A",
      "allDay": false,
      "reminderMinutes": 15,
      "createdAt": "2025-06-23T12:34:56.789Z",
      "updatedAt": "2025-06-23T12:34:56.789Z"
    }
  ],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 20
  },
  "totalElements": 1
}
```

## Note Endpoints

### Create Note
**Endpoint**: `POST /notes`
**Authentication**: Required

**Request Body**:
```json
{
  "title": "Meeting Notes",
  "content": "# Team Meeting Notes\n\n## Action Items\n- Follow up on project X\n- Review documentation",
  "tags": ["meeting", "project"],
  "isPublic": false
}
```

**Response** (201 Created):
```json
{
  "id": 789,
  "title": "Meeting Notes",
  "content": "# Team Meeting Notes\n\n## Action Items\n- Follow up on project X\n- Review documentation",
  "tags": ["meeting", "project"],
  "isPublic": false,
  "createdAt": "2025-06-23T12:34:56.789Z",
  "updatedAt": "2025-06-23T12:34:56.789Z"
}
```

### Search Notes
**Endpoint**: `GET /notes/search`
**Authentication**: Required

**Query Parameters**:
- `q`: Search query
- `tags` (optional): Comma-separated tag list
- `page` (optional): Page number
- `size` (optional): Page size

**Response** (200 OK):
```json
{
  "content": [
    {
      "id": 789,
      "title": "Meeting Notes",
      "content": "# Team Meeting Notes\n\n## Action Items\n- Follow up on project X\n- Review documentation",
      "tags": ["meeting", "project"],
      "isPublic": false,
      "createdAt": "2025-06-23T12:34:56.789Z",
      "updatedAt": "2025-06-23T12:34:56.789Z"
    }
  ],
  "totalElements": 1
}
```

## Analytics Endpoints

### Get Dashboard
**Endpoint**: `GET /analytics/dashboard`
**Authentication**: Required

**Response** (200 OK):
```json
{
  "totalTodos": 25,
  "completedTodos": 18,
  "completionRate": 72.0,
  "upcomingEvents": 3,
  "totalNotes": 12,
  "productivityScore": 85.5,
  "weeklyActivity": [
    {"date": "2025-06-17", "completedTasks": 3},
    {"date": "2025-06-18", "completedTasks": 5},
    {"date": "2025-06-19", "completedTasks": 2},
    {"date": "2025-06-20", "completedTasks": 4},
    {"date": "2025-06-21", "completedTasks": 3},
    {"date": "2025-06-22", "completedTasks": 1},
    {"date": "2025-06-23", "completedTasks": 0}
  ]
}
```

### Get TODO Activity
**Endpoint**: `GET /analytics/todos/activity`
**Authentication**: Required

**Query Parameters**:
- `period` (optional): DAY, WEEK, MONTH (default: WEEK)
- `startDate` (optional): Start date (ISO 8601)
- `endDate` (optional): End date (ISO 8601)

**Response** (200 OK):
```json
{
  "period": "WEEK",
  "startDate": "2025-06-17",
  "endDate": "2025-06-23",
  "totalCreated": 8,
  "totalCompleted": 18,
  "dailyActivity": [
    {
      "date": "2025-06-17",
      "created": 2,
      "completed": 3,
      "pending": 5
    },
    {
      "date": "2025-06-18",
      "created": 1,
      "completed": 5,
      "pending": 3
    }
  ],
  "priorityBreakdown": {
    "HIGH": 5,
    "MEDIUM": 12,
    "LOW": 8
  },
  "statusBreakdown": {
    "TODO": 7,
    "IN_PROGRESS": 3,
    "DONE": 15
  }
}
```

## Data Types

### TODO Priority
- `LOW`
- `MEDIUM`
- `HIGH`

### TODO Status
- `TODO`
- `IN_PROGRESS`
- `DONE`

### Repeat Types
- `DAILY`
- `WEEKLY`
- `MONTHLY`
- `YEARLY`

### Repeat Configuration
```json
{
  "repeatType": "WEEKLY",
  "interval": 1,
  "daysOfWeek": "MON,WED,FRI",
  "dayOfMonth": null,
  "endDate": "2025-12-31"
}
```

## Error Handling

### Validation Errors (400 Bad Request)
```json
{
  "timestamp": "2025-06-23T12:34:56.789Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "path": "/api/v1/todos",
  "validationErrors": [
    {
      "field": "title",
      "message": "Title is required and cannot be empty"
    },
    {
      "field": "priority",
      "message": "Priority must be one of: LOW, MEDIUM, HIGH"
    }
  ]
}
```

### Authentication Errors (401 Unauthorized)
```json
{
  "timestamp": "2025-06-23T12:34:56.789Z",
  "status": 401,
  "error": "Unauthorized",
  "message": "JWT token is expired or invalid",
  "path": "/api/v1/todos"
}
```

### Access Denied Errors (403 Forbidden)
```json
{
  "timestamp": "2025-06-23T12:34:56.789Z",
  "status": 403,
  "error": "Forbidden",
  "message": "Access denied to TODO with id: 123",
  "path": "/api/v1/todos/123"
}
```

### Resource Not Found (404 Not Found)
```json
{
  "timestamp": "2025-06-23T12:34:56.789Z",
  "status": 404,
  "error": "Not Found",
  "message": "TODO not found with id: 999",
  "path": "/api/v1/todos/999"
}
```

## Rate Limiting

### Limits
- **Authentication endpoints**: 1000 requests per minute per IP
- **General endpoints**: 1000 requests per minute per IP

### Rate Limit Headers
When rate limits are exceeded, the following headers are included:
```
X-RateLimit-Remaining: 0
X-RateLimit-Reset: 1640995200
```

### Rate Limit Error (429 Too Many Requests)
```json
{
  "timestamp": "2025-06-23T12:34:56.789Z",
  "status": 429,
  "error": "Too Many Requests",
  "message": "Rate limit exceeded. Please try again later.",
  "path": "/api/v1/auth/login"
}
```

## CORS Support

The API supports Cross-Origin Resource Sharing (CORS) for the following origins:
- `http://localhost:3000` (React default)
- `http://localhost:3001` (Alternative React port)
- `http://localhost:5173` (Vite default)
- `http://localhost:4173` (Vite preview)
- `http://127.0.0.1:3000`, `http://127.0.0.1:3001`, `http://127.0.0.1:5173`

### Allowed Methods
- GET, POST, PUT, DELETE, OPTIONS, PATCH

### Allowed Headers
- Authorization, Content-Type, Accept, X-XSRF-TOKEN, X-Requested-With

This API specification provides comprehensive coverage of all available endpoints and their usage patterns for the Personal Hub application.