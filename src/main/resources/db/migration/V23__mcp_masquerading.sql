-- V23__mcp_masquerading.sql
-- MCP Tool Masquerading Configuration
-- Allows dynamic control over which MCP tools are exposed to LLMs

CREATE TABLE mcp_tools (
    id BIGSERIAL PRIMARY KEY,

    -- Tool identification
    tool_name VARCHAR(100) NOT NULL UNIQUE,
    tool_group VARCHAR(50) NOT NULL,  -- DOCUMENTATION, MIGRATION, LANGUAGE, FLAVORS, FLAVOR_GROUPS, INITIALIZR, JAVADOC

    -- Configuration
    is_enabled BOOLEAN NOT NULL DEFAULT true,
    description TEXT NOT NULL,
    original_description TEXT NOT NULL,  -- Preserve original for reset

    -- Metadata
    display_order INT DEFAULT 0,

    -- Audit
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(100),

    -- Constraints
    CONSTRAINT chk_mcp_tool_name_length CHECK (LENGTH(tool_name) >= 3),
    CONSTRAINT chk_mcp_tool_group CHECK (tool_group IN (
        'DOCUMENTATION', 'MIGRATION', 'LANGUAGE',
        'FLAVORS', 'FLAVOR_GROUPS', 'INITIALIZR', 'JAVADOC'
    ))
);

-- Indexes for common queries
CREATE INDEX idx_mcp_tools_enabled ON mcp_tools(is_enabled) WHERE is_enabled = true;
CREATE INDEX idx_mcp_tools_group ON mcp_tools(tool_group);
CREATE INDEX idx_mcp_tools_name ON mcp_tools(tool_name);

-- Trigger for updated_at
CREATE OR REPLACE FUNCTION mcp_tools_update_timestamp()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER mcp_tools_update_trigger
    BEFORE UPDATE ON mcp_tools
    FOR EACH ROW
    EXECUTE FUNCTION mcp_tools_update_timestamp();

-- Add comment
COMMENT ON TABLE mcp_tools IS 'MCP Tool Masquerading - Dynamic visibility control for MCP tools exposed to LLMs';
COMMENT ON COLUMN mcp_tools.tool_name IS 'Unique tool identifier matching the @McpTool name';
COMMENT ON COLUMN mcp_tools.tool_group IS 'Tool group category for UI organization';
COMMENT ON COLUMN mcp_tools.is_enabled IS 'Whether the tool is exposed to LLMs via MCP protocol';
COMMENT ON COLUMN mcp_tools.description IS 'Current tool description (may be customized)';
COMMENT ON COLUMN mcp_tools.original_description IS 'Original tool description for reset functionality';
COMMENT ON COLUMN mcp_tools.display_order IS 'Sort order within the tool group';
