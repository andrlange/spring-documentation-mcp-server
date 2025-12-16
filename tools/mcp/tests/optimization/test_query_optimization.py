"""MCP Query Optimization Tests.

These tests analyze query patterns and responses to identify
optimization opportunities for MCP tools.
"""

import json
import sys
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
from models.test_results import QueryOptimizationResult


@pytest.mark.optimization
@pytest.mark.asyncio
class TestQueryPatterns:
    """Tests for analyzing query patterns."""

    async def test_search_query_variations(
        self, connected_mcp_client: MCPClient
    ) -> None:
        """Test different search query patterns."""
        queries = [
            {"query": "spring boot", "project": None, "version": None, "docType": None},
            {"query": "spring boot autoconfiguration", "project": None, "version": None, "docType": None},
            {"query": "autoconfiguration", "project": "spring-boot", "version": None, "docType": None},
            {"query": "security", "project": "spring-security", "version": None, "docType": None},
        ]

        print("\n=== Search Query Analysis ===")
        for q in queries:
            result = await connected_mcp_client.call_tool("searchSpringDocs", q)
            print(f"\nQuery: {q['query']}, Project: {q['project']}")
            print(f"  Response time: {result.duration_ms:.2f}ms")
            print(f"  Response size: {result.response_size_bytes} bytes")
            print(f"  Success: {result.success}")

    async def test_version_filter_effectiveness(
        self, connected_mcp_client: MCPClient
    ) -> None:
        """Test effectiveness of version filtering."""
        # Without version filter
        result_all = await connected_mcp_client.call_tool(
            "listSpringBootVersions",
            {"state": None, "limit": 50},
        )

        # With state filter
        result_ga = await connected_mcp_client.call_tool(
            "listSpringBootVersions",
            {"state": "GA", "limit": 50},
        )

        print("\n=== Version Filter Effectiveness ===")
        print(f"All versions - Time: {result_all.duration_ms:.2f}ms, Size: {result_all.response_size_bytes} bytes")
        print(f"GA only - Time: {result_ga.duration_ms:.2f}ms, Size: {result_ga.response_size_bytes} bytes")

    async def test_limit_parameter_impact(
        self, connected_mcp_client: MCPClient
    ) -> None:
        """Test impact of limit parameter on response size and time."""
        limits = [5, 10, 20, 50, 100]

        print("\n=== Limit Parameter Impact ===")
        for limit in limits:
            result = await connected_mcp_client.call_tool(
                "listSpringBootVersions",
                {"state": None, "limit": limit},
            )
            print(f"Limit {limit}: Time={result.duration_ms:.2f}ms, Size={result.response_size_bytes} bytes")


@pytest.mark.optimization
@pytest.mark.asyncio
class TestResponseAnalysis:
    """Tests for analyzing response content and structure."""

    async def test_response_completeness(
        self, connected_mcp_client: MCPClient
    ) -> None:
        """Analyze response completeness for various tools."""
        tools_to_check = [
            ("listSpringProjects", {}),
            ("listSpringBootVersions", {"state": "GA", "limit": 10}),
            ("searchSpringDocs", {"query": "spring boot", "project": None, "version": None, "docType": None}),
        ]

        print("\n=== Response Completeness Analysis ===")
        for tool_name, args in tools_to_check:
            result = await connected_mcp_client.call_tool(tool_name, args)

            content = result.get_text_content()
            json_content = result.get_json_content()

            print(f"\n{tool_name}:")
            print(f"  Has text content: {bool(content)}")
            print(f"  Has JSON content: {json_content is not None}")
            print(f"  Content length: {len(content)} chars")

            if json_content:
                if isinstance(json_content, list):
                    print(f"  JSON items: {len(json_content)}")
                elif isinstance(json_content, dict):
                    print(f"  JSON keys: {list(json_content.keys())[:5]}")

    async def test_empty_response_detection(
        self, connected_mcp_client: MCPClient
    ) -> None:
        """Test detection of empty or minimal responses."""
        # Search with very specific/unlikely query
        result = await connected_mcp_client.call_tool(
            "searchSpringDocs",
            {"query": "xyznonexistent123abc", "project": None, "version": None, "docType": None},
        )

        content = result.get_text_content()
        json_content = result.get_json_content()

        print("\n=== Empty Response Detection ===")
        print(f"Query: 'xyznonexistent123abc'")
        print(f"Content empty: {not content or len(content) < 10}")
        print(f"JSON empty: {json_content is None or (isinstance(json_content, list) and len(json_content) == 0)}")


@pytest.mark.optimization
@pytest.mark.asyncio
class TestResponseSizeOptimization:
    """Tests for identifying response size optimization opportunities."""

    async def test_large_response_identification(
        self, connected_mcp_client: MCPClient,
        config: Config,
    ) -> None:
        """Identify tools that return large responses."""
        tools = [
            ("listSpringProjects", {}),
            ("listSpringBootVersions", {"state": None, "limit": 100}),
            ("searchSpringDocs", {"query": "spring", "project": None, "version": None, "docType": None}),
            ("getCodeExamples", {"query": "spring boot", "project": None, "version": None, "language": None, "limit": 50}),
        ]

        threshold = config.optimization.response_size_threshold
        large_responses: list[tuple[str, int]] = []

        print(f"\n=== Large Response Analysis (threshold: {threshold} bytes) ===")
        for tool_name, args in tools:
            result = await connected_mcp_client.call_tool(tool_name, args)
            if result.success:
                size = result.response_size_bytes
                print(f"{tool_name}: {size} bytes")
                if size > threshold:
                    large_responses.append((tool_name, size))

        if large_responses:
            print(f"\nTools exceeding threshold:")
            for name, size in large_responses:
                print(f"  {name}: {size} bytes ({size / 1024:.1f} KB)")

    async def test_pagination_effectiveness(
        self, connected_mcp_client: MCPClient
    ) -> None:
        """Test effectiveness of pagination/limiting."""
        # Compare paginated vs full results
        full_result = await connected_mcp_client.call_tool(
            "listSpringBootVersions",
            {"state": None, "limit": 100},
        )

        paginated_result = await connected_mcp_client.call_tool(
            "listSpringBootVersions",
            {"state": None, "limit": 10},
        )

        print("\n=== Pagination Effectiveness ===")
        print(f"Full (limit=100): {full_result.response_size_bytes} bytes, {full_result.duration_ms:.2f}ms")
        print(f"Paginated (limit=10): {paginated_result.response_size_bytes} bytes, {paginated_result.duration_ms:.2f}ms")

        if full_result.response_size_bytes > 0:
            reduction = (1 - paginated_result.response_size_bytes / full_result.response_size_bytes) * 100
            print(f"Size reduction: {reduction:.1f}%")


@pytest.mark.optimization
@pytest.mark.asyncio
class TestQueryEfficiency:
    """Tests for query efficiency analysis."""

    async def test_filter_vs_full_scan(
        self, connected_mcp_client: MCPClient
    ) -> None:
        """Compare filtered queries vs full scans."""
        # Full scan
        full_result = await connected_mcp_client.call_tool(
            "searchSpringDocs",
            {"query": "boot", "project": None, "version": None, "docType": None},
        )

        # Filtered by project
        filtered_result = await connected_mcp_client.call_tool(
            "searchSpringDocs",
            {"query": "boot", "project": "spring-boot", "version": None, "docType": None},
        )

        print("\n=== Filter vs Full Scan ===")
        print(f"Full scan: {full_result.duration_ms:.2f}ms, {full_result.response_size_bytes} bytes")
        print(f"Filtered: {filtered_result.duration_ms:.2f}ms, {filtered_result.response_size_bytes} bytes")

    async def test_query_specificity_impact(
        self, connected_mcp_client: MCPClient
    ) -> None:
        """Test impact of query specificity on results."""
        queries = [
            "spring",
            "spring boot",
            "spring boot autoconfiguration",
            "spring boot autoconfiguration properties",
        ]

        print("\n=== Query Specificity Impact ===")
        for query in queries:
            result = await connected_mcp_client.call_tool(
                "searchSpringDocs",
                {"query": query, "project": None, "version": None, "docType": None},
            )
            print(f"'{query}': {result.duration_ms:.2f}ms, {result.response_size_bytes} bytes")


@pytest.mark.optimization
@pytest.mark.asyncio
class TestCachingOpportunities:
    """Tests for identifying caching opportunities."""

    async def test_repeated_query_performance(
        self, connected_mcp_client: MCPClient
    ) -> None:
        """Test performance of repeated identical queries."""
        query = {"query": "spring boot starter", "project": None, "version": None, "docType": None}

        timings: list[float] = []
        for i in range(5):
            result = await connected_mcp_client.call_tool("searchSpringDocs", query)
            if result.success:
                timings.append(result.duration_ms)

        print("\n=== Repeated Query Performance ===")
        print(f"Query: 'spring boot starter'")
        for i, t in enumerate(timings):
            print(f"  Request {i + 1}: {t:.2f}ms")

        if timings:
            avg = sum(timings) / len(timings)
            print(f"  Average: {avg:.2f}ms")
            # Check if there's improvement (caching effect)
            if len(timings) > 1 and timings[0] > avg * 1.2:
                print("  Possible caching effect detected")

    async def test_static_data_frequency(
        self, connected_mcp_client: MCPClient
    ) -> None:
        """Analyze frequency of accessing static/rarely-changing data."""
        static_tools = [
            "listSpringProjects",  # Projects rarely change
            "listSpringBootVersions",  # Versions change periodically
        ]

        print("\n=== Static Data Tool Performance ===")
        for tool in static_tools:
            timings = []
            for _ in range(3):
                result = await connected_mcp_client.call_tool(tool)
                if result.success:
                    timings.append(result.duration_ms)

            if timings:
                avg = sum(timings) / len(timings)
                print(f"{tool}: avg {avg:.2f}ms (cacheable)")


@pytest.mark.optimization
@pytest.mark.asyncio
class TestToolUsagePatterns:
    """Tests for analyzing tool usage patterns."""

    async def test_common_tool_sequence(
        self, connected_mcp_client: MCPClient
    ) -> None:
        """Test common tool call sequences."""
        # Typical usage pattern: list projects -> get versions -> search docs
        sequence = [
            ("listSpringProjects", {}),
            ("listSpringBootVersions", {"state": "GA", "limit": 5}),
            ("searchSpringDocs", {"query": "getting started", "project": "spring-boot", "version": None, "docType": None}),
        ]

        print("\n=== Common Tool Sequence ===")
        total_time = 0
        for tool_name, args in sequence:
            result = await connected_mcp_client.call_tool(tool_name, args)
            total_time += result.duration_ms
            print(f"{tool_name}: {result.duration_ms:.2f}ms")

        print(f"Total sequence time: {total_time:.2f}ms")

    async def test_tool_dependencies(
        self, connected_mcp_client: MCPClient
    ) -> None:
        """Identify potential tool call optimizations."""
        # Get projects first to use in subsequent calls
        projects_result = await connected_mcp_client.call_tool("listSpringProjects")
        projects_time = projects_result.duration_ms

        # Use project info in search
        search_result = await connected_mcp_client.call_tool(
            "searchSpringDocs",
            {"query": "configuration", "project": "spring-boot", "version": None, "docType": None},
        )
        search_time = search_result.duration_ms

        print("\n=== Tool Dependencies ===")
        print(f"Get projects: {projects_time:.2f}ms")
        print(f"Search with project filter: {search_time:.2f}ms")
        print(f"Total: {projects_time + search_time:.2f}ms")
        print("Optimization: Could cache project list client-side")
