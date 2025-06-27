-- Create goal table
CREATE TABLE goals (
    id BIGSERIAL PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    goal_type VARCHAR(20) NOT NULL CHECK (goal_type IN ('ANNUAL', 'MONTHLY', 'WEEKLY', 'DAILY')),
    metric_type VARCHAR(20) NOT NULL CHECK (metric_type IN ('COUNT', 'NUMERIC', 'PERCENTAGE', 'TIME')),
    metric_unit VARCHAR(50),
    target_value DECIMAL(10, 2) NOT NULL,
    current_value DECIMAL(10, 2) DEFAULT 0,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'COMPLETED', 'ARCHIVED', 'PAUSED')),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Create goal progress table for tracking daily progress
CREATE TABLE goal_progress (
    id BIGSERIAL PRIMARY KEY,
    goal_id BIGINT NOT NULL REFERENCES goals(id) ON DELETE CASCADE,
    date DATE NOT NULL,
    value DECIMAL(10, 2) NOT NULL,
    note TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(goal_id, date)
);

-- Create goal milestones table for tracking important checkpoints
CREATE TABLE goal_milestones (
    id BIGSERIAL PRIMARY KEY,
    goal_id BIGINT NOT NULL REFERENCES goals(id) ON DELETE CASCADE,
    title VARCHAR(255) NOT NULL,
    target_value DECIMAL(10, 2) NOT NULL,
    achieved BOOLEAN DEFAULT FALSE,
    achieved_date DATE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Add week_start_day to users table for configuring week start
ALTER TABLE users ADD COLUMN week_start_day INTEGER DEFAULT 1 CHECK (week_start_day >= 0 AND week_start_day <= 6);
-- 0 = Sunday, 1 = Monday, ..., 6 = Saturday

-- Create indexes for better performance
CREATE INDEX idx_goals_user_id ON goals(user_id);
CREATE INDEX idx_goals_status ON goals(status);
CREATE INDEX idx_goals_goal_type ON goals(goal_type);
CREATE INDEX idx_goal_progress_goal_id ON goal_progress(goal_id);
CREATE INDEX idx_goal_progress_date ON goal_progress(date);
CREATE INDEX idx_goal_milestones_goal_id ON goal_milestones(goal_id);

-- Add comments for documentation
COMMENT ON TABLE goals IS 'Stores user goals with different time periods and metric types';
COMMENT ON COLUMN goals.goal_type IS 'ANNUAL, MONTHLY, WEEKLY, or DAILY goal';
COMMENT ON COLUMN goals.metric_type IS 'COUNT (times), NUMERIC (with unit), PERCENTAGE (0-100), TIME (minutes)';
COMMENT ON COLUMN goals.metric_unit IS 'Unit for numeric metrics (e.g., kg, km, hours)';
COMMENT ON COLUMN users.week_start_day IS '0=Sunday, 1=Monday, ..., 6=Saturday';