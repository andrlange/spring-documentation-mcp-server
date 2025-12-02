-- Initialize PostgreSQL extensions for full-text search
CREATE EXTENSION IF NOT EXISTS pg_trgm;  -- Trigram matching for fuzzy search

-- Set timezone
SET timezone = 'UTC';

-- Create initial admin user will be handled by Flyway migrations
