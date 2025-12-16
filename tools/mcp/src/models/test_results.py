"""Models for test results and reports."""

from datetime import datetime
from enum import Enum
from typing import Any, Optional

from pydantic import BaseModel, Field


class TestStatus(str, Enum):
    """Test execution status."""

    PASSED = "passed"
    FAILED = "failed"
    SKIPPED = "skipped"
    ERROR = "error"


class TestResult(BaseModel):
    """Result of a single test."""

    name: str
    status: TestStatus
    duration_ms: float
    message: Optional[str] = None
    details: Optional[dict[str, Any]] = None
    timestamp: datetime = Field(default_factory=datetime.now)

    @property
    def duration_seconds(self) -> float:
        return self.duration_ms / 1000


class StabilityTestResult(TestResult):
    """Result of a stability test."""

    connection_established: bool = False
    heartbeats_received: int = 0
    reconnection_attempts: int = 0
    reconnection_success: bool = False
    message_loss_count: int = 0
    detection_time_ms: Optional[float] = None


class BenchmarkResult(BaseModel):
    """Result of a performance benchmark."""

    tool_name: str
    iterations: int
    min_ms: float
    max_ms: float
    mean_ms: float
    median_ms: float
    stddev_ms: float
    p95_ms: float
    p99_ms: float
    throughput_rps: float  # requests per second
    error_count: int = 0
    timestamp: datetime = Field(default_factory=datetime.now)

    @classmethod
    def from_timings(
        cls,
        tool_name: str,
        timings_ms: list[float],
        error_count: int = 0,
    ) -> "BenchmarkResult":
        """Create benchmark result from list of timing measurements."""
        import statistics

        if not timings_ms:
            raise ValueError("No timings provided")

        sorted_timings = sorted(timings_ms)
        n = len(sorted_timings)

        return cls(
            tool_name=tool_name,
            iterations=n,
            min_ms=min(timings_ms),
            max_ms=max(timings_ms),
            mean_ms=statistics.mean(timings_ms),
            median_ms=statistics.median(timings_ms),
            stddev_ms=statistics.stdev(timings_ms) if n > 1 else 0,
            p95_ms=sorted_timings[int(n * 0.95)] if n > 1 else sorted_timings[0],
            p99_ms=sorted_timings[int(n * 0.99)] if n > 1 else sorted_timings[0],
            throughput_rps=1000 / statistics.mean(timings_ms) if timings_ms else 0,
            error_count=error_count,
        )


class QueryOptimizationResult(BaseModel):
    """Result of query optimization analysis."""

    tool_name: str
    query_params: dict[str, Any]
    response_size_bytes: int
    response_time_ms: float
    result_count: int
    is_empty: bool
    completeness_score: float  # 0.0 to 1.0
    has_all_expected_fields: bool
    missing_fields: list[str] = Field(default_factory=list)
    timestamp: datetime = Field(default_factory=datetime.now)


class TestCategoryReport(BaseModel):
    """Report for a category of tests."""

    category: str
    total: int
    passed: int
    failed: int
    skipped: int
    errors: int
    duration_ms: float
    results: list[TestResult] = Field(default_factory=list)

    @property
    def success_rate(self) -> float:
        """Calculate success rate as percentage."""
        if self.total == 0:
            return 0.0
        return (self.passed / self.total) * 100


class TestSuiteReport(BaseModel):
    """Complete test suite report."""

    suite_name: str = "MCP Test Suite"
    started_at: datetime = Field(default_factory=datetime.now)
    completed_at: Optional[datetime] = None
    duration_ms: float = 0

    # Category reports
    stability: Optional[TestCategoryReport] = None
    performance: Optional[TestCategoryReport] = None
    optimization: Optional[TestCategoryReport] = None

    # Benchmark results
    benchmarks: list[BenchmarkResult] = Field(default_factory=list)

    # Summary
    total_tests: int = 0
    total_passed: int = 0
    total_failed: int = 0
    total_skipped: int = 0
    total_errors: int = 0

    # Environment info
    mcp_server_url: Optional[str] = None
    python_version: Optional[str] = None
    platform: Optional[str] = None

    def complete(self) -> None:
        """Mark report as complete and calculate totals."""
        self.completed_at = datetime.now()
        self.duration_ms = (self.completed_at - self.started_at).total_seconds() * 1000

        # Calculate totals from categories
        for category in [self.stability, self.performance, self.optimization]:
            if category:
                self.total_tests += category.total
                self.total_passed += category.passed
                self.total_failed += category.failed
                self.total_skipped += category.skipped
                self.total_errors += category.errors

    @property
    def success_rate(self) -> float:
        """Calculate overall success rate."""
        if self.total_tests == 0:
            return 0.0
        return (self.total_passed / self.total_tests) * 100

    def to_summary_dict(self) -> dict[str, Any]:
        """Get summary as dictionary for display."""
        return {
            "suite": self.suite_name,
            "duration": f"{self.duration_ms / 1000:.2f}s",
            "total": self.total_tests,
            "passed": self.total_passed,
            "failed": self.total_failed,
            "skipped": self.total_skipped,
            "errors": self.total_errors,
            "success_rate": f"{self.success_rate:.1f}%",
        }
