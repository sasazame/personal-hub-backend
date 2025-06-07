-- PostgreSQL initialization script for Docker
-- This file is automatically executed when the container starts

-- Grant additional permissions
GRANT ALL PRIVILEGES ON DATABASE personalhub TO personalhub;
ALTER USER personalhub CREATEDB;