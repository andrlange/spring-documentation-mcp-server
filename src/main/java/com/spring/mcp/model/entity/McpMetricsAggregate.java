package com.spring.mcp.model.entity;

import com.spring.mcp.model.enums.BucketType;
import com.spring.mcp.model.enums.MetricType;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * Entity representing aggregated MCP metrics for monitoring dashboard.
 * Stores metrics in time buckets (5-minute, 1-hour, 24-hour).
 *
 * @author Spring MCP Server
 * @version 1.4.4
 * @since 2025-12-16
 */
@Entity
@Table(name = "mcp_metrics_aggregate",
        uniqueConstraints = @UniqueConstraint(
                columnNames = {"bucket_type", "bucket_start", "tool_name", "metric_type"}
        ))
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@EqualsAndHashCode(of = {"id"})
public class McpMetricsAggregate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "bucket_type", nullable = false, length = 20)
    private BucketType bucketType;

    @Column(name = "bucket_start", nullable = false)
    private LocalDateTime bucketStart;

    @Column(name = "bucket_end", nullable = false)
    private LocalDateTime bucketEnd;

    @Column(name = "tool_name", length = 100)
    private String toolName;

    @Enumerated(EnumType.STRING)
    @Column(name = "metric_type", nullable = false, length = 50)
    private MetricType metricType;

    @Column(name = "total_count")
    @Builder.Default
    private Long totalCount = 0L;

    @Column(name = "success_count")
    @Builder.Default
    private Long successCount = 0L;

    @Column(name = "error_count")
    @Builder.Default
    private Long errorCount = 0L;

    @Column(name = "avg_duration_ms")
    private Double avgDurationMs;

    @Column(name = "min_duration_ms")
    private Double minDurationMs;

    @Column(name = "max_duration_ms")
    private Double maxDurationMs;

    @Column(name = "p95_duration_ms")
    private Double p95DurationMs;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Calculate success rate as a percentage.
     */
    public double getSuccessRate() {
        if (totalCount == null || totalCount == 0) {
            return 100.0;
        }
        return (successCount * 100.0) / totalCount;
    }

    /**
     * Calculate error rate as a percentage.
     */
    public double getErrorRate() {
        if (totalCount == null || totalCount == 0) {
            return 0.0;
        }
        return (errorCount * 100.0) / totalCount;
    }

    /**
     * Check if this is an overall metric (not tool-specific).
     */
    public boolean isOverallMetric() {
        return toolName == null || toolName.isBlank();
    }

    /**
     * Create a new aggregate for a bucket.
     */
    public static McpMetricsAggregate createBucket(
            BucketType bucketType,
            LocalDateTime timestamp,
            String toolName,
            MetricType metricType) {
        LocalDateTime bucketStart = bucketType.getBucketStart(timestamp);
        return McpMetricsAggregate.builder()
                .bucketType(bucketType)
                .bucketStart(bucketStart)
                .bucketEnd(bucketType.getBucketEnd(bucketStart))
                .toolName(toolName)
                .metricType(metricType)
                .totalCount(0L)
                .successCount(0L)
                .errorCount(0L)
                .build();
    }

    /**
     * Increment counts for a tool call.
     */
    public void incrementToolCall(boolean success, double durationMs) {
        this.totalCount++;
        if (success) {
            this.successCount++;
        } else {
            this.errorCount++;
        }
        updateDurationStats(durationMs);
    }

    /**
     * Update duration statistics.
     */
    private void updateDurationStats(double durationMs) {
        if (this.minDurationMs == null || durationMs < this.minDurationMs) {
            this.minDurationMs = durationMs;
        }
        if (this.maxDurationMs == null || durationMs > this.maxDurationMs) {
            this.maxDurationMs = durationMs;
        }
        // Simple running average (for more accurate stats, use Welford's algorithm)
        if (this.avgDurationMs == null) {
            this.avgDurationMs = durationMs;
        } else {
            this.avgDurationMs = ((this.avgDurationMs * (totalCount - 1)) + durationMs) / totalCount;
        }
    }
}
