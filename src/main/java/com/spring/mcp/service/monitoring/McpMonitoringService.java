package com.spring.mcp.service.monitoring;

import com.spring.mcp.model.dto.ApiKeyUsageDto;
import com.spring.mcp.model.dto.ClientUsageDto;
import com.spring.mcp.model.dto.MonitoringOverviewDto;
import com.spring.mcp.model.dto.ToolDetailDto;
import com.spring.mcp.model.dto.ToolGroupDto;
import com.spring.mcp.model.dto.ToolMetricDto;
import com.spring.mcp.model.entity.ApiKey;
import com.spring.mcp.model.entity.McpConnectionEvent;
import com.spring.mcp.model.entity.McpMetricsAggregate;
import com.spring.mcp.model.entity.McpRequest;
import com.spring.mcp.model.entity.MonitoringSettings;
import com.spring.mcp.model.enums.BucketType;
import com.spring.mcp.model.enums.ConnectionEventType;
import com.spring.mcp.model.enums.MetricType;
import com.spring.mcp.repository.ApiKeyRepository;
import com.spring.mcp.repository.McpConnectionEventRepository;
import com.spring.mcp.repository.McpMetricsAggregateRepository;
import com.spring.mcp.repository.McpRequestRepository;
import com.spring.mcp.repository.MonitoringSettingsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for recording and retrieving MCP monitoring metrics.
 *
 * @author Spring MCP Server
 * @version 1.4.4
 * @since 2025-12-16
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class McpMonitoringService {

    private final McpMetricsAggregateRepository metricsRepository;
    private final McpConnectionEventRepository connectionEventRepository;
    private final McpRequestRepository requestRepository;
    private final MonitoringSettingsRepository settingsRepository;
    private final ApiKeyRepository apiKeyRepository;

    /**
     * Record a tool call metric.
     * Uses REQUIRES_NEW to ensure metrics are recorded in a separate transaction,
     * independent of any read-only transactions from tool methods.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordToolCall(String toolName, double durationMs, boolean success, String errorMessage) {
        LocalDateTime now = LocalDateTime.now();

        // Record in 5-minute bucket for aggregated metrics
        recordMetricInBucket(BucketType.FIVE_MIN, now, toolName, durationMs, success);

        // Also record individual request for detailed tracking
        McpRequest request = McpRequest.builder()
                .toolName(toolName)
                .executionTimeMs((int) durationMs)
                .responseStatus(success ? "SUCCESS" : "ERROR")
                .createdAt(now)
                .build();
        requestRepository.save(request);

        // Log error details
        if (!success && errorMessage != null) {
            log.debug("Tool call error recorded: tool={}, error={}", toolName, errorMessage);
        }
    }

    /**
     * Record a connection event.
     */
    @Transactional
    public void recordConnectionEvent(String sessionId, ConnectionEventType eventType,
                                      Map<String, Object> clientInfo, String protocolVersion) {
        McpConnectionEvent event = McpConnectionEvent.builder()
                .sessionId(sessionId)
                .eventType(eventType)
                .clientInfo(clientInfo)
                .protocolVersion(protocolVersion)
                .build();
        connectionEventRepository.save(event);
        log.debug("Connection event recorded: session={}, type={}", sessionId, eventType);
    }

    /**
     * Record a connection error.
     */
    @Transactional
    public void recordConnectionError(String sessionId, String errorMessage, Map<String, Object> clientInfo) {
        McpConnectionEvent event = McpConnectionEvent.createError(sessionId, errorMessage, clientInfo);
        connectionEventRepository.save(event);
        log.warn("Connection error recorded: session={}, error={}", sessionId, errorMessage);
    }

    private void recordMetricInBucket(BucketType bucketType, LocalDateTime timestamp,
                                      String toolName, double durationMs, boolean success) {
        LocalDateTime bucketStart = bucketType.getBucketStart(timestamp);

        McpMetricsAggregate metric = metricsRepository
                .findByBucketTypeAndBucketStartAndToolNameAndMetricType(
                        bucketType, bucketStart, toolName, MetricType.TOOL_CALLS)
                .orElseGet(() -> McpMetricsAggregate.createBucket(
                        bucketType, timestamp, toolName, MetricType.TOOL_CALLS));

        metric.incrementToolCall(success, durationMs);
        metricsRepository.save(metric);
    }

    /**
     * Get monitoring overview metrics for the dashboard.
     */
    @Transactional(readOnly = true)
    public MonitoringOverviewDto getOverviewMetrics(BucketType period) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime periodStart = period.getLookbackTime();
        LocalDateTime last24h = now.minusHours(24);
        LocalDateTime lastHour = now.minusHours(1);
        LocalDateTime last5Min = now.minusMinutes(5);

        // Get request counts from actual request table for accuracy
        long totalLast24h = getTotalRequestCount(last24h);
        long totalLastHour = getTotalRequestCount(lastHour);
        long totalLast5Min = getTotalRequestCount(last5Min);

        // Get latency metrics (always query FIVE_MIN buckets since that's where metrics are stored)
        Double avgLatency = getAverageLatency(BucketType.FIVE_MIN, periodStart);
        Double minLatency = getMinLatency(BucketType.FIVE_MIN, periodStart);
        Double maxLatency = getMaxLatency(BucketType.FIVE_MIN, periodStart);

        // Get error metrics
        long errorCount = getErrorCount(periodStart);
        long successCount = totalLast24h - errorCount;
        double errorRate = totalLast24h > 0 ? (errorCount * 100.0) / totalLast24h : 0;
        double successRate = 100 - errorRate;

        // Get connection metrics
        long activeConnections = connectionEventRepository.countActiveConnections(last5Min);
        long connectionsLast24h = connectionEventRepository.countByEventTypeSince(
                ConnectionEventType.CONNECTED, last24h);
        long disconnectionsLast24h = connectionEventRepository.countByEventTypeSince(
                ConnectionEventType.DISCONNECTED, last24h);
        long connectionErrors = connectionEventRepository.countErrorsSince(last24h);

        // Get top tools (always query FIVE_MIN buckets since that's where metrics are stored)
        List<MonitoringOverviewDto.ToolSummaryDto> topTools = getTopTools(BucketType.FIVE_MIN, periodStart, 5);

        // Get event distribution
        Map<String, Long> eventDistribution = getEventDistribution(last24h);

        // Calculate throughput
        long durationSeconds = period.getDuration().toSeconds();
        long requestsInPeriod = switch (period) {
            case FIVE_MIN -> totalLast5Min;
            case ONE_HOUR -> totalLastHour;
            case TWENTY_FOUR_HOUR -> totalLast24h;
        };
        double rps = durationSeconds > 0 ? (double) requestsInPeriod / durationSeconds : 0;

        return MonitoringOverviewDto.builder()
                .activeConnections(activeConnections)
                .totalRequestsLast24h(totalLast24h)
                .totalRequestsLastHour(totalLastHour)
                .totalRequestsLast5Min(totalLast5Min)
                .avgLatencyMs(avgLatency != null ? avgLatency : 0)
                .minLatencyMs(minLatency != null ? minLatency : 0)
                .maxLatencyMs(maxLatency != null ? maxLatency : 0)
                .p95LatencyMs(0) // Would need percentile calculation
                .errorCount(errorCount)
                .errorRate(errorRate)
                .successCount(successCount)
                .successRate(successRate)
                .requestsPerSecond(rps)
                .requestsPerMinute(rps * 60)
                .connectionsLast24h(connectionsLast24h)
                .disconnectionsLast24h(disconnectionsLast24h)
                .connectionErrorsLast24h(connectionErrors)
                .selectedPeriod(period)
                .periodStart(periodStart)
                .periodEnd(now)
                .topTools(topTools)
                .eventDistribution(eventDistribution)
                .lastUpdated(now)
                .build();
    }

    /**
     * Get metrics for all tools.
     * Note: All metrics are stored in FIVE_MIN buckets, so we always query those
     * but use the period's lookback time to filter the results.
     */
    @Transactional(readOnly = true)
    public List<ToolMetricDto> getToolMetrics(BucketType period) {
        LocalDateTime periodStart = period.getLookbackTime();

        // Get tool names with activity
        List<String> toolNames = metricsRepository.findDistinctToolNames();

        // Always query FIVE_MIN buckets since that's where metrics are stored
        return toolNames.stream()
                .map(toolName -> getToolMetric(toolName, BucketType.FIVE_MIN, periodStart, period))
                .filter(Objects::nonNull)
                .sorted(Comparator.comparingLong(ToolMetricDto::getTotalRequests).reversed())
                .collect(Collectors.toList());
    }

    /**
     * Get detailed metrics for a specific tool.
     * Note: Always queries FIVE_MIN buckets since that's where metrics are stored.
     */
    @Transactional(readOnly = true)
    public ToolDetailDto getToolDetail(String toolName, BucketType period) {
        LocalDateTime periodStart = period.getLookbackTime();
        // Always query FIVE_MIN buckets since that's where metrics are stored
        ToolMetricDto metrics = getToolMetric(toolName, BucketType.FIVE_MIN, periodStart, period);

        if (metrics == null) {
            return null;
        }

        // Get time series data (always from FIVE_MIN buckets)
        List<ToolDetailDto.TimeSeriesPoint> requestTimeSeries = getRequestTimeSeries(toolName, period);
        List<ToolDetailDto.TimeSeriesPoint> latencyTimeSeries = getLatencyTimeSeries(toolName, period);

        return ToolDetailDto.builder()
                .toolName(toolName)
                .toolGroup(getToolGroup(toolName))
                .displayName(formatToolName(toolName))
                .currentMetrics(metrics)
                .requestTimeSeries(requestTimeSeries)
                .latencyTimeSeries(latencyTimeSeries)
                .period(period)
                .periodStart(periodStart)
                .periodEnd(LocalDateTime.now())
                .build();
    }

    /**
     * Get tool groups with metrics.
     */
    @Transactional(readOnly = true)
    public List<ToolGroupDto> getToolGroups(BucketType period) {
        List<ToolMetricDto> allMetrics = getToolMetrics(period);

        // Group by tool group
        Map<String, List<ToolMetricDto>> grouped = allMetrics.stream()
                .collect(Collectors.groupingBy(m -> {
                    String group = m.getToolGroup();
                    return group != null ? group : "OTHER";
                }));

        // Create ToolGroupDto for each group
        List<ToolGroupDto> result = new ArrayList<>();
        for (ToolGroupDto.Group group : ToolGroupDto.Group.values()) {
            ToolGroupDto dto = group.toDto();
            dto.setTools(grouped.getOrDefault(group.name(), Collections.emptyList()));
            dto.calculateAggregates();
            result.add(dto);
        }

        // Add any ungrouped tools
        List<ToolMetricDto> ungrouped = grouped.get("OTHER");
        if (ungrouped != null && !ungrouped.isEmpty()) {
            ToolGroupDto otherGroup = ToolGroupDto.builder()
                    .groupName("OTHER")
                    .displayName("Other Tools")
                    .description("Miscellaneous tools")
                    .iconClass("bi-three-dots")
                    .colorClass("muted")
                    .tools(ungrouped)
                    .build();
            otherGroup.calculateAggregates();
            result.add(otherGroup);
        }

        return result;
    }

    /**
     * Get count of active connections.
     */
    @Transactional(readOnly = true)
    public long getActiveConnections() {
        return connectionEventRepository.countActiveConnections(LocalDateTime.now().minusMinutes(5));
    }

    /**
     * Get monitoring settings.
     */
    @Transactional(readOnly = true)
    public Map<String, String> getSettings() {
        return settingsRepository.findAll().stream()
                .collect(Collectors.toMap(
                        MonitoringSettings::getSettingKey,
                        s -> s.getSettingValue() != null ? s.getSettingValue() : ""));
    }

    /**
     * Update a monitoring setting.
     */
    @Transactional
    public void updateSetting(String key, String value, String updatedBy) {
        settingsRepository.findBySettingKey(key).ifPresentOrElse(
                setting -> {
                    setting.updateValue(value, updatedBy);
                    settingsRepository.save(setting);
                },
                () -> settingsRepository.save(
                        MonitoringSettings.create(key, value, null, updatedBy))
        );
    }

    /**
     * Get API key usage statistics for the monitoring dashboard.
     * Returns top 5 keys by request count and remaining keys.
     */
    @Transactional(readOnly = true)
    public ApiKeyUsageDto.ApiKeyUsageSummary getApiKeyUsageStats() {
        // Get all API keys ordered by request count
        List<ApiKey> allKeys = apiKeyRepository.findAllByOrderByRequestCountDesc();

        // Convert to DTOs
        List<ApiKeyUsageDto> allDtos = allKeys.stream()
                .map(ApiKeyUsageDto::fromEntity)
                .collect(Collectors.toList());

        // Split into top 5 and others
        List<ApiKeyUsageDto> topKeys = allDtos.stream()
                .limit(5)
                .collect(Collectors.toList());

        List<ApiKeyUsageDto> otherKeys = allDtos.stream()
                .skip(5)
                .collect(Collectors.toList());

        // Calculate totals
        long totalRequests = allDtos.stream()
                .mapToLong(dto -> dto.getRequestCount() != null ? dto.getRequestCount() : 0L)
                .sum();

        long activeKeys = allDtos.stream()
                .filter(dto -> Boolean.TRUE.equals(dto.getIsActive()))
                .count();

        return ApiKeyUsageDto.ApiKeyUsageSummary.builder()
                .topKeys(topKeys)
                .otherKeys(otherKeys)
                .totalKeys(allDtos.size())
                .totalRequests(totalRequests)
                .activeKeys(activeKeys)
                .build();
    }

    /**
     * Get client usage statistics for the monitoring dashboard.
     * Returns top 5 clients by connection count and remaining clients.
     */
    @Transactional(readOnly = true)
    public ClientUsageDto.ClientUsageSummary getClientUsageStats() {
        LocalDateTime last24h = LocalDateTime.now().minusHours(24);

        // Get all client usage stats
        List<Object[]> rawStats = connectionEventRepository.getClientUsageStats(last24h);

        // Convert to DTOs
        List<ClientUsageDto> allDtos = rawStats.stream()
                .map(ClientUsageDto::fromQueryResult)
                .collect(Collectors.toList());

        // Split into top 5 and others
        List<ClientUsageDto> topClients = allDtos.stream()
                .limit(5)
                .collect(Collectors.toList());

        List<ClientUsageDto> otherClients = allDtos.stream()
                .skip(5)
                .collect(Collectors.toList());

        // Calculate totals
        long totalConnections = allDtos.stream()
                .mapToLong(dto -> dto.getConnectionCount() != null ? dto.getConnectionCount() : 0L)
                .sum();

        return ClientUsageDto.ClientUsageSummary.builder()
                .topClients(topClients)
                .otherClients(otherClients)
                .totalClients(allDtos.size())
                .totalConnections(totalConnections)
                .build();
    }

    // Helper methods

    private long getTotalRequestCount(LocalDateTime since) {
        return requestRepository.countByCreatedAtAfter(since);
    }

    private Double getAverageLatency(BucketType period, LocalDateTime since) {
        return metricsRepository.getAverageLatency(period, since);
    }

    private Double getMinLatency(BucketType period, LocalDateTime since) {
        List<McpMetricsAggregate> metrics = metricsRepository.findByBucketTypeAndStartAfter(period, since);
        return metrics.stream()
                .filter(m -> m.getMinDurationMs() != null)
                .mapToDouble(McpMetricsAggregate::getMinDurationMs)
                .min()
                .orElse(0);
    }

    private Double getMaxLatency(BucketType period, LocalDateTime since) {
        List<McpMetricsAggregate> metrics = metricsRepository.findByBucketTypeAndStartAfter(period, since);
        return metrics.stream()
                .filter(m -> m.getMaxDurationMs() != null)
                .mapToDouble(McpMetricsAggregate::getMaxDurationMs)
                .max()
                .orElse(0);
    }

    private long getErrorCount(LocalDateTime since) {
        return requestRepository.countByResponseStatusNotAndCreatedAtAfter("SUCCESS", since);
    }

    private List<MonitoringOverviewDto.ToolSummaryDto> getTopTools(BucketType period,
                                                                    LocalDateTime since, int limit) {
        List<Object[]> topToolsData = metricsRepository.findTopToolsByRequestCount(period, since);
        return topToolsData.stream()
                .limit(limit)
                .map(row -> {
                    String toolName = (String) row[0];
                    Long count = (Long) row[1];
                    return MonitoringOverviewDto.ToolSummaryDto.builder()
                            .toolName(toolName)
                            .requestCount(count)
                            .toolGroup(getToolGroup(toolName))
                            .build();
                })
                .collect(Collectors.toList());
    }

    private Map<String, Long> getEventDistribution(LocalDateTime since) {
        List<Object[]> distribution = connectionEventRepository.countByEventTypeGrouped(since);
        return distribution.stream()
                .collect(Collectors.toMap(
                        row -> ((ConnectionEventType) row[0]).name(),
                        row -> (Long) row[1]
                ));
    }

    /**
     * Get metrics for a specific tool.
     * @param toolName the tool name
     * @param storageBucket the bucket type to query (always FIVE_MIN since that's where metrics are stored)
     * @param since the start time for the query
     * @param displayPeriod the period to show in the DTO (for display purposes)
     */
    private ToolMetricDto getToolMetric(String toolName, BucketType storageBucket,
                                         LocalDateTime since, BucketType displayPeriod) {
        List<McpMetricsAggregate> metrics = metricsRepository.findByToolAndBucket(toolName, storageBucket, since);
        if (metrics.isEmpty()) {
            return null;
        }

        long total = metrics.stream().mapToLong(McpMetricsAggregate::getTotalCount).sum();
        long success = metrics.stream().mapToLong(McpMetricsAggregate::getSuccessCount).sum();
        long errors = metrics.stream().mapToLong(McpMetricsAggregate::getErrorCount).sum();

        Double avgLatency = metrics.stream()
                .filter(m -> m.getAvgDurationMs() != null && m.getTotalCount() > 0)
                .mapToDouble(m -> m.getAvgDurationMs() * m.getTotalCount())
                .sum() / Math.max(total, 1);

        Double minLatency = metrics.stream()
                .filter(m -> m.getMinDurationMs() != null)
                .mapToDouble(McpMetricsAggregate::getMinDurationMs)
                .min()
                .orElse(0);

        Double maxLatency = metrics.stream()
                .filter(m -> m.getMaxDurationMs() != null)
                .mapToDouble(McpMetricsAggregate::getMaxDurationMs)
                .max()
                .orElse(0);

        return ToolMetricDto.fromValues(
                toolName,
                getToolGroup(toolName),
                total, success, errors,
                avgLatency, minLatency, maxLatency, null,
                displayPeriod
        );
    }

    private List<ToolDetailDto.TimeSeriesPoint> getRequestTimeSeries(String toolName, BucketType period) {
        LocalDateTime since = period.getLookbackTime();
        // Always query FIVE_MIN buckets since that's where metrics are stored
        List<McpMetricsAggregate> metrics = metricsRepository.findByToolAndBucket(toolName, BucketType.FIVE_MIN, since);

        return metrics.stream()
                .sorted(Comparator.comparing(McpMetricsAggregate::getBucketStart))
                .map(m -> ToolDetailDto.TimeSeriesPoint.builder()
                        .timestamp(m.getBucketStart())
                        .value(m.getTotalCount())
                        .build())
                .collect(Collectors.toList());
    }

    private List<ToolDetailDto.TimeSeriesPoint> getLatencyTimeSeries(String toolName, BucketType period) {
        LocalDateTime since = period.getLookbackTime();
        // Always query FIVE_MIN buckets since that's where metrics are stored
        List<McpMetricsAggregate> metrics = metricsRepository.findByToolAndBucket(toolName, BucketType.FIVE_MIN, since);

        return metrics.stream()
                .filter(m -> m.getAvgDurationMs() != null)
                .sorted(Comparator.comparing(McpMetricsAggregate::getBucketStart))
                .map(m -> ToolDetailDto.TimeSeriesPoint.builder()
                        .timestamp(m.getBucketStart())
                        .value(m.getAvgDurationMs())
                        .build())
                .collect(Collectors.toList());
    }

    private String getToolGroup(String toolName) {
        ToolGroupDto.Group group = ToolGroupDto.Group.findGroupForTool(toolName);
        return group != null ? group.name() : "OTHER";
    }

    private String formatToolName(String toolName) {
        if (toolName == null || toolName.isEmpty()) {
            return toolName;
        }
        String formatted = toolName.replaceAll("([a-z])([A-Z])", "$1 $2");
        return Character.toUpperCase(formatted.charAt(0)) + formatted.substring(1);
    }
}
