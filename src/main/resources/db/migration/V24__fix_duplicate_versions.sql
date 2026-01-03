-- V24: Fix duplicate versions and ensure only one latest/default per project
-- This migration addresses data integrity issues where:
-- 1. Multiple records exist for the same project+version combination
-- 2. Multiple versions are marked as is_latest=true for the same project
-- 3. Multiple versions are marked as is_default=true for the same project

-- Step 1: Delete duplicate version records, keeping only the most recent one
-- First, identify and delete duplicates
DELETE FROM project_versions
WHERE id IN (
    SELECT id FROM (
        SELECT id,
               ROW_NUMBER() OVER (PARTITION BY project_id, version ORDER BY created_at DESC) as rn
        FROM project_versions
    ) subquery
    WHERE rn > 1
);

-- Step 2: Fix the is_latest flag - ensure only one version per project has is_latest=true
-- First, clear all is_latest flags
UPDATE project_versions SET is_latest = false;

-- Then, set is_latest=true for the highest GA version per project
UPDATE project_versions pv
SET is_latest = true
WHERE pv.id IN (
    SELECT DISTINCT ON (project_id) id
    FROM project_versions
    WHERE state = 'GA'
    ORDER BY project_id,
             major_version DESC,
             minor_version DESC,
             COALESCE(patch_version, 0) DESC
);

-- Step 3: Fix the is_default flag - ensure only one version per project has is_default=true
-- First, clear all is_default flags
UPDATE project_versions SET is_default = false;

-- Then, set is_default=true for the same version that is_latest=true
-- (default should typically be the latest stable version)
UPDATE project_versions SET is_default = true WHERE is_latest = true;

-- Step 4: Add a partial unique index to enforce only one is_latest per project
-- This prevents the issue from happening again
DROP INDEX IF EXISTS idx_unique_latest_per_project;
CREATE UNIQUE INDEX idx_unique_latest_per_project
ON project_versions (project_id)
WHERE is_latest = true;

-- Step 5: Add a similar index for is_default
DROP INDEX IF EXISTS idx_unique_default_per_project;
CREATE UNIQUE INDEX idx_unique_default_per_project
ON project_versions (project_id)
WHERE is_default = true;

-- Log the changes
DO $$
DECLARE
    latest_count INTEGER;
    default_count INTEGER;
    total_projects INTEGER;
BEGIN
    SELECT COUNT(*) INTO latest_count FROM project_versions WHERE is_latest = true;
    SELECT COUNT(*) INTO default_count FROM project_versions WHERE is_default = true;
    SELECT COUNT(*) INTO total_projects FROM spring_projects;
    RAISE NOTICE 'Fixed: % of % projects now have exactly one latest version', latest_count, total_projects;
    RAISE NOTICE 'Fixed: % of % projects now have exactly one default version', default_count, total_projects;
END $$;
