# Pomodoro Timer Feature

## Overview
The Pomodoro feature implements a time management system based on the Pomodoro Technique, allowing users to work in focused intervals with scheduled breaks.

## Core Concepts

### Sessions
- **Work Period**: Default 25 minutes of focused work
- **Short Break**: Default 5 minutes after each work period
- **Long Break**: Default 15 minutes after 4 work cycles

### Session States
- `ACTIVE`: Timer is running
- `PAUSED`: Timer is paused
- `COMPLETED`: Session finished successfully
- `CANCELLED`: Session terminated early

### Session Types
- `WORK`: Active work period
- `SHORT_BREAK`: Regular break between work periods
- `LONG_BREAK`: Extended break after completing cycles

## API Endpoints

### Session Management
| Method | Endpoint | Purpose |
|--------|----------|---------|
| POST | `/api/v1/pomodoro/sessions` | Create new session |
| PUT | `/api/v1/pomodoro/sessions/{id}` | Update session (start/pause/complete) |
| GET | `/api/v1/pomodoro/sessions/active` | Get current active session |
| GET | `/api/v1/pomodoro/sessions` | Get session history (paginated) |

### Task Management
| Method | Endpoint | Purpose |
|--------|----------|---------|
| POST | `/api/v1/pomodoro/sessions/{sessionId}/tasks` | Add task to session |
| PUT | `/api/v1/pomodoro/sessions/{sessionId}/tasks/{taskId}` | Update task status |
| DELETE | `/api/v1/pomodoro/sessions/{sessionId}/tasks/{taskId}` | Remove task from session |
| GET | `/api/v1/pomodoro/sessions/{sessionId}/tasks` | Get tasks for a session |

### Configuration
| Method | Endpoint | Purpose |
|--------|----------|---------|
| GET | `/api/v1/pomodoro/config` | Get user preferences |
| PUT | `/api/v1/pomodoro/config` | Update preferences |

## Data Models

### PomodoroSession
```
- id: UUID
- userId: UUID
- startTime: DateTime
- endTime: DateTime
- workDuration: Integer (minutes)
- breakDuration: Integer (minutes)
- completedCycles: Integer
- status: SessionStatus
- sessionType: SessionType
- tasks: List<PomodoroTask>
```

### PomodoroConfig
```
- workDuration: Integer (default: 25)
- shortBreakDuration: Integer (default: 5)
- longBreakDuration: Integer (default: 15)
- cyclesBeforeLongBreak: Integer (default: 4)
- alarmSound: String
- alarmVolume: Integer (0-100)
- autoStartBreaks: Boolean
- autoStartWork: Boolean
```

### PomodoroTask
```
- id: UUID
- sessionId: UUID
- todoId: UUID (optional)
- description: String
- completed: Boolean
```

## Usage Flow

1. **Start Session**: Create a new pomodoro session with initial tasks
2. **Work Period**: Timer counts down from configured work duration
3. **Break Period**: Automatically or manually switch to break after work
4. **Complete Cycle**: After 4 work periods, take a long break
5. **Track Progress**: Mark tasks as completed during session
6. **Session History**: Review past sessions and productivity metrics

## Configuration Options

### Timer Durations
- Work duration: 1-60 minutes
- Short break: 1-30 minutes
- Long break: 1-60 minutes
- Cycles before long break: 1-10

### Auto-start Settings
- Auto-start breaks: Begin break immediately after work
- Auto-start work: Begin work immediately after break

### Notification Settings
- Alarm sound selection
- Volume control (0-100%)

## Session Actions

- `START`: Begin timer countdown
- `PAUSE`: Temporarily stop timer
- `RESUME`: Continue from paused state
- `COMPLETE`: Mark current period as done
- `CANCEL`: Terminate session
- `SWITCH_TYPE`: Change between work/break