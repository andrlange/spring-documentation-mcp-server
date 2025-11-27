-- Spring MCP Server - Complete Initial Database Schema
-- Version: 1.0.0 (Consolidated)
-- Description: Complete schema for Spring MCP Server with all features
-- Date: 2025-11-12

-- ============================================================
-- CORE TABLES
-- ============================================================

-- ============================================================
-- Spring Projects Table
-- ============================================================
CREATE TABLE spring_projects (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    slug VARCHAR(255) NOT NULL UNIQUE,
    description TEXT,
    homepage_url VARCHAR(500),
    github_url VARCHAR(500),
    active BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE spring_projects IS 'Stores Spring ecosystem projects (Spring Boot, Framework, Data, etc.)';

-- ============================================================
-- Spring Boot Versions Table (PRIMARY/CENTRAL TABLE)
-- ============================================================
CREATE TABLE spring_boot_versions (
    id BIGSERIAL PRIMARY KEY,
    version VARCHAR(50) NOT NULL UNIQUE,
    major_version INT NOT NULL,
    minor_version INT NOT NULL,
    patch_version INT,
    state VARCHAR(20) NOT NULL,
    is_current BOOLEAN DEFAULT false,
    released_at DATE,
    oss_support_end DATE,
    enterprise_support_end DATE,
    reference_doc_url VARCHAR(500),
    api_doc_url VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT check_boot_state CHECK (state IN ('SNAPSHOT', 'MILESTONE', 'RC', 'GA'))
);

COMMENT ON TABLE spring_boot_versions IS 'Central table for Spring Boot versions - primary filter for entire system';
COMMENT ON COLUMN spring_boot_versions.state IS 'Version state: SNAPSHOT (development), MILESTONE (M1-Mn), RC (release candidate), GA (general availability/stable)';
COMMENT ON COLUMN spring_boot_versions.is_current IS 'Whether this is the current/recommended Spring Boot version';
COMMENT ON COLUMN spring_boot_versions.oss_support_end IS 'OSS (Open Source Software) support end date';
COMMENT ON COLUMN spring_boot_versions.enterprise_support_end IS 'Enterprise/Commercial support end date';

-- ============================================================
-- Project Versions Table
-- ============================================================
CREATE TABLE project_versions (
    id BIGSERIAL PRIMARY KEY,
    project_id BIGINT NOT NULL REFERENCES spring_projects(id) ON DELETE CASCADE,
    version VARCHAR(50) NOT NULL,
    major_version INT NOT NULL,
    minor_version INT NOT NULL,
    patch_version INT,
    state VARCHAR(20) NOT NULL,
    is_latest BOOLEAN DEFAULT false,
    is_default BOOLEAN DEFAULT false,
    release_date DATE,
    oss_support_end DATE,
    enterprise_support_end DATE,
    reference_doc_url VARCHAR(500),
    api_doc_url VARCHAR(500),
    status VARCHAR(20),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT unique_project_version UNIQUE(project_id, version),
    CONSTRAINT check_state CHECK (state IN ('SNAPSHOT', 'MILESTONE', 'RC', 'GA'))
);

COMMENT ON TABLE project_versions IS 'Stores version information for each Spring project';
COMMENT ON COLUMN project_versions.state IS 'Version state: SNAPSHOT, MILESTONE, RC, GA';
COMMENT ON COLUMN project_versions.release_date IS 'Initial release date';
COMMENT ON COLUMN project_versions.oss_support_end IS 'OSS (Open Source Software) support end date';
COMMENT ON COLUMN project_versions.enterprise_support_end IS 'Enterprise/Commercial support end date (End of Life)';
COMMENT ON COLUMN project_versions.reference_doc_url IS 'URL to the reference documentation for this version';
COMMENT ON COLUMN project_versions.api_doc_url IS 'URL to the API documentation (Javadoc) for this version';
COMMENT ON COLUMN project_versions.status IS 'Version status: CURRENT, GA, PRE, SNAPSHOT';

-- ============================================================
-- Spring Boot Compatibility Table (Junction Table)
-- ============================================================
CREATE TABLE spring_boot_compatibility (
    id BIGSERIAL PRIMARY KEY,
    spring_boot_version_id BIGINT NOT NULL REFERENCES spring_boot_versions(id) ON DELETE CASCADE,
    compatible_project_version_id BIGINT NOT NULL REFERENCES project_versions(id) ON DELETE CASCADE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(spring_boot_version_id, compatible_project_version_id)
);

COMMENT ON TABLE spring_boot_compatibility IS 'Maps Spring Boot versions to compatible versions of other Spring projects';

-- ============================================================
-- Project Relationships Table (Parent/Child Projects)
-- ============================================================
CREATE TABLE project_relationships (
    id BIGSERIAL PRIMARY KEY,
    parent_project_id BIGINT NOT NULL REFERENCES spring_projects(id) ON DELETE CASCADE,
    child_project_id BIGINT NOT NULL REFERENCES spring_projects(id) ON DELETE CASCADE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(parent_project_id, child_project_id),
    CONSTRAINT check_not_self_reference CHECK (parent_project_id != child_project_id)
);

COMMENT ON TABLE project_relationships IS 'Links parent projects to their subprojects (e.g., Spring Data → JPA, MongoDB, Redis)';

-- ============================================================
-- Documentation Types Table
-- ============================================================
CREATE TABLE documentation_types (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    slug VARCHAR(100) NOT NULL UNIQUE,
    display_order INT DEFAULT 0
);

COMMENT ON TABLE documentation_types IS 'Types of documentation (Overview, Learn, Support, Samples)';

-- Insert default documentation types
INSERT INTO documentation_types (name, slug, display_order) VALUES
    ('Overview', 'overview', 1),
    ('Learn', 'learn', 2),
    ('Support', 'support', 3),
    ('Samples', 'samples', 4),
    ('API Reference', 'api-reference', 5),
    ('Guides', 'guides', 6);

-- ============================================================
-- Documentation Links Table
-- ============================================================
CREATE TABLE documentation_links (
    id BIGSERIAL PRIMARY KEY,
    version_id BIGINT NOT NULL REFERENCES project_versions(id) ON DELETE CASCADE,
    doc_type_id BIGINT NOT NULL REFERENCES documentation_types(id),
    title VARCHAR(255) NOT NULL,
    url VARCHAR(1000) NOT NULL,
    description TEXT,
    content_hash VARCHAR(64),
    last_fetched TIMESTAMP,
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE documentation_links IS 'Links to documentation for each project version';

-- ============================================================
-- Cached Documentation Content Table
-- ============================================================
CREATE TABLE documentation_content (
    id BIGSERIAL PRIMARY KEY,
    link_id BIGINT NOT NULL REFERENCES documentation_links(id) ON DELETE CASCADE,
    content_type VARCHAR(50),
    content TEXT,
    metadata JSONB,
    indexed_content TSVECTOR,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT unique_link_content UNIQUE(link_id)
);

COMMENT ON TABLE documentation_content IS 'Cached content of documentation for search and display';

-- ============================================================
-- External Documentation Sources Table
-- ============================================================
CREATE TABLE external_docs (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    base_url VARCHAR(500) NOT NULL,
    doc_type VARCHAR(100),
    related_project_id BIGINT REFERENCES spring_projects(id),
    active BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE external_docs IS 'External documentation sources (SpringDoc, Baeldung, etc.)';

-- ============================================================
-- Code Examples Table
-- ============================================================
CREATE TABLE code_examples (
    id BIGSERIAL PRIMARY KEY,
    version_id BIGINT NOT NULL REFERENCES project_versions(id) ON DELETE CASCADE,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    code_snippet TEXT NOT NULL,
    language VARCHAR(50) DEFAULT 'java',
    category VARCHAR(100),
    tags TEXT[],
    source_url VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE code_examples IS 'Code examples for different Spring project versions';

-- ============================================================
-- Users Table (for Management UI)
-- ============================================================
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(255),
    role VARCHAR(50) NOT NULL DEFAULT 'VIEWER',
    enabled BOOLEAN DEFAULT true,
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT check_user_role CHECK (role IN ('ADMIN', 'VIEWER'))
);

COMMENT ON TABLE users IS 'Users with access to the management UI';
COMMENT ON COLUMN users.role IS 'User role: ADMIN (full access) or VIEWER (read-only)';
COMMENT ON COLUMN users.enabled IS 'Technical flag for Spring Security - whether account is not locked/expired';
COMMENT ON COLUMN users.is_active IS 'Business flag - whether user account is active and can login';

-- Insert default admin user (password: admin - should be changed in production!)
-- BCrypt hash for 'admin' with strength 10
INSERT INTO users (username, password, email, role, enabled, is_active) VALUES
    ('admin', '{bcrypt}$2a$10$f/n2U7h.G4s3FdeYYSkFuehqUtVPiBbeB0R5iJM7kL1lMVUsrenLa', 'admin@springmcp.local', 'ADMIN', true, true);

-- ============================================================
-- Settings Table (Singleton Pattern)
-- ============================================================
CREATE TABLE settings (
    id BIGSERIAL PRIMARY KEY,
    enterprise_subscription_enabled BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE settings IS 'System-wide settings (singleton - only one row)';

-- Insert default settings row
INSERT INTO settings (enterprise_subscription_enabled) VALUES (false);

-- Add a constraint to ensure only one row exists in the settings table
CREATE UNIQUE INDEX idx_settings_singleton ON settings ((id IS NOT NULL));

-- ============================================================
-- Scheduler Settings Table (Singleton Pattern)
-- ============================================================
CREATE TABLE scheduler_settings (
    id BIGSERIAL PRIMARY KEY,
    sync_enabled BOOLEAN NOT NULL DEFAULT true,
    sync_time VARCHAR(5) NOT NULL DEFAULT '03:00', -- HH:mm format (24h)
    time_format VARCHAR(3) NOT NULL DEFAULT '24h', -- '12h' or '24h'
    last_sync_run TIMESTAMP,
    next_sync_run TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_time_format CHECK (time_format IN ('12h', '24h')),
    CONSTRAINT chk_sync_time_format CHECK (sync_time ~ '^([0-1][0-9]|2[0-3]):[0-5][0-9]$')
);

COMMENT ON TABLE scheduler_settings IS 'Stores configuration for automatic comprehensive synchronization scheduling';
COMMENT ON COLUMN scheduler_settings.sync_enabled IS 'Whether automatic sync is enabled';
COMMENT ON COLUMN scheduler_settings.sync_time IS 'Time to run sync in HH:mm 24-hour format';
COMMENT ON COLUMN scheduler_settings.time_format IS 'Display format preference: 12h or 24h';
COMMENT ON COLUMN scheduler_settings.last_sync_run IS 'Timestamp of last automatic sync execution';
COMMENT ON COLUMN scheduler_settings.next_sync_run IS 'Calculated timestamp for next scheduled sync';

-- Insert default scheduler settings with calculated next_sync_run
INSERT INTO scheduler_settings (sync_enabled, sync_time, time_format, next_sync_run)
VALUES (
    true,
    '03:00',
    '24h',
    -- Calculate next run: if current time is before 03:00 today, use today at 03:00, otherwise tomorrow at 03:00
    CASE
        WHEN CURRENT_TIME < TIME '03:00' THEN
            CURRENT_DATE + TIME '03:00'
        ELSE
            (CURRENT_DATE + INTERVAL '1 day') + TIME '03:00'
    END
);

-- ============================================================
-- API Keys Table (for MCP Authentication)
-- ============================================================
CREATE TABLE api_keys (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    key_hash VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100) NOT NULL,
    last_used_at TIMESTAMP,
    is_active BOOLEAN NOT NULL DEFAULT true,
    description TEXT,
    CONSTRAINT chk_name_length CHECK (LENGTH(name) >= 3)
);

COMMENT ON TABLE api_keys IS 'API keys for MCP endpoint authentication';
COMMENT ON COLUMN api_keys.name IS 'Human-readable name for the API key (minimum 3 characters)';
COMMENT ON COLUMN api_keys.key_hash IS 'BCrypt hashed API key (never store plain text)';
COMMENT ON COLUMN api_keys.created_by IS 'Username of the user who created this API key';
COMMENT ON COLUMN api_keys.last_used_at IS 'Timestamp of last successful authentication with this key';
COMMENT ON COLUMN api_keys.is_active IS 'Whether this API key is currently active';

-- ============================================================
-- MCP Connection Logs Table
-- ============================================================
CREATE TABLE mcp_connections (
    id BIGSERIAL PRIMARY KEY,
    client_id VARCHAR(255),
    connected_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    disconnected_at TIMESTAMP,
    requests_count INT DEFAULT 0
);

COMMENT ON TABLE mcp_connections IS 'Logs of MCP client connections';

-- ============================================================
-- MCP Request Logs Table
-- ============================================================
CREATE TABLE mcp_requests (
    id BIGSERIAL PRIMARY KEY,
    connection_id BIGINT REFERENCES mcp_connections(id),
    tool_name VARCHAR(100),
    parameters JSONB,
    response_status VARCHAR(50),
    execution_time_ms INT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE mcp_requests IS 'Logs of MCP tool requests and responses';

-- ============================================================
-- INDEXES FOR PERFORMANCE
-- ============================================================

-- Spring Boot Versions indexes
CREATE INDEX idx_spring_boot_versions_version ON spring_boot_versions(version);
CREATE INDEX idx_spring_boot_versions_state ON spring_boot_versions(state);
CREATE INDEX idx_spring_boot_versions_current ON spring_boot_versions(is_current) WHERE is_current = true;
CREATE INDEX idx_spring_boot_versions_major_minor ON spring_boot_versions(major_version, minor_version);

-- Project Versions indexes
CREATE INDEX idx_project_versions_project ON project_versions(project_id);
CREATE INDEX idx_project_versions_latest ON project_versions(is_latest) WHERE is_latest = true;
CREATE INDEX idx_project_versions_default ON project_versions(is_default) WHERE is_default = true;
CREATE INDEX idx_project_versions_state ON project_versions(state);
CREATE INDEX idx_project_versions_status ON project_versions(status);
CREATE INDEX idx_project_versions_oss_support_end ON project_versions(oss_support_end);
CREATE INDEX idx_project_versions_enterprise_support_end ON project_versions(enterprise_support_end);

-- Spring Boot Compatibility indexes
CREATE INDEX idx_spring_boot_compatibility_boot_version ON spring_boot_compatibility(spring_boot_version_id);
CREATE INDEX idx_spring_boot_compatibility_project_version ON spring_boot_compatibility(compatible_project_version_id);

-- Project Relationships indexes
CREATE INDEX idx_project_relationships_parent ON project_relationships(parent_project_id);
CREATE INDEX idx_project_relationships_child ON project_relationships(child_project_id);

-- Documentation Links indexes
CREATE INDEX idx_documentation_links_version ON documentation_links(version_id);
CREATE INDEX idx_documentation_links_type ON documentation_links(doc_type_id);
CREATE INDEX idx_documentation_links_active ON documentation_links(is_active) WHERE is_active = true;

-- Documentation Content indexes (for full-text search)
CREATE INDEX idx_documentation_content_search ON documentation_content USING GIN(indexed_content);
CREATE INDEX idx_documentation_content_metadata ON documentation_content USING GIN(metadata);

-- Code Examples indexes
CREATE INDEX idx_code_examples_version ON code_examples(version_id);
CREATE INDEX idx_code_examples_language ON code_examples(language);
CREATE INDEX idx_code_examples_category ON code_examples(category);
CREATE INDEX idx_code_examples_tags ON code_examples USING GIN(tags);

-- Users indexes
CREATE INDEX idx_users_username ON users(username);
CREATE INDEX idx_users_role ON users(role);
CREATE INDEX idx_users_enabled ON users(enabled) WHERE enabled = true;
CREATE INDEX idx_users_is_active ON users(is_active) WHERE is_active = true;

-- Scheduler Settings indexes
CREATE INDEX idx_scheduler_settings_enabled ON scheduler_settings(sync_enabled);

-- API Keys indexes
CREATE INDEX idx_api_keys_key_hash ON api_keys(key_hash);
CREATE INDEX idx_api_keys_active ON api_keys(is_active) WHERE is_active = true;
CREATE INDEX idx_api_keys_created_by ON api_keys(created_by);

-- MCP Connection logs indexes
CREATE INDEX idx_mcp_connections_client ON mcp_connections(client_id);
CREATE INDEX idx_mcp_connections_connected_at ON mcp_connections(connected_at);

-- MCP Request logs indexes
CREATE INDEX idx_mcp_requests_connection ON mcp_requests(connection_id);
CREATE INDEX idx_mcp_requests_tool ON mcp_requests(tool_name);
CREATE INDEX idx_mcp_requests_created_at ON mcp_requests(created_at);

-- ============================================================
-- FUNCTIONS AND TRIGGERS
-- ============================================================

-- Function to update the updated_at timestamp
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Triggers for updated_at
CREATE TRIGGER update_spring_projects_updated_at
    BEFORE UPDATE ON spring_projects
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_spring_boot_versions_updated_at
    BEFORE UPDATE ON spring_boot_versions
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_documentation_links_updated_at
    BEFORE UPDATE ON documentation_links
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_documentation_content_updated_at
    BEFORE UPDATE ON documentation_content
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_code_examples_updated_at
    BEFORE UPDATE ON code_examples
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_users_updated_at
    BEFORE UPDATE ON users
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_settings_updated_at
    BEFORE UPDATE ON settings
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_scheduler_settings_updated_at
    BEFORE UPDATE ON scheduler_settings
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- Function to update indexed_content for full-text search
CREATE OR REPLACE FUNCTION update_documentation_content_search()
RETURNS TRIGGER AS $$
BEGIN
    NEW.indexed_content = to_tsvector('english', COALESCE(NEW.content, ''));
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Trigger to auto-update search index
CREATE TRIGGER update_documentation_content_search_trigger
    BEFORE INSERT OR UPDATE OF content ON documentation_content
    FOR EACH ROW
    EXECUTE FUNCTION update_documentation_content_search();

-- ============================================================
-- INITIAL DATA (Sample Spring Projects)
-- ============================================================

-- Insert core Spring projects
INSERT INTO spring_projects (name, slug, description, homepage_url, github_url) VALUES
    ('Spring Boot', 'spring-boot', 'Spring Boot makes it easy to create stand-alone, production-grade Spring based Applications', 'https://spring.io/projects/spring-boot', 'https://github.com/spring-projects/spring-boot'),
    ('Spring Framework', 'spring-framework', 'The Spring Framework provides a comprehensive programming and configuration model', 'https://spring.io/projects/spring-framework', 'https://github.com/spring-projects/spring-framework'),
    ('Spring Data', 'spring-data', 'Spring Data provides a familiar and consistent Spring-based programming model for data access', 'https://spring.io/projects/spring-data', 'https://github.com/spring-projects/spring-data'),
    ('Spring Security', 'spring-security', 'Spring Security is a framework that provides authentication, authorization and protection', 'https://spring.io/projects/spring-security', 'https://github.com/spring-projects/spring-security'),
    ('Spring Cloud', 'spring-cloud', 'Spring Cloud provides tools for developers to quickly build common patterns in distributed systems', 'https://spring.io/projects/spring-cloud', 'https://github.com/spring-cloud'),
    ('Spring Batch', 'spring-batch', 'Spring Batch provides reusable functions that are essential in processing large volumes of records, including logging/tracing, transaction management, job processing statistics, job restart, skip, and resource management.', 'https://spring.io/projects/spring-batch', 'https://github.com/spring-projects/spring-batch');
