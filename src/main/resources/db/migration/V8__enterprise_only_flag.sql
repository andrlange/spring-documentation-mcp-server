-- V8: Add is_enterprise_only flag to spring_boot_versions
-- This flag indicates versions that are only available under enterprise subscription
-- (OSS support ended but enterprise support is still active)

ALTER TABLE spring_boot_versions
    ADD COLUMN IF NOT EXISTS is_enterprise_only BOOLEAN DEFAULT FALSE;

-- Add comment for documentation
COMMENT ON COLUMN spring_boot_versions.is_enterprise_only IS 'Indicates if this version is only available under enterprise subscription (OSS support ended but enterprise support active)';
