-- Reset all POMODORO data
-- This script truncates all POMODORO-related tables while preserving the schema
-- 
-- Usage: psql -h localhost -U personalhub -d personalhub -f scripts/reset-pomodoro-data.sql

BEGIN;

-- Truncate in correct order due to foreign key constraints
-- CASCADE will handle any dependent records
TRUNCATE TABLE pomodoro_tasks CASCADE;
TRUNCATE TABLE pomodoro_sessions CASCADE;
TRUNCATE TABLE pomodoro_configs CASCADE;

-- Verify the truncation
DO $$
DECLARE
    tasks_count INTEGER;
    sessions_count INTEGER;
    configs_count INTEGER;
BEGIN
    SELECT COUNT(*) INTO tasks_count FROM pomodoro_tasks;
    SELECT COUNT(*) INTO sessions_count FROM pomodoro_sessions;
    SELECT COUNT(*) INTO configs_count FROM pomodoro_configs;
    
    RAISE NOTICE 'POMODORO data reset complete:';
    RAISE NOTICE '  pomodoro_tasks: % records', tasks_count;
    RAISE NOTICE '  pomodoro_sessions: % records', sessions_count;
    RAISE NOTICE '  pomodoro_configs: % records', configs_count;
END $$;

COMMIT;

-- Success message
\echo 'POMODORO tables have been successfully reset!'