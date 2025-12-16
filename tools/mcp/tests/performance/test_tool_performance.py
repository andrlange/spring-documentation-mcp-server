"""MCP Tool Performance Tests.

These tests measure the performance characteristics of MCP tools
including response times, throughput, and resource usage.
"""

import asyncio
import sys
import time
from pathlib import Path
from typing import Any

import pytest
import pytest_asyncio

# Add src to path for imports
src_path = str(Path(__file__).parent.parent.parent / "src")
if src_path not in sys.path:
    sys.path.insert(0, src_path)

from client.mcp_client import MCPClient, ToolCallResult
from config import Config, MCPServerConfig
from models.test_results import BenchmarkResult


@pytest.mark.performance
@pytest.mark.asyncio
class TestToolResponseTimes:
    """Tests for measuring tool response times."""

    async def test_list_projects_response_time(
        self, connected_mcp_client: MCPClient
    ) -> None:
        """Measure listSpringProjects response time."""
        result = await connected_mcp_client.call_tool("listSpringProjects")

        assert result.success, f"Tool call failed: {result.error}"
        assert result.duration_ms > 0

        # Log the timing for analysis
        print(f"\nlistSpringProjects response time: {result.duration_ms:.2f}ms")

    async def test_list_versions_response_time(
        self, connected_mcp_client: MCPClient
    ) -> None:
        """Measure listSpringBootVersions response time."""
        result = await connected_mcp_client.call_tool(
            "listSpringBootVersions",
            {"state": "GA", "limit": 10},
        )

        assert result.success, f"Tool call failed: {result.error}"
        print(f"\nlistSpringBootVersions response time: {result.duration_ms:.2f}ms")

    async def test_search_docs_response_time(
        self, connected_mcp_client: MCPClient
    ) -> None:
        """Measure searchSpringDocs response time."""
        result = await connected_mcp_client.call_tool(
            "searchSpringDocs",
            {"query": "spring boot autoconfiguration", "project": None, "version": None, "docType": None},
        )

        assert result.success, f"Tool call failed: {result.error}"
        print(f"\nsearchSpringDocs response time: {result.duration_ms:.2f}ms")


@pytest.mark.performance
@pytest.mark.asyncio
class TestToolBenchmarks:
    """Benchmark tests for MCP tools."""

    async def _run_benchmark(
        self,
        client: MCPClient,
        tool_name: str,
        arguments: dict[str, Any],
        iterations: int,
        warmup: int = 2,
    ) -> BenchmarkResult:
        """Run a benchmark for a tool."""
        timings: list[float] = []
        errors = 0

        # Warmup
        for _ in range(warmup):
            await client.call_tool(tool_name, arguments)

        # Benchmark
        for _ in range(iterations):
            result = await client.call_tool(tool_name, arguments)
            if result.success:
                timings.append(result.duration_ms)
            else:
                errors += 1

        if not timings:
            raise RuntimeError(f"All {iterations} benchmark iterations failed")

        return BenchmarkResult.from_timings(tool_name, timings, errors)

    async def test_benchmark_list_projects(
        self,
        connected_mcp_client: MCPClient,
        config: Config,
    ) -> None:
        """Benchmark listSpringProjects tool."""
        result = await self._run_benchmark(
            connected_mcp_client,
            "listSpringProjects",
            {},
            iterations=config.performance.benchmark_iterations,
            warmup=config.performance.warmup_iterations,
        )

        print(f"\n=== listSpringProjects Benchmark ===")
        print(f"Iterations: {result.iterations}")
        print(f"Mean: {result.mean_ms:.2f}ms")
        print(f"Median: {result.median_ms:.2f}ms")
        print(f"Min: {result.min_ms:.2f}ms")
        print(f"Max: {result.max_ms:.2f}ms")
        print(f"Std Dev: {result.stddev_ms:.2f}ms")
        print(f"P95: {result.p95_ms:.2f}ms")
        print(f"P99: {result.p99_ms:.2f}ms")
        print(f"Throughput: {result.throughput_rps:.2f} req/s")
        print(f"Errors: {result.error_count}")

        # Assert reasonable performance
        assert result.mean_ms < 5000, f"Mean response time too high: {result.mean_ms}ms"

    async def test_benchmark_list_versions(
        self,
        connected_mcp_client: MCPClient,
        config: Config,
    ) -> None:
        """Benchmark listSpringBootVersions tool."""
        result = await self._run_benchmark(
            connected_mcp_client,
            "listSpringBootVersions",
            {"state": "GA", "limit": 10},
            iterations=config.performance.benchmark_iterations,
            warmup=config.performance.warmup_iterations,
        )

        print(f"\n=== listSpringBootVersions Benchmark ===")
        print(f"Mean: {result.mean_ms:.2f}ms | P95: {result.p95_ms:.2f}ms | Throughput: {result.throughput_rps:.2f} req/s")

    async def test_benchmark_search_docs(
        self,
        connected_mcp_client: MCPClient,
        config: Config,
    ) -> None:
        """Benchmark searchSpringDocs tool."""
        result = await self._run_benchmark(
            connected_mcp_client,
            "searchSpringDocs",
            {"query": "spring boot", "project": None, "version": None, "docType": None},
            iterations=config.performance.benchmark_iterations,
            warmup=config.performance.warmup_iterations,
        )

        print(f"\n=== searchSpringDocs Benchmark ===")
        print(f"Mean: {result.mean_ms:.2f}ms | P95: {result.p95_ms:.2f}ms | Throughput: {result.throughput_rps:.2f} req/s")


@pytest.mark.performance
@pytest.mark.asyncio
class TestResponseSize:
    """Tests for measuring response sizes."""

    async def test_response_size_list_projects(
        self, connected_mcp_client: MCPClient
    ) -> None:
        """Measure response size for listSpringProjects."""
        result = await connected_mcp_client.call_tool("listSpringProjects")

        assert result.success
        print(f"\nlistSpringProjects response size: {result.response_size_bytes} bytes")

    async def test_response_size_list_versions(
        self, connected_mcp_client: MCPClient
    ) -> None:
        """Measure response size for listSpringBootVersions with different limits."""
        limits = [5, 10, 20, 50]

        print("\n=== listSpringBootVersions Response Sizes ===")
        for limit in limits:
            result = await connected_mcp_client.call_tool(
                "listSpringBootVersions",
                {"state": None, "limit": limit},
            )
            if result.success:
                print(f"Limit {limit}: {result.response_size_bytes} bytes")


@pytest.mark.performance
@pytest.mark.asyncio
class TestConnectionOverhead:
    """Tests for measuring connection overhead."""

    async def test_connection_time(self, mcp_server_config: MCPServerConfig) -> None:
        """Measure time to establish connection."""
        timings: list[float] = []

        for _ in range(5):
            client = MCPClient(mcp_server_config)
            start = time.time()
            try:
                connected = await client.connect()
                if connected:
                    duration_ms = (time.time() - start) * 1000
                    timings.append(duration_ms)
            finally:
                await client.close()

        if timings:
            avg = sum(timings) / len(timings)
            print(f"\nConnection time - Avg: {avg:.2f}ms, Min: {min(timings):.2f}ms, Max: {max(timings):.2f}ms")

    async def test_first_request_vs_subsequent(
        self, mcp_server_config: MCPServerConfig
    ) -> None:
        """Compare first request time vs subsequent requests."""
        client = MCPClient(mcp_server_config)
        try:
            await client.connect()

            # First request (cold)
            first_result = await client.call_tool("listSpringProjects")
            first_time = first_result.duration_ms

            # Subsequent requests (warm)
            subsequent_times: list[float] = []
            for _ in range(5):
                result = await client.call_tool("listSpringProjects")
                if result.success:
                    subsequent_times.append(result.duration_ms)

            avg_subsequent = sum(subsequent_times) / len(subsequent_times) if subsequent_times else 0

            print(f"\nFirst request: {first_time:.2f}ms")
            print(f"Subsequent avg: {avg_subsequent:.2f}ms")
            print(f"Overhead: {first_time - avg_subsequent:.2f}ms")

        finally:
            await client.close()


@pytest.mark.performance
@pytest.mark.slow
@pytest.mark.asyncio
class TestThroughput:
    """Tests for measuring throughput."""

    async def test_sequential_throughput(
        self,
        connected_mcp_client: MCPClient,
        config: Config,
    ) -> None:
        """Measure sequential request throughput."""
        duration_seconds = min(config.performance.throughput_duration, 30)
        request_count = 0
        error_count = 0

        start_time = time.time()
        while time.time() - start_time < duration_seconds:
            result = await connected_mcp_client.call_tool("listSpringProjects")
            request_count += 1
            if not result.success:
                error_count += 1

        elapsed = time.time() - start_time
        throughput = request_count / elapsed

        print(f"\n=== Sequential Throughput Test ===")
        print(f"Duration: {elapsed:.2f}s")
        print(f"Requests: {request_count}")
        print(f"Errors: {error_count}")
        print(f"Throughput: {throughput:.2f} req/s")

    async def test_concurrent_throughput(
        self,
        mcp_server_config: MCPServerConfig,
        config: Config,
    ) -> None:
        """Measure concurrent request throughput."""
        concurrent = config.performance.concurrent_connections
        duration_seconds = min(config.performance.throughput_duration, 30)

        async def worker(client: MCPClient, results: list[ToolCallResult]) -> None:
            start = time.time()
            while time.time() - start < duration_seconds:
                result = await client.call_tool("listSpringProjects")
                results.append(result)

        # Create clients
        clients: list[MCPClient] = []
        results: list[list[ToolCallResult]] = []

        for _ in range(concurrent):
            client = MCPClient(mcp_server_config)
            if await client.connect():
                clients.append(client)
                results.append([])

        try:
            # Run concurrent workers
            start_time = time.time()
            tasks = [worker(c, r) for c, r in zip(clients, results)]
            await asyncio.gather(*tasks)
            elapsed = time.time() - start_time

            # Calculate totals
            total_requests = sum(len(r) for r in results)
            total_errors = sum(1 for r_list in results for r in r_list if not r.success)
            throughput = total_requests / elapsed

            print(f"\n=== Concurrent Throughput Test ===")
            print(f"Concurrent clients: {len(clients)}")
            print(f"Duration: {elapsed:.2f}s")
            print(f"Total requests: {total_requests}")
            print(f"Total errors: {total_errors}")
            print(f"Throughput: {throughput:.2f} req/s")

        finally:
            for client in clients:
                await client.close()
