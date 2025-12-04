-- Spring MCP Server - Flavor Groups Feature
-- Version: 1.3.3
-- Description: Team-based authorization and organization for Flavors
-- Date: 2025-12-04

-- ============================================================
-- FLAVOR GROUPS TABLE - Main groups entity
-- ============================================================
CREATE TABLE flavor_groups (
    id BIGSERIAL PRIMARY KEY,

    -- Identification (unique_name is immutable after creation)
    unique_name VARCHAR(255) NOT NULL,
    display_name VARCHAR(255) NOT NULL,
    description TEXT,

    -- Status (inactive groups are completely hidden from UI and MCP)
    is_active BOOLEAN NOT NULL DEFAULT true,

    -- Audit fields
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),

    -- Constraints
    CONSTRAINT uk_flavor_groups_unique_name UNIQUE (unique_name),
    CONSTRAINT chk_flavor_groups_name_length CHECK (LENGTH(unique_name) >= 3)
);

-- Comments
COMMENT ON TABLE flavor_groups IS 'Flavor groups for team-based authorization and organization';
COMMENT ON COLUMN flavor_groups.unique_name IS 'Unique identifier for the group (immutable, URL-friendly)';
COMMENT ON COLUMN flavor_groups.is_active IS 'When false, group and all its flavors are hidden from UI and MCP';

-- Indexes
CREATE INDEX idx_flavor_groups_active ON flavor_groups(is_active) WHERE is_active = true;
CREATE INDEX idx_flavor_groups_name ON flavor_groups(unique_name);
CREATE INDEX idx_flavor_groups_updated ON flavor_groups(updated_at DESC);

-- ============================================================
-- GROUP USER MEMBERS - Users belonging to a group
-- ============================================================
CREATE TABLE group_user_members (
    id BIGSERIAL PRIMARY KEY,
    group_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,

    -- Audit fields
    added_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    added_by VARCHAR(100),

    -- Foreign keys with CASCADE DELETE
    CONSTRAINT fk_group_user_members_group FOREIGN KEY (group_id)
        REFERENCES flavor_groups(id) ON DELETE CASCADE,
    CONSTRAINT fk_group_user_members_user FOREIGN KEY (user_id)
        REFERENCES users(id) ON DELETE CASCADE,

    -- Unique constraint (user can only be member once per group)
    CONSTRAINT uk_group_user_member UNIQUE (group_id, user_id)
);

-- Comments
COMMENT ON TABLE group_user_members IS 'Junction table for group membership (users)';
COMMENT ON COLUMN group_user_members.group_id IS 'Reference to the flavor group';
COMMENT ON COLUMN group_user_members.user_id IS 'Reference to the user';

-- Indexes for efficient queries
CREATE INDEX idx_group_user_members_group ON group_user_members(group_id);
CREATE INDEX idx_group_user_members_user ON group_user_members(user_id);

-- ============================================================
-- GROUP API KEY MEMBERS - API Keys belonging to a group
-- ============================================================
CREATE TABLE group_apikey_members (
    id BIGSERIAL PRIMARY KEY,
    group_id BIGINT NOT NULL,
    api_key_id BIGINT NOT NULL,

    -- Audit fields
    added_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    added_by VARCHAR(100),

    -- Foreign keys with CASCADE DELETE
    CONSTRAINT fk_group_apikey_members_group FOREIGN KEY (group_id)
        REFERENCES flavor_groups(id) ON DELETE CASCADE,
    CONSTRAINT fk_group_apikey_members_apikey FOREIGN KEY (api_key_id)
        REFERENCES api_keys(id) ON DELETE CASCADE,

    -- Unique constraint (API key can only be member once per group)
    CONSTRAINT uk_group_apikey_member UNIQUE (group_id, api_key_id)
);

-- Comments
COMMENT ON TABLE group_apikey_members IS 'Junction table for group membership (API keys)';
COMMENT ON COLUMN group_apikey_members.group_id IS 'Reference to the flavor group';
COMMENT ON COLUMN group_apikey_members.api_key_id IS 'Reference to the API key';

-- Indexes for efficient queries
CREATE INDEX idx_group_apikey_members_group ON group_apikey_members(group_id);
CREATE INDEX idx_group_apikey_members_apikey ON group_apikey_members(api_key_id);

-- ============================================================
-- GROUP FLAVORS - Flavors belonging to a group
-- ============================================================
CREATE TABLE group_flavors (
    id BIGSERIAL PRIMARY KEY,
    group_id BIGINT NOT NULL,
    flavor_id BIGINT NOT NULL,

    -- Audit fields
    added_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    added_by VARCHAR(100),

    -- Foreign keys with CASCADE DELETE
    CONSTRAINT fk_group_flavors_group FOREIGN KEY (group_id)
        REFERENCES flavor_groups(id) ON DELETE CASCADE,
    CONSTRAINT fk_group_flavors_flavor FOREIGN KEY (flavor_id)
        REFERENCES flavors(id) ON DELETE CASCADE,

    -- Unique constraint (flavor can only be in each group once)
    CONSTRAINT uk_group_flavor UNIQUE (group_id, flavor_id)
);

-- Comments
COMMENT ON TABLE group_flavors IS 'Junction table for flavor-group association';
COMMENT ON COLUMN group_flavors.group_id IS 'Reference to the flavor group';
COMMENT ON COLUMN group_flavors.flavor_id IS 'Reference to the flavor';

-- Indexes for efficient queries
CREATE INDEX idx_group_flavors_group ON group_flavors(group_id);
CREATE INDEX idx_group_flavors_flavor ON group_flavors(flavor_id);

-- ============================================================
-- VIEW: Group Visibility (determines public vs private)
-- A group is PUBLIC if it has NO members (users or API keys)
-- ============================================================
CREATE OR REPLACE VIEW v_group_visibility AS
SELECT
    fg.id,
    fg.unique_name,
    fg.display_name,
    fg.description,
    fg.is_active,
    fg.created_at,
    fg.updated_at,
    -- Count members
    COALESCE(user_counts.user_count, 0) AS user_member_count,
    COALESCE(apikey_counts.apikey_count, 0) AS apikey_member_count,
    COALESCE(flavor_counts.flavor_count, 0) AS flavor_count,
    -- Determine if public (no members = public)
    CASE
        WHEN COALESCE(user_counts.user_count, 0) = 0
         AND COALESCE(apikey_counts.apikey_count, 0) = 0
        THEN true
        ELSE false
    END AS is_public
FROM flavor_groups fg
LEFT JOIN (
    SELECT group_id, COUNT(*) AS user_count
    FROM group_user_members
    GROUP BY group_id
) user_counts ON user_counts.group_id = fg.id
LEFT JOIN (
    SELECT group_id, COUNT(*) AS apikey_count
    FROM group_apikey_members
    GROUP BY group_id
) apikey_counts ON apikey_counts.group_id = fg.id
LEFT JOIN (
    SELECT group_id, COUNT(*) AS flavor_count
    FROM group_flavors
    GROUP BY group_id
) flavor_counts ON flavor_counts.group_id = fg.id;

-- Comment on view
COMMENT ON VIEW v_group_visibility IS 'View showing group visibility status (public if no members)';

-- ============================================================
-- FUNCTION: Get accessible flavor IDs for an API key
-- Returns: unassigned flavors + public group flavors + member group flavors
-- ============================================================
CREATE OR REPLACE FUNCTION get_accessible_flavor_ids_for_apikey(p_api_key_id BIGINT)
RETURNS TABLE (
    flavor_id BIGINT,
    access_type VARCHAR(20),
    group_unique_name VARCHAR(255)
) AS $$
BEGIN
    RETURN QUERY
    -- 1. Unassigned flavors (not in any group) - visible to everyone
    SELECT f.id, 'UNASSIGNED'::VARCHAR(20), NULL::VARCHAR(255)
    FROM flavors f
    WHERE f.is_active = true
    AND NOT EXISTS (
        SELECT 1 FROM group_flavors gf WHERE gf.flavor_id = f.id
    )

    UNION ALL

    -- 2. Public group flavors (group has no members AND is active)
    SELECT f.id, 'PUBLIC'::VARCHAR(20), fg.unique_name
    FROM flavors f
    JOIN group_flavors gf ON gf.flavor_id = f.id
    JOIN flavor_groups fg ON fg.id = gf.group_id
    WHERE f.is_active = true
    AND fg.is_active = true
    AND NOT EXISTS (SELECT 1 FROM group_user_members gum WHERE gum.group_id = fg.id)
    AND NOT EXISTS (SELECT 1 FROM group_apikey_members gam WHERE gam.group_id = fg.id)

    UNION ALL

    -- 3. Private group flavors where API key is a member (and group is active)
    SELECT f.id, 'PRIVATE'::VARCHAR(20), fg.unique_name
    FROM flavors f
    JOIN group_flavors gf ON gf.flavor_id = f.id
    JOIN flavor_groups fg ON fg.id = gf.group_id
    JOIN group_apikey_members gam ON gam.group_id = fg.id
    WHERE f.is_active = true
    AND fg.is_active = true
    AND gam.api_key_id = p_api_key_id;
END;
$$ LANGUAGE plpgsql;

-- Comment on function
COMMENT ON FUNCTION get_accessible_flavor_ids_for_apikey(BIGINT) IS
    'Returns all flavor IDs accessible to a specific API key (unassigned + public + member groups)';

-- ============================================================
-- FUNCTION: Get accessible flavor IDs for a user
-- Returns: unassigned flavors + public group flavors + member group flavors
-- ============================================================
CREATE OR REPLACE FUNCTION get_accessible_flavor_ids_for_user(p_user_id BIGINT)
RETURNS TABLE (
    flavor_id BIGINT,
    access_type VARCHAR(20),
    group_unique_name VARCHAR(255)
) AS $$
BEGIN
    RETURN QUERY
    -- 1. Unassigned flavors (not in any group) - visible to everyone
    SELECT f.id, 'UNASSIGNED'::VARCHAR(20), NULL::VARCHAR(255)
    FROM flavors f
    WHERE f.is_active = true
    AND NOT EXISTS (
        SELECT 1 FROM group_flavors gf WHERE gf.flavor_id = f.id
    )

    UNION ALL

    -- 2. Public group flavors (group has no members AND is active)
    SELECT f.id, 'PUBLIC'::VARCHAR(20), fg.unique_name
    FROM flavors f
    JOIN group_flavors gf ON gf.flavor_id = f.id
    JOIN flavor_groups fg ON fg.id = gf.group_id
    WHERE f.is_active = true
    AND fg.is_active = true
    AND NOT EXISTS (SELECT 1 FROM group_user_members gum WHERE gum.group_id = fg.id)
    AND NOT EXISTS (SELECT 1 FROM group_apikey_members gam WHERE gam.group_id = fg.id)

    UNION ALL

    -- 3. Private group flavors where user is a member (and group is active)
    SELECT f.id, 'PRIVATE'::VARCHAR(20), fg.unique_name
    FROM flavors f
    JOIN group_flavors gf ON gf.flavor_id = f.id
    JOIN flavor_groups fg ON fg.id = gf.group_id
    JOIN group_user_members gum ON gum.group_id = fg.id
    WHERE f.is_active = true
    AND fg.is_active = true
    AND gum.user_id = p_user_id;
END;
$$ LANGUAGE plpgsql;

-- Comment on function
COMMENT ON FUNCTION get_accessible_flavor_ids_for_user(BIGINT) IS
    'Returns all flavor IDs accessible to a specific user (unassigned + public + member groups)';

-- ============================================================
-- FUNCTION: Check if API key has access to a specific flavor
-- ============================================================
CREATE OR REPLACE FUNCTION apikey_can_access_flavor(p_api_key_id BIGINT, p_flavor_id BIGINT)
RETURNS BOOLEAN AS $$
DECLARE
    v_flavor_active BOOLEAN;
    v_has_groups BOOLEAN;
    v_in_public_group BOOLEAN;
    v_in_member_group BOOLEAN;
BEGIN
    -- Check if flavor exists and is active
    SELECT is_active INTO v_flavor_active
    FROM flavors WHERE id = p_flavor_id;

    IF NOT FOUND OR NOT v_flavor_active THEN
        RETURN false;
    END IF;

    -- Check if flavor is in any group
    SELECT EXISTS (
        SELECT 1 FROM group_flavors WHERE flavor_id = p_flavor_id
    ) INTO v_has_groups;

    -- If not in any group, it's accessible to everyone
    IF NOT v_has_groups THEN
        RETURN true;
    END IF;

    -- Check if flavor is in a public active group
    SELECT EXISTS (
        SELECT 1
        FROM group_flavors gf
        JOIN flavor_groups fg ON fg.id = gf.group_id
        WHERE gf.flavor_id = p_flavor_id
        AND fg.is_active = true
        AND NOT EXISTS (SELECT 1 FROM group_user_members gum WHERE gum.group_id = fg.id)
        AND NOT EXISTS (SELECT 1 FROM group_apikey_members gam WHERE gam.group_id = fg.id)
    ) INTO v_in_public_group;

    IF v_in_public_group THEN
        RETURN true;
    END IF;

    -- Check if flavor is in a group where API key is a member (and group is active)
    SELECT EXISTS (
        SELECT 1
        FROM group_flavors gf
        JOIN flavor_groups fg ON fg.id = gf.group_id
        JOIN group_apikey_members gam ON gam.group_id = fg.id
        WHERE gf.flavor_id = p_flavor_id
        AND fg.is_active = true
        AND gam.api_key_id = p_api_key_id
    ) INTO v_in_member_group;

    RETURN v_in_member_group;
END;
$$ LANGUAGE plpgsql;

-- Comment on function
COMMENT ON FUNCTION apikey_can_access_flavor(BIGINT, BIGINT) IS
    'Returns true if the API key can access the specified flavor';

-- ============================================================
-- TRIGGER: Update updated_at on flavor_groups changes
-- ============================================================
CREATE OR REPLACE FUNCTION flavor_groups_update_timestamp()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_flavor_groups_update
    BEFORE UPDATE ON flavor_groups
    FOR EACH ROW
    EXECUTE FUNCTION flavor_groups_update_timestamp();

-- ============================================================
-- SAMPLE DATA: Create sample public and private groups
-- ============================================================

-- Public Group: Engineering Standards (no members = public)
INSERT INTO flavor_groups (unique_name, display_name, description, created_by)
VALUES (
    'engineering-standards',
    'Engineering Standards',
    'Company-wide coding standards, conventions, and best practices visible to all teams',
    'system'
);

-- Public Group: Spring Boot Patterns (no members = public)
INSERT INTO flavor_groups (unique_name, display_name, description, created_by)
VALUES (
    'spring-boot-patterns',
    'Spring Boot Patterns',
    'Common Spring Boot architecture patterns and templates',
    'system'
);

-- Add some existing flavors to public groups
-- (These will be visible to everyone)

-- Add java-naming-conventions to Engineering Standards
INSERT INTO group_flavors (group_id, flavor_id, added_by)
SELECT
    (SELECT id FROM flavor_groups WHERE unique_name = 'engineering-standards'),
    f.id,
    'system'
FROM flavors f
WHERE f.unique_name = 'java-naming-conventions'
AND EXISTS (SELECT 1 FROM flavors WHERE unique_name = 'java-naming-conventions');

-- Add hexagonal-spring-boot to Spring Boot Patterns
INSERT INTO group_flavors (group_id, flavor_id, added_by)
SELECT
    (SELECT id FROM flavor_groups WHERE unique_name = 'spring-boot-patterns'),
    f.id,
    'system'
FROM flavors f
WHERE f.unique_name = 'hexagonal-spring-boot'
AND EXISTS (SELECT 1 FROM flavors WHERE unique_name = 'hexagonal-spring-boot');

-- Add spring-boot-microservice-init to Spring Boot Patterns
INSERT INTO group_flavors (group_id, flavor_id, added_by)
SELECT
    (SELECT id FROM flavor_groups WHERE unique_name = 'spring-boot-patterns'),
    f.id,
    'system'
FROM flavors f
WHERE f.unique_name = 'spring-boot-microservice-init'
AND EXISTS (SELECT 1 FROM flavors WHERE unique_name = 'spring-boot-microservice-init');
