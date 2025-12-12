-- ============================================================
-- V14: Javadoc Version Filter Settings
-- Release 1.4.3
-- ============================================================
-- Adds settings to control which version types are included in Javadoc sync:
-- - SNAPSHOT versions (e.g., 2.0.0-SNAPSHOT)
-- - RC versions (e.g., 1.0.0-RC1, 1.0.0-RC2)
-- - Milestone versions (e.g., 1.0.0-M1, 1.0.0-M2)
-- ============================================================

-- Add Javadoc sync filter columns to settings table
ALTER TABLE settings
ADD COLUMN IF NOT EXISTS javadoc_sync_snapshot BOOLEAN NOT NULL DEFAULT false,
ADD COLUMN IF NOT EXISTS javadoc_sync_rc BOOLEAN NOT NULL DEFAULT false,
ADD COLUMN IF NOT EXISTS javadoc_sync_milestone BOOLEAN NOT NULL DEFAULT false;

-- Add comments
COMMENT ON COLUMN settings.javadoc_sync_snapshot IS 'Include SNAPSHOT versions in Javadoc sync (e.g., 2.0.0-SNAPSHOT)';
COMMENT ON COLUMN settings.javadoc_sync_rc IS 'Include RC (Release Candidate) versions in Javadoc sync (e.g., 1.0.0-RC1)';
COMMENT ON COLUMN settings.javadoc_sync_milestone IS 'Include Milestone versions in Javadoc sync (e.g., 1.0.0-M1)';
