"""Configuration management for MCP Testing Tool Suite."""

import os
from pathlib import Path
from typing import Any, Optional

import yaml
from pydantic import BaseModel, Field
from pydantic_settings import BaseSettings


class MCPServerConfig(BaseModel):
    """MCP Server connection configuration."""

    base_url: str = "http://localhost:8080"
    sse_endpoint: str = "/mcp/spring/sse"
    message_endpoint: str = "/mcp/spring/messages"
    api_key: str = Field(..., description="API key for MCP authentication")
    auth_method: str = "header"  # header, bearer, query

    @property
    def sse_url(self) -> str:
        """Get full SSE endpoint URL."""
        return f"{self.base_url}{self.sse_endpoint}"

    @property
    def message_url(self) -> str:
        """Get full message endpoint URL."""
        return f"{self.base_url}{self.message_endpoint}"

    def get_auth_headers(self) -> dict[str, str]:
        """Get authentication headers based on configured method."""
        if self.auth_method == "header":
            return {"X-API-Key": self.api_key}
        elif self.auth_method == "bearer":
            return {"Authorization": f"Bearer {self.api_key}"}
        return {}

    def get_auth_params(self) -> dict[str, str]:
        """Get authentication query parameters (for query method only)."""
        if self.auth_method == "query":
            return {"api_key": self.api_key}
        return {}


class TimeoutsConfig(BaseModel):
    """Timeout configuration."""

    connection: int = 10000  # ms
    read: int = 30000  # ms
    heartbeat_interval: int = 30000  # ms

    @property
    def connection_seconds(self) -> float:
        return self.connection / 1000

    @property
    def read_seconds(self) -> float:
        return self.read / 1000

    @property
    def heartbeat_seconds(self) -> float:
        return self.heartbeat_interval / 1000


class StabilityConfig(BaseModel):
    """Stability test configuration."""

    reconnection_attempts: int = 5
    backoff_base: int = 1000  # ms
    backoff_max: int = 60000  # ms
    backoff_multiplier: float = 2.0
    idle_test_duration: int = 300  # seconds


class PerformanceConfig(BaseModel):
    """Performance test configuration."""

    warmup_iterations: int = 3
    benchmark_iterations: int = 10
    concurrent_connections: int = 5
    throughput_duration: int = 60  # seconds
    tool_timeouts: dict[str, int] = Field(default_factory=dict)

    def get_tool_timeout(self, tool_name: str) -> int:
        """Get timeout for a specific tool in milliseconds."""
        return self.tool_timeouts.get(tool_name, self.tool_timeouts.get("default", 30000))


class OptimizationConfig(BaseModel):
    """Optimization test configuration."""

    response_size_threshold: int = 100000  # bytes
    empty_result_alert: bool = True
    validate_json_schema: bool = True
    large_response_tools: list[str] = Field(default_factory=list)


class ReportingConfig(BaseModel):
    """Reporting configuration."""

    output_dir: str = "./reports"
    formats: list[str] = Field(default_factory=lambda: ["html", "json"])
    include_raw_data: bool = False
    include_timestamps: bool = True


class LoggingConfig(BaseModel):
    """Logging configuration."""

    level: str = "INFO"
    format: str = "%(asctime)s - %(name)s - %(levelname)s - %(message)s"


class Config(BaseModel):
    """Main configuration model."""

    mcp_server: MCPServerConfig
    timeouts: TimeoutsConfig = Field(default_factory=TimeoutsConfig)
    stability: StabilityConfig = Field(default_factory=StabilityConfig)
    performance: PerformanceConfig = Field(default_factory=PerformanceConfig)
    optimization: OptimizationConfig = Field(default_factory=OptimizationConfig)
    reporting: ReportingConfig = Field(default_factory=ReportingConfig)
    logging: LoggingConfig = Field(default_factory=LoggingConfig)


class Settings(BaseSettings):
    """Environment settings loaded from .env file or environment."""

    mcp_api_key: Optional[str] = Field(None, alias="MCP_API_KEY")
    mcp_base_url: Optional[str] = Field(None, alias="MCP_BASE_URL")
    mcp_sse_endpoint: Optional[str] = Field(None, alias="MCP_SSE_ENDPOINT")
    mcp_message_endpoint: Optional[str] = Field(None, alias="MCP_MESSAGE_ENDPOINT")
    mcp_auth_method: Optional[str] = Field(None, alias="MCP_AUTH_METHOD")

    class Config:
        env_file = ".env"
        env_file_encoding = "utf-8"
        extra = "ignore"


def _deep_merge(base: dict[str, Any], override: dict[str, Any]) -> None:
    """Deep merge override into base dict (in-place)."""
    for key, value in override.items():
        if key in base and isinstance(base[key], dict) and isinstance(value, dict):
            _deep_merge(base[key], value)
        else:
            base[key] = value


def _resolve_env_vars(config: dict[str, Any]) -> dict[str, Any]:
    """Resolve environment variable references in config values."""
    import re

    env_pattern = re.compile(r"\$\{([^}:]+)(?::-([^}]*))?\}")

    def resolve_value(value: Any) -> Any:
        if isinstance(value, str):
            match = env_pattern.match(value)
            if match:
                env_var = match.group(1)
                default = match.group(2) or ""
                return os.environ.get(env_var, default)
            return value
        elif isinstance(value, dict):
            return {k: resolve_value(v) for k, v in value.items()}
        elif isinstance(value, list):
            return [resolve_value(v) for v in value]
        return value

    return resolve_value(config)


def load_config(config_dir: Optional[Path] = None) -> Config:
    """
    Load configuration with priority:
    1. Environment variables (highest)
    2. config/local.yaml (gitignored)
    3. config/default.yaml (lowest)
    """
    if config_dir is None:
        # Find config directory relative to this file or current directory
        config_dir = Path(__file__).parent.parent / "config"
        if not config_dir.exists():
            config_dir = Path("config")

    config: dict[str, Any] = {}

    # Load default.yaml
    default_path = config_dir / "default.yaml"
    if default_path.exists():
        with open(default_path) as f:
            config = yaml.safe_load(f) or {}

    # Override with local.yaml if exists
    local_path = config_dir / "local.yaml"
    if local_path.exists():
        with open(local_path) as f:
            local_config = yaml.safe_load(f) or {}
            _deep_merge(config, local_config)

    # Resolve environment variable references in YAML
    config = _resolve_env_vars(config)

    # Load environment settings (highest priority)
    settings = Settings()

    # Ensure mcp_server section exists
    config.setdefault("mcp_server", {})

    # Override with environment variables
    if settings.mcp_api_key:
        config["mcp_server"]["api_key"] = settings.mcp_api_key
    if settings.mcp_base_url:
        config["mcp_server"]["base_url"] = settings.mcp_base_url
    if settings.mcp_sse_endpoint:
        config["mcp_server"]["sse_endpoint"] = settings.mcp_sse_endpoint
    if settings.mcp_message_endpoint:
        config["mcp_server"]["message_endpoint"] = settings.mcp_message_endpoint
    if settings.mcp_auth_method:
        config["mcp_server"]["auth_method"] = settings.mcp_auth_method

    # Validate API key is present
    if not config["mcp_server"].get("api_key"):
        raise ValueError(
            "MCP_API_KEY not configured. Set via:\n"
            "  1. Environment variable: export MCP_API_KEY=smcp_...\n"
            "  2. .env file: MCP_API_KEY=smcp_...\n"
            "  3. config/local.yaml: mcp_server.api_key: smcp_..."
        )

    return Config(**config)


def get_mcp_config() -> MCPServerConfig:
    """Get MCP server configuration with validation."""
    config = load_config()
    return config.mcp_server


# Singleton config instance
_config: Optional[Config] = None


def get_config() -> Config:
    """Get the global configuration instance."""
    global _config
    if _config is None:
        _config = load_config()
    return _config


def reset_config() -> None:
    """Reset the global configuration (useful for testing)."""
    global _config
    _config = None
