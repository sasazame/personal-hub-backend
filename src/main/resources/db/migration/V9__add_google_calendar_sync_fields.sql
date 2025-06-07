-- Add Google Calendar sync fields to events table
ALTER TABLE events 
    ADD COLUMN google_calendar_id VARCHAR(255),
    ADD COLUMN google_event_id VARCHAR(255),
    ADD COLUMN last_synced_at TIMESTAMPTZ,
    ADD COLUMN sync_status VARCHAR(50) DEFAULT 'NONE';

-- Create calendar sync settings table
CREATE TABLE calendar_sync_settings (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    google_calendar_id VARCHAR(255) NOT NULL,
    calendar_name VARCHAR(255),
    sync_enabled BOOLEAN DEFAULT TRUE,
    last_sync_at TIMESTAMPTZ,
    sync_direction VARCHAR(20) DEFAULT 'BIDIRECTIONAL', -- BIDIRECTIONAL, TO_GOOGLE, FROM_GOOGLE
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- Add indexes for performance
CREATE INDEX idx_events_google_event_id ON events(google_event_id);
CREATE INDEX idx_events_sync_status ON events(sync_status);
CREATE INDEX idx_calendar_sync_settings_user_id ON calendar_sync_settings(user_id);
CREATE INDEX idx_events_last_synced_at ON events(last_synced_at);

-- Add CHECK constraint for sync_status
ALTER TABLE events ADD CONSTRAINT chk_sync_status
    CHECK (sync_status IN ('NONE', 'SYNCED', 'SYNC_PENDING', 'SYNC_ERROR', 'SYNC_CONFLICT'));

-- Add CHECK constraint for sync_direction
ALTER TABLE calendar_sync_settings ADD CONSTRAINT chk_sync_direction
    CHECK (sync_direction IN ('BIDIRECTIONAL', 'TO_GOOGLE', 'FROM_GOOGLE'));

-- Add unique constraint to prevent duplicate calendar settings
ALTER TABLE calendar_sync_settings ADD CONSTRAINT uk_calendar_sync_settings_user_calendar
    UNIQUE (user_id, google_calendar_id);