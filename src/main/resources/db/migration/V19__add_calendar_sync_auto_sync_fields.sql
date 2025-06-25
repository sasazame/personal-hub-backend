-- Add auto sync and sync interval fields to calendar_sync_settings table
ALTER TABLE calendar_sync_settings 
    ADD COLUMN auto_sync BOOLEAN DEFAULT TRUE,
    ADD COLUMN sync_interval INTEGER DEFAULT 30;

-- Add comment for clarity
COMMENT ON COLUMN calendar_sync_settings.auto_sync IS 'Whether automatic synchronization is enabled';
COMMENT ON COLUMN calendar_sync_settings.sync_interval IS 'Synchronization interval in minutes';

-- Add CHECK constraint for sync_interval
ALTER TABLE calendar_sync_settings ADD CONSTRAINT chk_sync_interval
    CHECK (sync_interval > 0 AND sync_interval <= 1440);