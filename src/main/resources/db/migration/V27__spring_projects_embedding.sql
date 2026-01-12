-- ============================================================================
-- V27: Add embedding support for spring_projects table
-- ============================================================================
-- This migration adds vector embedding columns to spring_projects for semantic
-- search capabilities in the findProjectsByUseCase MCP tool.
--
-- Use cases:
-- - "database" should find "data access", "persistence", "JPA"
-- - "security" should find "authentication", "authorization", "OAuth"
-- - "messaging" should find "AMQP", "Kafka", "JMS", "event-driven"
-- ============================================================================

-- Add embedding columns to spring_projects
ALTER TABLE spring_projects
ADD COLUMN IF NOT EXISTS project_embedding vector(768);

ALTER TABLE spring_projects
ADD COLUMN IF NOT EXISTS embedding_model VARCHAR(100);

ALTER TABLE spring_projects
ADD COLUMN IF NOT EXISTS embedded_at TIMESTAMP;

-- Add indexes for vector similarity search
CREATE INDEX IF NOT EXISTS idx_spring_projects_embedding
ON spring_projects USING ivfflat (project_embedding vector_cosine_ops)
WHERE project_embedding IS NOT NULL;

-- Add comments
COMMENT ON COLUMN spring_projects.project_embedding IS 'Semantic embedding for project name + description, enabling semantic use case matching (768 dimensions for Ollama)';
COMMENT ON COLUMN spring_projects.embedding_model IS 'Model used to generate the embedding';
COMMENT ON COLUMN spring_projects.embedded_at IS 'Timestamp when embedding was generated';

-- Update embedding_jobs constraint to include PROJECT entity type
ALTER TABLE embedding_jobs
DROP CONSTRAINT IF EXISTS chk_embedding_entity_type;

ALTER TABLE embedding_jobs
ADD CONSTRAINT chk_embedding_entity_type
CHECK (entity_type IN ('DOCUMENTATION', 'TRANSFORMATION', 'FLAVOR', 'CODE_EXAMPLE', 'JAVADOC_CLASS', 'WIKI_RELEASE_NOTES', 'WIKI_MIGRATION_GUIDE', 'PROJECT'));
