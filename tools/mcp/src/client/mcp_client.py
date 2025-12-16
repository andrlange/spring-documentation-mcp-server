"""MCP protocol client for tool invocation."""

import asyncio
import json
import logging
import time
from typing import Any, Optional

import httpx

from config import MCPServerConfig, get_config
from models.mcp_messages import (
    MCPRequest,
    MCPResponse,
    MCPToolResult,
    ToolInfo,
    ToolsListResult,
)
from client.sse_client import SSEClient

logger = logging.getLogger(__name__)


class ToolCallResult:
    """Result of a tool call with timing information."""

    def __init__(
        self,
        tool_name: str,
        success: bool,
        result: Optional[MCPToolResult] = None,
        error: Optional[str] = None,
        duration_ms: float = 0,
        response_size_bytes: int = 0,
    ):
        self.tool_name = tool_name
        self.success = success
        self.result = result
        self.error = error
        self.duration_ms = duration_ms
        self.response_size_bytes = response_size_bytes

    def get_json_content(self) -> Optional[dict[str, Any]]:
        """Get result content as parsed JSON."""
        if self.result:
            return self.result.get_json_content()
        return None

    def get_text_content(self) -> str:
        """Get result content as text."""
        if self.result:
            return self.result.get_text_content()
        return ""


class MCPClient:
    """MCP protocol client for invoking tools."""

    def __init__(
        self,
        config: Optional[MCPServerConfig] = None,
        timeout: float = 30.0,
    ):
        """
        Initialize MCP client.

        Args:
            config: MCP server configuration (uses global config if not provided)
            timeout: Default timeout for requests in seconds
        """
        self.config = config or get_config().mcp_server
        self.timeout = timeout
        self._sse_client: Optional[SSEClient] = None
        self._http_client: Optional[httpx.AsyncClient] = None
        self._request_id = 0
        self._tools_cache: Optional[list[ToolInfo]] = None
        self._initialized = False
        self._response_queue: asyncio.Queue[MCPResponse] = asyncio.Queue()
        self._listener_task: Optional[asyncio.Task] = None

    def _next_request_id(self) -> int:
        """Get next request ID."""
        self._request_id += 1
        return self._request_id

    async def _ensure_http_client(self) -> httpx.AsyncClient:
        """Ensure HTTP client is initialized."""
        if self._http_client is None:
            self._http_client = httpx.AsyncClient(
                timeout=self.timeout,
                headers=self.config.get_auth_headers(),
            )
        return self._http_client

    async def connect(self) -> bool:
        """
        Connect to MCP server via SSE and initialize.

        Returns:
            True if connection successful
        """
        try:
            # Create and connect SSE client
            self._sse_client = SSEClient(self.config)
            if not await self._sse_client.connect():
                return False

            # Start background SSE listener
            self._listener_task = asyncio.create_task(self._listen_for_responses())

            # Initialize MCP session
            await self._initialize()
            return True

        except Exception as e:
            logger.error(f"Connection failed: {e}")
            return False

    async def _listen_for_responses(self) -> None:
        """Background task to listen for SSE responses and queue them."""
        if not self._sse_client or not self._sse_client._event_source:
            return

        try:
            async for event in self._sse_client._event_source:
                # Skip endpoint events
                if event.type == "endpoint":
                    continue

                # Parse message event
                if event.data:
                    try:
                        data = json.loads(event.data)
                        response = MCPResponse(**data)
                        await self._response_queue.put(response)
                        logger.debug(f"Queued response for request {response.id}")
                    except json.JSONDecodeError:
                        logger.debug(f"Non-JSON SSE event: {event.data[:100] if event.data else 'empty'}")
                        continue
                    except Exception as e:
                        logger.warning(f"Failed to parse SSE event: {e}")

        except asyncio.CancelledError:
            logger.debug("SSE listener task cancelled")
        except Exception as e:
            logger.error(f"SSE listener error: {e}")

    async def _initialize(self) -> None:
        """Send initialize request to MCP server."""
        if not self._sse_client or not self._sse_client.message_endpoint:
            raise RuntimeError("SSE client not connected")

        request = MCPRequest.initialize(self._next_request_id())
        response = await self._send_request(request)

        if response.is_error:
            raise RuntimeError(f"Initialize failed: {response.error}")

        # Send initialized notification (required by MCP protocol)
        notification = MCPRequest.initialized_notification()
        await self._send_notification(notification)

        self._initialized = True
        logger.info("MCP session initialized")

    async def _send_notification(self, notification: MCPRequest) -> None:
        """
        Send a notification to MCP server (no response expected).

        Args:
            notification: MCP notification to send
        """
        if not self._sse_client:
            raise RuntimeError("Not connected")

        endpoint = self._sse_client.message_endpoint
        if not endpoint:
            endpoint = self.config.message_url

        client = await self._ensure_http_client()
        headers = {"Content-Type": "application/json"}

        try:
            response = await client.post(
                endpoint,
                json=notification.model_dump(exclude_none=True),
                headers=headers,
            )
            response.raise_for_status()
            logger.debug(f"Notification sent: {notification.method}")
        except Exception as e:
            logger.warning(f"Failed to send notification {notification.method}: {e}")

    async def _send_request(self, request: MCPRequest) -> MCPResponse:
        """
        Send request to MCP message endpoint.

        The Spring AI MCP Server returns 202 Accepted and sends the response
        via SSE. This method handles both direct JSON responses and async SSE responses.

        Args:
            request: MCP request to send

        Returns:
            MCP response
        """
        if not self._sse_client:
            raise RuntimeError("Not connected")

        endpoint = self._sse_client.message_endpoint
        if not endpoint:
            endpoint = self.config.message_url

        client = await self._ensure_http_client()
        headers = {"Content-Type": "application/json"}

        try:
            response = await client.post(
                endpoint,
                json=request.model_dump(exclude_none=True),
                headers=headers,
            )
            response.raise_for_status()

            # Handle 202 Accepted - response comes via SSE
            if response.status_code == 202:
                return await self._wait_for_sse_response(request.id)

            # Handle direct JSON response
            text = response.text
            if not text or text.strip() == "":
                # Empty response, wait for SSE
                return await self._wait_for_sse_response(request.id)

            data = response.json()
            return MCPResponse(**data)

        except httpx.HTTPStatusError as e:
            logger.error(f"HTTP error: {e.response.status_code} - {e.response.text}")
            raise
        except Exception as e:
            logger.error(f"Request failed: {e}")
            raise

    async def _wait_for_sse_response(
        self, request_id: Optional[int | str], timeout: float = 30.0
    ) -> MCPResponse:
        """
        Wait for response via SSE connection (using response queue).

        Args:
            request_id: The request ID to match
            timeout: Timeout in seconds

        Returns:
            MCP response
        """
        start_time = time.time()
        pending_responses: list[MCPResponse] = []

        while True:
            remaining_timeout = timeout - (time.time() - start_time)
            if remaining_timeout <= 0:
                raise asyncio.TimeoutError(f"Timeout waiting for response to request {request_id}")

            try:
                response = await asyncio.wait_for(
                    self._response_queue.get(),
                    timeout=remaining_timeout
                )

                # Match response to request ID if provided
                if request_id is None or response.id == request_id:
                    # Re-queue any pending responses that weren't for us
                    for pending in pending_responses:
                        await self._response_queue.put(pending)
                    return response
                else:
                    # Not our response, save it for later
                    pending_responses.append(response)

            except asyncio.TimeoutError:
                # Re-queue any pending responses before raising
                for pending in pending_responses:
                    await self._response_queue.put(pending)
                raise asyncio.TimeoutError(f"Timeout waiting for response to request {request_id}")

    async def list_tools(self, use_cache: bool = True) -> list[ToolInfo]:
        """
        List available MCP tools.

        Args:
            use_cache: Use cached tools list if available

        Returns:
            List of available tools
        """
        if use_cache and self._tools_cache is not None:
            return self._tools_cache

        request = MCPRequest.list_tools(self._next_request_id())
        response = await self._send_request(request)

        if response.is_error:
            raise RuntimeError(f"List tools failed: {response.error}")

        if response.result:
            result = ToolsListResult(**response.result)
            self._tools_cache = result.tools
            return result.tools

        return []

    async def call_tool(
        self,
        tool_name: str,
        arguments: Optional[dict[str, Any]] = None,
        timeout: Optional[float] = None,
    ) -> ToolCallResult:
        """
        Call an MCP tool.

        Args:
            tool_name: Name of the tool to call
            arguments: Tool arguments
            timeout: Request timeout in seconds

        Returns:
            ToolCallResult with result and timing info
        """
        arguments = arguments or {}
        start_time = time.time()

        try:
            request = MCPRequest.tool_call(tool_name, arguments, self._next_request_id())

            if timeout:
                async with asyncio.timeout(timeout):
                    response = await self._send_request(request)
            else:
                response = await self._send_request(request)

            duration_ms = (time.time() - start_time) * 1000

            if response.is_error:
                return ToolCallResult(
                    tool_name=tool_name,
                    success=False,
                    error=str(response.error),
                    duration_ms=duration_ms,
                )

            tool_result = response.get_tool_result()
            response_size = len(json.dumps(response.result)) if response.result else 0

            return ToolCallResult(
                tool_name=tool_name,
                success=True,
                result=tool_result,
                duration_ms=duration_ms,
                response_size_bytes=response_size,
            )

        except asyncio.TimeoutError:
            duration_ms = (time.time() - start_time) * 1000
            return ToolCallResult(
                tool_name=tool_name,
                success=False,
                error=f"Timeout after {timeout}s",
                duration_ms=duration_ms,
            )
        except Exception as e:
            duration_ms = (time.time() - start_time) * 1000
            return ToolCallResult(
                tool_name=tool_name,
                success=False,
                error=str(e),
                duration_ms=duration_ms,
            )

    async def call_tool_sync(
        self,
        tool_name: str,
        arguments: Optional[dict[str, Any]] = None,
    ) -> ToolCallResult:
        """
        Synchronous-style tool call (connects if needed).

        This method handles connection automatically, making it suitable
        for simple one-off tool calls.

        Args:
            tool_name: Name of the tool to call
            arguments: Tool arguments

        Returns:
            ToolCallResult with result and timing info
        """
        if not self._initialized:
            if not await self.connect():
                return ToolCallResult(
                    tool_name=tool_name,
                    success=False,
                    error="Failed to connect to MCP server",
                )

        return await self.call_tool(tool_name, arguments)

    async def health_check(self) -> dict[str, Any]:
        """
        Perform health check by calling a simple tool.

        Returns:
            Health check result
        """
        start_time = time.time()

        try:
            if not self._initialized:
                await self.connect()

            # Try listing tools as health check
            tools = await self.list_tools(use_cache=False)
            duration_ms = (time.time() - start_time) * 1000

            return {
                "status": "healthy",
                "connected": True,
                "tools_count": len(tools),
                "duration_ms": duration_ms,
                "session_id": self._sse_client.session_id if self._sse_client else None,
            }

        except Exception as e:
            duration_ms = (time.time() - start_time) * 1000
            return {
                "status": "unhealthy",
                "connected": False,
                "error": str(e),
                "duration_ms": duration_ms,
            }

    async def close(self) -> None:
        """Close the MCP client."""
        # Cancel listener task
        if self._listener_task:
            self._listener_task.cancel()
            try:
                await self._listener_task
            except asyncio.CancelledError:
                pass
            self._listener_task = None

        if self._http_client:
            await self._http_client.aclose()
            self._http_client = None

        if self._sse_client:
            await self._sse_client.close()
            self._sse_client = None

        self._initialized = False
        self._tools_cache = None
        # Clear response queue
        while not self._response_queue.empty():
            try:
                self._response_queue.get_nowait()
            except asyncio.QueueEmpty:
                break

    async def __aenter__(self) -> "MCPClient":
        """Async context manager entry."""
        await self.connect()
        return self

    async def __aexit__(self, exc_type: Any, exc_val: Any, exc_tb: Any) -> None:
        """Async context manager exit."""
        await self.close()


class SyncMCPClient:
    """Synchronous wrapper for MCPClient."""

    def __init__(self, config: Optional[MCPServerConfig] = None, timeout: float = 30.0):
        """Initialize sync MCP client."""
        self._async_client = MCPClient(config, timeout)
        self._loop: Optional[asyncio.AbstractEventLoop] = None

    def _get_loop(self) -> asyncio.AbstractEventLoop:
        """Get or create event loop."""
        try:
            return asyncio.get_running_loop()
        except RuntimeError:
            if self._loop is None or self._loop.is_closed():
                self._loop = asyncio.new_event_loop()
            return self._loop

    def connect(self) -> bool:
        """Connect to MCP server."""
        loop = self._get_loop()
        return loop.run_until_complete(self._async_client.connect())

    def list_tools(self) -> list[ToolInfo]:
        """List available tools."""
        loop = self._get_loop()
        return loop.run_until_complete(self._async_client.list_tools())

    def call_tool(
        self,
        tool_name: str,
        arguments: Optional[dict[str, Any]] = None,
    ) -> ToolCallResult:
        """Call a tool."""
        loop = self._get_loop()
        return loop.run_until_complete(
            self._async_client.call_tool_sync(tool_name, arguments)
        )

    def health_check(self) -> dict[str, Any]:
        """Perform health check."""
        loop = self._get_loop()
        return loop.run_until_complete(self._async_client.health_check())

    def close(self) -> None:
        """Close the client."""
        if self._loop and not self._loop.is_closed():
            self._loop.run_until_complete(self._async_client.close())
            self._loop.close()
