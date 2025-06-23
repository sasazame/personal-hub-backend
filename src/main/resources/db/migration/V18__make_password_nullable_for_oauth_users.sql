-- OAuthユーザーのためにpasswordカラムをNULL許可に変更
-- Social login users don't have passwords
ALTER TABLE users ALTER COLUMN password DROP NOT NULL;

-- Add comment to clarify the field's purpose
COMMENT ON COLUMN users.password IS 'BCrypt hashed password for regular users. NULL for OAuth/social login users.';