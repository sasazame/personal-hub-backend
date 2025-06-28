-- Create new achievement history table
CREATE TABLE goal_achievement_history (
    id VARCHAR(36) PRIMARY KEY,
    goal_id VARCHAR(36) NOT NULL,
    achieved_date DATE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_goal_date (goal_id, achieved_date),
    FOREIGN KEY (goal_id) REFERENCES goals(id) ON DELETE CASCADE
);

-- Add new columns to goals table
ALTER TABLE goals 
  ADD COLUMN is_active BOOLEAN DEFAULT TRUE,
  ADD COLUMN start_date DATE,
  ADD COLUMN end_date DATE;

-- Update existing goals with default dates
UPDATE goals SET 
  is_active = (status = 'ACTIVE'),
  start_date = DATE_FORMAT(created_at, '%Y-01-01'),
  end_date = DATE_FORMAT(created_at, '%Y-12-31')
WHERE start_date IS NULL OR end_date IS NULL;

-- Make columns non-nullable
ALTER TABLE goals 
  MODIFY is_active BOOLEAN NOT NULL,
  MODIFY start_date DATE NOT NULL,
  MODIFY end_date DATE NOT NULL;

-- Change id column to VARCHAR for UUID
ALTER TABLE goals DROP FOREIGN KEY IF EXISTS fk_goals_user_id;
ALTER TABLE goal_milestones DROP FOREIGN KEY IF EXISTS fk_goal_milestones_goal_id;
ALTER TABLE goal_progress DROP FOREIGN KEY IF EXISTS fk_goal_progress_goal_id;
ALTER TABLE goal_streaks DROP FOREIGN KEY IF EXISTS fk_goal_streaks_goal_id;

-- Create new goals table with UUID id
CREATE TABLE goals_new (
    id VARCHAR(36) PRIMARY KEY,
    user_id CHAR(36) NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    goal_type VARCHAR(50) NOT NULL,
    is_active BOOLEAN NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_active (user_id, is_active),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Copy data from old table to new
INSERT INTO goals_new (id, user_id, title, description, goal_type, is_active, start_date, end_date, created_at, updated_at)
SELECT 
    UUID() as id,
    user_id,
    title,
    description,
    goal_type,
    is_active,
    start_date,
    end_date,
    created_at,
    updated_at
FROM goals;

-- Drop old tables
DROP TABLE IF EXISTS goal_milestones;
DROP TABLE IF EXISTS goal_progress;
DROP TABLE IF EXISTS goal_streaks;
DROP TABLE goals;

-- Rename new table
RENAME TABLE goals_new TO goals;

-- Create indexes for performance
CREATE INDEX idx_goal_achievement_goal_date ON goal_achievement_history(goal_id, achieved_date);
CREATE INDEX idx_goals_user_type ON goals(user_id, goal_type);
CREATE INDEX idx_goals_dates ON goals(start_date, end_date);