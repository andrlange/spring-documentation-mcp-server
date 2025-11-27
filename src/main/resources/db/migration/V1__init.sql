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
create TABLE spring_projects (
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

comment on table spring_projects is 'Stores Spring ecosystem projects (Spring Boot, Framework, Data, etc.)';

-- ============================================================
-- Spring Boot Versions Table (PRIMARY/CENTRAL TABLE)
-- ============================================================
create TABLE spring_boot_versions (
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

comment on table spring_boot_versions is 'Central table for Spring Boot versions - primary filter for entire system';
comment on column spring_boot_versions.state is 'Version state: SNAPSHOT (development), MILESTONE (M1-Mn), RC (release candidate), GA (general availability/stable)';
comment on column spring_boot_versions.is_current is 'Whether this is the current/recommended Spring Boot version';
comment on column spring_boot_versions.oss_support_end is 'OSS (Open Source Software) support end date';
comment on column spring_boot_versions.enterprise_support_end is 'Enterprise/Commercial support end date';

-- ============================================================
-- Project Versions Table
-- ============================================================
create TABLE project_versions (
    id BIGSERIAL PRIMARY KEY,
    project_id BIGINT NOT NULL REFERENCES spring_projects(id) ON delete CASCADE,
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

comment on table project_versions is 'Stores version information for each Spring project';
comment on column project_versions.state is 'Version state: SNAPSHOT, MILESTONE, RC, GA';
comment on column project_versions.release_date is 'Initial release date';
comment on column project_versions.oss_support_end is 'OSS (Open Source Software) support end date';
comment on column project_versions.enterprise_support_end is 'Enterprise/Commercial support end date (End of Life)';
comment on column project_versions.reference_doc_url is 'URL to the reference documentation for this version';
comment on column project_versions.api_doc_url is 'URL to the API documentation (Javadoc) for this version';
comment on column project_versions.status is 'Version status: CURRENT, GA, PRE, SNAPSHOT';

-- ============================================================
-- Spring Boot Compatibility Table (Junction Table)
-- ============================================================
create TABLE spring_boot_compatibility (
    id BIGSERIAL PRIMARY KEY,
    spring_boot_version_id BIGINT NOT NULL REFERENCES spring_boot_versions(id) ON delete CASCADE,
    compatible_project_version_id BIGINT NOT NULL REFERENCES project_versions(id) ON delete CASCADE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(spring_boot_version_id, compatible_project_version_id)
);

comment on table spring_boot_compatibility is 'Maps Spring Boot versions to compatible versions of other Spring projects';

-- ============================================================
-- Project Relationships Table (Parent/Child Projects)
-- ============================================================
create TABLE project_relationships (
    id BIGSERIAL PRIMARY KEY,
    parent_project_id BIGINT NOT NULL REFERENCES spring_projects(id) ON delete CASCADE,
    child_project_id BIGINT NOT NULL REFERENCES spring_projects(id) ON delete CASCADE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(parent_project_id, child_project_id),
    CONSTRAINT check_not_self_reference CHECK (parent_project_id != child_project_id)
);

comment on table project_relationships is 'Links parent projects to their subprojects (e.g., Spring Data → JPA, MongoDB, Redis)';

-- ============================================================
-- Documentation Types Table
-- ============================================================
create TABLE documentation_types (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    slug VARCHAR(100) NOT NULL UNIQUE,
    display_order INT DEFAULT 0
);

comment on table documentation_types is 'Types of documentation (Overview, Learn, Support, Samples)';

-- Insert default documentation types
insert into documentation_types (name, slug, display_order) values
    ('Overview', 'overview', 1),
    ('Learn', 'learn', 2),
    ('Support', 'support', 3),
    ('Samples', 'samples', 4),
    ('API Reference', 'api-reference', 5),
    ('Guides', 'guides', 6);

-- ============================================================
-- Documentation Links Table
-- ============================================================
create TABLE documentation_links (
    id BIGSERIAL PRIMARY KEY,
    version_id BIGINT NOT NULL REFERENCES project_versions(id) ON delete CASCADE,
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

comment on table documentation_links is 'Links to documentation for each project version';

-- ============================================================
-- Cached Documentation Content Table
-- ============================================================
create TABLE documentation_content (
    id BIGSERIAL PRIMARY KEY,
    link_id BIGINT NOT NULL REFERENCES documentation_links(id) ON delete CASCADE,
    content_type VARCHAR(50),
    content TEXT,
    metadata JSONB,
    indexed_content TSVECTOR,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT unique_link_content UNIQUE(link_id)
);

comment on table documentation_content is 'Cached content of documentation for search and display';

-- ============================================================
-- External Documentation Sources Table
-- ============================================================
create TABLE external_docs (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    base_url VARCHAR(500) NOT NULL,
    doc_type VARCHAR(100),
    related_project_id BIGINT REFERENCES spring_projects(id),
    active BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

comment on table external_docs is 'External documentation sources (SpringDoc, Baeldung, etc.)';

-- ============================================================
-- Code Examples Table
-- ============================================================
create TABLE code_examples (
    id BIGSERIAL PRIMARY KEY,
    version_id BIGINT NOT NULL REFERENCES project_versions(id) ON delete CASCADE,
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

comment on table code_examples is 'Code examples for different Spring project versions';

-- ============================================================
-- Users Table (for Management UI)
-- ============================================================
create TABLE users (
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

comment on table users is 'Users with access to the management UI';
comment on column users.role is 'User role: ADMIN (full access) or VIEWER (read-only)';
comment on column users.enabled is 'Technical flag for Spring Security - whether account is not locked/expired';
comment on column users.is_active is 'Business flag - whether user account is active and can login';

-- Insert default admin user (password: admin - should be changed in production!)
-- BCrypt hash for 'admin' with strength 10
insert into users (username, password, email, role, enabled, is_active) values
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

comment on table settings is 'System-wide settings (singleton - only one row)';

-- Insert default settings row
insert into settings (enterprise_subscription_enabled) values (false);

-- Add a constraint to ensure only one row exists in the settings table
create unique index idx_settings_singleton on settings ((id IS NOT NULL));

-- ============================================================
-- Scheduler Settings Table (Singleton Pattern)
-- ============================================================
create TABLE scheduler_settings (
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

comment on table scheduler_settings is 'Stores configuration for automatic comprehensive synchronization scheduling';
comment on column scheduler_settings.sync_enabled is 'Whether automatic sync is enabled';
comment on column scheduler_settings.sync_time is 'Time to run sync in HH:mm 24-hour format';
comment on column scheduler_settings.time_format is 'Display format preference: 12h or 24h';
comment on column scheduler_settings.last_sync_run is 'Timestamp of last automatic sync execution';
comment on column scheduler_settings.next_sync_run is 'Calculated timestamp for next scheduled sync';

-- Insert default scheduler settings with calculated next_sync_run
insert into scheduler_settings (sync_enabled, sync_time, time_format, next_sync_run)
values (
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
create TABLE api_keys (
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

comment on table api_keys is 'API keys for MCP endpoint authentication';
comment on column api_keys.name is 'Human-readable name for the API key (minimum 3 characters)';
comment on column api_keys.key_hash is 'BCrypt hashed API key (never store plain text)';
comment on column api_keys.created_by is 'Username of the user who created this API key';
comment on column api_keys.last_used_at is 'Timestamp of last successful authentication with this key';
comment on column api_keys.is_active is 'Whether this API key is currently active';

-- ============================================================
-- MCP Connection Logs Table
-- ============================================================
create TABLE mcp_connections (
    id BIGSERIAL PRIMARY KEY,
    client_id VARCHAR(255),
    connected_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    disconnected_at TIMESTAMP,
    requests_count INT DEFAULT 0
);

comment on table mcp_connections is 'Logs of MCP client connections';

-- ============================================================
-- MCP Request Logs Table
-- ============================================================
create TABLE mcp_requests (
    id BIGSERIAL PRIMARY KEY,
    connection_id BIGINT REFERENCES mcp_connections(id),
    tool_name VARCHAR(100),
    parameters JSONB,
    response_status VARCHAR(50),
    execution_time_ms INT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

comment on table mcp_requests is 'Logs of MCP tool requests and responses';

-- ============================================================
-- INDEXES FOR PERFORMANCE
-- ============================================================

-- Spring Boot Versions indexes
create index idx_spring_boot_versions_version on spring_boot_versions(version);
create index idx_spring_boot_versions_state on spring_boot_versions(state);
create index idx_spring_boot_versions_current on spring_boot_versions(is_current) WHERE is_current = true;
create index idx_spring_boot_versions_major_minor on spring_boot_versions(major_version, minor_version);

-- Project Versions indexes
create index idx_project_versions_project on project_versions(project_id);
create index idx_project_versions_latest on project_versions(is_latest) WHERE is_latest = true;
create index idx_project_versions_default on project_versions(is_default) WHERE is_default = true;
create index idx_project_versions_state on project_versions(state);
create index idx_project_versions_status on project_versions(status);
create index idx_project_versions_oss_support_end on project_versions(oss_support_end);
create index idx_project_versions_enterprise_support_end on project_versions(enterprise_support_end);

-- Spring Boot Compatibility indexes
create index idx_spring_boot_compatibility_boot_version on spring_boot_compatibility(spring_boot_version_id);
create index idx_spring_boot_compatibility_project_version on spring_boot_compatibility(compatible_project_version_id);

-- Project Relationships indexes
create index idx_project_relationships_parent on project_relationships(parent_project_id);
create index idx_project_relationships_child on project_relationships(child_project_id);

-- Documentation Links indexes
create index idx_documentation_links_version on documentation_links(version_id);
create index idx_documentation_links_type on documentation_links(doc_type_id);
create index idx_documentation_links_active on documentation_links(is_active) WHERE is_active = true;

-- Documentation Content indexes (for full-text search)
create index idx_documentation_content_search on documentation_content USING GIN(indexed_content);
create index idx_documentation_content_metadata on documentation_content USING GIN(metadata);

-- Code Examples indexes
create index idx_code_examples_version on code_examples(version_id);
create index idx_code_examples_language on code_examples(language);
create index idx_code_examples_category on code_examples(category);
create index idx_code_examples_tags on code_examples USING GIN(tags);

-- Users indexes
create index idx_users_username on users(username);
create index idx_users_role on users(role);
create index idx_users_enabled on users(enabled) WHERE enabled = true;
create index idx_users_is_active on users(is_active) WHERE is_active = true;

-- Scheduler Settings indexes
create index idx_scheduler_settings_enabled on scheduler_settings(sync_enabled);

-- API Keys indexes
create index idx_api_keys_key_hash on api_keys(key_hash);
create index idx_api_keys_active on api_keys(is_active) WHERE is_active = true;
create index idx_api_keys_created_by on api_keys(created_by);

-- MCP Connection logs indexes
create index idx_mcp_connections_client on mcp_connections(client_id);
create index idx_mcp_connections_connected_at on mcp_connections(connected_at);

-- MCP Request logs indexes
create index idx_mcp_requests_connection on mcp_requests(connection_id);
create index idx_mcp_requests_tool on mcp_requests(tool_name);
create index idx_mcp_requests_created_at on mcp_requests(created_at);

-- ============================================================
-- FUNCTIONS AND TRIGGERS
-- ============================================================

-- Function to update the updated_at timestamp
create or replace function update_updated_at_column()
RETURNS trigger AS $$
begin
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Triggers for updated_at
create trigger update_spring_projects_updated_at
    before update on spring_projects
    for each row
    EXECUTE function update_updated_at_column();

create trigger update_spring_boot_versions_updated_at
    before update on spring_boot_versions
    for each row
    EXECUTE function update_updated_at_column();

create trigger update_documentation_links_updated_at
    before update on documentation_links
    for each row
    EXECUTE function update_updated_at_column();

create trigger update_documentation_content_updated_at
    before update on documentation_content
    for each row
    EXECUTE function update_updated_at_column();

create trigger update_code_examples_updated_at
    before update on code_examples
    for each row
    EXECUTE function update_updated_at_column();

create trigger update_users_updated_at
    before update on users
    for each row
    EXECUTE function update_updated_at_column();

create trigger update_settings_updated_at
    before update on settings
    for each row
    EXECUTE function update_updated_at_column();

create trigger update_scheduler_settings_updated_at
    before update on scheduler_settings
    for each row
    EXECUTE function update_updated_at_column();

-- Function to update indexed_content for full-text search
create or replace function update_documentation_content_search()
RETURNS trigger AS $$
begin
    NEW.indexed_content = to_tsvector('english', COALESCE(NEW.content, ''));
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Trigger to auto-update search index
create trigger update_documentation_content_search_trigger
    before insert or update of content on documentation_content
    for each row
    EXECUTE function update_documentation_content_search();

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
