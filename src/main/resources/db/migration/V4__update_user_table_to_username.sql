-- Update user table to use username instead of firstName and lastName
ALTER TABLE users DROP COLUMN first_name;
ALTER TABLE users DROP COLUMN last_name;
ALTER TABLE users ADD COLUMN username VARCHAR(20) NOT NULL;
ALTER TABLE users ADD CONSTRAINT uk_users_username UNIQUE (username);