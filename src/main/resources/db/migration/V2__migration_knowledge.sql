-- ============================================
-- Migration Knowledge Tables
-- Based on OpenRewrite recipe structure
-- ============================================

-- Migration recipes (composite transformations)
CREATE TABLE migration_recipes (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    display_name VARCHAR(255) NOT NULL,
    description TEXT,
    from_project VARCHAR(100) NOT NULL,        -- e.g., 'spring-boot'
    from_version_min VARCHAR(50) NOT NULL,     -- e.g., '3.0.0'
    from_version_max VARCHAR(50),              -- e.g., '3.5.99' (null = any)
    to_version VARCHAR(50) NOT NULL,           -- e.g., '4.0.0'
    source_url VARCHAR(500),                   -- OpenRewrite docs URL
    source_type VARCHAR(50) DEFAULT 'OPENREWRITE', -- OPENREWRITE, SPRING_DOCS, MANUAL
    license VARCHAR(100),
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_recipe_name UNIQUE (name)
);

-- Individual transformations within recipes
CREATE TABLE migration_transformations (
    id BIGSERIAL PRIMARY KEY,
    recipe_id BIGINT NOT NULL REFERENCES migration_recipes(id) ON DELETE CASCADE,
    transformation_type VARCHAR(50) NOT NULL,  -- IMPORT, DEPENDENCY, PROPERTY, CODE, BUILD, TEMPLATE, ANNOTATION, CONFIG
    category VARCHAR(100),                     -- actuator, security, data, web, etc.
    subcategory VARCHAR(100),                  -- health, oauth2, jpa, etc.
    old_pattern TEXT NOT NULL,
    new_pattern TEXT NOT NULL,
    file_pattern VARCHAR(255),                 -- *.java, build.gradle, application.yml, *.html
    regex_pattern BOOLEAN DEFAULT false,       -- Is old_pattern a regex?
    explanation TEXT,
    code_example TEXT,                         -- Full code example if applicable
    additional_steps TEXT,                     -- Manual steps required
    breaking_change BOOLEAN DEFAULT false,
    severity VARCHAR(20) DEFAULT 'INFO',       -- INFO, WARNING, ERROR, CRITICAL
    priority INT DEFAULT 0,                    -- Higher = more important
    tags TEXT[],                               -- Array of tags for search
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Version compatibility matrix
CREATE TABLE version_compatibility (
    id BIGSERIAL PRIMARY KEY,
    spring_boot_version VARCHAR(50) NOT NULL,
    dependency_group VARCHAR(100) NOT NULL,    -- e.g., 'org.springframework.security'
    dependency_artifact VARCHAR(100) NOT NULL, -- e.g., 'spring-security-core'
    compatible_version VARCHAR(50) NOT NULL,   -- e.g., '7.0.0'
    notes TEXT,
    verified BOOLEAN DEFAULT false,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_compatibility UNIQUE (spring_boot_version, dependency_group, dependency_artifact)
);

-- Deprecation tracking
CREATE TABLE deprecation_replacements (
    id BIGSERIAL PRIMARY KEY,
    deprecated_class VARCHAR(500) NOT NULL,
    deprecated_method VARCHAR(255),            -- null = entire class deprecated
    replacement_class VARCHAR(500),
    replacement_method VARCHAR(255),
    deprecated_since VARCHAR(50),              -- Version deprecated
    removed_in VARCHAR(50),                    -- Version removed (null = not yet)
    migration_notes TEXT,
    code_before TEXT,
    code_after TEXT,
    project_slug VARCHAR(100) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_deprecation UNIQUE (deprecated_class, deprecated_method)
);

-- Recipe to Project mapping (many-to-many)
CREATE TABLE recipe_project_mapping (
    id BIGSERIAL PRIMARY KEY,
    recipe_id BIGINT NOT NULL REFERENCES migration_recipes(id) ON DELETE CASCADE,
    project_id BIGINT NOT NULL REFERENCES spring_projects(id) ON DELETE CASCADE,
    relevance_score INT DEFAULT 100,  -- 0-100, higher = more relevant
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_recipe_project UNIQUE (recipe_id, project_id)
);

-- Recipe to Version mapping (which versions the recipe migrates between)
CREATE TABLE recipe_version_mapping (
    id BIGSERIAL PRIMARY KEY,
    recipe_id BIGINT NOT NULL REFERENCES migration_recipes(id) ON DELETE CASCADE,
    version_id BIGINT NOT NULL REFERENCES project_versions(id) ON DELETE CASCADE,
    mapping_type VARCHAR(20) NOT NULL,  -- SOURCE, TARGET
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_recipe_version UNIQUE (recipe_id, version_id, mapping_type)
);

-- Indexes for efficient querying
CREATE INDEX idx_recipes_project_version ON migration_recipes(from_project, to_version);
CREATE INDEX idx_recipes_active ON migration_recipes(is_active) WHERE is_active = true;
CREATE INDEX idx_transformations_recipe ON migration_transformations(recipe_id);
CREATE INDEX idx_transformations_type ON migration_transformations(transformation_type);
CREATE INDEX idx_transformations_breaking ON migration_transformations(breaking_change) WHERE breaking_change = true;
CREATE INDEX idx_transformations_category ON migration_transformations(category);
CREATE INDEX idx_transformations_tags ON migration_transformations USING GIN(tags);
CREATE INDEX idx_compatibility_boot ON version_compatibility(spring_boot_version);
CREATE INDEX idx_deprecation_class ON deprecation_replacements(deprecated_class);
CREATE INDEX idx_deprecation_project ON deprecation_replacements(project_slug);
CREATE INDEX idx_recipe_project_recipe ON recipe_project_mapping(recipe_id);
CREATE INDEX idx_recipe_project_project ON recipe_project_mapping(project_id);
CREATE INDEX idx_recipe_version_recipe ON recipe_version_mapping(recipe_id);
CREATE INDEX idx_recipe_version_version ON recipe_version_mapping(version_id);
CREATE INDEX idx_recipe_version_type ON recipe_version_mapping(mapping_type);

-- Full-text search on transformation explanations
ALTER TABLE migration_transformations ADD COLUMN search_vector TSVECTOR;
CREATE INDEX idx_transformations_search ON migration_transformations USING GIN(search_vector);

-- Trigger to update search vector
CREATE OR REPLACE FUNCTION update_transformation_search_vector()
RETURNS TRIGGER AS $$
BEGIN
    NEW.search_vector := to_tsvector('english',
        COALESCE(NEW.explanation, '') || ' ' ||
        COALESCE(NEW.category, '') || ' ' ||
        COALESCE(NEW.subcategory, '') || ' ' ||
        COALESCE(NEW.old_pattern, '') || ' ' ||
        COALESCE(NEW.new_pattern, '')
    );
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_transformation_search_vector
    BEFORE INSERT OR UPDATE ON migration_transformations
    FOR EACH ROW EXECUTE FUNCTION update_transformation_search_vector();

-- Full-text search on recipes
ALTER TABLE migration_recipes ADD COLUMN search_vector TSVECTOR;
CREATE INDEX idx_recipes_search ON migration_recipes USING GIN(search_vector);

-- Trigger to update recipe search vector
CREATE OR REPLACE FUNCTION update_recipe_search_vector()
RETURNS TRIGGER AS $$
BEGIN
    NEW.search_vector := to_tsvector('english',
        COALESCE(NEW.name, '') || ' ' ||
        COALESCE(NEW.display_name, '') || ' ' ||
        COALESCE(NEW.description, '') || ' ' ||
        COALESCE(NEW.from_project, '') || ' ' ||
        COALESCE(NEW.to_version, '')
    );
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_recipe_search_vector
    BEFORE INSERT OR UPDATE ON migration_recipes
    FOR EACH ROW EXECUTE FUNCTION update_recipe_search_vector();
