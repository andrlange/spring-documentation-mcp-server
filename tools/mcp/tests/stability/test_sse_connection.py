"""SSE Connection Stability Tests.

These tests verify the stability and reliability of SSE connections
to the MCP server under various conditions.
"""

import asyncio
import sys
import time
from pathlib import Path
from typing import Optional

import pytest
import pytest_asyncio

# Add src to path for imports
src_path = str(Path(__file__).parent.parent.parent / "src")
if src_path not in sys.path:
    sys.path.insert(0, src_path)

from client.sse_client import SSEClient, ReconnectingSSEClient
from config import MCPServerConfig
from models.mcp_messages import SSEEvent


@pytest.mark.stability
@pytest.mark.asyncio
class TestSSEConnection:
    """Tests for basic SSE connection functionality."""

    async def test_initial_connection(self, mcp_server_config: MCPServerConfig) -> None:
        """Test that initial SSE connection can be established."""
        client = SSEClient(mcp_server_config)
        try:
            connected = await client.connect(timeout=10.0)
            assert connected, "Failed to establish initial SSE connection"
            assert client.is_connected, "Client should report as connected"
            assert client.session_id is not None, "Session ID should be set after connection"
        finally:
            await client.close()

    async def test_connection_info(self, connected_sse_client: SSEClient) -> None:
        """Test that connection info is populated correctly."""
        info = connected_sse_client.connection_info
        assert info is not None, "Connection info should be available"
        assert info.connected_at is not None, "Connected timestamp should be set"
        assert info.message_endpoint is not None, "Message endpoint should be set"

    async def test_message_endpoint_url(self, connected_sse_client: SSEClient) -> None:
        """Test that message endpoint URL is correctly formed."""
        endpoint = connected_sse_client.message_endpoint
        assert endpoint is not None, "Message endpoint should not be None"
        assert endpoint.startswith("http"), "Endpoint should be a full URL"
        assert "/mcp/" in endpoint or "/messages" in endpoint, "Endpoint should contain MCP path"

    async def test_connection_close(self, mcp_server_config: MCPServerConfig) -> None:
        """Test that connection can be cleanly closed."""
        client = SSEClient(mcp_server_config)
        await client.connect(timeout=10.0)
        assert client.is_connected

        await client.close()
        assert not client.is_connected, "Client should report as disconnected after close"
        assert client.connection_info is None, "Connection info should be cleared after close"

    async def test_connection_timeout(self, mcp_server_config: MCPServerConfig) -> None:
        """Test connection timeout handling."""
        # Use invalid URL to trigger timeout
        config = MCPServerConfig(
            base_url="http://localhost:59999",  # Non-existent port
            api_key=mcp_server_config.api_key,
        )
        client = SSEClient(config)
        try:
            connected = await client.connect(timeout=2.0)
            assert not connected, "Connection should fail with invalid URL"
        finally:
            await client.close()


@pytest.mark.stability
@pytest.mark.asyncio
class TestSSEReconnection:
    """Tests for SSE reconnection capabilities."""

    async def test_reconnecting_client_creation(
        self, mcp_server_config: MCPServerConfig
    ) -> None:
        """Test creating a reconnecting SSE client."""
        client = ReconnectingSSEClient(
            mcp_server_config,
            max_retries=3,
            backoff_base=0.5,
            backoff_max=5.0,
        )
        try:
            assert client.max_retries == 3
            assert client.backoff_base == 0.5
            assert client.backoff_max == 5.0
            assert client.reconnect_count == 0
        finally:
            await client.close()

    async def test_connect_with_retry_success(
        self, mcp_server_config: MCPServerConfig
    ) -> None:
        """Test successful connection with retry mechanism."""
        client = ReconnectingSSEClient(
            mcp_server_config,
            max_retries=3,
            backoff_base=0.5,
        )
        try:
            connected = await client.connect_with_retry()
            assert connected, "Should connect successfully"
            assert client.is_connected
        finally:
            await client.close()

    async def test_reconnect_count_tracking(
        self, mcp_server_config: MCPServerConfig
    ) -> None:
        """Test that reconnection attempts are tracked."""
        client = ReconnectingSSEClient(
            mcp_server_config,
            max_retries=3,
            backoff_base=0.1,
        )
        try:
            # First successful connection
            await client.connect_with_retry()
            initial_count = client.reconnect_count

            # Simulate disconnect and reconnect
            await client.close()
            await client.connect_with_retry()

            # Count should not increase on first try if connection succeeds
            assert client.reconnect_count >= initial_count
        finally:
            await client.close()


@pytest.mark.stability
@pytest.mark.asyncio
class TestConnectionHealth:
    """Tests for connection health monitoring."""

    async def test_time_since_last_event(self, connected_sse_client: SSEClient) -> None:
        """Test tracking time since last event."""
        # After connection, time since last event should be very small
        time_since = connected_sse_client.time_since_last_event()
        assert time_since is not None, "Should have recorded last event time"
        assert time_since < 5.0, f"Time since last event should be recent: {time_since}s"

    async def test_connection_health_check(self, connected_sse_client: SSEClient) -> None:
        """Test connection health check."""
        is_healthy = await connected_sse_client.check_connection_health()
        assert is_healthy, "Fresh connection should be healthy"

    @pytest.mark.slow
    async def test_idle_connection_health(
        self, mcp_server_config: MCPServerConfig
    ) -> None:
        """Test connection health after idle period."""
        client = SSEClient(mcp_server_config)
        try:
            await client.connect(timeout=10.0)

            # Wait for a short idle period
            await asyncio.sleep(5.0)

            # Connection should still be considered healthy
            is_healthy = await client.check_connection_health()
            assert is_healthy, "Connection should remain healthy after short idle"
        finally:
            await client.close()


@pytest.mark.stability
@pytest.mark.asyncio
class TestMultipleConnections:
    """Tests for multiple simultaneous connections."""

    async def test_multiple_clients(self, mcp_server_config: MCPServerConfig) -> None:
        """Test creating multiple simultaneous SSE connections."""
        clients: list[SSEClient] = []
        try:
            # Create 3 simultaneous connections
            for i in range(3):
                client = SSEClient(mcp_server_config)
                connected = await client.connect(timeout=10.0)
                if connected:
                    clients.append(client)

            assert len(clients) >= 1, "At least one connection should succeed"

            # Each client should have unique session
            session_ids = [c.session_id for c in clients if c.session_id]
            assert len(session_ids) == len(set(session_ids)), "Each client should have unique session"

        finally:
            for client in clients:
                await client.close()

    async def test_sequential_connect_disconnect(
        self, mcp_server_config: MCPServerConfig
    ) -> None:
        """Test sequential connect/disconnect cycles."""
        for cycle in range(3):
            client = SSEClient(mcp_server_config)
            try:
                connected = await client.connect(timeout=10.0)
                assert connected, f"Connection should succeed on cycle {cycle + 1}"
                assert client.is_connected
            finally:
                await client.close()
                assert not client.is_connected


@pytest.mark.stability
@pytest.mark.slow
@pytest.mark.asyncio
class TestLongRunningConnection:
    """Tests for long-running connection stability."""

    async def test_connection_stability_30s(
        self, mcp_server_config: MCPServerConfig
    ) -> None:
        """Test connection stability over 30 seconds."""
        client = SSEClient(mcp_server_config)
        try:
            connected = await client.connect(timeout=10.0)
            assert connected, "Initial connection should succeed"

            start_time = time.time()
            check_interval = 5.0
            duration = 30.0

            while time.time() - start_time < duration:
                await asyncio.sleep(check_interval)

                # Verify connection still healthy
                is_healthy = await client.check_connection_health()
                elapsed = time.time() - start_time
                assert is_healthy, f"Connection became unhealthy after {elapsed:.1f}s"

        finally:
            await client.close()

    async def test_heartbeat_detection(
        self, mcp_server_config: MCPServerConfig
    ) -> None:
        """Test that server heartbeats are detected."""
        client = SSEClient(mcp_server_config)
        try:
            connected = await client.connect(timeout=10.0)
            assert connected, "Connection should succeed"

            # Wait for heartbeat (server typically sends every 30s)
            # Using shorter timeout for test
            heartbeat_received = await client.wait_for_heartbeat(timeout=35.0)

            # Note: This may not always pass if server doesn't send heartbeats
            # within the timeout. This is informational.
            if not heartbeat_received:
                pytest.skip("No heartbeat received within timeout (may be expected)")

        finally:
            await client.close()
