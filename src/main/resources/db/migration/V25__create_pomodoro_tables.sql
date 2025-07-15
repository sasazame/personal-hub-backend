-- Create pomodoro_sessions table
CREATE TABLE pomodoro_sessions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    start_time TIMESTAMP,
    end_time TIMESTAMP,
    work_duration INTEGER NOT NULL,
    break_duration INTEGER NOT NULL,
    completed_cycles INTEGER NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL CHECK (status IN ('ACTIVE', 'PAUSED', 'COMPLETED', 'CANCELLED')),
    session_type VARCHAR(20) NOT NULL CHECK (session_type IN ('WORK', 'SHORT_BREAK', 'LONG_BREAK')),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create pomodoro_tasks table
CREATE TABLE pomodoro_tasks (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    session_id UUID NOT NULL REFERENCES pomodoro_sessions(id) ON DELETE CASCADE,
    todo_id BIGINT REFERENCES todos(id) ON DELETE SET NULL,
    description VARCHAR(500) NOT NULL,
    completed BOOLEAN NOT NULL DEFAULT FALSE,
    order_index INTEGER NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create pomodoro_configs table
CREATE TABLE pomodoro_configs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    work_duration INTEGER NOT NULL DEFAULT 25,
    short_break_duration INTEGER NOT NULL DEFAULT 5,
    long_break_duration INTEGER NOT NULL DEFAULT 15,
    cycles_before_long_break INTEGER NOT NULL DEFAULT 4,
    alarm_sound VARCHAR(50) NOT NULL DEFAULT 'default',
    alarm_volume INTEGER NOT NULL DEFAULT 50 CHECK (alarm_volume >= 0 AND alarm_volume <= 100),
    auto_start_breaks BOOLEAN NOT NULL DEFAULT TRUE,
    auto_start_work BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes
CREATE INDEX idx_pomodoro_sessions_user_id ON pomodoro_sessions(user_id);
CREATE INDEX idx_pomodoro_sessions_status ON pomodoro_sessions(status);
CREATE INDEX idx_pomodoro_sessions_start_time ON pomodoro_sessions(start_time);
CREATE INDEX idx_pomodoro_tasks_session_id ON pomodoro_tasks(session_id);
CREATE INDEX idx_pomodoro_tasks_todo_id ON pomodoro_tasks(todo_id);
CREATE INDEX idx_pomodoro_configs_user_id ON pomodoro_configs(user_id);

-- Add trigger to update updated_at timestamp
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_pomodoro_sessions_updated_at BEFORE UPDATE ON pomodoro_sessions
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_pomodoro_tasks_updated_at BEFORE UPDATE ON pomodoro_tasks
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_pomodoro_configs_updated_at BEFORE UPDATE ON pomodoro_configs
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();