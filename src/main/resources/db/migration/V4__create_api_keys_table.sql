-- API Keys table for MCP endpoint authentication
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

-- Index for fast lookup by key hash
CREATE INDEX idx_api_keys_key_hash ON api_keys(key_hash);

-- Index for active keys
CREATE INDEX idx_api_keys_active ON api_keys(is_active) WHERE is_active = true;

-- Index for created_by
CREATE INDEX idx_api_keys_created_by ON api_keys(created_by);

-- Comments
COMMENT ON TABLE api_keys IS 'API keys for MCP endpoint authentication';
COMMENT ON COLUMN api_keys.name IS 'Human-readable name for the API key (minimum 3 characters)';
COMMENT ON COLUMN api_keys.key_hash IS 'BCrypt hashed API key (never store plain text)';
COMMENT ON COLUMN api_keys.created_by IS 'Username of the user who created this API key';
COMMENT ON COLUMN api_keys.last_used_at IS 'Timestamp of last successful authentication with this key';
COMMENT ON COLUMN api_keys.is_active IS 'Whether this API key is currently active';
