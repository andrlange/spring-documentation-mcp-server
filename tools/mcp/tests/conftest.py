"""Pytest configuration and fixtures for MCP testing."""

import asyncio
import os
import sys
from pathlib import Path
from typing import AsyncGenerator, Generator, Optional

import pytest
import pytest_asyncio

# Add src to path for imports - must be before other imports
src_path = str(Path(__file__).parent.parent / "src")
if src_path not in sys.path:
    sys.path.insert(0, src_path)

from config import Config, MCPServerConfig, load_config, reset_config
from client.mcp_client import MCPClient, ToolCallResult
from client.sse_client import SSEClient, ReconnectingSSEClient


# ============================================================================
# Configuration Fixtures
# ============================================================================


@pytest.fixture(scope="session")
def config() -> Config:
    """Load test configuration."""
    reset_config()
    return load_config()


@pytest.fixture(scope="session")
def mcp_server_config(config: Config) -> MCPServerConfig:
    """Get MCP server configuration."""
    return config.mcp_server


@pytest.fixture(scope="session")
def mcp_base_url(mcp_server_config: MCPServerConfig) -> str:
    """Get MCP server base URL."""
    return mcp_server_config.base_url


@pytest.fixture(scope="session")
def mcp_api_key(mcp_server_config: MCPServerConfig) -> str:
    """Get MCP API key."""
    return mcp_server_config.api_key


# ============================================================================
# Event Loop Fixtures
# ============================================================================


@pytest.fixture(scope="session")
def event_loop() -> Generator[asyncio.AbstractEventLoop, None, None]:
    """Create event loop for async tests."""
    loop = asyncio.new_event_loop()
    yield loop
    loop.close()


# ============================================================================
# SSE Client Fixtures
# ============================================================================


@pytest_asyncio.fixture
async def sse_client(mcp_server_config: MCPServerConfig) -> AsyncGenerator[SSEClient, None]:
    """Create SSE client for testing."""
    client = SSEClient(mcp_server_config)
    yield client
    await client.close()


@pytest_asyncio.fixture
async def connected_sse_client(
    mcp_server_config: MCPServerConfig,
) -> AsyncGenerator[SSEClient, None]:
    """Create and connect SSE client."""
    client = SSEClient(mcp_server_config)
    connected = await client.connect(timeout=10.0)
    if not connected:
        pytest.skip("Could not connect to MCP server")
    yield client
    await client.close()


@pytest_asyncio.fixture
async def reconnecting_sse_client(
    mcp_server_config: MCPServerConfig,
) -> AsyncGenerator[ReconnectingSSEClient, None]:
    """Create reconnecting SSE client for stability tests."""
    client = ReconnectingSSEClient(
        mcp_server_config,
        max_retries=3,
        backoff_base=0.5,
        backoff_max=5.0,
    )
    yield client
    await client.close()


# ============================================================================
# MCP Client Fixtures
# ============================================================================


@pytest_asyncio.fixture
async def mcp_client(mcp_server_config: MCPServerConfig) -> AsyncGenerator[MCPClient, None]:
    """Create MCP client for testing."""
    client = MCPClient(mcp_server_config, timeout=30.0)
    yield client
    await client.close()


@pytest_asyncio.fixture
async def connected_mcp_client(
    mcp_server_config: MCPServerConfig,
) -> AsyncGenerator[MCPClient, None]:
    """Create and connect MCP client."""
    client = MCPClient(mcp_server_config, timeout=30.0)
    connected = await client.connect()
    if not connected:
        pytest.skip("Could not connect to MCP server")
    yield client
    await client.close()


# ============================================================================
# Test Data Fixtures
# ============================================================================


@pytest.fixture
def sample_tool_names() -> list[str]:
    """Sample tool names for testing."""
    return [
        "listSpringBootVersions",
        "listSpringProjects",
        "searchSpringDocs",
        "getCodeExamples",
        "getBreakingChanges",
    ]


@pytest.fixture
def sample_search_queries() -> list[dict]:
    """Sample search queries for testing."""
    return [
        {"query": "spring boot", "project": None, "version": None, "docType": None},
        {"query": "autoconfiguration", "project": "spring-boot", "version": None, "docType": None},
        {"query": "security", "project": "spring-security", "version": "6.4.x", "docType": None},
        {"query": "data jpa", "project": "spring-data", "version": None, "docType": "reference"},
    ]


@pytest.fixture
def performance_iterations(config: Config) -> int:
    """Get number of performance test iterations."""
    return config.performance.benchmark_iterations


@pytest.fixture
def warmup_iterations(config: Config) -> int:
    """Get number of warmup iterations."""
    return config.performance.warmup_iterations


# ============================================================================
# Helper Functions
# ============================================================================


def pytest_configure(config: pytest.Config) -> None:
    """Configure pytest with custom markers."""
    config.addinivalue_line("markers", "stability: marks tests as stability tests")
    config.addinivalue_line("markers", "performance: marks tests as performance tests")
    config.addinivalue_line("markers", "optimization: marks tests as optimization tests")
    config.addinivalue_line("markers", "slow: marks tests as slow running")
    config.addinivalue_line("markers", "integration: marks tests as integration tests")
    config.addinivalue_line("markers", "protocol: marks tests as MCP protocol compliance tests")


def pytest_collection_modifyitems(config: pytest.Config, items: list[pytest.Item]) -> None:
    """Modify test collection based on markers."""
    # Skip slow tests unless explicitly requested
    if not config.getoption("--run-slow", default=False):
        skip_slow = pytest.mark.skip(reason="need --run-slow option to run")
        for item in items:
            if "slow" in item.keywords:
                item.add_marker(skip_slow)


def pytest_addoption(parser: pytest.Parser) -> None:
    """Add custom command line options."""
    parser.addoption(
        "--run-slow",
        action="store_true",
        default=False,
        help="run slow tests",
    )
    parser.addoption(
        "--mcp-url",
        action="store",
        default=None,
        help="MCP server URL override",
    )
    parser.addoption(
        "--mcp-api-key",
        action="store",
        default=None,
        help="MCP API key override",
    )


# ============================================================================
# Report Fixtures
# ============================================================================


@pytest.fixture(scope="session")
def reports_dir() -> Path:
    """Get reports directory path."""
    reports = Path(__file__).parent.parent / "reports"
    reports.mkdir(exist_ok=True)
    return reports


@pytest.fixture
def timing_results() -> list[float]:
    """Container for collecting timing results."""
    return []


# ============================================================================
# Async Utilities
# ============================================================================


@pytest.fixture
def async_timeout() -> float:
    """Default async operation timeout."""
    return 30.0


@pytest_asyncio.fixture
async def measure_time():
    """Fixture to measure async operation time."""
    import time

    class TimeMeasure:
        def __init__(self):
            self.start_time: Optional[float] = None
            self.end_time: Optional[float] = None

        def start(self):
            self.start_time = time.time()

        def stop(self):
            self.end_time = time.time()

        @property
        def duration_ms(self) -> float:
            if self.start_time is None or self.end_time is None:
                return 0.0
            return (self.end_time - self.start_time) * 1000

    return TimeMeasure()
