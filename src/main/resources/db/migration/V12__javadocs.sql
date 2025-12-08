-- =====================================================
-- V12: Javadocs and Reference Documentation Storage
-- Feature: Javadocs Downloader v1.4.2
-- =====================================================

-- -----------------------------------------------------
-- Table: javadoc_sync_status
-- Purpose: Track sync status per project (enabled/disabled, failures)
-- -----------------------------------------------------
CREATE TABLE javadoc_sync_status (
    id BIGSERIAL PRIMARY KEY,
    project_id BIGINT NOT NULL REFERENCES spring_projects(id) ON DELETE CASCADE,
    enabled BOOLEAN NOT NULL DEFAULT false,
    last_sync_at TIMESTAMP,
    failure_count INT NOT NULL DEFAULT 0,
    last_error TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_javadoc_sync_project UNIQUE (project_id)
);

COMMENT ON TABLE javadoc_sync_status IS 'Tracks Javadoc sync status per project with failure counting';
COMMENT ON COLUMN javadoc_sync_status.enabled IS 'Whether scheduled Javadoc sync is enabled for this project';
COMMENT ON COLUMN javadoc_sync_status.failure_count IS 'Consecutive failure count, auto-disables after threshold';
COMMENT ON COLUMN javadoc_sync_status.last_error IS 'Last error message if sync failed';

CREATE INDEX idx_javadoc_sync_enabled ON javadoc_sync_status(enabled) WHERE enabled = true;
CREATE INDEX idx_javadoc_sync_project ON javadoc_sync_status(project_id);

-- -----------------------------------------------------
-- Table: javadoc_packages
-- Purpose: Store package-level documentation
-- -----------------------------------------------------
CREATE TABLE javadoc_packages (
    id BIGSERIAL PRIMARY KEY,
    library_name VARCHAR(100) NOT NULL,
    version VARCHAR(50) NOT NULL,
    package_name VARCHAR(500) NOT NULL,
    summary TEXT,
    description TEXT,
    source_url VARCHAR(1000),
    indexed_content TSVECTOR,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_javadoc_package UNIQUE (library_name, version, package_name)
);

COMMENT ON TABLE javadoc_packages IS 'Stores package-level Javadoc documentation per library version';
COMMENT ON COLUMN javadoc_packages.library_name IS 'Library identifier (e.g., spring-ai, spring-boot)';
COMMENT ON COLUMN javadoc_packages.version IS 'Library version (e.g., 1.1.1, 3.5.8)';
COMMENT ON COLUMN javadoc_packages.package_name IS 'Full package name (e.g., org.springframework.ai.chat.client)';
COMMENT ON COLUMN javadoc_packages.indexed_content IS 'Full-text search index for package documentation';

CREATE INDEX idx_javadoc_pkg_library_version ON javadoc_packages(library_name, version);
CREATE INDEX idx_javadoc_pkg_name ON javadoc_packages(package_name);
CREATE INDEX idx_javadoc_pkg_search ON javadoc_packages USING GIN(indexed_content);

-- -----------------------------------------------------
-- Table: javadoc_classes
-- Purpose: Store class/interface/enum/annotation documentation
-- -----------------------------------------------------
CREATE TABLE javadoc_classes (
    id BIGSERIAL PRIMARY KEY,
    package_id BIGINT NOT NULL REFERENCES javadoc_packages(id) ON DELETE CASCADE,
    fqcn VARCHAR(500) NOT NULL,
    simple_name VARCHAR(255) NOT NULL,
    kind VARCHAR(20) NOT NULL,
    modifiers VARCHAR(100),
    summary TEXT,
    description TEXT,
    super_class VARCHAR(500),
    interfaces TEXT[],
    source_url VARCHAR(1000),
    deprecated BOOLEAN NOT NULL DEFAULT false,
    deprecated_message TEXT,
    annotations TEXT[],
    metadata JSONB,
    indexed_content TSVECTOR,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_javadoc_class UNIQUE (package_id, fqcn)
);

COMMENT ON TABLE javadoc_classes IS 'Stores class-level Javadoc documentation (classes, interfaces, enums, annotations, records)';
COMMENT ON COLUMN javadoc_classes.fqcn IS 'Fully qualified class name (e.g., org.springframework.ai.chat.client.ChatClient)';
COMMENT ON COLUMN javadoc_classes.kind IS 'Type: CLASS, INTERFACE, ENUM, ANNOTATION, RECORD';
COMMENT ON COLUMN javadoc_classes.modifiers IS 'Access modifiers (e.g., public, abstract, final)';
COMMENT ON COLUMN javadoc_classes.interfaces IS 'Array of implemented interface FQCNs';
COMMENT ON COLUMN javadoc_classes.metadata IS 'Additional metadata as JSON (type parameters, nested classes, etc.)';

CREATE INDEX idx_javadoc_class_fqcn ON javadoc_classes(fqcn);
CREATE INDEX idx_javadoc_class_simple ON javadoc_classes(simple_name);
CREATE INDEX idx_javadoc_class_kind ON javadoc_classes(kind);
CREATE INDEX idx_javadoc_class_package ON javadoc_classes(package_id);
CREATE INDEX idx_javadoc_class_pkg_kind ON javadoc_classes(package_id, kind);
CREATE INDEX idx_javadoc_class_search ON javadoc_classes USING GIN(indexed_content);
CREATE INDEX idx_javadoc_class_interfaces ON javadoc_classes USING GIN(interfaces);
CREATE INDEX idx_javadoc_class_annotations ON javadoc_classes USING GIN(annotations);
CREATE INDEX idx_javadoc_class_metadata ON javadoc_classes USING GIN(metadata);

-- -----------------------------------------------------
-- Table: javadoc_methods
-- Purpose: Store method documentation
-- -----------------------------------------------------
CREATE TABLE javadoc_methods (
    id BIGSERIAL PRIMARY KEY,
    class_id BIGINT NOT NULL REFERENCES javadoc_classes(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    signature TEXT NOT NULL,
    return_type VARCHAR(500),
    parameters JSONB,
    throws_list TEXT[],
    summary TEXT,
    description TEXT,
    deprecated BOOLEAN NOT NULL DEFAULT false,
    deprecated_message TEXT,
    annotations TEXT[],
    metadata JSONB,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE javadoc_methods IS 'Stores method-level Javadoc documentation';
COMMENT ON COLUMN javadoc_methods.signature IS 'Full method signature including generics';
COMMENT ON COLUMN javadoc_methods.parameters IS 'JSON array of {name, type, description} objects';
COMMENT ON COLUMN javadoc_methods.throws_list IS 'Array of exception types that may be thrown';
COMMENT ON COLUMN javadoc_methods.metadata IS 'Additional metadata (type parameters, default values, etc.)';

CREATE INDEX idx_javadoc_method_class ON javadoc_methods(class_id);
CREATE INDEX idx_javadoc_method_name ON javadoc_methods(name);
CREATE INDEX idx_javadoc_method_class_name ON javadoc_methods(class_id, name);
CREATE INDEX idx_javadoc_method_deprecated ON javadoc_methods(deprecated) WHERE deprecated = true;
CREATE INDEX idx_javadoc_method_params ON javadoc_methods USING GIN(parameters);

-- -----------------------------------------------------
-- Table: javadoc_fields
-- Purpose: Store field documentation
-- -----------------------------------------------------
CREATE TABLE javadoc_fields (
    id BIGSERIAL PRIMARY KEY,
    class_id BIGINT NOT NULL REFERENCES javadoc_classes(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    type VARCHAR(500) NOT NULL,
    modifiers VARCHAR(100),
    summary TEXT,
    deprecated BOOLEAN NOT NULL DEFAULT false,
    constant_value TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE javadoc_fields IS 'Stores field-level Javadoc documentation';
COMMENT ON COLUMN javadoc_fields.modifiers IS 'Field modifiers (e.g., public static final)';
COMMENT ON COLUMN javadoc_fields.constant_value IS 'Compile-time constant value if applicable';

CREATE INDEX idx_javadoc_field_class ON javadoc_fields(class_id);
CREATE INDEX idx_javadoc_field_name ON javadoc_fields(name);

-- -----------------------------------------------------
-- Table: javadoc_constructors
-- Purpose: Store constructor documentation
-- -----------------------------------------------------
CREATE TABLE javadoc_constructors (
    id BIGSERIAL PRIMARY KEY,
    class_id BIGINT NOT NULL REFERENCES javadoc_classes(id) ON DELETE CASCADE,
    signature TEXT NOT NULL,
    parameters JSONB,
    throws_list TEXT[],
    summary TEXT,
    deprecated BOOLEAN NOT NULL DEFAULT false,
    annotations TEXT[],
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE javadoc_constructors IS 'Stores constructor-level Javadoc documentation';
COMMENT ON COLUMN javadoc_constructors.signature IS 'Full constructor signature';
COMMENT ON COLUMN javadoc_constructors.parameters IS 'JSON array of {name, type, description} objects';

CREATE INDEX idx_javadoc_constructor_class ON javadoc_constructors(class_id);

-- -----------------------------------------------------
-- Triggers: Auto-update indexed_content tsvector columns
-- -----------------------------------------------------

-- Trigger function for javadoc_packages
CREATE OR REPLACE FUNCTION update_javadoc_package_search_index()
RETURNS TRIGGER AS $$
BEGIN
    NEW.indexed_content := to_tsvector('english',
        COALESCE(NEW.package_name, '') || ' ' ||
        COALESCE(NEW.summary, '') || ' ' ||
        COALESCE(NEW.description, '')
    );
    NEW.updated_at := CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_javadoc_package_search_index
    BEFORE INSERT OR UPDATE ON javadoc_packages
    FOR EACH ROW
    EXECUTE FUNCTION update_javadoc_package_search_index();

-- Trigger function for javadoc_classes
CREATE OR REPLACE FUNCTION update_javadoc_class_search_index()
RETURNS TRIGGER AS $$
BEGIN
    NEW.indexed_content := to_tsvector('english',
        COALESCE(NEW.fqcn, '') || ' ' ||
        COALESCE(NEW.simple_name, '') || ' ' ||
        COALESCE(NEW.summary, '') || ' ' ||
        COALESCE(NEW.description, '')
    );
    NEW.updated_at := CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_javadoc_class_search_index
    BEFORE INSERT OR UPDATE ON javadoc_classes
    FOR EACH ROW
    EXECUTE FUNCTION update_javadoc_class_search_index();

-- Trigger for javadoc_sync_status updated_at
CREATE OR REPLACE FUNCTION update_javadoc_sync_status_timestamp()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at := CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_javadoc_sync_status_updated
    BEFORE UPDATE ON javadoc_sync_status
    FOR EACH ROW
    EXECUTE FUNCTION update_javadoc_sync_status_timestamp();

-- -----------------------------------------------------
-- Summary
-- -----------------------------------------------------
-- Tables created:
--   1. javadoc_sync_status - Per-project sync tracking
--   2. javadoc_packages - Package documentation
--   3. javadoc_classes - Class/interface/enum documentation
--   4. javadoc_methods - Method documentation
--   5. javadoc_fields - Field documentation
--   6. javadoc_constructors - Constructor documentation
--
-- Indexes created:
--   - GIN indexes for full-text search (tsvector)
--   - GIN indexes for array columns (interfaces, annotations)
--   - GIN indexes for JSONB columns (metadata, parameters)
--   - B-tree indexes for common lookups (fqcn, name, kind)
--
-- Triggers created:
--   - Auto-update indexed_content for packages and classes
--   - Auto-update updated_at timestamp
