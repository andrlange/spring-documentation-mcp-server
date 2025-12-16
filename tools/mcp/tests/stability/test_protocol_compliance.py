"""MCP Transport 2025-06-18 Protocol Compliance Tests.

These tests verify compliance with the MCP Transport 2025-06-18 specification.
See: https://spec.modelcontextprotocol.io/2025-06-18/basic/transports/

Key compliance features tested:
- MCP-Protocol-Version header on responses
- Mcp-Session-Id header support (alternative to query param)
- Origin header validation
- Backwards compatibility with existing clients
"""

import asyncio
import sys
import uuid
from pathlib import Path
from typing import Optional

import aiohttp
import pytest
import pytest_asyncio

# Add src to path for imports
src_path = str(Path(__file__).parent.parent.parent / "src")
if src_path not in sys.path:
    sys.path.insert(0, src_path)

from client.sse_client import SSEClient
from config import MCPServerConfig


@pytest.mark.stability
@pytest.mark.asyncio
class TestProtocolVersionHeader:
    """Tests for MCP-Protocol-Version header compliance."""

    async def test_response_includes_protocol_version_header(
        self, mcp_server_config: MCPServerConfig
    ) -> None:
        """Test that MCP responses include the MCP-Protocol-Version header."""
        url = f"{mcp_server_config.base_url}/mcp/spring/sse"
        headers = {"X-API-Key": mcp_server_config.api_key}

        async with aiohttp.ClientSession() as session:
            async with session.get(url, headers=headers, timeout=aiohttp.ClientTimeout(total=5)) as response:
                # Check for the protocol version header
                protocol_version = response.headers.get("MCP-Protocol-Version")
                assert protocol_version is not None, (
                    "Response should include MCP-Protocol-Version header"
                )
                assert protocol_version == "2025-06-18", (
                    f"Protocol version should be 2025-06-18, got: {protocol_version}"
                )

    async def test_protocol_header_on_sse_endpoint(
        self, mcp_server_config: MCPServerConfig
    ) -> None:
        """Test protocol header is present on SSE endpoint."""
        url = f"{mcp_server_config.base_url}/mcp/spring/sse"
        headers = {"X-API-Key": mcp_server_config.api_key}

        async with aiohttp.ClientSession() as session:
            async with session.get(url, headers=headers, timeout=aiohttp.ClientTimeout(total=5)) as response:
                assert "MCP-Protocol-Version" in response.headers
                assert response.headers["MCP-Protocol-Version"] == "2025-06-18"

    async def test_protocol_header_on_message_endpoint(
        self, mcp_server_config: MCPServerConfig
    ) -> None:
        """Test protocol header is present on message endpoint."""
        # First connect to get session ID
        client = SSEClient(mcp_server_config)
        try:
            connected = await client.connect(timeout=10.0)
            if not connected:
                pytest.skip("Could not connect to MCP server")

            message_endpoint = client.message_endpoint
            if not message_endpoint:
                pytest.skip("No message endpoint available")

            headers = {
                "X-API-Key": mcp_server_config.api_key,
                "Content-Type": "application/json",
            }

            # Send a simple request to the message endpoint
            async with aiohttp.ClientSession() as session:
                payload = {
                    "jsonrpc": "2.0",
                    "method": "tools/list",
                    "id": 1,
                }
                async with session.post(
                    message_endpoint,
                    json=payload,
                    headers=headers,
                    timeout=aiohttp.ClientTimeout(total=10)
                ) as response:
                    assert "MCP-Protocol-Version" in response.headers
                    assert response.headers["MCP-Protocol-Version"] == "2025-06-18"

        finally:
            await client.close()


@pytest.mark.stability
@pytest.mark.asyncio
class TestSessionIdHeader:
    """Tests for Mcp-Session-Id header support."""

    async def test_session_id_from_header(
        self, mcp_server_config: MCPServerConfig
    ) -> None:
        """Test that session ID can be provided via header."""
        session_id = str(uuid.uuid4())
        url = f"{mcp_server_config.base_url}/mcp/spring/sse"
        headers = {
            "X-API-Key": mcp_server_config.api_key,
            "Mcp-Session-Id": session_id,
        }

        async with aiohttp.ClientSession() as session:
            async with session.get(url, headers=headers, timeout=aiohttp.ClientTimeout(total=5)) as response:
                # Response should echo back the session ID
                response_session_id = response.headers.get("Mcp-Session-Id")
                assert response_session_id is not None, (
                    "Response should include Mcp-Session-Id header"
                )
                assert response_session_id == session_id, (
                    f"Session ID should match: expected {session_id}, got {response_session_id}"
                )

    async def test_session_id_from_query_param_backwards_compat(
        self, mcp_server_config: MCPServerConfig
    ) -> None:
        """Test backwards compatibility with sessionId query parameter."""
        session_id = str(uuid.uuid4())
        url = f"{mcp_server_config.base_url}/mcp/spring/sse?sessionId={session_id}"
        headers = {"X-API-Key": mcp_server_config.api_key}

        async with aiohttp.ClientSession() as session:
            async with session.get(url, headers=headers, timeout=aiohttp.ClientTimeout(total=5)) as response:
                # Server should accept query param and echo in header
                response_session_id = response.headers.get("Mcp-Session-Id")
                assert response_session_id is not None, (
                    "Response should include Mcp-Session-Id header for query param"
                )
                assert response_session_id == session_id, (
                    f"Session ID should match query param: expected {session_id}, got {response_session_id}"
                )

    async def test_header_takes_precedence_over_query_param(
        self, mcp_server_config: MCPServerConfig
    ) -> None:
        """Test that Mcp-Session-Id header takes precedence over query param."""
        header_session_id = str(uuid.uuid4())
        query_session_id = str(uuid.uuid4())

        url = f"{mcp_server_config.base_url}/mcp/spring/sse?sessionId={query_session_id}"
        headers = {
            "X-API-Key": mcp_server_config.api_key,
            "Mcp-Session-Id": header_session_id,
        }

        async with aiohttp.ClientSession() as session:
            async with session.get(url, headers=headers, timeout=aiohttp.ClientTimeout(total=5)) as response:
                response_session_id = response.headers.get("Mcp-Session-Id")
                assert response_session_id == header_session_id, (
                    f"Header should take precedence: expected {header_session_id}, got {response_session_id}"
                )


@pytest.mark.stability
@pytest.mark.asyncio
class TestOriginValidation:
    """Tests for Origin header validation."""

    async def test_request_without_origin_succeeds(
        self, mcp_server_config: MCPServerConfig
    ) -> None:
        """Test that requests without Origin header succeed (CLI tools)."""
        url = f"{mcp_server_config.base_url}/mcp/spring/sse"
        headers = {"X-API-Key": mcp_server_config.api_key}

        async with aiohttp.ClientSession() as session:
            async with session.get(url, headers=headers, timeout=aiohttp.ClientTimeout(total=5)) as response:
                # Request without Origin should succeed (for CLI tools)
                assert response.status in (200, 202), (
                    f"Request without Origin should succeed, got status {response.status}"
                )

    async def test_request_with_valid_origin_succeeds(
        self, mcp_server_config: MCPServerConfig
    ) -> None:
        """Test that requests with valid Origin header succeed."""
        url = f"{mcp_server_config.base_url}/mcp/spring/sse"
        headers = {
            "X-API-Key": mcp_server_config.api_key,
            "Origin": "https://example.com",  # Should be allowed with "*" config
        }

        async with aiohttp.ClientSession() as session:
            async with session.get(url, headers=headers, timeout=aiohttp.ClientTimeout(total=5)) as response:
                # With allowed-origins set to "*", any origin should work
                assert response.status in (200, 202), (
                    f"Request with valid Origin should succeed, got status {response.status}"
                )


@pytest.mark.stability
@pytest.mark.asyncio
class TestBackwardsCompatibility:
    """Tests for backwards compatibility with existing clients."""

    async def test_bearer_token_auth_still_works(
        self, mcp_server_config: MCPServerConfig
    ) -> None:
        """Test that Bearer token authentication still works."""
        url = f"{mcp_server_config.base_url}/mcp/spring/sse"
        headers = {"Authorization": f"Bearer {mcp_server_config.api_key}"}

        async with aiohttp.ClientSession() as session:
            async with session.get(url, headers=headers, timeout=aiohttp.ClientTimeout(total=5)) as response:
                assert response.status in (200, 202), (
                    f"Bearer auth should work, got status {response.status}"
                )

    async def test_x_api_key_header_still_works(
        self, mcp_server_config: MCPServerConfig
    ) -> None:
        """Test that X-API-Key header authentication still works."""
        url = f"{mcp_server_config.base_url}/mcp/spring/sse"
        headers = {"X-API-Key": mcp_server_config.api_key}

        async with aiohttp.ClientSession() as session:
            async with session.get(url, headers=headers, timeout=aiohttp.ClientTimeout(total=5)) as response:
                assert response.status in (200, 202), (
                    f"X-API-Key auth should work, got status {response.status}"
                )

    async def test_query_param_session_id_still_works(
        self, mcp_server_config: MCPServerConfig
    ) -> None:
        """Test that sessionId query parameter still works."""
        session_id = str(uuid.uuid4())
        url = f"{mcp_server_config.base_url}/mcp/spring/sse?sessionId={session_id}"
        headers = {"X-API-Key": mcp_server_config.api_key}

        async with aiohttp.ClientSession() as session:
            async with session.get(url, headers=headers, timeout=aiohttp.ClientTimeout(total=5)) as response:
                assert response.status in (200, 202), (
                    f"Query param session ID should work, got status {response.status}"
                )

    async def test_existing_client_connection_flow(
        self, mcp_server_config: MCPServerConfig
    ) -> None:
        """Test that existing client connection flow still works."""
        client = SSEClient(mcp_server_config)
        try:
            # Standard connection flow
            connected = await client.connect(timeout=10.0)
            assert connected, "Standard client connection should still work"
            assert client.is_connected, "Client should report as connected"
            assert client.session_id is not None, "Session ID should be set"

            # Connection info should be available
            info = client.connection_info
            assert info is not None, "Connection info should be available"

        finally:
            await client.close()


@pytest.mark.stability
@pytest.mark.asyncio
class TestCORSHeaders:
    """Tests for CORS header compliance with MCP headers."""

    async def test_mcp_session_id_in_allowed_headers(
        self, mcp_server_config: MCPServerConfig
    ) -> None:
        """Test that Mcp-Session-Id is in Access-Control-Allow-Headers."""
        url = f"{mcp_server_config.base_url}/mcp/spring/sse"

        async with aiohttp.ClientSession() as session:
            # Send preflight OPTIONS request
            async with session.options(
                url,
                headers={
                    "Origin": "https://example.com",
                    "Access-Control-Request-Method": "GET",
                    "Access-Control-Request-Headers": "Mcp-Session-Id,X-API-Key",
                },
                timeout=aiohttp.ClientTimeout(total=5)
            ) as response:
                allowed_headers = response.headers.get("Access-Control-Allow-Headers", "")
                # Check that Mcp-Session-Id is allowed
                assert "Mcp-Session-Id" in allowed_headers or allowed_headers == "*", (
                    f"Mcp-Session-Id should be in allowed headers: {allowed_headers}"
                )

    async def test_mcp_protocol_version_in_exposed_headers(
        self, mcp_server_config: MCPServerConfig
    ) -> None:
        """Test that MCP-Protocol-Version is in Access-Control-Expose-Headers."""
        url = f"{mcp_server_config.base_url}/mcp/spring/sse"
        headers = {
            "X-API-Key": mcp_server_config.api_key,
            "Origin": "https://example.com",
        }

        async with aiohttp.ClientSession() as session:
            async with session.get(url, headers=headers, timeout=aiohttp.ClientTimeout(total=5)) as response:
                exposed_headers = response.headers.get("Access-Control-Expose-Headers", "")
                # Check that MCP-Protocol-Version is exposed
                assert "MCP-Protocol-Version" in exposed_headers, (
                    f"MCP-Protocol-Version should be in exposed headers: {exposed_headers}"
                )


@pytest.mark.stability
@pytest.mark.asyncio
class TestJSONRPCCompliance:
    """Tests for JSON-RPC 2.0 compliance in MCP messages."""

    async def test_response_has_jsonrpc_version(
        self, mcp_server_config: MCPServerConfig
    ) -> None:
        """Test that responses include jsonrpc version field."""
        client = SSEClient(mcp_server_config)
        try:
            connected = await client.connect(timeout=10.0)
            if not connected:
                pytest.skip("Could not connect to MCP server")

            message_endpoint = client.message_endpoint
            if not message_endpoint:
                pytest.skip("No message endpoint available")

            headers = {
                "X-API-Key": mcp_server_config.api_key,
                "Content-Type": "application/json",
            }

            async with aiohttp.ClientSession() as session:
                payload = {
                    "jsonrpc": "2.0",
                    "method": "tools/list",
                    "id": 1,
                }
                async with session.post(
                    message_endpoint,
                    json=payload,
                    headers=headers,
                    timeout=aiohttp.ClientTimeout(total=10)
                ) as response:
                    if response.status == 200:
                        data = await response.json()
                        assert data.get("jsonrpc") == "2.0", (
                            f"Response should have jsonrpc: 2.0, got: {data.get('jsonrpc')}"
                        )
                        assert "id" in data, "Response should have id field"
                        assert data["id"] == 1, f"Response id should match request: got {data['id']}"

        finally:
            await client.close()

    async def test_error_response_format(
        self, mcp_server_config: MCPServerConfig
    ) -> None:
        """Test that error responses follow JSON-RPC 2.0 format."""
        client = SSEClient(mcp_server_config)
        try:
            connected = await client.connect(timeout=10.0)
            if not connected:
                pytest.skip("Could not connect to MCP server")

            message_endpoint = client.message_endpoint
            if not message_endpoint:
                pytest.skip("No message endpoint available")

            headers = {
                "X-API-Key": mcp_server_config.api_key,
                "Content-Type": "application/json",
            }

            # Send invalid request to trigger error
            async with aiohttp.ClientSession() as session:
                payload = {
                    "jsonrpc": "2.0",
                    "method": "invalid/method",
                    "id": 99,
                }
                async with session.post(
                    message_endpoint,
                    json=payload,
                    headers=headers,
                    timeout=aiohttp.ClientTimeout(total=10)
                ) as response:
                    if response.status in (200, 400):
                        data = await response.json()
                        # Error response should have error field
                        if "error" in data:
                            error = data["error"]
                            assert "code" in error, "Error should have code field"
                            assert "message" in error, "Error should have message field"

        finally:
            await client.close()
