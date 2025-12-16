package com.spring.mcp.model.dto;

import com.spring.mcp.model.enums.BucketType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * DTO for monitoring dashboard overview data.
 *
 * @author Spring MCP Server
 * @version 1.4.4
 * @since 2025-12-16
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MonitoringOverviewDto {

    // Current state metrics
    private long activeConnections;
    private long totalRequestsLast24h;
    private long totalRequestsLastHour;
    private long totalRequestsLast5Min;

    // Latency metrics (in milliseconds)
    private double avgLatencyMs;
    private double minLatencyMs;
    private double maxLatencyMs;
    private double p95LatencyMs;

    // Error metrics
    private long errorCount;
    private double errorRate;

    // Success metrics
    private long successCount;
    private double successRate;

    // Throughput
    private double requestsPerSecond;
    private double requestsPerMinute;

    // Connection events
    private long connectionsLast24h;
    private long disconnectionsLast24h;
    private long connectionErrorsLast24h;

    // Selected period
    private BucketType selectedPeriod;
    private LocalDateTime periodStart;
    private LocalDateTime periodEnd;

    // Top tools summary
    private List<ToolSummaryDto> topTools;

    // Event distribution
    private Map<String, Long> eventDistribution;

    // Last update timestamp
    private LocalDateTime lastUpdated;

    /**
     * Summary of a tool's metrics for the overview.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ToolSummaryDto {
        private String toolName;
        private long requestCount;
        private double avgLatencyMs;
        private double errorRate;
        private String toolGroup;
    }

    /**
     * Calculate throughput from total requests and time period.
     */
    public void calculateThroughput() {
        if (selectedPeriod != null && totalRequestsLast5Min > 0) {
            long durationSeconds = selectedPeriod.getDuration().toSeconds();
            long requests = switch (selectedPeriod) {
                case FIVE_MIN -> totalRequestsLast5Min;
                case ONE_HOUR -> totalRequestsLastHour;
                case TWENTY_FOUR_HOUR -> totalRequestsLast24h;
            };
            this.requestsPerSecond = durationSeconds > 0 ? (double) requests / durationSeconds : 0;
            this.requestsPerMinute = this.requestsPerSecond * 60;
        }
    }

    /**
     * Create an empty overview for when there's no data.
     */
    public static MonitoringOverviewDto empty(BucketType period) {
        return MonitoringOverviewDto.builder()
                .activeConnections(0)
                .totalRequestsLast24h(0)
                .totalRequestsLastHour(0)
                .totalRequestsLast5Min(0)
                .avgLatencyMs(0)
                .minLatencyMs(0)
                .maxLatencyMs(0)
                .p95LatencyMs(0)
                .errorCount(0)
                .errorRate(0)
                .successCount(0)
                .successRate(100)
                .requestsPerSecond(0)
                .requestsPerMinute(0)
                .connectionsLast24h(0)
                .disconnectionsLast24h(0)
                .connectionErrorsLast24h(0)
                .selectedPeriod(period)
                .lastUpdated(LocalDateTime.now())
                .build();
    }
}
