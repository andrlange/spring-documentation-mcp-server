#!/usr/bin/env python3
"""
Test script for Documentation MCP Tools (12 tools)

This script tests each Documentation MCP tool with 10 different calls for two use cases:
1. App Modernization - Spring Boot 2.x/3.x to 4.x migration
2. App Seeding - Creating new apps with specific Spring Boot version

Usage:
    python test_documentation_tools.py --base-url http://localhost:8080 --api-key YOUR_API_KEY

Results are logged to usecases/Documentation.md
"""

import argparse
import json
import requests
from datetime import datetime
from typing import Any

# MCP Server configuration
DEFAULT_BASE_URL = "http://localhost:8080"
MCP_ENDPOINT = "/mcp/spring"

# Documentation Tools (12 tools)
DOCUMENTATION_TOOLS = [
    "listSpringProjects",
    "getSpringVersions",
    "listSpringBootVersions",
    "filterSpringBootVersionsBySupport",
    "searchSpringDocs",
    "getCodeExamples",
    "findProjectsByUseCase",
    "getDocumentationByVersion",
    "getLatestSpringBootVersion",
    "listProjectsBySpringBootVersion",
    "getWikiReleaseNotes",
    "getWikiMigrationGuide",
]

# Test cases for App Modernization (migrating from 2.x/3.x to 4.x)
MODERNIZATION_TESTS = {
    "listSpringProjects": [
        {"description": "List all Spring projects to understand the ecosystem"},
    ],
    "getSpringVersions": [
        {"project": "spring-boot", "description": "Get Spring Boot versions to plan migration"},
        {"project": "spring-framework", "description": "Get Spring Framework versions for compatibility"},
    ],
    "listSpringBootVersions": [
        {"state": "GA", "limit": 10, "description": "List all GA versions to understand upgrade path"},
    ],
    "filterSpringBootVersionsBySupport": [
        {"supportActive": True, "limit": 10, "description": "Find supported versions for migration target"},
    ],
    "searchSpringDocs": [
        {"query": "migration Spring Boot 4", "limit": 5, "description": "Search for migration documentation"},
        {"query": "breaking changes Spring Boot 4", "limit": 5, "description": "Search for breaking changes"},
        {"query": "deprecated features Spring Boot 3", "limit": 5, "description": "Search for deprecated features"},
    ],
    "getCodeExamples": [
        {"query": "migration", "limit": 10, "description": "Find migration code examples"},
        {"query": "Spring Boot 4 configuration", "limit": 10, "description": "Find new configuration examples"},
    ],
    "findProjectsByUseCase": [
        {"useCase": "migration", "description": "Find projects related to migration"},
    ],
    "getDocumentationByVersion": [
        {"project": "spring-boot", "version": "4.0.1", "description": "Get docs for target version 4.0.1"},
    ],
    "getLatestSpringBootVersion": [
        {"description": "Get latest GA versions (default, no params)"},
        {"majorVersion": 4, "minorVersion": 0, "description": "Get latest 4.0.x patch version"},
        {"majorVersion": 3, "minorVersion": 5, "description": "Get latest 3.5.x patch version for source"},
    ],
    "listProjectsBySpringBootVersion": [
        {"majorVersion": 4, "minorVersion": 0, "allVersions": False, "description": "List compatible projects for 4.0"},
    ],
    "getWikiReleaseNotes": [
        {"version": "4.0", "description": "Get Spring Boot 4.0 release notes"},
        {"version": "3.5", "description": "Get Spring Boot 3.5 release notes for current state"},
    ],
    "getWikiMigrationGuide": [
        {"fromVersion": "3.5", "toVersion": "4.0", "description": "Get migration guide from 3.5 to 4.0"},
        {"fromVersion": "2.7", "toVersion": "3.0", "description": "Get migration guide from 2.7 to 3.0"},
    ],
}

# Test cases for App Seeding (creating new apps)
SEEDING_TESTS = {
    "listSpringProjects": [
        {"description": "List all Spring projects to select dependencies"},
    ],
    "getSpringVersions": [
        {"project": "spring-boot", "description": "Get available Spring Boot versions"},
        {"project": "spring-data", "description": "Get Spring Data versions for persistence"},
        {"project": "spring-security", "description": "Get Spring Security versions"},
    ],
    "listSpringBootVersions": [
        {"state": "GA", "limit": 5, "description": "List latest GA versions for new project"},
    ],
    "filterSpringBootVersionsBySupport": [
        {"supportActive": True, "limit": 5, "description": "Find actively supported versions"},
    ],
    "searchSpringDocs": [
        {"query": "getting started Spring Boot", "limit": 5, "description": "Search for getting started guides"},
        {"query": "REST API tutorial", "limit": 5, "description": "Search for REST API documentation"},
        {"query": "Spring Data JPA configuration", "limit": 5, "description": "Search for JPA setup"},
    ],
    "getCodeExamples": [
        {"query": "REST controller", "limit": 10, "description": "Find REST controller examples"},
        {"query": "JPA repository", "limit": 10, "description": "Find JPA repository examples"},
        {"query": "security configuration", "limit": 10, "description": "Find security config examples"},
    ],
    "findProjectsByUseCase": [
        {"useCase": "web", "description": "Find projects for web development"},
        {"useCase": "database", "description": "Find projects for database access"},
        {"useCase": "security", "description": "Find projects for security"},
        {"useCase": "messaging", "description": "Find projects for messaging"},
    ],
    "getDocumentationByVersion": [
        {"project": "spring-boot", "version": "4.0.1", "description": "Get docs for Spring Boot 4.0.1"},
        {"project": "spring-boot", "version": "3.5.9", "description": "Get docs for Spring Boot 3.5.9"},
    ],
    "getLatestSpringBootVersion": [
        {"description": "Get latest GA versions (all current versions)"},
        {"majorVersion": 4, "minorVersion": 0, "description": "Get latest 4.0.x for newest features"},
    ],
    "listProjectsBySpringBootVersion": [
        {"majorVersion": 4, "minorVersion": 0, "allVersions": False, "description": "List compatible projects for new 4.0 app"},
        {"majorVersion": 3, "minorVersion": 5, "allVersions": False, "description": "List compatible projects for 3.5 app"},
    ],
    "getWikiReleaseNotes": [
        {"version": "4.0", "description": "Get 4.0 features for new project"},
    ],
    "getWikiMigrationGuide": [
        {"fromVersion": "3.5", "toVersion": "4.0", "description": "Understand changes from 3.5 to 4.0"},
    ],
}


class McpSession:
    """MCP Streamable-HTTP session handler."""

    def __init__(self, base_url: str, api_key: str):
        self.base_url = base_url
        self.api_key = api_key
        self.session_id = None
        self.request_id = 0

    def _get_headers(self) -> dict:
        """Get headers for MCP requests."""
        headers = {
            "Content-Type": "application/json",
            "Accept": "application/json, text/event-stream",
            "X-API-Key": self.api_key,
        }
        if self.session_id:
            headers["Mcp-Session-Id"] = self.session_id
        return headers

    def _next_id(self) -> int:
        """Get next request ID."""
        self.request_id += 1
        return self.request_id

    def _parse_response(self, response: requests.Response) -> dict:
        """Parse response from Streamable-HTTP transport (which uses SSE format)."""
        content_type = response.headers.get("Content-Type", "")
        text = response.text.strip()

        if not text:
            return {"error": "Empty response from server"}

        # Streamable-HTTP uses SSE format: id:, event:, data: lines
        if "text/event-stream" in content_type or text.startswith("id:") or text.startswith("event:"):
            # Parse SSE format - look for data: lines
            for line in text.split("\n"):
                line = line.strip()
                if line.startswith("data:"):
                    json_data = line[5:].strip()
                    if json_data:
                        try:
                            return json.loads(json_data)
                        except json.JSONDecodeError:
                            continue
            return {"error": f"Could not find valid JSON in SSE response. Response: {text[:300]}"}

        # Try direct JSON parsing
        try:
            return json.loads(text)
        except json.JSONDecodeError as e:
            return {"error": f"JSON decode error: {e}. Content-Type: {content_type}. Response: {text[:300]}"}

    def initialize(self) -> bool:
        """Initialize the MCP session."""
        url = f"{self.base_url}{MCP_ENDPOINT}"

        request_body = {
            "jsonrpc": "2.0",
            "id": self._next_id(),
            "method": "initialize",
            "params": {
                "protocolVersion": "2024-11-05",
                "capabilities": {},
                "clientInfo": {
                    "name": "documentation-test-client",
                    "version": "1.0.0"
                }
            }
        }

        try:
            response = requests.post(url, json=request_body, headers=self._get_headers(), timeout=30)
            print(f"Initialize response status: {response.status_code}")
            print(f"Initialize response headers: {dict(response.headers)}")

            if response.status_code >= 400:
                print(f"Initialize error response: {response.text[:500]}")
                return False

            # Extract session ID from response header
            self.session_id = response.headers.get("Mcp-Session-Id")
            if self.session_id:
                print(f"Session initialized: {self.session_id[:20]}...")

                # Send initialized notification
                self._send_initialized()
                return True
            else:
                print("Warning: No session ID received, continuing without session")
                # Try to parse the response anyway
                result = self._parse_response(response)
                print(f"Initialize result: {json.dumps(result, indent=2)[:500]}")
                return True
        except requests.exceptions.RequestException as e:
            print(f"Failed to initialize session: {e}")
            return False

    def _send_initialized(self):
        """Send the initialized notification."""
        url = f"{self.base_url}{MCP_ENDPOINT}"

        notification_body = {
            "jsonrpc": "2.0",
            "method": "notifications/initialized"
        }

        try:
            requests.post(url, json=notification_body, headers=self._get_headers(), timeout=10)
        except requests.exceptions.RequestException:
            pass  # Notifications don't require a response

    def call_tool(self, tool_name: str, params: dict) -> dict:
        """Call an MCP tool and return the response."""
        url = f"{self.base_url}{MCP_ENDPOINT}"

        # Build MCP tool call request
        request_body = {
            "jsonrpc": "2.0",
            "id": self._next_id(),
            "method": "tools/call",
            "params": {
                "name": tool_name,
                "arguments": params
            }
        }

        try:
            response = requests.post(url, json=request_body, headers=self._get_headers(), timeout=60)

            if response.status_code >= 400:
                return {"error": f"HTTP {response.status_code}: {response.text[:300]}"}

            return self._parse_response(response)
        except requests.exceptions.RequestException as e:
            return {"error": str(e)}


def format_result(result: dict) -> str:
    """Format the result for markdown output."""
    if "error" in result:
        return f"**Error:** {result['error']}"

    if "result" in result:
        content = result["result"]
        if isinstance(content, dict) and "content" in content:
            # Extract text content from MCP response
            for item in content.get("content", []):
                if item.get("type") == "text":
                    try:
                        parsed = json.loads(item["text"])
                        return f"```json\n{json.dumps(parsed, indent=2)[:2000]}...\n```" if len(json.dumps(parsed, indent=2)) > 2000 else f"```json\n{json.dumps(parsed, indent=2)}\n```"
                    except json.JSONDecodeError:
                        return f"```\n{item['text'][:2000]}...\n```" if len(item["text"]) > 2000 else f"```\n{item['text']}\n```"
        return f"```json\n{json.dumps(content, indent=2)[:2000]}...\n```"

    return f"```json\n{json.dumps(result, indent=2)[:2000]}...\n```"


def run_tests(base_url: str, api_key: str, output_file: str):
    """Run all tests and write results to markdown file."""
    timestamp = datetime.now().strftime("%Y-%m-%d %H:%M:%S")

    # Initialize MCP session
    print(f"Connecting to MCP server at {base_url}...")
    session = McpSession(base_url, api_key)
    if not session.initialize():
        print("Failed to initialize MCP session. Exiting.")
        return

    with open(output_file, "w") as f:
        f.write(f"# Documentation MCP Tools Test Results\n\n")
        f.write(f"**Generated:** {timestamp}\n\n")
        f.write(f"**Base URL:** {base_url}\n\n")
        f.write(f"**Tools Tested:** {len(DOCUMENTATION_TOOLS)}\n\n")
        f.write("---\n\n")

        # App Modernization Tests
        f.write("## Use Case 1: App Modernization (Spring Boot 2.x/3.x to 4.x)\n\n")
        f.write("This use case covers migrating existing applications to Spring Boot 4.x.\n\n")

        for tool_name in DOCUMENTATION_TOOLS:
            tests = MODERNIZATION_TESTS.get(tool_name, [])
            if tests:
                f.write(f"### {tool_name}\n\n")
                for i, test in enumerate(tests, 1):
                    params = {k: v for k, v in test.items() if k != "description"}
                    description = test.get("description", "No description")

                    f.write(f"#### Test {i}: {description}\n\n")
                    f.write(f"**Parameters:** `{json.dumps(params) if params else '{}'}`\n\n")

                    print(f"Testing {tool_name} - {description}...")
                    result = session.call_tool(tool_name, params)
                    f.write(f"**Result:**\n\n{format_result(result)}\n\n")

                f.write("---\n\n")

        # App Seeding Tests
        f.write("## Use Case 2: App Seeding (Creating New Spring Boot Applications)\n\n")
        f.write("This use case covers creating new applications with a specific Spring Boot version.\n\n")

        for tool_name in DOCUMENTATION_TOOLS:
            tests = SEEDING_TESTS.get(tool_name, [])
            if tests:
                f.write(f"### {tool_name}\n\n")
                for i, test in enumerate(tests, 1):
                    params = {k: v for k, v in test.items() if k != "description"}
                    description = test.get("description", "No description")

                    f.write(f"#### Test {i}: {description}\n\n")
                    f.write(f"**Parameters:** `{json.dumps(params) if params else '{}'}`\n\n")

                    print(f"Testing {tool_name} - {description}...")
                    result = session.call_tool(tool_name, params)
                    f.write(f"**Result:**\n\n{format_result(result)}\n\n")

                f.write("---\n\n")

        # Summary
        f.write("## Summary\n\n")
        f.write(f"| Tool | Modernization Tests | Seeding Tests | Total |\n")
        f.write(f"|------|---------------------|---------------|-------|\n")
        total_modernization = 0
        total_seeding = 0
        for tool_name in DOCUMENTATION_TOOLS:
            mod_count = len(MODERNIZATION_TESTS.get(tool_name, []))
            seed_count = len(SEEDING_TESTS.get(tool_name, []))
            total_modernization += mod_count
            total_seeding += seed_count
            f.write(f"| {tool_name} | {mod_count} | {seed_count} | {mod_count + seed_count} |\n")
        f.write(f"| **Total** | **{total_modernization}** | **{total_seeding}** | **{total_modernization + total_seeding}** |\n")

    print(f"\nResults written to {output_file}")


def main():
    parser = argparse.ArgumentParser(description="Test Documentation MCP Tools")
    parser.add_argument("--base-url", default=DEFAULT_BASE_URL, help="MCP server base URL")
    parser.add_argument("--api-key", required=True, help="API key for authentication")
    parser.add_argument("--output", default="usecases/Documentation.md", help="Output file path")

    args = parser.parse_args()

    run_tests(args.base_url, args.api_key, args.output)


if __name__ == "__main__":
    main()
