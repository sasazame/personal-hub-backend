-- Create new achievement history table
CREATE TABLE goal_achievement_history (
    id BIGSERIAL PRIMARY KEY,
    goal_id BIGINT NOT NULL,
    achieved_date DATE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_goal_date UNIQUE (goal_id, achieved_date),
    CONSTRAINT fk_goal_achievement_goal FOREIGN KEY (goal_id) REFERENCES goals(id) ON DELETE CASCADE
);

-- Add new columns to goals table
ALTER TABLE goals 
  ADD COLUMN is_active BOOLEAN DEFAULT TRUE;

-- Update existing goals with active status
UPDATE goals SET 
  is_active = (status = 'ACTIVE');

-- Make is_active column non-nullable
ALTER TABLE goals 
  ALTER COLUMN is_active SET NOT NULL;

-- Drop deprecated columns from goals table
ALTER TABLE goals 
  DROP COLUMN status,
  DROP COLUMN metric_type,
  DROP COLUMN metric_unit,
  DROP COLUMN target_value,
  DROP COLUMN current_value;

-- Drop old tables
DROP TABLE IF EXISTS goal_milestones;
DROP TABLE IF EXISTS goal_progress;
DROP TABLE IF EXISTS goal_streaks;

-- Create indexes for performance
CREATE INDEX idx_goal_achievement_goal_date ON goal_achievement_history(goal_id, achieved_date);
CREATE INDEX idx_goals_user_type ON goals(user_id, goal_type);
CREATE INDEX idx_goals_dates ON goals(start_date, end_date);