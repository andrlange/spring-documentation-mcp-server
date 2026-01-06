-- ============================================================================
-- V26: Spring Boot Wiki Integration
-- ============================================================================
-- This migration adds tables for storing Release Notes and Migration Guides
-- from the Spring Boot GitHub Wiki (https://github.com/spring-projects/spring-boot/wiki)
--
-- The wiki content is synced from the GitHub wiki repository and converted
-- from AsciiDoc to Markdown for storage and rendering.
-- ============================================================================

-- ============================================================================
-- Wiki Release Notes Table
-- Stores release notes for each Spring Boot version (e.g., 3.5, 4.0)
-- ============================================================================
CREATE TABLE IF NOT EXISTS wiki_release_notes (
    id BIGSERIAL PRIMARY KEY,

    -- Version reference
    spring_boot_version_id BIGINT,
    version_string VARCHAR(50) NOT NULL,
    major_version INT NOT NULL,
    minor_version INT NOT NULL,

    -- Content
    title VARCHAR(500) NOT NULL,
    content TEXT NOT NULL,
    content_markdown TEXT,
    content_hash VARCHAR(64),

    -- Metadata
    source_url VARCHAR(1000),
    source_file VARCHAR(255),
    wiki_last_modified TIMESTAMP,

    -- Full-text search (auto-generated TSVECTOR)
    search_vector TSVECTOR GENERATED ALWAYS AS (
        setweight(to_tsvector('english', COALESCE(title, '')), 'A') ||
        setweight(to_tsvector('english', COALESCE(content_markdown, '')), 'B')
    ) STORED,

    -- Embeddings (768 dimensions for nomic-embed-text)
    content_embedding vector(768),
    embedding_model VARCHAR(100),
    embedded_at TIMESTAMP,

    -- Timestamps
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Constraints
    CONSTRAINT uq_wiki_release_notes_version UNIQUE(version_string)
);

-- ============================================================================
-- Wiki Migration Guides Table
-- Stores migration guides between Spring Boot versions (e.g., 3.5 -> 4.0)
-- ============================================================================
CREATE TABLE IF NOT EXISTS wiki_migration_guides (
    id BIGSERIAL PRIMARY KEY,

    -- Version references (source -> target)
    source_version_id BIGINT,
    target_version_id BIGINT,
    source_version_string VARCHAR(50) NOT NULL,
    target_version_string VARCHAR(50) NOT NULL,
    source_major INT NOT NULL,
    source_minor INT NOT NULL,
    target_major INT NOT NULL,
    target_minor INT NOT NULL,

    -- Content
    title VARCHAR(500) NOT NULL,
    content TEXT NOT NULL,
    content_markdown TEXT,
    content_hash VARCHAR(64),

    -- Metadata
    source_url VARCHAR(1000),
    source_file VARCHAR(255),
    wiki_last_modified TIMESTAMP,

    -- Full-text search (auto-generated TSVECTOR)
    search_vector TSVECTOR GENERATED ALWAYS AS (
        setweight(to_tsvector('english', COALESCE(title, '')), 'A') ||
        setweight(to_tsvector('english', COALESCE(content_markdown, '')), 'B')
    ) STORED,

    -- Embeddings (768 dimensions for nomic-embed-text)
    content_embedding vector(768),
    embedding_model VARCHAR(100),
    embedded_at TIMESTAMP,

    -- Timestamps
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Constraints
    CONSTRAINT uq_wiki_migration_guide_versions UNIQUE(source_version_string, target_version_string)
);

-- ============================================================================
-- Indexes for Performance
-- ============================================================================

-- Release Notes indexes
CREATE INDEX IF NOT EXISTS idx_wiki_release_notes_version
    ON wiki_release_notes(version_string);
CREATE INDEX IF NOT EXISTS idx_wiki_release_notes_major_minor
    ON wiki_release_notes(major_version DESC, minor_version DESC);
CREATE INDEX IF NOT EXISTS idx_wiki_release_notes_search
    ON wiki_release_notes USING gin(search_vector);

-- Migration Guides indexes
CREATE INDEX IF NOT EXISTS idx_wiki_migration_guides_source
    ON wiki_migration_guides(source_version_string);
CREATE INDEX IF NOT EXISTS idx_wiki_migration_guides_target
    ON wiki_migration_guides(target_version_string);
CREATE INDEX IF NOT EXISTS idx_wiki_migration_guides_versions
    ON wiki_migration_guides(target_major DESC, target_minor DESC);
CREATE INDEX IF NOT EXISTS idx_wiki_migration_guides_search
    ON wiki_migration_guides USING gin(search_vector);

-- Embedding indexes (HNSW for fast approximate nearest neighbor)
CREATE INDEX IF NOT EXISTS idx_wiki_release_notes_embedding
    ON wiki_release_notes USING hnsw (content_embedding vector_cosine_ops)
    WITH (m = 16, ef_construction = 64);
CREATE INDEX IF NOT EXISTS idx_wiki_migration_guides_embedding
    ON wiki_migration_guides USING hnsw (content_embedding vector_cosine_ops)
    WITH (m = 16, ef_construction = 64);

-- Partial indexes for entities with embeddings
CREATE INDEX IF NOT EXISTS idx_wiki_release_notes_has_embedding
    ON wiki_release_notes (id) WHERE content_embedding IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_wiki_migration_guides_has_embedding
    ON wiki_migration_guides (id) WHERE content_embedding IS NOT NULL;

-- ============================================================================
-- Update embedding_jobs constraint to include wiki entity types
-- ============================================================================
ALTER TABLE embedding_jobs DROP CONSTRAINT IF EXISTS chk_embedding_entity_type;
ALTER TABLE embedding_jobs ADD CONSTRAINT chk_embedding_entity_type
    CHECK (entity_type IN (
        'DOCUMENTATION',
        'TRANSFORMATION',
        'FLAVOR',
        'CODE_EXAMPLE',
        'JAVADOC_CLASS',
        'WIKI_RELEASE_NOTES',
        'WIKI_MIGRATION_GUIDE'
    ));

-- ============================================================================
-- Comments for Documentation
-- ============================================================================
COMMENT ON TABLE wiki_release_notes IS 'Spring Boot release notes from GitHub wiki (AsciiDoc converted to Markdown)';
COMMENT ON TABLE wiki_migration_guides IS 'Spring Boot migration guides from GitHub wiki (AsciiDoc converted to Markdown)';
COMMENT ON COLUMN wiki_release_notes.content_embedding IS 'Semantic embedding vector for similarity search (768 dimensions)';
COMMENT ON COLUMN wiki_migration_guides.content_embedding IS 'Semantic embedding vector for similarity search (768 dimensions)';
COMMENT ON COLUMN wiki_release_notes.content IS 'Original AsciiDoc content from wiki';
COMMENT ON COLUMN wiki_release_notes.content_markdown IS 'Converted Markdown content for rendering';
COMMENT ON COLUMN wiki_migration_guides.content IS 'Original AsciiDoc content from wiki';
COMMENT ON COLUMN wiki_migration_guides.content_markdown IS 'Converted Markdown content for rendering';
