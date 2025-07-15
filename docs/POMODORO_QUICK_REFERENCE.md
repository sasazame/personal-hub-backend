# Pomodoro Quick Reference

## Default Timer Settings
- **Work**: 25 minutes
- **Short Break**: 5 minutes  
- **Long Break**: 15 minutes (after 4 cycles)

## API Quick Reference

### Essential Operations
```bash
# Create session
POST /api/pomodoro/sessions
{
  "workDuration": 25,
  "breakDuration": 5,
  "tasks": [{"todoId": "...", "description": "..."}]
}

# Control session
PUT /api/pomodoro/sessions/{id}
{ "action": "START|PAUSE|RESUME|COMPLETE|CANCEL" }

# Get active session
GET /api/pomodoro/sessions/active

# Update configuration
PUT /api/pomodoro/config
{ "workDuration": 30, "autoStartBreaks": true }
```

## Session Lifecycle
1. `CREATE` → New session with ACTIVE status
2. `START` → Timer begins countdown
3. `PAUSE/RESUME` → Control timer as needed
4. `COMPLETE` → Finish current period, switch work/break
5. `CANCEL` → End session early

## Key Features
- Link tasks to existing todos
- Track completed work cycles
- Configurable timer durations
- Auto-start next period option
- Session history with analytics
- Custom alarm sounds and volume