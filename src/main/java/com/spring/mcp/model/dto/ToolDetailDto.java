package com.spring.mcp.model.dto;

import com.spring.mcp.model.enums.BucketType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO for detailed tool metrics with time series data.
 *
 * @author Spring MCP Server
 * @version 1.4.4
 * @since 2025-12-16
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ToolDetailDto {

    // Basic info
    private String toolName;
    private String toolGroup;
    private String displayName;
    private String description;

    // Current metrics
    private ToolMetricDto currentMetrics;

    // Time series data for charts
    private List<TimeSeriesPoint> requestTimeSeries;
    private List<TimeSeriesPoint> latencyTimeSeries;
    private List<TimeSeriesPoint> errorTimeSeries;

    // Period context
    private BucketType period;
    private LocalDateTime periodStart;
    private LocalDateTime periodEnd;

    // Recent errors (if any)
    private List<ErrorDetail> recentErrors;

    // Percentile breakdown
    private LatencyPercentiles latencyPercentiles;

    /**
     * Time series data point for charts.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TimeSeriesPoint {
        private LocalDateTime timestamp;
        private double value;
        private String label; // Formatted timestamp for display

        public String getLabel() {
            if (label != null) {
                return label;
            }
            if (timestamp != null) {
                return timestamp.toLocalTime().toString();
            }
            return "";
        }
    }

    /**
     * Error detail for recent errors list.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ErrorDetail {
        private LocalDateTime timestamp;
        private String errorMessage;
        private String sessionId;
        private Integer durationMs;
    }

    /**
     * Latency percentile breakdown.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LatencyPercentiles {
        private double p50;  // Median
        private double p75;
        private double p90;
        private double p95;
        private double p99;
        private double max;
    }

    /**
     * Check if time series data is available.
     */
    public boolean hasTimeSeries() {
        return requestTimeSeries != null && !requestTimeSeries.isEmpty();
    }

    /**
     * Check if there are recent errors.
     */
    public boolean hasRecentErrors() {
        return recentErrors != null && !recentErrors.isEmpty();
    }

    /**
     * Get total requests from current metrics.
     */
    public long getTotalRequests() {
        return currentMetrics != null ? currentMetrics.getTotalRequests() : 0;
    }

    /**
     * Get average latency from current metrics.
     */
    public double getAverageLatency() {
        return currentMetrics != null ? currentMetrics.getAvgLatencyMs() : 0;
    }

    /**
     * Get error rate from current metrics.
     */
    public double getErrorRate() {
        return currentMetrics != null ? currentMetrics.getErrorRate() : 0;
    }

    /**
     * Create a basic detail DTO from metrics.
     */
    public static ToolDetailDto fromMetrics(ToolMetricDto metrics, BucketType period) {
        return ToolDetailDto.builder()
                .toolName(metrics.getToolName())
                .toolGroup(metrics.getToolGroup())
                .displayName(metrics.getDisplayName())
                .currentMetrics(metrics)
                .period(period)
                .periodStart(period.getLookbackTime())
                .periodEnd(LocalDateTime.now())
                .build();
    }
}
