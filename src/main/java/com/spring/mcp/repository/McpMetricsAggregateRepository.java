package com.spring.mcp.repository;

import com.spring.mcp.model.entity.McpMetricsAggregate;
import com.spring.mcp.model.enums.BucketType;
import com.spring.mcp.model.enums.MetricType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for McpMetricsAggregate entities.
 * Provides queries for accessing aggregated MCP metrics.
 *
 * @author Spring MCP Server
 * @version 1.4.4
 * @since 2025-12-16
 */
@Repository
public interface McpMetricsAggregateRepository extends JpaRepository<McpMetricsAggregate, Long> {

    /**
     * Find metrics by bucket type and start time.
     */
    @Query("SELECT m FROM McpMetricsAggregate m WHERE m.bucketType = :type AND m.bucketStart >= :start ORDER BY m.bucketStart DESC")
    List<McpMetricsAggregate> findByBucketTypeAndStartAfter(
            @Param("type") BucketType type,
            @Param("start") LocalDateTime start);

    /**
     * Find metrics for a specific tool and bucket type.
     */
    @Query("SELECT m FROM McpMetricsAggregate m WHERE m.toolName = :tool AND m.bucketType = :type AND m.bucketStart >= :start ORDER BY m.bucketStart DESC")
    List<McpMetricsAggregate> findByToolAndBucket(
            @Param("tool") String tool,
            @Param("type") BucketType type,
            @Param("start") LocalDateTime start);

    /**
     * Find overall metrics (not tool-specific) for a bucket type.
     */
    @Query("SELECT m FROM McpMetricsAggregate m WHERE m.toolName IS NULL AND m.bucketType = :type AND m.bucketStart >= :start ORDER BY m.bucketStart DESC")
    List<McpMetricsAggregate> findOverallMetrics(
            @Param("type") BucketType type,
            @Param("start") LocalDateTime start);

    /**
     * Find a specific bucket by type, start time, tool, and metric type.
     */
    Optional<McpMetricsAggregate> findByBucketTypeAndBucketStartAndToolNameAndMetricType(
            BucketType bucketType,
            LocalDateTime bucketStart,
            String toolName,
            MetricType metricType);

    /**
     * Get total tool calls in a time range.
     */
    @Query("SELECT COALESCE(SUM(m.totalCount), 0) FROM McpMetricsAggregate m " +
           "WHERE m.bucketType = :type AND m.metricType = 'TOOL_CALLS' AND m.bucketStart >= :start")
    Long getTotalToolCalls(
            @Param("type") BucketType type,
            @Param("start") LocalDateTime start);

    /**
     * Get average latency for all tools in a time range.
     */
    @Query("SELECT COALESCE(AVG(m.avgDurationMs), 0) FROM McpMetricsAggregate m " +
           "WHERE m.bucketType = :type AND m.metricType = 'TOOL_CALLS' AND m.bucketStart >= :start AND m.avgDurationMs IS NOT NULL")
    Double getAverageLatency(
            @Param("type") BucketType type,
            @Param("start") LocalDateTime start);

    /**
     * Get error count in a time range.
     */
    @Query("SELECT COALESCE(SUM(m.errorCount), 0) FROM McpMetricsAggregate m " +
           "WHERE m.bucketType = :type AND m.metricType = 'TOOL_CALLS' AND m.bucketStart >= :start")
    Long getTotalErrors(
            @Param("type") BucketType type,
            @Param("start") LocalDateTime start);

    /**
     * Get distinct tool names with metrics.
     */
    @Query("SELECT DISTINCT m.toolName FROM McpMetricsAggregate m WHERE m.toolName IS NOT NULL ORDER BY m.toolName")
    List<String> findDistinctToolNames();

    /**
     * Get top N tools by request count.
     */
    @Query("SELECT m.toolName, SUM(m.totalCount) as total FROM McpMetricsAggregate m " +
           "WHERE m.toolName IS NOT NULL AND m.bucketType = :type AND m.bucketStart >= :start " +
           "GROUP BY m.toolName ORDER BY total DESC")
    List<Object[]> findTopToolsByRequestCount(
            @Param("type") BucketType type,
            @Param("start") LocalDateTime start);

    /**
     * Delete metrics older than cutoff date.
     */
    @Modifying
    @Query("DELETE FROM McpMetricsAggregate m WHERE m.createdAt < :cutoff")
    int deleteOlderThan(@Param("cutoff") LocalDateTime cutoff);

    /**
     * Delete metrics by bucket type older than cutoff.
     */
    @Modifying
    @Query("DELETE FROM McpMetricsAggregate m WHERE m.bucketType = :type AND m.bucketStart < :cutoff")
    int deleteByBucketTypeOlderThan(
            @Param("type") BucketType type,
            @Param("cutoff") LocalDateTime cutoff);
}
