-- Spring MCP Server - JEP/KEP Specifications Storage
-- Version: 1.5.2
-- Description: Store full JEP and KEP specification content for detail pages

-- ============================================================================
-- JEP Specifications (Java Enhancement Proposals)
-- Source: https://openjdk.org/jeps/{number}
-- ============================================================================
CREATE TABLE jep_specifications (
    id BIGSERIAL PRIMARY KEY,
    jep_number VARCHAR(20) NOT NULL UNIQUE,
    title VARCHAR(500),
    summary TEXT,
    description TEXT,
    motivation TEXT,
    goals TEXT,
    non_goals TEXT,
    html_content TEXT,
    status VARCHAR(50),
    target_version VARCHAR(50),
    source_url VARCHAR(500),
    fetched_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================================
-- KEP Specifications (Kotlin Enhancement Proposals / KEEP)
-- Sources:
--   Primary: https://github.com/Kotlin/KEEP (KEEP type)
--   Fallback: https://youtrack.jetbrains.com/issue/{KT-number} (YOUTRACK type)
-- ============================================================================
CREATE TABLE kep_specifications (
    id BIGSERIAL PRIMARY KEY,
    kep_number VARCHAR(50) NOT NULL UNIQUE,
    title VARCHAR(500),
    summary TEXT,
    description TEXT,
    motivation TEXT,
    markdown_content TEXT,
    html_content TEXT,
    status VARCHAR(50),
    source_type VARCHAR(20) NOT NULL DEFAULT 'YOUTRACK', -- KEEP, YOUTRACK
    source_url VARCHAR(500),
    fetched_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================================
-- Add example source type to language_features
-- Tracks whether code example is from official spec or manually curated
-- ============================================================================
ALTER TABLE language_features
    ADD COLUMN IF NOT EXISTS example_source_type VARCHAR(20) DEFAULT 'OFFICIAL';

COMMENT ON COLUMN language_features.example_source_type IS
    'Source of code example: OFFICIAL (from specification) or SYNTHESIZED (manually curated)';

-- ============================================================================
-- Indexes for JEP Specifications
-- ============================================================================
CREATE INDEX idx_jep_spec_number ON jep_specifications(jep_number);
CREATE INDEX idx_jep_spec_status ON jep_specifications(status);
CREATE INDEX idx_jep_spec_target_version ON jep_specifications(target_version);
CREATE INDEX idx_jep_spec_fetched ON jep_specifications(fetched_at) WHERE fetched_at IS NOT NULL;

-- ============================================================================
-- Indexes for KEP Specifications
-- ============================================================================
CREATE INDEX idx_kep_spec_number ON kep_specifications(kep_number);
CREATE INDEX idx_kep_spec_status ON kep_specifications(status);
CREATE INDEX idx_kep_spec_source_type ON kep_specifications(source_type);
CREATE INDEX idx_kep_spec_fetched ON kep_specifications(fetched_at) WHERE fetched_at IS NOT NULL;

-- ============================================================================
-- Full-text search indexes for searching within specifications
-- ============================================================================
CREATE INDEX idx_jep_spec_fts ON jep_specifications
    USING gin(to_tsvector('english',
        COALESCE(title, '') || ' ' ||
        COALESCE(summary, '') || ' ' ||
        COALESCE(description, '')));

CREATE INDEX idx_kep_spec_fts ON kep_specifications
    USING gin(to_tsvector('english',
        COALESCE(title, '') || ' ' ||
        COALESCE(summary, '') || ' ' ||
        COALESCE(description, '')));

-- ============================================================================
-- Table comments
-- ============================================================================
COMMENT ON TABLE jep_specifications IS
    'Cached JEP specification content from openjdk.org for detail page display';
COMMENT ON TABLE kep_specifications IS
    'Cached KEP/KEEP specification content from GitHub KEEP repo or JetBrains YouTrack';

COMMENT ON COLUMN jep_specifications.jep_number IS 'JEP number (e.g., 444, 501)';
COMMENT ON COLUMN jep_specifications.status IS 'JEP status (Candidate, Preview, Final, etc.)';
COMMENT ON COLUMN jep_specifications.target_version IS 'Target Java version (e.g., 21, 25)';
COMMENT ON COLUMN jep_specifications.html_content IS 'Full HTML content for rendering';
COMMENT ON COLUMN jep_specifications.fetched_at IS 'When content was fetched (NULL = not fetched yet)';

COMMENT ON COLUMN kep_specifications.kep_number IS 'KEP identifier (e.g., KT-11550 or context-parameters)';
COMMENT ON COLUMN kep_specifications.source_type IS 'KEEP = GitHub KEEP repo, YOUTRACK = JetBrains issue tracker';
COMMENT ON COLUMN kep_specifications.markdown_content IS 'Original markdown content (for KEEP sources)';
COMMENT ON COLUMN kep_specifications.html_content IS 'Rendered HTML content for display';
COMMENT ON COLUMN kep_specifications.fetched_at IS 'When content was fetched (NULL = not fetched yet)';
