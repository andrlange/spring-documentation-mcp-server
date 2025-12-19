-- V20: Add display_name column to users table
-- Allows users to specify an optional display name for personalization

ALTER TABLE users ADD COLUMN display_name VARCHAR(100);

COMMENT ON COLUMN users.display_name IS 'Optional display name shown in the UI instead of username';
