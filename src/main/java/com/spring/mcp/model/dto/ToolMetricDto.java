package com.spring.mcp.model.dto;

import com.spring.mcp.model.enums.BucketType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for individual tool metrics.
 *
 * @author Spring MCP Server
 * @version 1.4.4
 * @since 2025-12-16
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ToolMetricDto {

    private String toolName;
    private String toolGroup;
    private String displayName;

    // Request counts
    private long totalRequests;
    private long successCount;
    private long errorCount;

    // Rates
    private double successRate;
    private double errorRate;

    // Latency metrics (in milliseconds)
    private double avgLatencyMs;
    private double minLatencyMs;
    private double maxLatencyMs;
    private double p95LatencyMs;

    // Time context
    private BucketType period;
    private LocalDateTime periodStart;
    private LocalDateTime periodEnd;

    // Trend indicators (compared to previous period)
    private Double requestCountTrend; // Percentage change
    private Double latencyTrend;      // Percentage change
    private Double errorRateTrend;    // Absolute change

    /**
     * Get a formatted latency string.
     */
    public String getFormattedLatency() {
        if (avgLatencyMs < 1) {
            return String.format("%.2f ms", avgLatencyMs);
        } else if (avgLatencyMs < 1000) {
            return String.format("%.0f ms", avgLatencyMs);
        } else {
            return String.format("%.2f s", avgLatencyMs / 1000);
        }
    }

    /**
     * Get the latency status class for UI styling.
     */
    public String getLatencyStatusClass() {
        if (avgLatencyMs < 200) {
            return "success";
        } else if (avgLatencyMs < 500) {
            return "warning";
        } else {
            return "danger";
        }
    }

    /**
     * Get the error rate status class for UI styling.
     */
    public String getErrorRateStatusClass() {
        if (errorRate < 1) {
            return "success";
        } else if (errorRate < 5) {
            return "warning";
        } else {
            return "danger";
        }
    }

    /**
     * Check if this tool has any requests.
     */
    public boolean hasActivity() {
        return totalRequests > 0;
    }

    /**
     * Calculate rates from counts.
     */
    public void calculateRates() {
        if (totalRequests > 0) {
            this.successRate = (successCount * 100.0) / totalRequests;
            this.errorRate = (errorCount * 100.0) / totalRequests;
        } else {
            this.successRate = 100.0;
            this.errorRate = 0.0;
        }
    }

    /**
     * Get trend indicator for UI.
     */
    public String getTrendIndicator(Double trendValue) {
        if (trendValue == null) {
            return "neutral";
        }
        if (trendValue > 5) {
            return "up";
        } else if (trendValue < -5) {
            return "down";
        }
        return "stable";
    }

    /**
     * Create a metric from raw values.
     */
    public static ToolMetricDto fromValues(
            String toolName,
            String toolGroup,
            long total,
            long success,
            long errors,
            Double avgMs,
            Double minMs,
            Double maxMs,
            Double p95Ms,
            BucketType period) {

        ToolMetricDto dto = ToolMetricDto.builder()
                .toolName(toolName)
                .toolGroup(toolGroup)
                .displayName(formatToolName(toolName))
                .totalRequests(total)
                .successCount(success)
                .errorCount(errors)
                .avgLatencyMs(avgMs != null ? avgMs : 0)
                .minLatencyMs(minMs != null ? minMs : 0)
                .maxLatencyMs(maxMs != null ? maxMs : 0)
                .p95LatencyMs(p95Ms != null ? p95Ms : 0)
                .period(period)
                .build();
        dto.calculateRates();
        return dto;
    }

    /**
     * Format tool name for display (camelCase to Title Case).
     */
    private static String formatToolName(String toolName) {
        if (toolName == null || toolName.isEmpty()) {
            return toolName;
        }
        // Insert space before uppercase letters and capitalize first letter
        String formatted = toolName.replaceAll("([a-z])([A-Z])", "$1 $2");
        return Character.toUpperCase(formatted.charAt(0)) + formatted.substring(1);
    }
}
