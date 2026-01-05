-- V25: Fix bucket_type column size
-- The bucket_type column was created with VARCHAR(10) but TWENTY_FOUR_HOUR is 16 characters
-- This fixes the "value too long for type character varying(10)" error

ALTER TABLE mcp_metrics_aggregate
    ALTER COLUMN bucket_type TYPE VARCHAR(20);

COMMENT ON COLUMN mcp_metrics_aggregate.bucket_type IS 'Bucket type: FIVE_MIN, ONE_HOUR, TWENTY_FOUR_HOUR';
