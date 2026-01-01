-- ============================================================================
-- V22: Create HNSW indexes for vector similarity search
-- ============================================================================
-- HNSW (Hierarchical Navigable Small World) indexes provide fast approximate
-- nearest neighbor search for high-dimensional vectors.
--
-- Index parameters:
--   m = 16: Number of connections per layer (higher = better recall, more memory)
--   ef_construction = 64: Build-time quality (higher = better index, slower build)
--
-- Using vector_cosine_ops for cosine similarity (most common for text embeddings)
-- Alternative operators: vector_l2_ops (Euclidean), vector_ip_ops (inner product)
-- ============================================================================

-- Documentation content embedding index
CREATE INDEX IF NOT EXISTS idx_documentation_content_embedding
ON documentation_content
USING hnsw (content_embedding vector_cosine_ops)
WITH (m = 16, ef_construction = 64);

-- Migration transformations embedding index
CREATE INDEX IF NOT EXISTS idx_migration_transformations_embedding
ON migration_transformations
USING hnsw (transformation_embedding vector_cosine_ops)
WITH (m = 16, ef_construction = 64);

-- Flavors embedding index
CREATE INDEX IF NOT EXISTS idx_flavors_embedding
ON flavors
USING hnsw (flavor_embedding vector_cosine_ops)
WITH (m = 16, ef_construction = 64);

-- Code examples embedding index
CREATE INDEX IF NOT EXISTS idx_code_examples_embedding
ON code_examples
USING hnsw (example_embedding vector_cosine_ops)
WITH (m = 16, ef_construction = 64);

-- ============================================================================
-- Embedding metadata indexes for chunk lookup and vector search
-- ============================================================================

-- Index for entity lookup
CREATE INDEX IF NOT EXISTS idx_embedding_metadata_lookup
ON embedding_metadata (entity_type, entity_id);

-- HNSW index for chunk vector search
CREATE INDEX IF NOT EXISTS idx_embedding_metadata_vector
ON embedding_metadata
USING hnsw (embedding vector_cosine_ops)
WITH (m = 16, ef_construction = 64);

-- ============================================================================
-- Embedding jobs indexes for efficient job processing
-- ============================================================================

-- Index for finding pending jobs by status
CREATE INDEX IF NOT EXISTS idx_embedding_jobs_status
ON embedding_jobs(status);

-- Composite index for job queue processing (pending jobs ordered by priority)
CREATE INDEX IF NOT EXISTS idx_embedding_jobs_pending
ON embedding_jobs(status, priority, created_at)
WHERE status IN ('PENDING', 'RETRY_PENDING');

-- Index for entity lookup (find job for specific entity)
CREATE INDEX IF NOT EXISTS idx_embedding_jobs_entity
ON embedding_jobs(entity_type, entity_id);

-- Index for retry scheduling
CREATE INDEX IF NOT EXISTS idx_embedding_jobs_retry
ON embedding_jobs(next_retry_at)
WHERE status = 'RETRY_PENDING';

-- ============================================================================
-- Partial indexes for non-null embeddings (query optimization)
-- These help queries that filter on "WHERE embedding IS NOT NULL"
-- ============================================================================

CREATE INDEX IF NOT EXISTS idx_documentation_content_has_embedding
ON documentation_content (id)
WHERE content_embedding IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_migration_transformations_has_embedding
ON migration_transformations (id)
WHERE transformation_embedding IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_flavors_has_embedding
ON flavors (id)
WHERE flavor_embedding IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_code_examples_has_embedding
ON code_examples (id)
WHERE example_embedding IS NOT NULL;

-- ============================================================================
-- Update statistics for query planner
-- ============================================================================
ANALYZE documentation_content;
ANALYZE migration_transformations;
ANALYZE flavors;
ANALYZE code_examples;
ANALYZE embedding_metadata;
ANALYZE embedding_jobs;
