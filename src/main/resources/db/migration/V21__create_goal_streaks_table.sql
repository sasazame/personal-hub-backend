-- Create goal_streaks table for tracking consecutive achievements
CREATE TABLE goal_streaks (
    id BIGSERIAL PRIMARY KEY,
    goal_id BIGINT NOT NULL REFERENCES goals(id) ON DELETE CASCADE,
    current_streak INT DEFAULT 0,
    longest_streak INT DEFAULT 0,
    last_achieved_date DATE,
    streak_broken_date DATE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_goal_streaks_goal_id UNIQUE (goal_id)
);

-- Create index for faster lookups
CREATE INDEX idx_goal_streaks_goal_id ON goal_streaks(goal_id);