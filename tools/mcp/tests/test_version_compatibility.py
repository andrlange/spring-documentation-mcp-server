"""Test for checkVersionCompatibility tool using spring_boot_compatibility data."""

import asyncio
import json
import os
import sys
from pathlib import Path

# Add src to path
src_path = str(Path(__file__).parent.parent / "src")
if src_path not in sys.path:
    sys.path.insert(0, src_path)

from client.mcp_client import MCPClient
from config import MCPServerConfig


def print_result(result, test_name: str):
    """Print tool call result."""
    if result.success:
        content = result.get_json_content()
        print(f"Success: {result.success}")
        print(f"Duration: {result.duration_ms:.2f} ms")
        print(f"Raw JSON: {json.dumps(content, indent=2)}")
        print(f"All compatible: {content.get('allCompatible')}")
        print(f"Warnings: {content.get('warnings')}")
        print(f"Dependencies:")
        for dep in content.get('dependencies', []):
            # Fields are: dependency, compatibleVersion, verified, notes
            print(f"  - {dep.get('dependency')}: {dep.get('compatibleVersion')} (verified: {dep.get('verified')})")
            if dep.get('notes'):
                print(f"    Notes: {dep.get('notes')}")
    else:
        print(f"ERROR: {result.error}")


async def test_check_version_compatibility():
    """Test checkVersionCompatibility with Spring AI."""

    # Configure client
    config = MCPServerConfig(
        base_url=os.environ.get("MCP_BASE_URL", "http://localhost:8080"),
        api_key=os.environ.get("MCP_API_KEY", "smcp_dQdhF-x2EWXs5EPzbsmfx8nkzPMSdcaq0ELY6tlq33w"),
        auth_method="header"
    )

    client = MCPClient(config, timeout=60.0)

    try:
        print("Connecting to MCP server...")
        connected = await client.connect()
        if not connected:
            print("ERROR: Could not connect to MCP server")
            return

        print("Connected! Testing checkVersionCompatibility...")

        # Test 1: Spring Boot 3.5.8 with Spring AI and Spring Security
        print("\n" + "=" * 70)
        print("Test 1: Spring Boot 3.5.8 with spring-ai and spring-security")
        print("=" * 70)
        result = await client.call_tool(
            "checkVersionCompatibility",
            {
                "springBootVersion": "3.5.8",
                "dependencies": ["spring-ai", "spring-security"]
            }
        )
        print_result(result, "Test 1")

        # Test 2: Spring Boot 4.0.0 with various dependencies
        print("\n" + "=" * 70)
        print("Test 2: Spring Boot 4.0.0 with spring-security, spring-data")
        print("=" * 70)
        result = await client.call_tool(
            "checkVersionCompatibility",
            {
                "springBootVersion": "4.0.0",
                "dependencies": ["spring-security", "spring-data"]
            }
        )
        print_result(result, "Test 2")

        # Test 3: Non-existent dependency
        print("\n" + "=" * 70)
        print("Test 3: Spring Boot 3.5.8 with non-existent-lib")
        print("=" * 70)
        result = await client.call_tool(
            "checkVersionCompatibility",
            {
                "springBootVersion": "3.5.8",
                "dependencies": ["non-existent-lib"]
            }
        )
        print_result(result, "Test 3")

        # Test 4: Spring AI specifically
        print("\n" + "=" * 70)
        print("Test 4: Spring Boot 3.5.8 with just spring-ai")
        print("=" * 70)
        result = await client.call_tool(
            "checkVersionCompatibility",
            {
                "springBootVersion": "3.5.8",
                "dependencies": ["spring-ai"]
            }
        )
        print_result(result, "Test 4")

    finally:
        await client.close()
        print("\n" + "=" * 70)
        print("Test completed")
        print("=" * 70)


if __name__ == "__main__":
    asyncio.run(test_check_version_compatibility())
