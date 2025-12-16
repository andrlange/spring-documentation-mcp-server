-- ============================================================
-- V17: Increase category column size in code_examples table
-- ============================================================
-- The category column was VARCHAR(100) which is too short for deeply nested
-- file paths that generate category names like:
-- "Web / Servlet / Filters / Authentication / Oauth2 / Custom"
--
-- Increasing to VARCHAR(255) to accommodate longer category paths.
-- ============================================================

ALTER TABLE code_examples ALTER COLUMN category TYPE VARCHAR(255);

-- Also add a comment explaining the column purpose
COMMENT ON COLUMN code_examples.category IS 'Category path derived from file structure, e.g., "Web / Security / OAuth2"';
