# MCP Tools Optimization Plan

> **Created**: 2025-12-16
> **Version**: 1.4.3
> **Status**: Planning Phase
> **Purpose**: Comprehensive analysis and optimization of all 43 MCP tools served by Spring Documentation MCP Server

---

## Executive Summary

This document provides a structured approach to analyzing and optimizing the 43 MCP tools provided by the Spring Documentation MCP Server. The optimization process covers performance, usability, reliability, and the critical SSE connection stability issues observed with Claude Code MCP client integration.

**Total MCP Tools**: 43
- Documentation Tools: 10
- Migration Tools: 7
- Language Evolution Tools: 6
- Flavors Tools: 8
- Flavor Groups Tools: 3
- Initializr Tools: 5
- Javadoc Tools: 4

---

## Table of Contents

1. [Known Issues](#1-known-issues)
2. [MCP Testing Tool Suite](#2-mcp-testing-tool-suite)
3. [MCP Tools Inventory](#3-mcp-tools-inventory)
4. [Optimization Process](#4-optimization-process)
5. [Tool Analysis Template](#5-tool-analysis-template)
6. [Optimization Checklist](#6-optimization-checklist)
7. [Implementation Tracking](#7-implementation-tracking)

---

## 1. Known Issues

### 1.1 SSE Connection Stability (Critical)

**Problem Description**:
The Claude Code MCP client experiences connection issues with the SSE endpoint under specific conditions:

| Scenario | Behavior | Impact |
|----------|----------|--------|
| WiFi network change | SSE connection drops silently | Client sends requests but receives no responses |
| MCP Server restart | Existing SSE connections become stale | Client appears connected but is non-functional |
| Long idle periods | Connection may timeout | Requires manual reconnection |

**Technical Analysis**:

```
┌─────────────────────┐     SSE Connection      ┌─────────────────────┐
│  Claude Code MCP    │─────────────────────────│  Spring MCP Server  │
│  Client             │         ↓               │  (SSE Endpoint)     │
└─────────────────────┘   Connection Lost       └─────────────────────┘
                                │
                                ▼
                      ┌─────────────────────┐
                      │  Client still sends │
                      │  requests but gets  │
                      │  no responses       │
                      └─────────────────────┘
```

**Root Causes**:
1. **SSE Protocol Limitation**: SSE is a unidirectional protocol (server → client). When the connection drops, the client may not immediately detect it.
2. **No Heartbeat Detection**: Missing heartbeat/ping mechanism to detect stale connections.
3. **Network Change Events**: WiFi switching doesn't trigger reconnection logic.
4. **Server-Side Session State**: Server may hold stale session references after restart.

**Potential Solutions**:

| Solution | Complexity | Effectiveness | Priority |
|----------|------------|---------------|----------|
| Implement heartbeat/ping mechanism | Medium | High | P0 |
| Add connection health check endpoint | Low | Medium | P1 |
| Implement automatic reconnection with exponential backoff | High | High | P1 |
| Add connection timeout with forced reconnect | Medium | Medium | P2 |
| Implement WebSocket alternative (bidirectional) | High | High | P3 |

**Workaround (Current)**:
Users must manually run `/mcp` command in Claude Code to reconnect after:
- WiFi network changes
- MCP Server restarts
- Extended idle periods

### 1.2 Tool Discovery Issues

**Problem**: Some tools return empty results for valid queries.

| Tool | Issue | Status |
|------|-------|--------|
| `searchSpringDocs` | May return empty for broad queries | Needs query optimization |
| `getCodeExamples` | Limited example database | Needs content population |
| `findProjectsByUseCase` | Limited keyword matching | Needs synonym expansion |

### 1.3 Performance Variability

**Observed Latencies**:

| Tool Category | Min | Avg | Max | Target |
|---------------|-----|-----|-----|--------|
| Documentation | 2ms | 25ms | 72ms | <50ms |
| Migration | 15ms | 50ms | 150ms | <100ms |
| Language | 10ms | 40ms | 120ms | <80ms |
| Flavors | 5ms | 30ms | 100ms | <50ms |
| Initializr | 20ms | 100ms | 500ms | <200ms |
| Javadoc | 30ms | 150ms | 1000ms | <300ms |

---

## 2. MCP Testing Tool Suite

### 2.1 Overview

A comprehensive testing tool suite for validating MCP server functionality, stability, and performance. Located in `tools/mcp/`.

**Test Coverage Areas**:
1. **SSE Stability & Reconnectability** - Connection lifecycle, network interruptions, server restarts
2. **MCP Tool Performance** - Response times, throughput, latency percentiles
3. **Query & Response Optimization** - Query patterns, response completeness, data quality

### 2.2 Technology Stack Recommendation

#### Primary Language: **Python 3.11+**

**Rationale**:

| Criteria | Python | Node.js | Java | Go |
|----------|--------|---------|------|-----|
| SSE Client Libraries | Excellent (`sseclient-py`, `aiohttp`) | Good (`eventsource`) | Limited | Good |
| Async Support | Excellent (`asyncio`) | Native | Complex | Native |
| Test Frameworks | Excellent (`pytest`) | Good (`jest`) | Good (`JUnit`) | Good |
| JSON Handling | Native | Native | Verbose | Good |
| Rapid Development | Excellent | Good | Slow | Medium |
| Data Analysis | Excellent (`pandas`) | Limited | Limited | Limited |
| Report Generation | Excellent | Medium | Medium | Limited |

**Decision**: Python provides the best balance of SSE support, async capabilities, testing frameworks, and rapid development for this use case.

#### Core Libraries

```
# requirements.txt for tools/mcp/

# SSE Client
sseclient-py>=1.8.0          # Simple SSE client
aiohttp>=3.9.0               # Async HTTP client with SSE support
aiohttp-sse-client>=0.2.1    # Async SSE client

# HTTP & JSON
httpx>=0.27.0                # Modern async HTTP client
pydantic>=2.5.0              # Data validation and JSON schemas
pydantic-settings>=2.1.0     # Settings management with env support
orjson>=3.9.0                # Fast JSON serialization

# Testing Framework
pytest>=8.0.0                # Test framework
pytest-asyncio>=0.23.0       # Async test support
pytest-benchmark>=4.0.0      # Performance benchmarking
pytest-timeout>=2.2.0        # Test timeouts
pytest-html>=4.1.0           # HTML report generation

# Performance & Analysis
pandas>=2.1.0                # Data analysis
matplotlib>=3.8.0            # Visualization
rich>=13.7.0                 # Rich console output
tabulate>=0.9.0              # Table formatting

# Utilities
python-dotenv>=1.0.0         # Environment configuration
click>=8.1.0                 # CLI interface
pyyaml>=6.0.0                # YAML configuration
```

### 2.3 Directory Structure

```
tools/
└── mcp/
    ├── README.md                    # Tool suite documentation
    ├── requirements.txt             # Python dependencies
    ├── pyproject.toml               # Project configuration
    ├── config/
    │   ├── default.yaml             # Default configuration
    │   ├── local.yaml               # Local overrides (gitignored)
    │   └── test_scenarios.yaml      # Test scenario definitions
    │
    ├── src/
    │   ├── __init__.py
    │   ├── client/
    │   │   ├── __init__.py
    │   │   ├── sse_client.py        # SSE connection management
    │   │   ├── mcp_client.py        # MCP protocol client
    │   │   └── reconnection.py      # Reconnection strategies
    │   │
    │   ├── models/
    │   │   ├── __init__.py
    │   │   ├── mcp_messages.py      # MCP message types (Pydantic)
    │   │   ├── tool_definitions.py  # Tool parameter schemas
    │   │   └── test_results.py      # Test result models
    │   │
    │   ├── utils/
    │   │   ├── __init__.py
    │   │   ├── metrics.py           # Metrics collection
    │   │   ├── reporting.py         # Report generation
    │   │   └── network_simulation.py # Network condition simulation
    │   │
    │   └── runners/
    │       ├── __init__.py
    │       ├── stability_runner.py  # SSE stability tests
    │       ├── performance_runner.py # Performance benchmarks
    │       └── optimization_runner.py # Query optimization tests
    │
    ├── tests/
    │   ├── __init__.py
    │   ├── conftest.py              # pytest fixtures
    │   │
    │   ├── stability/
    │   │   ├── __init__.py
    │   │   ├── test_sse_connection.py
    │   │   ├── test_reconnection.py
    │   │   ├── test_server_restart.py
    │   │   └── test_network_interruption.py
    │   │
    │   ├── performance/
    │   │   ├── __init__.py
    │   │   ├── test_tool_latency.py
    │   │   ├── test_throughput.py
    │   │   ├── test_concurrent_requests.py
    │   │   └── benchmarks/
    │   │       ├── __init__.py
    │   │       └── tool_benchmarks.py
    │   │
    │   └── optimization/
    │       ├── __init__.py
    │       ├── test_query_patterns.py
    │       ├── test_response_completeness.py
    │       ├── test_response_size.py
    │       └── test_caching_effectiveness.py
    │
    ├── scripts/
    │   ├── run_all_tests.sh         # Run complete test suite
    │   ├── run_stability.sh         # Run stability tests only
    │   ├── run_performance.sh       # Run performance tests only
    │   ├── run_optimization.sh      # Run optimization tests only
    │   └── generate_report.py       # Generate HTML/PDF reports
    │
    └── reports/                     # Generated reports (gitignored)
        ├── stability/
        ├── performance/
        └── optimization/
```

### 2.4 Test Categories

#### 2.4.1 SSE Stability Tests

| Test | Description | Priority |
|------|-------------|----------|
| `test_initial_connection` | Verify SSE connection establishment | P0 |
| `test_heartbeat_reception` | Verify heartbeat/ping reception | P0 |
| `test_connection_timeout` | Test connection timeout handling | P0 |
| `test_server_restart_recovery` | Test recovery after server restart | P0 |
| `test_network_interruption` | Simulate network drop and recovery | P0 |
| `test_idle_connection` | Test long idle connection stability | P1 |
| `test_multiple_connections` | Test multiple concurrent SSE connections | P1 |
| `test_reconnection_backoff` | Verify exponential backoff on reconnect | P1 |
| `test_session_persistence` | Verify session state after reconnection | P2 |
| `test_message_ordering` | Verify message order after reconnection | P2 |

**Key Metrics**:
- Connection establishment time
- Time to detect connection loss
- Reconnection success rate
- Message loss during reconnection
- Session state consistency

#### 2.4.2 MCP Tool Performance Tests

| Test | Description | Priority |
|------|-------------|----------|
| `test_tool_response_time` | Measure individual tool response times | P0 |
| `test_tool_throughput` | Measure requests/second per tool | P0 |
| `test_concurrent_tool_calls` | Test parallel tool invocations | P1 |
| `test_large_response_handling` | Test tools with large responses (Javadoc) | P1 |
| `test_cache_effectiveness` | Measure cache hit rates and speedup | P1 |
| `test_database_query_time` | Isolate database query performance | P2 |
| `test_serialization_overhead` | Measure JSON serialization time | P2 |
| `test_load_under_stress` | Test performance under high load | P2 |

**Key Metrics**:
- Response time percentiles (p50, p95, p99)
- Throughput (requests/second)
- Error rate under load
- Memory usage
- Cache hit/miss ratio

#### 2.4.3 Query & Response Optimization Tests

| Test | Description | Priority |
|------|-------------|----------|
| `test_query_effectiveness` | Test various query patterns | P0 |
| `test_response_completeness` | Verify all expected fields present | P0 |
| `test_response_size` | Analyze response payload sizes | P1 |
| `test_empty_result_handling` | Test graceful empty result handling | P1 |
| `test_parameter_validation` | Test input validation effectiveness | P1 |
| `test_error_message_quality` | Assess error message usefulness | P2 |
| `test_pagination_efficiency` | Test pagination for large results | P2 |
| `test_filter_combinations` | Test various filter parameter combos | P2 |

**Key Metrics**:
- Query success rate by pattern
- Response completeness score
- Average response size (bytes)
- Empty result rate
- Error message clarity score

### 2.5 Implementation Plan

#### Phase 1: Foundation (Week 1) ✅ COMPLETED
- [x] Set up `tools/mcp/` directory structure
- [x] Create `requirements.txt` and `pyproject.toml`
- [x] Implement base SSE client (`src/client/sse_client.py`)
- [x] Implement MCP protocol client (`src/client/mcp_client.py`)
- [x] Create Pydantic models for MCP messages
- [x] Set up pytest configuration (`tests/conftest.py`)
- [x] Implement configuration loader with env/yaml support (`src/config.py`)

#### Phase 2: Stability Tests (Week 2) ✅ COMPLETED
- [x] Implement `test_initial_connection`
- [x] Implement `test_heartbeat_reception`
- [x] Implement `test_connection_timeout`
- [x] Implement `test_reconnection` tests
- [x] Implement `test_connection_health` tests
- [x] Implement `test_multiple_connections`
- [x] Implement MCP client stability tests

#### Phase 3: Performance Tests (Week 3) ✅ COMPLETED
- [x] Implement `test_tool_response_time` for core tools
- [x] Implement `test_tool_throughput`
- [x] Implement `test_concurrent_tool_calls`
- [x] Create benchmark suite with timing collection
- [x] Implement response size measurement
- [x] Implement connection overhead tests

#### Phase 4: Optimization Tests (Week 4) ✅ COMPLETED
- [x] Implement query pattern analysis tests
- [x] Implement response completeness validation
- [x] Create response size analysis tools
- [x] Implement filter effectiveness tests
- [x] Implement caching opportunity detection
- [ ] Generate baseline metrics report (pending server test)

### 2.6 Implementation Status

> **Last Updated**: 2025-12-16
> **Status**: Phase 1-4 Complete - Ready for Testing

#### Implemented Files

| File | Purpose | Status |
|------|---------|--------|
| `tools/mcp/README.md` | Tool suite documentation | ✅ Complete |
| `tools/mcp/requirements.txt` | Python dependencies | ✅ Complete |
| `tools/mcp/pyproject.toml` | Project configuration & pytest settings | ✅ Complete |
| `tools/mcp/.env.example` | Environment template | ✅ Complete |
| `tools/mcp/.gitignore` | Ignore patterns | ✅ Complete |
| `tools/mcp/config/default.yaml` | Default configuration | ✅ Complete |

**Source Code**:

| File | Purpose | Status |
|------|---------|--------|
| `src/__init__.py` | Package root | ✅ Complete |
| `src/config.py` | Configuration loader with env/yaml support | ✅ Complete |
| `src/client/__init__.py` | Client package | ✅ Complete |
| `src/client/sse_client.py` | SSE client with reconnection support | ✅ Complete |
| `src/client/mcp_client.py` | MCP protocol client for tool invocation | ✅ Complete |
| `src/models/__init__.py` | Models package | ✅ Complete |
| `src/models/mcp_messages.py` | Pydantic models for MCP protocol | ✅ Complete |
| `src/models/test_results.py` | Test result models with BenchmarkResult | ✅ Complete |

**Tests**:

| File | Purpose | Tests |
|------|---------|-------|
| `tests/conftest.py` | Pytest fixtures and configuration | 15+ fixtures |
| `tests/stability/__init__.py` | Stability tests package | - |
| `tests/stability/test_sse_connection.py` | SSE connection stability tests | 15 tests |
| `tests/stability/test_mcp_client.py` | MCP client stability tests | 20 tests |
| `tests/performance/__init__.py` | Performance tests package | - |
| `tests/performance/test_tool_performance.py` | Tool performance benchmarks | 12 tests |
| `tests/optimization/__init__.py` | Optimization tests package | - |
| `tests/optimization/test_query_optimization.py` | Query optimization analysis | 15 tests |

#### Test Categories

| Category | Test Count | Markers |
|----------|------------|---------|
| Stability | ~35 | `@pytest.mark.stability` |
| Performance | ~12 | `@pytest.mark.performance` |
| Optimization | ~15 | `@pytest.mark.optimization` |
| Slow | ~5 | `@pytest.mark.slow` |
| **Total** | **~62** | - |

#### Running the Tests

```bash
# Navigate to tool suite directory
cd tools/mcp

# Create virtual environment and install dependencies
python3 -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt

# Set API key (required)
export MCP_API_KEY="smcp_dQdhF-x2EWXs5EPzbsmfx8nkzPMSdcaq0ELY6tlq33w"

# Run all tests
pytest tests/ -v

# Run specific category
pytest tests/stability/ -v -m stability
pytest tests/performance/ -v -m performance
pytest tests/optimization/ -v -m optimization

# Run with slow tests included
pytest tests/ -v --run-slow

# Generate HTML report
pytest tests/ --html=reports/test_report.html
```

### 2.7 API Key Authentication

The MCP server requires API key authentication for all tool calls. The tool suite must be configured with a valid API key.

#### Authentication Methods Supported

| Method | Header/Parameter | Priority | Use Case |
|--------|------------------|----------|----------|
| X-API-Key Header | `X-API-Key: smcp_...` | Recommended | Production testing |
| Bearer Token | `Authorization: Bearer smcp_...` | Alternative | OAuth-style flows |
| Query Parameter | `?api_key=smcp_...` | Testing only | Quick manual tests |

#### Global Configuration

**Environment Variable** (Recommended):
```bash
# Set in .env file or shell environment
export MCP_API_KEY="smcp_dQdhF-x2EWXs5EPzbsmfx8nkzPMSdcaq0ELY6tlq33w"
```

**Local Configuration File** (gitignored):
```yaml
# config/local.yaml (DO NOT COMMIT - add to .gitignore)
mcp_server:
  api_key: "smcp_dQdhF-x2EWXs5EPzbsmfx8nkzPMSdcaq0ELY6tlq33w"
```

**Configuration Priority** (highest to lowest):
1. Environment variable `MCP_API_KEY`
2. `config/local.yaml` (gitignored)
3. `config/default.yaml` (placeholder only)

#### Environment File Template

```bash
# tools/mcp/.env.example (commit this)
# Copy to .env and fill in your values

# MCP Server Configuration
MCP_BASE_URL=http://localhost:8080
MCP_API_KEY=your_api_key_here

# Optional: Override endpoints
# MCP_SSE_ENDPOINT=/mcp/spring/sse
# MCP_MESSAGE_ENDPOINT=/mcp/spring/messages
```

```bash
# tools/mcp/.env (DO NOT COMMIT - add to .gitignore)
MCP_BASE_URL=http://localhost:8080
MCP_API_KEY=smcp_dQdhF-x2EWXs5EPzbsmfx8nkzPMSdcaq0ELY6tlq33w
```

### 2.8 Configuration Schema

```yaml
# config/default.yaml
mcp_server:
  base_url: "${MCP_BASE_URL:-http://localhost:8080}"
  sse_endpoint: "${MCP_SSE_ENDPOINT:-/mcp/spring/sse}"
  message_endpoint: "${MCP_MESSAGE_ENDPOINT:-/mcp/spring/messages}"
  api_key: "${MCP_API_KEY}"  # REQUIRED - from environment or local.yaml

  # Authentication method to use
  auth_method: "header"  # Options: "header" (X-API-Key), "bearer", "query"

timeouts:
  connection: 10000       # ms
  read: 30000             # ms
  heartbeat_interval: 30000  # expected heartbeat interval

stability:
  reconnection_attempts: 5
  backoff_base: 1000      # ms
  backoff_max: 60000      # ms
  idle_test_duration: 300 # seconds

performance:
  warmup_iterations: 3
  benchmark_iterations: 10
  concurrent_connections: 5
  throughput_duration: 60 # seconds

optimization:
  response_size_threshold: 100000  # bytes
  empty_result_alert: true
  validate_json_schema: true

reporting:
  output_dir: "./reports"
  formats: ["html", "json"]
  include_raw_data: false
```

### 2.9 Configuration Loader Implementation

```python
# src/config.py
import os
from pathlib import Path
from typing import Optional
from pydantic import BaseModel, Field
from pydantic_settings import BaseSettings
import yaml

class MCPServerConfig(BaseModel):
    """MCP Server connection configuration."""
    base_url: str = "http://localhost:8080"
    sse_endpoint: str = "/mcp/spring/sse"
    message_endpoint: str = "/mcp/spring/messages"
    api_key: str = Field(..., description="API key for MCP authentication")
    auth_method: str = "header"  # header, bearer, query

    @property
    def sse_url(self) -> str:
        return f"{self.base_url}{self.sse_endpoint}"

    @property
    def message_url(self) -> str:
        return f"{self.base_url}{self.message_endpoint}"

    def get_auth_headers(self) -> dict:
        """Get authentication headers based on configured method."""
        if self.auth_method == "header":
            return {"X-API-Key": self.api_key}
        elif self.auth_method == "bearer":
            return {"Authorization": f"Bearer {self.api_key}"}
        return {}

    def get_auth_params(self) -> dict:
        """Get authentication query parameters (for query method only)."""
        if self.auth_method == "query":
            return {"api_key": self.api_key}
        return {}


class Settings(BaseSettings):
    """Global settings loaded from environment and config files."""

    # Environment variables (highest priority)
    mcp_api_key: Optional[str] = Field(None, env="MCP_API_KEY")
    mcp_base_url: Optional[str] = Field(None, env="MCP_BASE_URL")

    class Config:
        env_file = ".env"
        env_file_encoding = "utf-8"


def load_config(config_dir: Path = Path("config")) -> dict:
    """
    Load configuration with priority:
    1. Environment variables
    2. config/local.yaml (gitignored)
    3. config/default.yaml
    """
    config = {}

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

    # Override with environment variables
    settings = Settings()
    if settings.mcp_api_key:
        config.setdefault("mcp_server", {})["api_key"] = settings.mcp_api_key
    if settings.mcp_base_url:
        config.setdefault("mcp_server", {})["base_url"] = settings.mcp_base_url

    return config


def get_mcp_config() -> MCPServerConfig:
    """Get MCP server configuration with validation."""
    config = load_config()
    mcp_config = config.get("mcp_server", {})

    if not mcp_config.get("api_key"):
        raise ValueError(
            "MCP_API_KEY not configured. Set via:\n"
            "  1. Environment variable: export MCP_API_KEY=smcp_...\n"
            "  2. .env file: MCP_API_KEY=smcp_...\n"
            "  3. config/local.yaml: mcp_server.api_key: smcp_..."
        )

    return MCPServerConfig(**mcp_config)


def _deep_merge(base: dict, override: dict) -> None:
    """Deep merge override into base dict."""
    for key, value in override.items():
        if key in base and isinstance(base[key], dict) and isinstance(value, dict):
            _deep_merge(base[key], value)
        else:
            base[key] = value
```

### 2.10 Sample Test Implementation

#### SSE Stability Test Example

```python
# tests/stability/test_sse_connection.py
import pytest
import asyncio
from src.client.sse_client import SSEClient
from src.models.mcp_messages import MCPMessage

class TestSSEConnection:
    """SSE Connection stability tests."""

    @pytest.fixture
    async def sse_client(self, config):
        """Create SSE client for testing."""
        client = SSEClient(
            base_url=config.mcp_server.base_url,
            sse_endpoint=config.mcp_server.sse_endpoint,
            api_key=config.mcp_server.api_key
        )
        yield client
        await client.close()

    @pytest.mark.asyncio
    async def test_initial_connection(self, sse_client):
        """Test SSE connection can be established."""
        # Arrange
        connection_timeout = 10.0

        # Act
        connected = await asyncio.wait_for(
            sse_client.connect(),
            timeout=connection_timeout
        )

        # Assert
        assert connected is True
        assert sse_client.is_connected
        assert sse_client.session_id is not None

    @pytest.mark.asyncio
    async def test_heartbeat_reception(self, sse_client):
        """Test that heartbeats are received within expected interval."""
        # Arrange
        await sse_client.connect()
        heartbeat_timeout = 35.0  # 30s interval + 5s buffer

        # Act
        heartbeat_received = await asyncio.wait_for(
            sse_client.wait_for_heartbeat(),
            timeout=heartbeat_timeout
        )

        # Assert
        assert heartbeat_received is True

    @pytest.mark.asyncio
    async def test_connection_loss_detection(self, sse_client):
        """Test that connection loss is detected within acceptable time."""
        # Arrange
        await sse_client.connect()

        # Act - Simulate server-side disconnect
        # (Requires coordination with test server or network simulation)
        disconnect_detected_time = await sse_client.simulate_disconnect()

        # Assert
        assert disconnect_detected_time < 35.0  # Should detect within heartbeat interval
        assert not sse_client.is_connected
```

#### Performance Benchmark Example

```python
# tests/performance/benchmarks/tool_benchmarks.py
import pytest
from src.client.mcp_client import MCPClient

class TestToolPerformance:
    """MCP Tool performance benchmarks."""

    @pytest.fixture
    def mcp_client(self, config):
        """Create MCP client for benchmarking."""
        return MCPClient(
            base_url=config.mcp_server.base_url,
            api_key=config.mcp_server.api_key
        )

    @pytest.mark.benchmark(group="documentation-tools")
    def test_listSpringProjects_performance(self, benchmark, mcp_client):
        """Benchmark listSpringProjects tool."""
        result = benchmark(
            mcp_client.call_tool,
            tool_name="listSpringProjects",
            params={}
        )
        assert result.success is True

    @pytest.mark.benchmark(group="documentation-tools")
    def test_searchSpringDocs_performance(self, benchmark, mcp_client):
        """Benchmark searchSpringDocs tool with typical query."""
        result = benchmark(
            mcp_client.call_tool,
            tool_name="searchSpringDocs",
            params={
                "query": "autoconfiguration",
                "project": "spring-boot",
                "version": "3.5.8"
            }
        )
        assert result.success is True

    @pytest.mark.benchmark(group="javadoc-tools")
    def test_searchJavadocs_performance(self, benchmark, mcp_client):
        """Benchmark searchJavadocs tool."""
        result = benchmark(
            mcp_client.call_tool,
            tool_name="searchJavadocs",
            params={
                "query": "RestTemplate",
                "library": "spring-framework",
                "limit": 10
            }
        )
        assert result.success is True
```

### 2.11 Report Output Example

```
╔═════════════════════════════════════════════════════════════════════════════╗
║                    MCP Server Test Suite Report                             ║
║                    Generated: 2025-12-16 14:30:00                           ║
╠═════════════════════════════════════════════════════════════════════════════╣

┌─────────────────────────────────────────────────────────────────────────────┐
│ SSE STABILITY TESTS                                                         │
├─────────────────────────────────────────────────────────────────────────────┤
│ Test                          │ Status │ Duration │ Details                 │
├───────────────────────────────┼────────┼──────────┼─────────────────────────┤
│ test_initial_connection       │ PASS   │ 0.23s    │ Connected in 230ms      │
│ test_heartbeat_reception      │ PASS   │ 31.2s    │ Heartbeat at 30.1s      │
│ test_server_restart_recovery  │ FAIL   │ 45.0s    │ No reconnection detected│
│ test_network_interruption     │ PASS   │ 5.4s     │ Recovered in 2.3s       │
└───────────────────────────────┴────────┴──────────┴─────────────────────────┘

┌─────────────────────────────────────────────────────────────────────────────┐
│ TOOL PERFORMANCE BENCHMARKS                                                 │
├─────────────────────────────────────────────────────────────────────────────┤
│ Tool                          │ p50    │ p95    │ p99    │ Throughput       │
├───────────────────────────────┼────────┼────────┼────────┼──────────────────┤
│ listSpringProjects            │ 18ms   │ 25ms   │ 42ms   │ 55 req/s         │
│ searchSpringDocs              │ 45ms   │ 72ms   │ 120ms  │ 22 req/s         │
│ listSpringBootVersions        │ 15ms   │ 22ms   │ 38ms   │ 65 req/s         │
│ getClassDoc                   │ 85ms   │ 150ms  │ 280ms  │ 11 req/s         │
│ searchJavadocs                │ 120ms  │ 250ms  │ 450ms  │ 8 req/s          │
└───────────────────────────────┴────────┴────────┴────────┴──────────────────┘

┌─────────────────────────────────────────────────────────────────────────────┐
│ QUERY OPTIMIZATION ANALYSIS                                                 │
├─────────────────────────────────────────────────────────────────────────────┤
│ Tool                          │ Avg Size │ Empty % │ Completeness │ Score   │
├───────────────────────────────┼──────────┼─────────┼──────────────┼─────────┤
│ searchSpringDocs              │ 12.4 KB  │ 15%     │ 85%          │ 7.2/10  │
│ getCodeExamples               │ 8.2 KB   │ 45%     │ 72%          │ 5.5/10  │
│ findProjectsByUseCase         │ 2.1 KB   │ 30%     │ 90%          │ 6.3/10  │
└───────────────────────────────┴──────────┴─────────┴──────────────┴─────────┘

╠═════════════════════════════════════════════════════════════════════════════╣
║ Summary: 42/45 tests passed │ 3 failures │ Total time: 4m 32s               ║
╚═════════════════════════════════════════════════════════════════════════════╝
```

### 2.12 CLI Interface

```bash
# Run complete test suite
python -m mcp_test_suite run --all

# Run specific test category
python -m mcp_test_suite run --stability
python -m mcp_test_suite run --performance
python -m mcp_test_suite run --optimization

# Run with custom configuration
python -m mcp_test_suite run --all --config config/production.yaml

# Run specific tool benchmarks
python -m mcp_test_suite benchmark --tools listSpringProjects,searchSpringDocs

# Generate report only (from existing results)
python -m mcp_test_suite report --format html --output reports/

# Interactive mode for debugging
python -m mcp_test_suite interactive

# Quick health check
python -m mcp_test_suite health-check
```

---

## 3. MCP Tools Inventory

### 3.1 Documentation Tools (10 tools)

| # | Tool Name | Purpose | Parameters | Optimization Status |
|---|-----------|---------|------------|---------------------|
| 1 | `searchSpringDocs` | Search across all Spring documentation | `query`, `project?`, `version?`, `docType?` | [ ] Pending |
| 2 | `getSpringVersions` | List versions for a Spring project | `project` | [ ] Pending |
| 3 | `listSpringProjects` | List all 55 Spring projects | (none) | [ ] Pending |
| 4 | `getDocumentationByVersion` | Get all docs for a version | `project`, `version` | [ ] Pending |
| 5 | `getCodeExamples` | Search code examples | `query?`, `project?`, `version?`, `language?`, `limit?` | [ ] Pending |
| 6 | `listSpringBootVersions` | List Spring Boot versions | `state?`, `limit?` | [ ] Pending |
| 7 | `getLatestSpringBootVersion` | Get latest patch for major.minor | `majorVersion`, `minorVersion` | [ ] Pending |
| 8 | `filterSpringBootVersionsBySupport` | Filter by support status | `supportActive?`, `limit?` | [ ] Pending |
| 9 | `listProjectsBySpringBootVersion` | List compatible projects | `majorVersion`, `minorVersion` | [ ] Pending |
| 10 | `findProjectsByUseCase` | Search projects by use case | `useCase` | [ ] Pending |

### 3.2 Migration Tools (7 tools)

> **Feature Flag**: `mcp.features.openrewrite.enabled=true`

| # | Tool Name | Purpose | Parameters | Optimization Status |
|---|-----------|---------|------------|---------------------|
| 11 | `getSpringMigrationGuide` | Comprehensive migration guide | `fromVersion`, `toVersion` | [ ] Pending |
| 12 | `getBreakingChanges` | Breaking changes for a version | `project`, `version` | [ ] Pending |
| 13 | `searchMigrationKnowledge` | Search migration knowledge base | `searchTerm`, `project?`, `limit?` | [ ] Pending |
| 14 | `getAvailableMigrationPaths` | List documented upgrade paths | `project` | [ ] Pending |
| 15 | `getTransformationsByType` | Get transformations by type | `project`, `version`, `type` | [ ] Pending |
| 16 | `getDeprecationReplacement` | Find replacement for deprecated API | `className`, `methodName?` | [ ] Pending |
| 17 | `checkVersionCompatibility` | Check dependency compatibility | `springBootVersion`, `dependencies[]` | [ ] Pending |

### 3.3 Language Evolution Tools (6 tools)

> **Feature Flag**: `mcp.features.language-evolution.enabled=true`

| # | Tool Name | Purpose | Parameters | Optimization Status |
|---|-----------|---------|------------|---------------------|
| 18 | `getLanguageVersions` | List all Java/Kotlin versions | `language` | [ ] Pending |
| 19 | `getLanguageFeatures` | Get features for a version | `language`, `version?`, `status?`, `category?` | [ ] Pending |
| 20 | `getModernPatterns` | Get old vs new code patterns | `featureId` | [ ] Pending |
| 21 | `getLanguageVersionDiff` | Compare features between versions | `language`, `fromVersion`, `toVersion` | [ ] Pending |
| 22 | `getSpringBootLanguageRequirements` | Get language requirements | `springBootVersion` | [ ] Pending |
| 23 | `searchLanguageFeatures` | Search features by keyword | `searchTerm`, `language?` | [ ] Pending |

### 3.4 Flavors Tools (8 tools)

> **Feature Flag**: `mcp.features.flavors.enabled=true`

| # | Tool Name | Purpose | Parameters | Optimization Status |
|---|-----------|---------|------------|---------------------|
| 24 | `searchFlavors` | Search company guidelines | `query`, `category?`, `tags?`, `limit?` | [ ] Pending |
| 25 | `getFlavorByName` | Get complete flavor content | `uniqueName` | [ ] Pending |
| 26 | `getFlavorsByCategory` | List flavors in a category | `category` | [ ] Pending |
| 27 | `getArchitecturePatterns` | Get patterns for technologies | `slugs[]` | [ ] Pending |
| 28 | `getComplianceRules` | Get compliance rules | `rules[]` | [ ] Pending |
| 29 | `getAgentConfiguration` | Get AI agent configuration | `useCase` | [ ] Pending |
| 30 | `getProjectInitialization` | Get project init template | `useCase` | [ ] Pending |
| 31 | `listFlavorCategories` | List categories with counts | (none) | [ ] Pending |

### 3.5 Flavor Groups Tools (3 tools)

| # | Tool Name | Purpose | Parameters | Optimization Status |
|---|-----------|---------|------------|---------------------|
| 32 | `listFlavorGroups` | List accessible flavor groups | `includePublic?`, `includePrivate?` | [ ] Pending |
| 33 | `getFlavorsGroup` | Get all flavors in a group | `groupName` | [ ] Pending |
| 34 | `getFlavorGroupStatistics` | Get group statistics | (none) | [ ] Pending |

### 3.6 Initializr Tools (5 tools)

> **Feature Flag**: `mcp.features.initializr.enabled=true`

| # | Tool Name | Purpose | Parameters | Optimization Status |
|---|-----------|---------|------------|---------------------|
| 35 | `initializrGetDependency` | Get dependency with snippet | `dependencyId`, `bootVersion?`, `format?` | [ ] Pending |
| 36 | `initializrSearchDependencies` | Search dependencies | `query`, `bootVersion?`, `category?`, `limit?` | [ ] Pending |
| 37 | `initializrCheckCompatibility` | Check version compatibility | `dependencyId`, `bootVersion` | [ ] Pending |
| 38 | `initializrGetBootVersions` | List available Boot versions | (none) | [ ] Pending |
| 39 | `initializrGetDependencyCategories` | Browse by category | `bootVersion?` | [ ] Pending |

### 3.7 Javadoc Tools (4 tools)

> **Feature Flag**: `mcp.features.javadocs.enabled=true`

| # | Tool Name | Purpose | Parameters | Optimization Status |
|---|-----------|---------|------------|---------------------|
| 40 | `getClassDoc` | Get full class documentation | `fqcn`, `library`, `version?` | [ ] Pending |
| 41 | `getPackageDoc` | Get package documentation | `packageName`, `library`, `version?` | [ ] Pending |
| 42 | `searchJavadocs` | Full-text search Javadocs | `query`, `library?`, `version?`, `limit?` | [ ] Pending |
| 43 | `listJavadocLibraries` | List libraries with versions | (none) | [ ] Pending |

---

## 4. Optimization Process

### 4.1 Phase 1: Analysis (Per Tool)

For each tool, complete the following analysis:

```markdown
### Tool: [tool_name]

**Current State**:
- Average response time: X ms
- Success rate: X%
- Common failure modes: [list]

**Input Analysis**:
- Required parameters: [list]
- Optional parameters: [list]
- Validation rules: [list]
- Edge cases: [list]

**Output Analysis**:
- Response structure: [JSON schema]
- Data completeness: X%
- Useful metadata included: [yes/no]

**Performance Bottlenecks**:
- Database queries: [count, time]
- External API calls: [count, time]
- Serialization overhead: [time]

**LLM Usability**:
- Tool description clarity: [1-5]
- Parameter naming clarity: [1-5]
- Response interpretability: [1-5]
- Example usefulness: [1-5]
```

### 4.2 Phase 2: Optimization Categories

| Category | Focus Areas | Priority |
|----------|-------------|----------|
| **Performance** | Query optimization, caching, batch processing | High |
| **Reliability** | Error handling, retry logic, graceful degradation | High |
| **Usability** | Tool descriptions, parameter naming, examples | Medium |
| **Data Quality** | Response completeness, accuracy, freshness | Medium |
| **Documentation** | Usage examples, edge cases, limitations | Low |

### 4.3 Phase 3: Implementation

1. **Identify quick wins** (< 1 hour)
2. **Group related optimizations**
3. **Implement with tests**
4. **Measure before/after**
5. **Document changes**

---

## 5. Tool Analysis Template

Use this template for detailed analysis of each tool:

```markdown
## Tool Analysis: [tool_name]

### 5.1 Overview
- **Category**: [Documentation/Migration/Language/Flavors/Groups/Initializr/Javadoc]
- **Feature Flag**: [if applicable]
- **Service Class**: [Java class path]
- **Repository**: [if applicable]

### 5.2 Current Implementation

**Method Signature**:
```java
@Tool(description = "...")
public ToolResponse toolName(ToolRequest request) { ... }
```

**Database Queries**:
- Query 1: [description] - [avg time]
- Query 2: [description] - [avg time]

**External Dependencies**:
- [list any external API calls]

### 5.3 Performance Metrics

| Metric | Current | Target | Gap |
|--------|---------|--------|-----|
| Response Time (p50) | X ms | Y ms | Z ms |
| Response Time (p95) | X ms | Y ms | Z ms |
| Success Rate | X% | 99.9% | Y% |
| Cache Hit Rate | X% | Y% | Z% |

### 5.4 Identified Issues

1. **Issue 1**: [description]
   - Impact: [high/medium/low]
   - Root Cause: [analysis]
   - Solution: [proposed fix]

2. **Issue 2**: [description]
   ...

### 5.5 Optimization Recommendations

| Recommendation | Effort | Impact | Priority |
|----------------|--------|--------|----------|
| [Optimization 1] | [S/M/L] | [1-5] | [P0-P3] |
| [Optimization 2] | [S/M/L] | [1-5] | [P0-P3] |

### 5.6 LLM Usability Assessment

**Tool Description**:
- Current: "[current description]"
- Improved: "[suggested improvement]"

**Parameter Descriptions**:
| Parameter | Current | Suggested Improvement |
|-----------|---------|----------------------|
| param1 | "..." | "..." |

**Example Queries**:
```json
// Good example
{"param1": "value1", "param2": "value2"}

// Edge case example
{"param1": "edge_case_value"}
```

### 5.7 Implementation Notes

- [ ] Task 1
- [ ] Task 2
- [ ] Task 3


---

## 6. Optimization Checklist

### 6.1 SSE Connection Optimization

- [ ] **P0**: Implement heartbeat mechanism (server sends ping every 30s)
- [ ] **P0**: Add connection state tracking
- [ ] **P1**: Add health check endpoint for MCP connection
- [ ] **P1**: Implement reconnection detection
- [ ] **P2**: Add connection timeout configuration
- [ ] **P2**: Log connection lifecycle events
- [ ] **P3**: Evaluate WebSocket alternative

### 6.2 Per-Tool Optimization

For each of the 43 tools:

- [ ] Review tool description for clarity
- [ ] Validate parameter descriptions
- [ ] Add/improve examples in tool annotations
- [ ] Identify and fix N+1 query issues
- [ ] Implement/verify caching where appropriate
- [ ] Add proper error handling and messages
- [ ] Measure and document response times
- [ ] Verify response structure completeness

### 6.3 Cross-Cutting Optimizations

- [ ] Implement request/response logging for debugging
- [ ] Add metrics collection (Micrometer)
- [ ] Standardize error response format
- [ ] Implement rate limiting
- [ ] Add request validation middleware
- [ ] Optimize JSON serialization

---

## 7. Implementation Tracking

### 7.1 Progress Overview

| Category | Total | Analyzed | Optimized | Verified |
|----------|-------|----------|-----------|----------|
| Documentation | 10 | 0 | 0 | 0 |
| Migration | 7 | 0 | 0 | 0 |
| Language | 6 | 0 | 0 | 0 |
| Flavors | 8 | 0 | 0 | 0 |
| Groups | 3 | 0 | 0 | 0 |
| Initializr | 5 | 0 | 0 | 0 |
| Javadoc | 4 | 0 | 0 | 0 |
| **Total** | **43** | **0** | **0** | **0** |

### 7.2 Analysis Status

#### Documentation Tools
| Tool | Analysis | Optimization | Verification |
|------|----------|--------------|--------------|
| searchSpringDocs | [ ] | [ ] | [ ] |
| getSpringVersions | [ ] | [ ] | [ ] |
| listSpringProjects | [ ] | [ ] | [ ] |
| getDocumentationByVersion | [ ] | [ ] | [ ] |
| getCodeExamples | [ ] | [ ] | [ ] |
| listSpringBootVersions | [ ] | [ ] | [ ] |
| getLatestSpringBootVersion | [ ] | [ ] | [ ] |
| filterSpringBootVersionsBySupport | [ ] | [ ] | [ ] |
| listProjectsBySpringBootVersion | [ ] | [ ] | [ ] |
| findProjectsByUseCase | [ ] | [ ] | [ ] |

#### Migration Tools
| Tool | Analysis | Optimization | Verification |
|------|----------|--------------|--------------|
| getSpringMigrationGuide | [ ] | [ ] | [ ] |
| getBreakingChanges | [ ] | [ ] | [ ] |
| searchMigrationKnowledge | [ ] | [ ] | [ ] |
| getAvailableMigrationPaths | [ ] | [ ] | [ ] |
| getTransformationsByType | [ ] | [ ] | [ ] |
| getDeprecationReplacement | [ ] | [ ] | [ ] |
| checkVersionCompatibility | [ ] | [ ] | [ ] |

#### Language Evolution Tools
| Tool | Analysis | Optimization | Verification |
|------|----------|--------------|--------------|
| getLanguageVersions | [ ] | [ ] | [ ] |
| getLanguageFeatures | [ ] | [ ] | [ ] |
| getModernPatterns | [ ] | [ ] | [ ] |
| getLanguageVersionDiff | [ ] | [ ] | [ ] |
| getSpringBootLanguageRequirements | [ ] | [ ] | [ ] |
| searchLanguageFeatures | [ ] | [ ] | [ ] |

#### Flavors Tools
| Tool | Analysis | Optimization | Verification |
|------|----------|--------------|--------------|
| searchFlavors | [ ] | [ ] | [ ] |
| getFlavorByName | [ ] | [ ] | [ ] |
| getFlavorsByCategory | [ ] | [ ] | [ ] |
| getArchitecturePatterns | [ ] | [ ] | [ ] |
| getComplianceRules | [ ] | [ ] | [ ] |
| getAgentConfiguration | [ ] | [ ] | [ ] |
| getProjectInitialization | [ ] | [ ] | [ ] |
| listFlavorCategories | [ ] | [ ] | [ ] |

#### Flavor Groups Tools
| Tool | Analysis | Optimization | Verification |
|------|----------|--------------|--------------|
| listFlavorGroups | [ ] | [ ] | [ ] |
| getFlavorsGroup | [ ] | [ ] | [ ] |
| getFlavorGroupStatistics | [ ] | [ ] | [ ] |

#### Initializr Tools
| Tool | Analysis | Optimization | Verification |
|------|----------|--------------|--------------|
| initializrGetDependency | [ ] | [ ] | [ ] |
| initializrSearchDependencies | [ ] | [ ] | [ ] |
| initializrCheckCompatibility | [ ] | [ ] | [ ] |
| initializrGetBootVersions | [ ] | [ ] | [ ] |
| initializrGetDependencyCategories | [ ] | [ ] | [ ] |

#### Javadoc Tools
| Tool | Analysis | Optimization | Verification |
|------|----------|--------------|--------------|
| getClassDoc | [ ] | [ ] | [ ] |
| getPackageDoc | [ ] | [ ] | [ ] |
| searchJavadocs | [ ] | [ ] | [ ] |
| listJavadocLibraries | [ ] | [ ] | [ ] |

---

## 8. Next Steps

1. **Immediate (P0)**:
   - [ ] Investigate SSE connection stability issue in depth
   - [ ] Review Spring AI MCP Server SSE implementation
   - [ ] Test heartbeat/keep-alive mechanisms

2. **Short-term (P1)**:
   - [ ] Analyze top 10 most-used tools
   - [ ] Identify quick-win optimizations
   - [ ] Document current baseline metrics

3. **Medium-term (P2)**:
   - [ ] Complete analysis for all 43 tools
   - [ ] Implement performance optimizations
   - [ ] Improve tool descriptions for LLM usability

4. **Long-term (P3)**:
   - [ ] Evaluate WebSocket alternative to SSE
   - [ ] Implement comprehensive metrics/monitoring
   - [ ] Create automated testing suite for MCP tools

---

## Appendix A: MCP Tool Categories by Feature Flag

| Feature Flag | Tools Count | Enabled by Default |
|--------------|-------------|-------------------|
| (core) | 10 | Yes |
| `mcp.features.openrewrite.enabled` | 7 | Yes |
| `mcp.features.language-evolution.enabled` | 6 | Yes |
| `mcp.features.flavors.enabled` | 11 (8+3) | Yes |
| `mcp.features.initializr.enabled` | 5 | Yes |
| `mcp.features.javadocs.enabled` | 4 | Yes |

## Appendix B: Related Documentation

- [README.md](../../README.md) - Main project documentation
- [ADDITIONAL_CONTENT.md](../../ADDITIONAL_CONTENT.md) - Technical reference with full tool parameters
- [CAPABILITY_PLANNING.md](../CAPABILITY_PLANNING.md) - Capability analysis document
- [CAPABILITY_PLANNING_HOWTO.md](../CAPABILITY_PLANNING_HOWTO.md) - Capability planning guide

---

*Document created: 2025-12-16*
*Last updated: 2025-12-16*
*Author: AI-assisted with Claude Code*
