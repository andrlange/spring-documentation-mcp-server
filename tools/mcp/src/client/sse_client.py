"""SSE (Server-Sent Events) client for MCP connections."""

import asyncio
import logging
import time
from datetime import datetime
from typing import Any, AsyncIterator, Callable, Optional

import aiohttp
from aiohttp_sse_client import client as sse_client

from config import MCPServerConfig
from models.mcp_messages import ConnectionInfo, SSEEvent

logger = logging.getLogger(__name__)


class SSEClient:
    """Async SSE client for MCP server connections."""

    def __init__(
        self,
        config: MCPServerConfig,
        on_message: Optional[Callable[[SSEEvent], None]] = None,
        on_error: Optional[Callable[[Exception], None]] = None,
        on_disconnect: Optional[Callable[[], None]] = None,
    ):
        """
        Initialize SSE client.

        Args:
            config: MCP server configuration
            on_message: Callback for received messages
            on_error: Callback for errors
            on_disconnect: Callback for disconnection
        """
        self.config = config
        self.on_message = on_message
        self.on_error = on_error
        self.on_disconnect = on_disconnect

        self._session: Optional[aiohttp.ClientSession] = None
        self._event_source: Optional[sse_client.EventSource] = None
        self._is_connected = False
        self._connection_info: Optional[ConnectionInfo] = None
        self._last_event_time: Optional[float] = None
        self._heartbeat_task: Optional[asyncio.Task] = None
        self._message_queue: asyncio.Queue[SSEEvent] = asyncio.Queue()

    @property
    def is_connected(self) -> bool:
        """Check if currently connected."""
        return self._is_connected

    @property
    def connection_info(self) -> Optional[ConnectionInfo]:
        """Get current connection info."""
        return self._connection_info

    @property
    def session_id(self) -> Optional[str]:
        """Get current session ID."""
        return self._connection_info.session_id if self._connection_info else None

    @property
    def message_endpoint(self) -> Optional[str]:
        """Get message endpoint URL from connection."""
        if self._connection_info and self._connection_info.message_endpoint:
            # If message endpoint is relative, make it absolute
            endpoint = self._connection_info.message_endpoint
            if endpoint.startswith("/"):
                return f"{self.config.base_url}{endpoint}"
            return endpoint
        return self.config.message_url

    async def connect(self, timeout: Optional[float] = None) -> bool:
        """
        Establish SSE connection.

        Args:
            timeout: Connection timeout in seconds (default from config)

        Returns:
            True if connection successful, False otherwise
        """
        if self._is_connected:
            logger.warning("Already connected")
            return True

        timeout = timeout or (self.config.base_url and 10.0)

        try:
            # Create session with auth headers
            headers = self.config.get_auth_headers()
            headers["Accept"] = "text/event-stream"
            headers["Cache-Control"] = "no-cache"

            self._session = aiohttp.ClientSession(headers=headers)

            # Build URL with auth params if using query auth
            url = self.config.sse_url
            params = self.config.get_auth_params()

            logger.info(f"Connecting to SSE endpoint: {url}")

            # Create event source
            self._event_source = sse_client.EventSource(
                url,
                session=self._session,
                params=params if params else None,
                timeout=aiohttp.ClientTimeout(total=None, connect=timeout),
            )

            # Connect and wait for initial endpoint event
            await self._event_source.connect()
            self._is_connected = True
            self._last_event_time = time.time()

            # Wait for endpoint event with session info
            async with asyncio.timeout(timeout or 10.0):
                async for event in self._event_source:
                    sse_event = SSEEvent(
                        event=event.type,
                        data=event.data,
                        id=event.last_event_id,
                    )

                    if sse_event.is_endpoint:
                        # Parse endpoint data for session info
                        endpoint_data = sse_event.parse_json()
                        if endpoint_data:
                            self._connection_info = ConnectionInfo(
                                connected_at=datetime.now(),
                                message_endpoint=endpoint_data.get("uri"),
                                session_id=self._extract_session_id(endpoint_data.get("uri")),
                            )
                        else:
                            # Data might be just the endpoint URL string
                            self._connection_info = ConnectionInfo(
                                connected_at=datetime.now(),
                                message_endpoint=sse_event.data,
                                session_id=self._extract_session_id(sse_event.data),
                            )

                        logger.info(f"Connected with session: {self.session_id}")
                        return True

            logger.warning("Connected but no endpoint event received")
            return True

        except asyncio.TimeoutError:
            logger.error(f"Connection timeout after {timeout}s")
            await self.close()
            return False
        except Exception as e:
            logger.error(f"Connection failed: {e}")
            if self.on_error:
                self.on_error(e)
            await self.close()
            return False

    def _extract_session_id(self, endpoint: Optional[str]) -> Optional[str]:
        """Extract session ID from endpoint URL."""
        if not endpoint:
            return None
        # Session ID is typically a query parameter or path segment
        if "sessionId=" in endpoint:
            import re
            match = re.search(r"sessionId=([^&]+)", endpoint)
            if match:
                return match.group(1)
        # Try to extract from path
        if "/" in endpoint:
            parts = endpoint.rstrip("/").split("/")
            if parts:
                return parts[-1]
        return None

    async def listen(self) -> AsyncIterator[SSEEvent]:
        """
        Listen for SSE events.

        Yields:
            SSEEvent objects as they arrive
        """
        if not self._is_connected or not self._event_source:
            raise RuntimeError("Not connected. Call connect() first.")

        try:
            async for event in self._event_source:
                self._last_event_time = time.time()

                sse_event = SSEEvent(
                    event=event.type,
                    data=event.data,
                    id=event.last_event_id,
                )

                # Put in queue for other consumers
                await self._message_queue.put(sse_event)

                # Call callback if set
                if self.on_message:
                    self.on_message(sse_event)

                yield sse_event

        except Exception as e:
            logger.error(f"Error during event listening: {e}")
            self._is_connected = False
            if self.on_error:
                self.on_error(e)
            if self.on_disconnect:
                self.on_disconnect()
            raise

    async def wait_for_message(self, timeout: Optional[float] = None) -> Optional[SSEEvent]:
        """
        Wait for next message event.

        Args:
            timeout: Maximum time to wait in seconds

        Returns:
            SSEEvent or None if timeout
        """
        try:
            if timeout:
                return await asyncio.wait_for(self._message_queue.get(), timeout)
            return await self._message_queue.get()
        except asyncio.TimeoutError:
            return None

    async def wait_for_heartbeat(self, timeout: Optional[float] = None) -> bool:
        """
        Wait for a heartbeat event.

        Args:
            timeout: Maximum time to wait in seconds

        Returns:
            True if heartbeat received, False if timeout
        """
        start_time = time.time()
        timeout = timeout or 35.0  # Default: heartbeat interval + 5s buffer

        try:
            async for event in self.listen():
                # Check for heartbeat/ping events
                if event.event in ("heartbeat", "ping", "keep-alive"):
                    return True

                # Also consider any event as sign of connection being alive
                if event.data:
                    return True

                # Check timeout
                if time.time() - start_time > timeout:
                    return False

        except asyncio.TimeoutError:
            return False
        except Exception:
            return False

        return False

    def time_since_last_event(self) -> Optional[float]:
        """Get seconds since last event received."""
        if self._last_event_time is None:
            return None
        return time.time() - self._last_event_time

    async def check_connection_health(self) -> bool:
        """
        Check if connection appears healthy.

        Returns:
            True if connection seems healthy
        """
        if not self._is_connected:
            return False

        # Check if we've received events recently
        time_since = self.time_since_last_event()
        if time_since is not None and time_since > 60:  # 60 seconds without events
            logger.warning(f"No events received for {time_since:.1f}s")
            return False

        return True

    async def close(self) -> None:
        """Close the SSE connection."""
        self._is_connected = False

        if self._heartbeat_task:
            self._heartbeat_task.cancel()
            try:
                await self._heartbeat_task
            except asyncio.CancelledError:
                pass
            self._heartbeat_task = None

        if self._event_source:
            await self._event_source.close()
            self._event_source = None

        if self._session:
            await self._session.close()
            self._session = None

        self._connection_info = None
        logger.info("SSE connection closed")

    async def __aenter__(self) -> "SSEClient":
        """Async context manager entry."""
        await self.connect()
        return self

    async def __aexit__(self, exc_type: Any, exc_val: Any, exc_tb: Any) -> None:
        """Async context manager exit."""
        await self.close()


class ReconnectingSSEClient(SSEClient):
    """SSE client with automatic reconnection support."""

    def __init__(
        self,
        config: MCPServerConfig,
        max_retries: int = 5,
        backoff_base: float = 1.0,
        backoff_max: float = 60.0,
        backoff_multiplier: float = 2.0,
        **kwargs: Any,
    ):
        """
        Initialize reconnecting SSE client.

        Args:
            config: MCP server configuration
            max_retries: Maximum reconnection attempts
            backoff_base: Initial backoff delay in seconds
            backoff_max: Maximum backoff delay in seconds
            backoff_multiplier: Backoff multiplier
            **kwargs: Additional arguments for SSEClient
        """
        super().__init__(config, **kwargs)
        self.max_retries = max_retries
        self.backoff_base = backoff_base
        self.backoff_max = backoff_max
        self.backoff_multiplier = backoff_multiplier
        self._reconnect_count = 0

    @property
    def reconnect_count(self) -> int:
        """Get number of reconnection attempts."""
        return self._reconnect_count

    async def connect_with_retry(self) -> bool:
        """
        Connect with automatic retry on failure.

        Returns:
            True if connection successful
        """
        delay = self.backoff_base

        for attempt in range(self.max_retries + 1):
            if attempt > 0:
                logger.info(f"Reconnection attempt {attempt}/{self.max_retries}")
                self._reconnect_count += 1
                await asyncio.sleep(delay)
                delay = min(delay * self.backoff_multiplier, self.backoff_max)

            if await self.connect():
                if attempt > 0:
                    logger.info(f"Reconnected after {attempt} attempts")
                return True

        logger.error(f"Failed to connect after {self.max_retries} attempts")
        return False

    async def listen_with_reconnect(self) -> AsyncIterator[SSEEvent]:
        """
        Listen for events with automatic reconnection.

        Yields:
            SSEEvent objects
        """
        while True:
            try:
                if not self._is_connected:
                    if not await self.connect_with_retry():
                        raise RuntimeError("Could not establish connection")

                async for event in self.listen():
                    yield event

            except Exception as e:
                logger.warning(f"Connection lost: {e}")
                await self.close()

                if not await self.connect_with_retry():
                    raise RuntimeError("Reconnection failed") from e
