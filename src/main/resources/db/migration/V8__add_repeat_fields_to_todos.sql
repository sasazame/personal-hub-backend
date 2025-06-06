-- Add repeat fields to todos table for repeatable functionality
ALTER TABLE todos 
    ADD COLUMN is_repeatable BOOLEAN DEFAULT FALSE,
    ADD COLUMN repeat_type VARCHAR(50),
    ADD COLUMN repeat_interval INTEGER DEFAULT 1,
    ADD COLUMN repeat_days_of_week VARCHAR(20),
    ADD COLUMN repeat_day_of_month INTEGER,
    ADD COLUMN repeat_end_date DATE,
    ADD COLUMN original_todo_id BIGINT;

-- Add check constraints for repeat_type
ALTER TABLE todos ADD CONSTRAINT chk_repeat_type
    CHECK (repeat_type IS NULL OR repeat_type IN ('DAILY', 'WEEKLY', 'MONTHLY', 'YEARLY', 'ONCE'));

-- Add check constraint for repeat_interval (must be positive)
ALTER TABLE todos ADD CONSTRAINT chk_repeat_interval
    CHECK (repeat_interval IS NULL OR repeat_interval > 0);

-- Add check constraint for repeat_days_of_week (comma-separated numbers 1-7)
ALTER TABLE todos ADD CONSTRAINT chk_repeat_days_of_week
    CHECK (repeat_days_of_week IS NULL OR repeat_days_of_week ~ '^[1-7](,[1-7])*$');

-- Add check constraint for repeat_day_of_month (1-31)
ALTER TABLE todos ADD CONSTRAINT chk_repeat_day_of_month
    CHECK (repeat_day_of_month IS NULL OR (repeat_day_of_month >= 1 AND repeat_day_of_month <= 31));

-- Add foreign key constraint for original_todo_id
ALTER TABLE todos
    ADD CONSTRAINT fk_todo_original
        FOREIGN KEY (original_todo_id)
            REFERENCES todos(id)
            ON DELETE CASCADE;

-- Add indexes for better query performance
CREATE INDEX idx_todos_is_repeatable ON todos(is_repeatable);
CREATE INDEX idx_todos_repeat_type ON todos(repeat_type);
CREATE INDEX idx_todos_original_todo_id ON todos(original_todo_id);
CREATE INDEX idx_todos_repeat_end_date ON todos(repeat_end_date);

-- Add comments for documentation
COMMENT ON COLUMN todos.is_repeatable IS 'Whether this todo has repeat settings';
COMMENT ON COLUMN todos.repeat_type IS 'Type of repetition: DAILY, WEEKLY, MONTHLY, YEARLY, ONCE';
COMMENT ON COLUMN todos.repeat_interval IS 'Interval for repetition (e.g., every 2 weeks)';
COMMENT ON COLUMN todos.repeat_days_of_week IS 'Days of week for weekly repeat (1=Monday, 7=Sunday)';
COMMENT ON COLUMN todos.repeat_day_of_month IS 'Day of month for monthly repeat (1-31)';
COMMENT ON COLUMN todos.repeat_end_date IS 'End date for repetition (null means no end)';
COMMENT ON COLUMN todos.original_todo_id IS 'Reference to the original repeatable todo';