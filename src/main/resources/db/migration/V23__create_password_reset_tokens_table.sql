-- Create password_reset_tokens table
CREATE TABLE password_reset_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    token VARCHAR(255) NOT NULL UNIQUE,
    user_id UUID NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    used BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_password_reset_token_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Create index on token for faster lookups
CREATE INDEX idx_password_reset_token ON password_reset_tokens(token);

-- Create index on user_id for user's token lookups
CREATE INDEX idx_password_reset_user_id ON password_reset_tokens(user_id);

-- Create index on expires_at for cleanup queries
CREATE INDEX idx_password_reset_expires_at ON password_reset_tokens(expires_at);