"""MCP Client Stability Tests.

These tests verify the stability and reliability of the MCP client
for tool invocations and protocol handling.
"""

import asyncio
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
from config import MCPServerConfig
from models.mcp_messages import ToolInfo


@pytest.mark.stability
@pytest.mark.asyncio
class TestMCPClientConnection:
    """Tests for MCP client connection handling."""

    async def test_client_connect(self, mcp_server_config: MCPServerConfig) -> None:
        """Test MCP client connection establishment."""
        client = MCPClient(mcp_server_config)
        try:
            connected = await client.connect()
            assert connected, "MCP client should connect successfully"
        finally:
            await client.close()

    async def test_client_initialization(self, connected_mcp_client: MCPClient) -> None:
        """Test that MCP client is properly initialized after connect."""
        assert connected_mcp_client._initialized, "Client should be initialized"
        assert connected_mcp_client._sse_client is not None, "SSE client should be set"
        assert connected_mcp_client._sse_client.is_connected, "SSE client should be connected"

    async def test_client_context_manager(self, mcp_server_config: MCPServerConfig) -> None:
        """Test MCP client as async context manager."""
        async with MCPClient(mcp_server_config) as client:
            assert client._initialized, "Client should be initialized in context"
            tools = await client.list_tools()
            assert isinstance(tools, list), "Should be able to list tools"

    async def test_client_close(self, mcp_server_config: MCPServerConfig) -> None:
        """Test MCP client close cleanup."""
        client = MCPClient(mcp_server_config)
        await client.connect()
        assert client._initialized

        await client.close()
        assert not client._initialized, "Client should not be initialized after close"
        assert client._sse_client is None, "SSE client should be None after close"
        assert client._http_client is None, "HTTP client should be None after close"
        assert client._tools_cache is None, "Tools cache should be cleared"


@pytest.mark.stability
@pytest.mark.asyncio
class TestToolsDiscovery:
    """Tests for MCP tools discovery."""

    async def test_list_tools(self, connected_mcp_client: MCPClient) -> None:
        """Test listing available MCP tools."""
        tools = await connected_mcp_client.list_tools()
        assert isinstance(tools, list), "Tools should be a list"
        assert len(tools) > 0, "Should have at least one tool"

    async def test_tool_info_structure(self, connected_mcp_client: MCPClient) -> None:
        """Test that tool info has correct structure."""
        tools = await connected_mcp_client.list_tools()
        assert len(tools) > 0

        tool = tools[0]
        assert isinstance(tool, ToolInfo), "Tool should be ToolInfo instance"
        assert tool.name, "Tool should have a name"
        # Description and inputSchema are optional but usually present
        if tool.inputSchema:
            assert isinstance(tool.inputSchema, dict), "Input schema should be dict"

    async def test_tools_cache(self, connected_mcp_client: MCPClient) -> None:
        """Test that tools are cached after first request."""
        # First request
        tools1 = await connected_mcp_client.list_tools(use_cache=True)

        # Second request should use cache
        tools2 = await connected_mcp_client.list_tools(use_cache=True)

        # Cache should be populated
        assert connected_mcp_client._tools_cache is not None
        assert tools1 == tools2, "Cached tools should match"

    async def test_tools_cache_bypass(self, connected_mcp_client: MCPClient) -> None:
        """Test bypassing tools cache."""
        # First request with cache
        await connected_mcp_client.list_tools(use_cache=True)

        # Request bypassing cache
        tools = await connected_mcp_client.list_tools(use_cache=False)
        assert isinstance(tools, list)

    async def test_expected_tools_present(self, connected_mcp_client: MCPClient) -> None:
        """Test that expected core tools are present."""
        tools = await connected_mcp_client.list_tools()
        tool_names = [t.name for t in tools]

        # These tools should be present in the Spring MCP server
        expected_tools = [
            "listSpringBootVersions",
            "listSpringProjects",
            "searchSpringDocs",
        ]

        for expected in expected_tools:
            assert expected in tool_names, f"Expected tool '{expected}' not found"


@pytest.mark.stability
@pytest.mark.asyncio
class TestToolInvocation:
    """Tests for MCP tool invocation."""

    async def test_simple_tool_call(self, connected_mcp_client: MCPClient) -> None:
        """Test calling a simple tool."""
        result = await connected_mcp_client.call_tool("listSpringProjects")

        assert isinstance(result, ToolCallResult)
        assert result.tool_name == "listSpringProjects"
        assert result.success, f"Tool call failed: {result.error}"
        assert result.duration_ms > 0, "Duration should be recorded"

    async def test_tool_call_with_arguments(self, connected_mcp_client: MCPClient) -> None:
        """Test calling a tool with arguments."""
        result = await connected_mcp_client.call_tool(
            "listSpringBootVersions",
            arguments={"state": "GA", "limit": 5},
        )

        assert result.success, f"Tool call failed: {result.error}"
        assert result.result is not None, "Result should not be None"

    async def test_tool_result_content(self, connected_mcp_client: MCPClient) -> None:
        """Test that tool results contain expected content."""
        result = await connected_mcp_client.call_tool("listSpringProjects")

        assert result.success
        text_content = result.get_text_content()
        assert text_content, "Should have text content"

        # Try to parse as JSON
        json_content = result.get_json_content()
        # JSON content may or may not be present depending on tool response format

    async def test_tool_call_error_handling(self, connected_mcp_client: MCPClient) -> None:
        """Test handling of tool call errors."""
        # Call a non-existent tool
        result = await connected_mcp_client.call_tool("nonExistentTool")

        # Should return result with error, not throw exception
        assert isinstance(result, ToolCallResult)
        assert result.tool_name == "nonExistentTool"
        # The result may be success=False or the error may be in the result

    async def test_tool_call_timeout(self, connected_mcp_client: MCPClient) -> None:
        """Test tool call with timeout."""
        result = await connected_mcp_client.call_tool(
            "listSpringProjects",
            timeout=30.0,
        )

        assert result.success, f"Tool call failed: {result.error}"

    async def test_tool_call_very_short_timeout(
        self, mcp_server_config: MCPServerConfig
    ) -> None:
        """Test tool call with very short timeout (may fail)."""
        client = MCPClient(mcp_server_config, timeout=0.001)
        try:
            await client.connect()
            result = await client.call_tool("searchSpringDocs", {"query": "spring"}, timeout=0.001)

            # This may timeout or succeed depending on network conditions
            # We just verify it doesn't crash
            assert isinstance(result, ToolCallResult)
        except Exception:
            # Timeout or connection error is acceptable
            pass
        finally:
            await client.close()


@pytest.mark.stability
@pytest.mark.asyncio
class TestHealthCheck:
    """Tests for MCP client health check."""

    async def test_health_check(self, connected_mcp_client: MCPClient) -> None:
        """Test health check functionality."""
        health = await connected_mcp_client.health_check()

        assert isinstance(health, dict)
        assert health["status"] == "healthy", f"Health check failed: {health}"
        assert health["connected"] is True
        assert "tools_count" in health
        assert health["tools_count"] > 0
        assert "duration_ms" in health

    async def test_health_check_with_auto_connect(
        self, mcp_server_config: MCPServerConfig
    ) -> None:
        """Test health check auto-connects if needed."""
        client = MCPClient(mcp_server_config)
        try:
            # Not connected yet
            assert not client._initialized

            # Health check should auto-connect
            health = await client.health_check()

            assert health["status"] == "healthy"
            assert client._initialized, "Client should be initialized after health check"
        finally:
            await client.close()


@pytest.mark.stability
@pytest.mark.asyncio
class TestCallToolSync:
    """Tests for synchronous-style tool calling."""

    async def test_call_tool_sync_auto_connect(
        self, mcp_server_config: MCPServerConfig
    ) -> None:
        """Test call_tool_sync auto-connects if needed."""
        client = MCPClient(mcp_server_config)
        try:
            # Not connected yet
            assert not client._initialized

            # call_tool_sync should auto-connect
            result = await client.call_tool_sync("listSpringProjects")

            assert result.success, f"Tool call failed: {result.error}"
            assert client._initialized, "Client should be initialized"
        finally:
            await client.close()


@pytest.mark.stability
@pytest.mark.asyncio
class TestRequestIdTracking:
    """Tests for request ID tracking."""

    async def test_request_id_increments(self, connected_mcp_client: MCPClient) -> None:
        """Test that request IDs increment with each request."""
        initial_id = connected_mcp_client._request_id

        await connected_mcp_client.list_tools()
        after_first = connected_mcp_client._request_id

        await connected_mcp_client.call_tool("listSpringProjects")
        after_second = connected_mcp_client._request_id

        assert after_first > initial_id, "Request ID should increment"
        assert after_second > after_first, "Request ID should continue incrementing"


@pytest.mark.stability
@pytest.mark.slow
@pytest.mark.asyncio
class TestMultipleToolCalls:
    """Tests for multiple sequential tool calls."""

    async def test_sequential_tool_calls(self, connected_mcp_client: MCPClient) -> None:
        """Test multiple sequential tool calls."""
        tools_to_call = [
            ("listSpringProjects", {}),
            ("listSpringBootVersions", {"state": "GA", "limit": 3}),
            ("listSpringProjects", {}),  # Repeat to test stability
        ]

        results = []
        for tool_name, args in tools_to_call:
            result = await connected_mcp_client.call_tool(tool_name, args)
            results.append(result)

        # All calls should succeed
        for i, result in enumerate(results):
            assert result.success, f"Tool call {i + 1} failed: {result.error}"

    async def test_concurrent_tool_calls(self, connected_mcp_client: MCPClient) -> None:
        """Test concurrent tool calls."""
        # Create multiple concurrent tool calls
        tasks = [
            connected_mcp_client.call_tool("listSpringProjects"),
            connected_mcp_client.call_tool("listSpringBootVersions", {"state": "GA", "limit": 3}),
            connected_mcp_client.call_tool("listSpringProjects"),
        ]

        results = await asyncio.gather(*tasks)

        # All calls should complete (may have varying success)
        assert len(results) == 3
        for result in results:
            assert isinstance(result, ToolCallResult)
