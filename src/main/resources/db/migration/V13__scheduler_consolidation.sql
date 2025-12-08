-- V13: Scheduler Consolidation
-- Adds frequency support to comprehensive sync scheduler
-- Creates global_settings table for shared preferences (time format)

-- Add frequency support to scheduler_settings
ALTER TABLE scheduler_settings
    ADD COLUMN IF NOT EXISTS frequency VARCHAR(10) NOT NULL DEFAULT 'DAILY',
    ADD COLUMN IF NOT EXISTS day_of_month INTEGER DEFAULT 1;

-- Create global_settings table for shared settings like time format
CREATE TABLE IF NOT EXISTS global_settings (
    id BIGSERIAL PRIMARY KEY,
    setting_key VARCHAR(100) NOT NULL UNIQUE,
    setting_value VARCHAR(500) NOT NULL,
    description TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Insert default time format setting
INSERT INTO global_settings (setting_key, setting_value, description)
VALUES ('time_format', '24h', 'Display format for time (12h or 24h)')
ON CONFLICT (setting_key) DO NOTHING;

-- Create index for fast lookups
CREATE INDEX IF NOT EXISTS idx_global_settings_key ON global_settings(setting_key);

-- Update existing scheduler_settings records to have DAILY frequency
UPDATE scheduler_settings SET frequency = 'DAILY' WHERE frequency IS NULL;
