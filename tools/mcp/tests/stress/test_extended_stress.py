"""
Extended Stress Test for MCP Connection Stability.

This test runs many iterations to check for:
- Connection degradation over time
- Memory leaks
- SSE connection stability
"""

import asyncio
import json
import logging
import sys
import time
from datetime import datetime
from pathlib import Path

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

# Quick tools for stress testing
QUICK_TOOLS = [
    ("getBreakingChanges", {"fromVersion": "3.4.0", "toVersion": "3.5.0"}),
    ("listJavadocLibraries", {}),
    ("searchFlavors", {"query": "postgres", "limit": 3}),
    ("getLatestSpringBootVersion", {}),
    ("listFlavorCategories", {}),
]


async def run_extended_test(iterations: int = 50):
    """Run extended test with many iterations."""
    config = MCPServerConfig(
        base_url="http://localhost:8080",
        api_key="smcp_-MgLbRkCWPUb9V1V_IjKq-7imdCLgFFlrCg9LczNDnA",
        auth_method="header",
    )

    results = {
        "start_time": datetime.now().isoformat(),
        "iterations": iterations,
        "calls": [],
        "failed_calls": [],
        "timing_buckets": {
            "<500ms": 0,
            "500-1000ms": 0,
            "1000-2000ms": 0,
            "2000-5000ms": 0,
            ">5000ms": 0,
        }
    }

    logger.info(f"Running {iterations} iterations with {len(QUICK_TOOLS)} tools each")
    logger.info(f"Total expected calls: {iterations * len(QUICK_TOOLS)}")

    async with MCPClient(config) as client:
        total_calls = 0
        failed = 0

        for i in range(iterations):
            for tool_name, args in QUICK_TOOLS:
                start = time.time()
                result = await client.call_tool(tool_name, args, timeout=10.0)
                duration_ms = (time.time() - start) * 1000
                total_calls += 1

                call_data = {
                    "iteration": i + 1,
                    "tool": tool_name,
                    "success": result.success,
                    "duration_ms": round(duration_ms, 2),
                    "error": result.error if not result.success else None,
                }
                results["calls"].append(call_data)

                # Update buckets
                if duration_ms < 500:
                    results["timing_buckets"]["<500ms"] += 1
                elif duration_ms < 1000:
                    results["timing_buckets"]["500-1000ms"] += 1
                elif duration_ms < 2000:
                    results["timing_buckets"]["1000-2000ms"] += 1
                elif duration_ms < 5000:
                    results["timing_buckets"]["2000-5000ms"] += 1
                else:
                    results["timing_buckets"][">5000ms"] += 1

                if not result.success:
                    failed += 1
                    results["failed_calls"].append(call_data)
                    logger.warning(f"FAILED: {tool_name} at iteration {i+1}: {result.error}")

            if (i + 1) % 10 == 0:
                logger.info(f"Progress: {i+1}/{iterations} iterations ({total_calls} calls, {failed} failed)")

            # Small delay between iterations
            await asyncio.sleep(0.05)

    results["end_time"] = datetime.now().isoformat()
    results["total_calls"] = total_calls
    results["total_failed"] = failed
    results["success_rate"] = (total_calls - failed) / total_calls * 100 if total_calls > 0 else 0

    # Calculate timing stats
    durations = [c["duration_ms"] for c in results["calls"]]
    results["timing_stats"] = {
        "min_ms": min(durations),
        "max_ms": max(durations),
        "avg_ms": sum(durations) / len(durations),
        "p50_ms": sorted(durations)[len(durations) // 2],
        "p95_ms": sorted(durations)[int(len(durations) * 0.95)],
        "p99_ms": sorted(durations)[int(len(durations) * 0.99)],
    }

    return results


async def main():
    results = await run_extended_test(iterations=50)

    logger.info("\n" + "=" * 60)
    logger.info("EXTENDED STRESS TEST RESULTS")
    logger.info("=" * 60)
    logger.info(f"Total calls: {results['total_calls']}")
    logger.info(f"Failed calls: {results['total_failed']}")
    logger.info(f"Success rate: {results['success_rate']:.1f}%")
    logger.info(f"\nTiming Distribution:")
    for bucket, count in results["timing_buckets"].items():
        pct = count / results['total_calls'] * 100
        logger.info(f"  {bucket}: {count} ({pct:.1f}%)")
    logger.info(f"\nTiming Statistics:")
    stats = results["timing_stats"]
    logger.info(f"  Min: {stats['min_ms']:.0f}ms")
    logger.info(f"  Max: {stats['max_ms']:.0f}ms")
    logger.info(f"  Avg: {stats['avg_ms']:.0f}ms")
    logger.info(f"  P50: {stats['p50_ms']:.0f}ms")
    logger.info(f"  P95: {stats['p95_ms']:.0f}ms")
    logger.info(f"  P99: {stats['p99_ms']:.0f}ms")
    logger.info("=" * 60)

    # Save results
    output_dir = Path(__file__).parent.parent.parent / "reports"
    output_dir.mkdir(exist_ok=True)
    timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
    json_path = output_dir / f"extended_stress_{timestamp}.json"
    with open(json_path, "w") as f:
        json.dump(results, f, indent=2)
    logger.info(f"\nResults saved to: {json_path}")

    return results


if __name__ == "__main__":
    asyncio.run(main())
