-- ============================================================
-- V16: Add request count to API keys for usage tracking
-- ============================================================
-- This migration adds a request_count column to track how many
-- requests each API key has made, enabling usage statistics
-- on the monitoring dashboard.
-- ============================================================

-- Add request_count column with default 0
ALTER TABLE api_keys ADD COLUMN request_count BIGINT NOT NULL DEFAULT 0;

-- Add comment for the new column
COMMENT ON COLUMN api_keys.request_count IS 'Total number of API requests made with this key';

-- Create index for sorting by usage
CREATE INDEX idx_api_keys_request_count ON api_keys(request_count DESC);
