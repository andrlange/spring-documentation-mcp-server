"""Pydantic models for MCP protocol messages."""

from datetime import datetime
from typing import Any, Optional

from pydantic import BaseModel, Field


class MCPError(BaseModel):
    """MCP error response."""

    code: int
    message: str
    data: Optional[Any] = None


class MCPToolCall(BaseModel):
    """Tool call parameters."""

    name: str
    arguments: dict[str, Any] = Field(default_factory=dict)


class MCPRequest(BaseModel):
    """MCP JSON-RPC 2.0 request."""

    jsonrpc: str = "2.0"
    method: str
    params: Optional[dict[str, Any]] = None
    id: Optional[int | str] = None

    @classmethod
    def tool_call(cls, tool_name: str, arguments: dict[str, Any], request_id: int | str) -> "MCPRequest":
        """Create a tool call request."""
        return cls(
            method="tools/call",
            params={"name": tool_name, "arguments": arguments},
            id=request_id,
        )

    @classmethod
    def list_tools(cls, request_id: int | str) -> "MCPRequest":
        """Create a list tools request."""
        return cls(method="tools/list", id=request_id)

    @classmethod
    def initialize(cls, request_id: int | str) -> "MCPRequest":
        """Create an initialize request."""
        return cls(
            method="initialize",
            params={
                "protocolVersion": "2024-11-05",
                "capabilities": {},
                "clientInfo": {"name": "mcp-test-suite", "version": "0.1.0"},
            },
            id=request_id,
        )

    @classmethod
    def initialized_notification(cls) -> "MCPRequest":
        """Create an initialized notification (no id = notification)."""
        return cls(
            method="notifications/initialized",
            # No id means it's a notification, not a request
        )


class MCPToolResult(BaseModel):
    """Result from a tool invocation."""

    content: list[dict[str, Any]] = Field(default_factory=list)
    isError: bool = False

    def get_text_content(self) -> str:
        """Extract text content from result."""
        texts = []
        for item in self.content:
            if item.get("type") == "text":
                texts.append(item.get("text", ""))
        return "\n".join(texts)

    def get_json_content(self) -> Optional[dict[str, Any]]:
        """Extract and parse JSON from text content."""
        import json

        text = self.get_text_content()
        if text:
            try:
                return json.loads(text)
            except json.JSONDecodeError:
                return None
        return None


class MCPResponse(BaseModel):
    """MCP JSON-RPC 2.0 response."""

    jsonrpc: str = "2.0"
    result: Optional[Any] = None
    error: Optional[MCPError] = None
    id: Optional[int | str] = None

    @property
    def is_success(self) -> bool:
        """Check if response indicates success."""
        return self.error is None

    @property
    def is_error(self) -> bool:
        """Check if response indicates error."""
        return self.error is not None

    def get_tool_result(self) -> Optional[MCPToolResult]:
        """Get result as MCPToolResult if applicable."""
        if self.result and isinstance(self.result, dict):
            return MCPToolResult(**self.result)
        return None


class SSEEvent(BaseModel):
    """Server-Sent Event."""

    event: Optional[str] = None
    data: str = ""
    id: Optional[str] = None
    retry: Optional[int] = None

    @property
    def is_message(self) -> bool:
        """Check if this is a message event."""
        return self.event == "message" or self.event is None

    @property
    def is_endpoint(self) -> bool:
        """Check if this is an endpoint event (initial connection)."""
        return self.event == "endpoint"

    def parse_json(self) -> Optional[dict[str, Any]]:
        """Parse data as JSON."""
        import json

        try:
            return json.loads(self.data)
        except json.JSONDecodeError:
            return None


class ToolInfo(BaseModel):
    """Information about an MCP tool."""

    name: str
    description: Optional[str] = None
    inputSchema: Optional[dict[str, Any]] = None


class ToolsListResult(BaseModel):
    """Result of tools/list request."""

    tools: list[ToolInfo] = Field(default_factory=list)


class ConnectionInfo(BaseModel):
    """Information about an MCP connection."""

    connected_at: datetime = Field(default_factory=datetime.now)
    session_id: Optional[str] = None
    message_endpoint: Optional[str] = None
    server_info: Optional[dict[str, Any]] = None
    protocol_version: Optional[str] = None

    @property
    def connection_duration_seconds(self) -> float:
        """Get connection duration in seconds."""
        return (datetime.now() - self.connected_at).total_seconds()
