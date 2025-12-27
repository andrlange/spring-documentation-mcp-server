"""
Comprehensive MCP Tools Stress Test.

This test calls all MCP tools multiple times to identify:
- Response time issues (>5-8 seconds)
- SSE connection failures
- Timeout issues
- Memory/resource leaks

Run with: PYTHONPATH=src pytest tests/stress/test_all_tools_stress.py -v -s
"""

import asyncio
import json
import logging
import sys
import time
from dataclasses import dataclass, field
from datetime import datetime
from pathlib import Path
from typing import Any, Optional

# Add src to path for imports
src_path = str(Path(__file__).parent.parent.parent / "src")
if src_path not in sys.path:
    sys.path.insert(0, src_path)

from client.mcp_client import MCPClient
from config import MCPServerConfig

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s - %(levelname)s - %(message)s"
)
logger = logging.getLogger(__name__)


@dataclass
class ToolCallMetrics:
    """Metrics for a single tool call."""
    tool_name: str
    iteration: int
    success: bool
    duration_ms: float
    error: Optional[str] = None
    response_size_bytes: int = 0
    timestamp: datetime = field(default_factory=datetime.now)


@dataclass
class ToolSummary:
    """Summary statistics for a tool."""
    tool_name: str
    total_calls: int = 0
    successful_calls: int = 0
    failed_calls: int = 0
    avg_duration_ms: float = 0
    min_duration_ms: float = float('inf')
    max_duration_ms: float = 0
    slow_calls: int = 0  # Calls > 5000ms
    timeout_calls: int = 0  # Calls > 8000ms
    errors: list = field(default_factory=list)


# Default test arguments for tools (minimal to test basic functionality)
TOOL_TEST_ARGS: dict[str, dict[str, Any]] = {
    # Core Version Tools
    "getSpringVersions": {},
    "listSpringBootVersions": {"state": "GA", "limit": 3},
    "filterSpringBootVersionsBySupport": {"supportState": "COMMERCIAL"},
    "getLatestSpringBootVersion": {},

    # Project Tools
    "listSpringProjects": {},

    # Documentation Tools
    "searchSpringDocs": {"query": "spring boot", "limit": 3},
    "getDocumentationByVersion": {"project": "spring-boot", "version": "3.5.7"},

    # Code Examples Tools
    "getCodeExamples": {"project": "spring-boot", "limit": 3},

    # Use Case Tools
    "findProjectsByUseCase": {"useCase": "REST API"},

    # Language Version Tools
    "getLanguageVersions": {},
    "getLanguageFeatures": {"language": "java", "minVersion": "21"},
    "getLanguageFeatureExample": {"featureId": "java21-virtual-threads"},
    "getLanguageVersionDiff": {"language": "java", "fromVersion": "17", "toVersion": "21"},
    "getSpringBootLanguageRequirements": {"bootVersion": "3.5.7"},

    # Flavor Tools
    "listFlavorCategories": {},
    "listFlavorGroups": {"category": "database"},
    "getFlavorsGroup": {"groupSlug": "postgresql"},
    "getFlavorByName": {"name": "postgresql"},
    "getFlavorsByCategory": {"category": "database"},
    "searchFlavors": {"query": "postgres", "limit": 3},

    # Initializr Tools
    "initializrGetDependencyCategories": {"bootVersion": "3.5.7"},
    "initializrSearchDependencies": {"query": "web", "bootVersion": "3.5.7", "limit": 5},
    "initializrGetDependency": {"dependencyId": "web", "bootVersion": "3.5.7"},
    "initializrCheckCompatibility": {"dependencies": ["web", "data-jpa"], "bootVersion": "3.5.7"},
    "checkVersionCompatibility": {"bootVersion": "3.5.7", "javaVersion": "21"},

    # Javadoc Tools
    "listJavadocLibraries": {},
    "searchJavadocs": {"query": "RestTemplate", "limit": 3},
    "getClassDoc": {"library": "spring-boot", "className": "SpringApplication"},
    "getPackageDoc": {"library": "spring-boot", "packageName": "org.springframework.boot"},

    # Migration Tools
    "getBreakingChanges": {"fromVersion": "3.4.0", "toVersion": "3.5.0"},
    "searchMigrationKnowledge": {"query": "security migration", "limit": 3},
}

# These tools may need special handling or have known issues
SLOW_TOOLS = [
    "searchSpringDocs",
    "searchJavadocs",
    "getCodeExamples",
    "searchMigrationKnowledge",
]

# Skip these tools in stress test (broken or special purpose)
SKIP_TOOLS = []


class MCPStressTest:
    """Comprehensive stress test for MCP tools."""

    def __init__(
        self,
        config: MCPServerConfig,
        iterations: int = 3,
        timeout_warning_ms: float = 5000,
        timeout_error_ms: float = 8000,
    ):
        self.config = config
        self.iterations = iterations
        self.timeout_warning_ms = timeout_warning_ms
        self.timeout_error_ms = timeout_error_ms
        self.metrics: list[ToolCallMetrics] = []
        self.tool_summaries: dict[str, ToolSummary] = {}
        self.all_tools: list[str] = []

    async def discover_tools(self, client: MCPClient) -> list[str]:
        """Discover all available tools from the server."""
        tools = await client.list_tools()
        return [t.name for t in tools]

    async def call_tool_with_metrics(
        self,
        client: MCPClient,
        tool_name: str,
        args: dict[str, Any],
        iteration: int,
    ) -> ToolCallMetrics:
        """Call a tool and collect metrics."""
        start_time = time.time()

        try:
            result = await client.call_tool(tool_name, args, timeout=15.0)
            duration_ms = (time.time() - start_time) * 1000

            return ToolCallMetrics(
                tool_name=tool_name,
                iteration=iteration,
                success=result.success,
                duration_ms=duration_ms,
                error=result.error if not result.success else None,
                response_size_bytes=result.response_size_bytes,
            )

        except asyncio.TimeoutError:
            duration_ms = (time.time() - start_time) * 1000
            return ToolCallMetrics(
                tool_name=tool_name,
                iteration=iteration,
                success=False,
                duration_ms=duration_ms,
                error="TIMEOUT",
            )
        except Exception as e:
            duration_ms = (time.time() - start_time) * 1000
            return ToolCallMetrics(
                tool_name=tool_name,
                iteration=iteration,
                success=False,
                duration_ms=duration_ms,
                error=str(e),
            )

    def update_summary(self, metric: ToolCallMetrics) -> None:
        """Update tool summary with new metric."""
        if metric.tool_name not in self.tool_summaries:
            self.tool_summaries[metric.tool_name] = ToolSummary(tool_name=metric.tool_name)

        summary = self.tool_summaries[metric.tool_name]
        summary.total_calls += 1

        if metric.success:
            summary.successful_calls += 1
        else:
            summary.failed_calls += 1
            if metric.error:
                summary.errors.append(f"Iter {metric.iteration}: {metric.error}")

        # Update timing stats
        if metric.duration_ms < summary.min_duration_ms:
            summary.min_duration_ms = metric.duration_ms
        if metric.duration_ms > summary.max_duration_ms:
            summary.max_duration_ms = metric.duration_ms

        # Count slow/timeout calls
        if metric.duration_ms > self.timeout_warning_ms:
            summary.slow_calls += 1
        if metric.duration_ms > self.timeout_error_ms:
            summary.timeout_calls += 1

        # Recalculate average
        all_durations = [m.duration_ms for m in self.metrics if m.tool_name == metric.tool_name]
        summary.avg_duration_ms = sum(all_durations) / len(all_durations)

    async def run_stress_test(self) -> dict[str, Any]:
        """Run the complete stress test."""
        logger.info("=" * 60)
        logger.info("MCP Tools Stress Test")
        logger.info("=" * 60)
        logger.info(f"Server: {self.config.base_url}")
        logger.info(f"Iterations per tool: {self.iterations}")
        logger.info(f"Timeout warning: {self.timeout_warning_ms}ms")
        logger.info(f"Timeout error: {self.timeout_error_ms}ms")
        logger.info("=" * 60)

        start_time = time.time()
        results = {
            "start_time": datetime.now().isoformat(),
            "config": {
                "iterations": self.iterations,
                "timeout_warning_ms": self.timeout_warning_ms,
                "timeout_error_ms": self.timeout_error_ms,
            },
            "tools": {},
            "summary": {},
        }

        async with MCPClient(self.config) as client:
            # Discover all tools
            logger.info("\n📋 Discovering available tools...")
            self.all_tools = await self.discover_tools(client)
            logger.info(f"Found {len(self.all_tools)} tools")

            # Filter tools
            tools_to_test = [
                t for t in self.all_tools
                if t not in SKIP_TOOLS
            ]
            logger.info(f"Testing {len(tools_to_test)} tools")

            # Run tests for each tool
            for tool_idx, tool_name in enumerate(tools_to_test, 1):
                args = TOOL_TEST_ARGS.get(tool_name, {})

                logger.info(f"\n[{tool_idx}/{len(tools_to_test)}] Testing: {tool_name}")

                for iteration in range(1, self.iterations + 1):
                    metric = await self.call_tool_with_metrics(
                        client, tool_name, args, iteration
                    )
                    self.metrics.append(metric)
                    self.update_summary(metric)

                    # Log result
                    status = "✓" if metric.success else "✗"
                    duration_indicator = ""
                    if metric.duration_ms > self.timeout_error_ms:
                        duration_indicator = " ⚠️ TIMEOUT"
                    elif metric.duration_ms > self.timeout_warning_ms:
                        duration_indicator = " ⚡ SLOW"

                    logger.info(f"  Iteration {iteration}/{self.iterations}: {status} {metric.duration_ms:.0f}ms{duration_indicator}")

                    if not metric.success:
                        logger.warning(f"    Error: {metric.error}")

                    # Small delay between calls
                    await asyncio.sleep(0.1)

        # Calculate final statistics
        total_time = time.time() - start_time

        results["end_time"] = datetime.now().isoformat()
        results["total_duration_seconds"] = total_time
        results["tools_tested"] = len(tools_to_test)
        results["total_calls"] = len(self.metrics)

        # Tool-level results
        for tool_name, summary in self.tool_summaries.items():
            results["tools"][tool_name] = {
                "total_calls": summary.total_calls,
                "successful_calls": summary.successful_calls,
                "failed_calls": summary.failed_calls,
                "success_rate": summary.successful_calls / summary.total_calls * 100 if summary.total_calls > 0 else 0,
                "avg_duration_ms": round(summary.avg_duration_ms, 2),
                "min_duration_ms": round(summary.min_duration_ms, 2) if summary.min_duration_ms != float('inf') else None,
                "max_duration_ms": round(summary.max_duration_ms, 2),
                "slow_calls": summary.slow_calls,
                "timeout_calls": summary.timeout_calls,
                "errors": summary.errors[:5],  # Limit errors to 5
            }

        # Overall summary
        total_successful = sum(s.successful_calls for s in self.tool_summaries.values())
        total_failed = sum(s.failed_calls for s in self.tool_summaries.values())
        total_slow = sum(s.slow_calls for s in self.tool_summaries.values())
        total_timeout = sum(s.timeout_calls for s in self.tool_summaries.values())

        results["summary"] = {
            "total_successful": total_successful,
            "total_failed": total_failed,
            "total_slow": total_slow,
            "total_timeout": total_timeout,
            "overall_success_rate": total_successful / len(self.metrics) * 100 if self.metrics else 0,
            "problematic_tools": [
                name for name, s in self.tool_summaries.items()
                if s.failed_calls > 0 or s.slow_calls > 0
            ],
        }

        return results

    def print_report(self, results: dict[str, Any]) -> str:
        """Generate a human-readable report."""
        report = []
        report.append("\n" + "=" * 60)
        report.append("STRESS TEST REPORT")
        report.append("=" * 60)

        # Summary
        summary = results["summary"]
        report.append(f"\n📊 SUMMARY")
        report.append(f"   Total calls: {results['total_calls']}")
        report.append(f"   Duration: {results['total_duration_seconds']:.1f}s")
        report.append(f"   Success rate: {summary['overall_success_rate']:.1f}%")
        report.append(f"   Failed calls: {summary['total_failed']}")
        report.append(f"   Slow calls (>{self.timeout_warning_ms}ms): {summary['total_slow']}")
        report.append(f"   Timeout calls (>{self.timeout_error_ms}ms): {summary['total_timeout']}")

        # Problematic tools
        if summary['problematic_tools']:
            report.append(f"\n⚠️  PROBLEMATIC TOOLS:")
            for tool_name in summary['problematic_tools']:
                tool_data = results['tools'][tool_name]
                report.append(f"   - {tool_name}:")
                report.append(f"     Success: {tool_data['success_rate']:.0f}% | Avg: {tool_data['avg_duration_ms']:.0f}ms | Max: {tool_data['max_duration_ms']:.0f}ms")
                if tool_data['errors']:
                    report.append(f"     Errors: {tool_data['errors'][0]}")

        # All tools sorted by max duration
        report.append(f"\n📈 ALL TOOLS (sorted by max duration):")
        sorted_tools = sorted(
            results['tools'].items(),
            key=lambda x: x[1]['max_duration_ms'],
            reverse=True
        )

        for tool_name, tool_data in sorted_tools:
            status = "✓" if tool_data['failed_calls'] == 0 else "✗"
            slow_indicator = " 🐌" if tool_data['slow_calls'] > 0 else ""
            report.append(
                f"   {status} {tool_name}: "
                f"{tool_data['avg_duration_ms']:.0f}ms avg, "
                f"{tool_data['max_duration_ms']:.0f}ms max"
                f"{slow_indicator}"
            )

        report.append("=" * 60)
        report_str = "\n".join(report)
        logger.info(report_str)
        return report_str


async def main():
    """Run the stress test."""
    # Configuration
    config = MCPServerConfig(
        base_url="http://localhost:8080",
        api_key="smcp_-MgLbRkCWPUb9V1V_IjKq-7imdCLgFFlrCg9LczNDnA",
        auth_method="header",
    )

    # Create and run stress test
    stress_test = MCPStressTest(
        config=config,
        iterations=3,
        timeout_warning_ms=5000,
        timeout_error_ms=8000,
    )

    results = await stress_test.run_stress_test()
    report = stress_test.print_report(results)

    # Save results
    output_dir = Path(__file__).parent.parent.parent / "reports"
    output_dir.mkdir(exist_ok=True)

    timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")

    # Save JSON results
    json_path = output_dir / f"stress_test_{timestamp}.json"
    with open(json_path, "w") as f:
        json.dump(results, f, indent=2, default=str)
    logger.info(f"\n📁 Results saved to: {json_path}")

    # Save report
    report_path = output_dir / f"stress_test_{timestamp}.txt"
    with open(report_path, "w") as f:
        f.write(report)
    logger.info(f"📁 Report saved to: {report_path}")

    return results


if __name__ == "__main__":
    asyncio.run(main())
