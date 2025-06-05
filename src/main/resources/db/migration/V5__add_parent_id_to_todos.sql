-- Add parent_id column to support hierarchical task structure
ALTER TABLE todos
    ADD COLUMN parent_id BIGINT;

-- Add foreign key constraint to ensure parent_id references existing todos
ALTER TABLE todos
    ADD CONSTRAINT fk_todo_parent
        FOREIGN KEY (parent_id)
            REFERENCES todos(id)
            ON DELETE CASCADE;

-- Add index for better query performance when fetching child tasks
CREATE INDEX idx_todos_parent_id ON todos(parent_id);