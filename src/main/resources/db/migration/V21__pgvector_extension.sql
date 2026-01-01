-- ============================================================================
-- V21: Add pgvector extension for semantic embeddings
-- ============================================================================
-- This migration adds vector search capabilities using pgvector extension.
--
-- IMPORTANT: This migration requires PostgreSQL with pgvector extension installed.
-- Use the pgvector/pgvector:pg18 Docker image or install pgvector manually.
--
-- The embeddings feature is OPTIONAL and can be disabled via configuration:
--   EMBEDDINGS_ENABLED=false (default)
--
-- When disabled, the application will use traditional TSVECTOR full-text search.
-- When enabled, it uses hybrid search combining TSVECTOR + vector similarity.
-- ============================================================================

-- Enable pgvector extension (required for vector data type and similarity operators)
CREATE EXTENSION IF NOT EXISTS vector;

-- ============================================================================
-- Add embedding columns to existing tables
-- Using VECTOR(768) for Ollama nomic-embed-text (768 dimensions)
-- For OpenAI text-embedding-3-small, use VECTOR(1536)
-- ============================================================================

-- Add embedding column to documentation_content
ALTER TABLE documentation_content
ADD COLUMN IF NOT EXISTS content_embedding vector(768);

ALTER TABLE documentation_content
ADD COLUMN IF NOT EXISTS embedding_model VARCHAR(100);

ALTER TABLE documentation_content
ADD COLUMN IF NOT EXISTS embedded_at TIMESTAMP;

-- Add embedding column to migration_transformations
ALTER TABLE migration_transformations
ADD COLUMN IF NOT EXISTS transformation_embedding vector(768);

ALTER TABLE migration_transformations
ADD COLUMN IF NOT EXISTS embedding_model VARCHAR(100);

ALTER TABLE migration_transformations
ADD COLUMN IF NOT EXISTS embedded_at TIMESTAMP;

-- Add embedding column to flavors
ALTER TABLE flavors
ADD COLUMN IF NOT EXISTS flavor_embedding vector(768);

ALTER TABLE flavors
ADD COLUMN IF NOT EXISTS embedding_model VARCHAR(100);

ALTER TABLE flavors
ADD COLUMN IF NOT EXISTS embedded_at TIMESTAMP;

-- Add embedding column to code_examples
ALTER TABLE code_examples
ADD COLUMN IF NOT EXISTS example_embedding vector(768);

ALTER TABLE code_examples
ADD COLUMN IF NOT EXISTS embedding_model VARCHAR(100);

ALTER TABLE code_examples
ADD COLUMN IF NOT EXISTS embedded_at TIMESTAMP;

-- ============================================================================
-- Create embedding metadata table for tracking chunked embeddings
-- Large documents are split into chunks, each with its own embedding
-- ============================================================================
CREATE TABLE IF NOT EXISTS embedding_metadata (
    id BIGSERIAL PRIMARY KEY,
    entity_type VARCHAR(100) NOT NULL,
    entity_id BIGINT NOT NULL,
    chunk_index INT DEFAULT 0,
    chunk_text TEXT,
    embedding vector(768),
    embedding_model VARCHAR(100) NOT NULL,
    token_count INT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(entity_type, entity_id, chunk_index)
);

-- ============================================================================
-- Embedding Jobs Table for async processing state tracking
-- Embeddings are generated asynchronously after sync operations
-- ============================================================================
CREATE TABLE IF NOT EXISTS embedding_jobs (
    id BIGSERIAL PRIMARY KEY,

    -- Job identification
    entity_type VARCHAR(100) NOT NULL,
    entity_id BIGINT NOT NULL,
    job_type VARCHAR(50) NOT NULL DEFAULT 'SINGLE_ENTITY',

    -- Job state
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    priority INT NOT NULL DEFAULT 5,

    -- Retry tracking
    retry_count INT NOT NULL DEFAULT 0,
    max_retries INT NOT NULL DEFAULT 10,
    next_retry_at TIMESTAMP,
    last_error TEXT,

    -- Provider tracking
    provider VARCHAR(50),
    model VARCHAR(100),

    -- Timestamps
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    started_at TIMESTAMP,
    completed_at TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Constraints
    CONSTRAINT chk_embedding_job_status CHECK (status IN ('PENDING', 'IN_PROGRESS', 'COMPLETED', 'FAILED', 'RETRY_PENDING', 'CANCELLED')),
    CONSTRAINT chk_embedding_entity_type CHECK (entity_type IN ('DOCUMENTATION', 'TRANSFORMATION', 'FLAVOR', 'CODE_EXAMPLE', 'JAVADOC_CLASS'))
);

-- ============================================================================
-- Embedding Provider Health Table
-- Tracks the availability of embedding providers (Ollama, OpenAI, etc.)
-- ============================================================================
CREATE TABLE IF NOT EXISTS embedding_provider_health (
    id BIGSERIAL PRIMARY KEY,
    provider VARCHAR(50) NOT NULL UNIQUE,
    is_available BOOLEAN NOT NULL DEFAULT false,
    last_check_at TIMESTAMP,
    last_success_at TIMESTAMP,
    last_error TEXT,
    consecutive_failures INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Insert default provider health records
INSERT INTO embedding_provider_health (provider, is_available) VALUES
    ('ollama', false),
    ('openai', false)
ON CONFLICT (provider) DO NOTHING;

-- ============================================================================
-- Add comments for documentation
-- ============================================================================
COMMENT ON COLUMN documentation_content.content_embedding IS 'Semantic embedding vector for content similarity search (768 dimensions for Ollama)';
COMMENT ON COLUMN migration_transformations.transformation_embedding IS 'Semantic embedding for migration pattern matching (768 dimensions)';
COMMENT ON COLUMN flavors.flavor_embedding IS 'Semantic embedding for architecture/compliance pattern search (768 dimensions)';
COMMENT ON COLUMN code_examples.example_embedding IS 'Semantic embedding for code example discovery (768 dimensions)';

COMMENT ON TABLE embedding_metadata IS 'Stores chunked embeddings for large documents that exceed token limits';
COMMENT ON TABLE embedding_jobs IS 'Async job queue for generating embeddings after sync operations';
COMMENT ON TABLE embedding_provider_health IS 'Tracks availability of embedding providers for health monitoring';
