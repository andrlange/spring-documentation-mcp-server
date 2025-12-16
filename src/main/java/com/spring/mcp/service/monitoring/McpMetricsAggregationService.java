package com.spring.mcp.service.monitoring;

import com.spring.mcp.model.entity.McpMetricsAggregate;
import com.spring.mcp.model.entity.McpRequest;
import com.spring.mcp.model.entity.MonitoringSettings;
import com.spring.mcp.model.enums.BucketType;
import com.spring.mcp.model.enums.MetricType;
import com.spring.mcp.repository.McpMetricsAggregateRepository;
import com.spring.mcp.repository.McpRequestRepository;
import com.spring.mcp.repository.MonitoringSettingsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service for aggregating MCP metrics into time buckets.
 * Runs scheduled tasks to aggregate raw request data into
 * 5-minute, 1-hour, and 24-hour buckets.
 *
 * @author Spring MCP Server
 * @version 1.4.4
 * @since 2025-12-16
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class McpMetricsAggregationService {

    private final McpMetricsAggregateRepository metricsRepository;
    private final McpRequestRepository requestRepository;
    private final MonitoringSettingsRepository settingsRepository;

    /**
     * Aggregate metrics into 5-minute buckets.
     * Runs every 5 minutes.
     */
    @Scheduled(fixedRate = 300000) // 5 minutes
    @Transactional
    public void aggregate5MinuteBuckets() {
        if (!isAggregationEnabled()) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime bucketStart = BucketType.FIVE_MIN.getBucketStart(now.minusMinutes(5));
        LocalDateTime bucketEnd = BucketType.FIVE_MIN.getBucketEnd(bucketStart);

        log.debug("Aggregating 5-minute metrics for bucket: {} to {}", bucketStart, bucketEnd);
        aggregateMetrics(BucketType.FIVE_MIN, bucketStart, bucketEnd);
    }

    /**
     * Aggregate metrics into hourly buckets.
     * Runs at the start of every hour.
     */
    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void aggregateHourlyBuckets() {
        if (!isAggregationEnabled()) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime bucketStart = BucketType.ONE_HOUR.getBucketStart(now.minusHours(1));
        LocalDateTime bucketEnd = BucketType.ONE_HOUR.getBucketEnd(bucketStart);

        log.debug("Aggregating hourly metrics for bucket: {} to {}", bucketStart, bucketEnd);
        aggregateMetrics(BucketType.ONE_HOUR, bucketStart, bucketEnd);
    }

    /**
     * Aggregate metrics into daily buckets.
     * Runs at midnight every day.
     */
    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public void aggregateDailyBuckets() {
        if (!isAggregationEnabled()) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime bucketStart = BucketType.TWENTY_FOUR_HOUR.getBucketStart(now.minusDays(1));
        LocalDateTime bucketEnd = BucketType.TWENTY_FOUR_HOUR.getBucketEnd(bucketStart);

        log.debug("Aggregating daily metrics for bucket: {} to {}", bucketStart, bucketEnd);
        aggregateMetrics(BucketType.TWENTY_FOUR_HOUR, bucketStart, bucketEnd);
    }

    /**
     * Manually trigger aggregation for a specific bucket type and time range.
     */
    @Transactional
    public void aggregateMetrics(BucketType bucketType, LocalDateTime bucketStart, LocalDateTime bucketEnd) {
        // Get raw requests in the time range
        List<McpRequest> requests = requestRepository.findByCreatedAtBetween(bucketStart, bucketEnd);

        if (requests.isEmpty()) {
            log.debug("No requests found for bucket {} from {} to {}", bucketType, bucketStart, bucketEnd);
            return;
        }

        // Group by tool name
        Map<String, List<McpRequest>> byTool = requests.stream()
                .filter(r -> r.getToolName() != null)
                .collect(Collectors.groupingBy(McpRequest::getToolName));

        // Aggregate each tool's metrics
        for (Map.Entry<String, List<McpRequest>> entry : byTool.entrySet()) {
            String toolName = entry.getKey();
            List<McpRequest> toolRequests = entry.getValue();

            aggregateToolMetrics(bucketType, bucketStart, bucketEnd, toolName, toolRequests);
        }

        // Also aggregate overall metrics (without tool name)
        aggregateOverallMetrics(bucketType, bucketStart, bucketEnd, requests);

        log.info("Aggregated {} bucket from {} to {}: {} tools, {} total requests",
                bucketType, bucketStart, bucketEnd, byTool.size(), requests.size());
    }

    private void aggregateToolMetrics(BucketType bucketType, LocalDateTime bucketStart,
                                      LocalDateTime bucketEnd, String toolName,
                                      List<McpRequest> requests) {
        // Find or create the aggregate
        McpMetricsAggregate aggregate = metricsRepository
                .findByBucketTypeAndBucketStartAndToolNameAndMetricType(
                        bucketType, bucketStart, toolName, MetricType.TOOL_CALLS)
                .orElseGet(() -> McpMetricsAggregate.builder()
                        .bucketType(bucketType)
                        .bucketStart(bucketStart)
                        .bucketEnd(bucketEnd)
                        .toolName(toolName)
                        .metricType(MetricType.TOOL_CALLS)
                        .totalCount(0L)
                        .successCount(0L)
                        .errorCount(0L)
                        .build());

        // Calculate metrics
        long total = requests.size();
        long success = requests.stream()
                .filter(r -> "SUCCESS".equalsIgnoreCase(r.getResponseStatus()))
                .count();
        long errors = total - success;

        // Calculate latency statistics
        List<Integer> latencies = requests.stream()
                .filter(r -> r.getExecutionTimeMs() != null)
                .map(McpRequest::getExecutionTimeMs)
                .sorted()
                .toList();

        double avgLatency = latencies.isEmpty() ? 0 :
                latencies.stream().mapToInt(Integer::intValue).average().orElse(0);
        double minLatency = latencies.isEmpty() ? 0 :
                latencies.get(0);
        double maxLatency = latencies.isEmpty() ? 0 :
                latencies.get(latencies.size() - 1);
        double p95Latency = latencies.isEmpty() ? 0 :
                getPercentile(latencies, 95);

        // Update aggregate
        aggregate.setTotalCount(total);
        aggregate.setSuccessCount(success);
        aggregate.setErrorCount(errors);
        aggregate.setAvgDurationMs(avgLatency);
        aggregate.setMinDurationMs(minLatency);
        aggregate.setMaxDurationMs(maxLatency);
        aggregate.setP95DurationMs(p95Latency);

        metricsRepository.save(aggregate);
    }

    private void aggregateOverallMetrics(BucketType bucketType, LocalDateTime bucketStart,
                                         LocalDateTime bucketEnd, List<McpRequest> requests) {
        // Find or create the overall aggregate (toolName = null)
        McpMetricsAggregate aggregate = metricsRepository
                .findByBucketTypeAndBucketStartAndToolNameAndMetricType(
                        bucketType, bucketStart, null, MetricType.TOOL_CALLS)
                .orElseGet(() -> McpMetricsAggregate.builder()
                        .bucketType(bucketType)
                        .bucketStart(bucketStart)
                        .bucketEnd(bucketEnd)
                        .toolName(null)
                        .metricType(MetricType.TOOL_CALLS)
                        .totalCount(0L)
                        .successCount(0L)
                        .errorCount(0L)
                        .build());

        // Calculate metrics
        long total = requests.size();
        long success = requests.stream()
                .filter(r -> "SUCCESS".equalsIgnoreCase(r.getResponseStatus()))
                .count();
        long errors = total - success;

        // Calculate latency statistics
        List<Integer> latencies = requests.stream()
                .filter(r -> r.getExecutionTimeMs() != null)
                .map(McpRequest::getExecutionTimeMs)
                .sorted()
                .toList();

        double avgLatency = latencies.isEmpty() ? 0 :
                latencies.stream().mapToInt(Integer::intValue).average().orElse(0);
        double minLatency = latencies.isEmpty() ? 0 :
                latencies.get(0);
        double maxLatency = latencies.isEmpty() ? 0 :
                latencies.get(latencies.size() - 1);
        double p95Latency = latencies.isEmpty() ? 0 :
                getPercentile(latencies, 95);

        // Update aggregate
        aggregate.setTotalCount(total);
        aggregate.setSuccessCount(success);
        aggregate.setErrorCount(errors);
        aggregate.setAvgDurationMs(avgLatency);
        aggregate.setMinDurationMs(minLatency);
        aggregate.setMaxDurationMs(maxLatency);
        aggregate.setP95DurationMs(p95Latency);

        metricsRepository.save(aggregate);
    }

    private double getPercentile(List<Integer> sortedValues, int percentile) {
        if (sortedValues.isEmpty()) {
            return 0;
        }
        int index = (int) Math.ceil((percentile / 100.0) * sortedValues.size()) - 1;
        index = Math.max(0, Math.min(index, sortedValues.size() - 1));
        return sortedValues.get(index);
    }

    private boolean isAggregationEnabled() {
        return settingsRepository.findBySettingKey(MonitoringSettings.KEY_AGGREGATION_ENABLED)
                .map(s -> s.getBooleanValue(true))
                .orElse(true);
    }
}
