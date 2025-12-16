-- V15: MCP Monitoring Tables
-- Adds aggregated metrics and monitoring settings for the monitoring dashboard

-- Aggregate metrics table (5min, 1h, 24h buckets)
CREATE TABLE mcp_metrics_aggregate (
    id BIGSERIAL PRIMARY KEY,
    bucket_type VARCHAR(10) NOT NULL,  -- 'FIVE_MIN', 'ONE_HOUR', 'TWENTY_FOUR_HOUR'
    bucket_start TIMESTAMP NOT NULL,
    bucket_end TIMESTAMP NOT NULL,
    tool_name VARCHAR(100),            -- NULL for overall/connection metrics
    metric_type VARCHAR(50) NOT NULL,  -- 'TOOL_CALLS', 'CONNECTION', 'ERROR', 'LATENCY'
    total_count BIGINT DEFAULT 0,
    success_count BIGINT DEFAULT 0,
    error_count BIGINT DEFAULT 0,
    avg_duration_ms DOUBLE PRECISION,
    min_duration_ms DOUBLE PRECISION,
    max_duration_ms DOUBLE PRECISION,
    p95_duration_ms DOUBLE PRECISION,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(bucket_type, bucket_start, tool_name, metric_type)
);

-- Connection events for real-time monitoring
CREATE TABLE mcp_connection_events (
    id BIGSERIAL PRIMARY KEY,
    session_id VARCHAR(255) NOT NULL,
    event_type VARCHAR(50) NOT NULL,   -- 'CONNECTED', 'DISCONNECTED', 'ERROR', 'HEARTBEAT', 'TIMEOUT'
    client_info JSONB,
    protocol_version VARCHAR(50),
    error_message TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Monitoring settings
CREATE TABLE monitoring_settings (
    id BIGSERIAL PRIMARY KEY,
    setting_key VARCHAR(100) NOT NULL UNIQUE,
    setting_value VARCHAR(500),
    description TEXT,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(100)
);

-- Indexes for efficient querying
CREATE INDEX idx_metrics_bucket ON mcp_metrics_aggregate(bucket_type, bucket_start);
CREATE INDEX idx_metrics_bucket_start ON mcp_metrics_aggregate(bucket_start DESC);
CREATE INDEX idx_metrics_tool ON mcp_metrics_aggregate(tool_name) WHERE tool_name IS NOT NULL;
CREATE INDEX idx_metrics_type ON mcp_metrics_aggregate(metric_type);
CREATE INDEX idx_conn_events_session ON mcp_connection_events(session_id);
CREATE INDEX idx_conn_events_created ON mcp_connection_events(created_at DESC);
CREATE INDEX idx_conn_events_type ON mcp_connection_events(event_type);

-- Insert default monitoring settings
INSERT INTO monitoring_settings (setting_key, setting_value, description, updated_by) VALUES
('retention_hours', '24', 'Hours to retain detailed metrics before cleanup', 'system'),
('aggregation_enabled', 'true', 'Enable automatic metrics aggregation', 'system'),
('heartbeat_interval_ms', '30000', 'Heartbeat interval in milliseconds for connection health checks', 'system'),
('auto_refresh_seconds', '30', 'Dashboard auto-refresh interval in seconds', 'system'),
('cleanup_interval_hours', '6', 'Interval between cleanup runs in hours', 'system');

-- Add comment for documentation
COMMENT ON TABLE mcp_metrics_aggregate IS 'Stores aggregated MCP metrics for monitoring dashboard. Buckets: 5-minute, 1-hour, 24-hour.';
COMMENT ON TABLE mcp_connection_events IS 'Stores detailed connection events for real-time monitoring and debugging.';
COMMENT ON TABLE monitoring_settings IS 'Configuration settings for the monitoring system.';
