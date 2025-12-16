# MCP Testing Tool Suite

A comprehensive testing tool suite for validating the Spring Documentation MCP Server functionality, stability, and performance.

## Quick Start

### 1. Install Dependencies

```bash
cd tools/mcp
python -m venv .venv
source .venv/bin/activate  # On Windows: .venv\Scripts\activate
pip install -r requirements.txt
```

### 2. Configure API Key

Copy the example environment file and add your API key:

```bash
cp .env.example .env
# Edit .env and set your MCP_API_KEY
```

Or set via environment variable:
```bash
export MCP_API_KEY="smcp_your_api_key_here"
```

### 3. Run Tests

```bash
# Run all tests
pytest

# Run specific test categories
pytest tests/stability/      # SSE stability tests
pytest tests/performance/    # Performance benchmarks
pytest tests/optimization/   # Query optimization tests

# Run with verbose output
pytest -v

# Run with HTML report
pytest --html=reports/report.html
```

## Test Categories

### Stability Tests (`tests/stability/`)
- SSE connection establishment
- Heartbeat reception
- Connection timeout handling
- Server restart recovery
- Network interruption simulation

### Performance Tests (`tests/performance/`)
- Tool response time benchmarks
- Throughput measurements
- Concurrent request handling
- Cache effectiveness

### Optimization Tests (`tests/optimization/`)
- Query pattern effectiveness
- Response completeness validation
- Response size analysis
- Parameter validation

## Configuration

### Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `MCP_API_KEY` | API key for authentication | (required) |
| `MCP_BASE_URL` | MCP server base URL | `http://localhost:8080` |
| `MCP_SSE_ENDPOINT` | SSE endpoint path | `/mcp/spring/sse` |
| `MCP_MESSAGE_ENDPOINT` | Message endpoint path | `/mcp/spring/messages` |

### Configuration Files

- `config/default.yaml` - Default configuration (committed)
- `config/local.yaml` - Local overrides (gitignored)
- `.env` - Environment variables (gitignored)

## Project Structure

```
tools/mcp/
├── src/
│   ├── client/          # SSE & MCP protocol clients
│   ├── models/          # Pydantic message types
│   ├── utils/           # Metrics, reporting utilities
│   └── runners/         # Test runners
├── tests/
│   ├── stability/       # SSE connection tests
│   ├── performance/     # Tool benchmarks
│   └── optimization/    # Query/response tests
├── config/              # YAML configuration
├── scripts/             # Shell scripts
└── reports/             # Generated reports (gitignored)
```

## CLI Usage

```bash
# Quick health check
python -m src.cli health-check

# Run specific tool benchmark
python -m src.cli benchmark --tool listSpringProjects

# Interactive mode
python -m src.cli interactive
```

## License

MIT License - See root project LICENSE file.
