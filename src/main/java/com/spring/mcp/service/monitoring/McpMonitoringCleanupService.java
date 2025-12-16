package com.spring.mcp.service.monitoring;

import com.spring.mcp.model.entity.MonitoringSettings;
import com.spring.mcp.model.enums.BucketType;
import com.spring.mcp.repository.McpConnectionEventRepository;
import com.spring.mcp.repository.McpMetricsAggregateRepository;
import com.spring.mcp.repository.MonitoringSettingsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Service for cleaning up old monitoring data.
 * Removes data older than the configured retention period (default: 24 hours).
 *
 * @author Spring MCP Server
 * @version 1.4.4
 * @since 2025-12-16
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class McpMonitoringCleanupService {

    private final McpMetricsAggregateRepository metricsRepository;
    private final McpConnectionEventRepository connectionEventRepository;
    private final MonitoringSettingsRepository settingsRepository;

    /**
     * Clean up old monitoring data.
     * Runs every 6 hours by default.
     */
    @Scheduled(cron = "0 0 */6 * * *")
    @Transactional
    public void cleanupOldData() {
        int retentionHours = getRetentionHours();
        LocalDateTime cutoff = LocalDateTime.now().minusHours(retentionHours);

        log.info("Starting monitoring data cleanup. Retention: {} hours, Cutoff: {}", retentionHours, cutoff);

        // Clean up 5-minute metrics (keep for retention period)
        int deleted5Min = metricsRepository.deleteByBucketTypeOlderThan(BucketType.FIVE_MIN, cutoff);
        log.debug("Deleted {} 5-minute metric buckets older than {}", deleted5Min, cutoff);

        // Clean up hourly metrics (keep for 7 days)
        LocalDateTime hourlyCutoff = LocalDateTime.now().minusDays(7);
        int deletedHourly = metricsRepository.deleteByBucketTypeOlderThan(BucketType.ONE_HOUR, hourlyCutoff);
        log.debug("Deleted {} hourly metric buckets older than {}", deletedHourly, hourlyCutoff);

        // Clean up daily metrics (keep for 30 days)
        LocalDateTime dailyCutoff = LocalDateTime.now().minusDays(30);
        int deletedDaily = metricsRepository.deleteByBucketTypeOlderThan(BucketType.TWENTY_FOUR_HOUR, dailyCutoff);
        log.debug("Deleted {} daily metric buckets older than {}", deletedDaily, dailyCutoff);

        // Clean up connection events (keep for retention period)
        int deletedEvents = connectionEventRepository.deleteOlderThan(cutoff);
        log.debug("Deleted {} connection events older than {}", deletedEvents, cutoff);

        log.info("Monitoring cleanup complete. Deleted: {} 5-min, {} hourly, {} daily metrics, {} events",
                deleted5Min, deletedHourly, deletedDaily, deletedEvents);
    }

    /**
     * Manually trigger cleanup with custom cutoff.
     */
    @Transactional
    public CleanupResult cleanupOlderThan(LocalDateTime cutoff) {
        log.info("Manual cleanup triggered with cutoff: {}", cutoff);

        int deletedMetrics = metricsRepository.deleteOlderThan(cutoff);
        int deletedEvents = connectionEventRepository.deleteOlderThan(cutoff);

        log.info("Manual cleanup complete. Deleted: {} metrics, {} events", deletedMetrics, deletedEvents);

        return new CleanupResult(deletedMetrics, deletedEvents, cutoff);
    }

    /**
     * Get current data retention statistics.
     */
    @Transactional(readOnly = true)
    public RetentionStats getRetentionStats() {
        long totalMetrics = metricsRepository.count();
        long totalEvents = connectionEventRepository.count();

        LocalDateTime now = LocalDateTime.now();
        int retentionHours = getRetentionHours();

        // Count records that would be deleted
        LocalDateTime cutoff = now.minusHours(retentionHours);
        // Note: We don't have a count query, so we'll estimate
        // In a real implementation, you'd add a count query

        return new RetentionStats(
                totalMetrics,
                totalEvents,
                retentionHours,
                cutoff,
                now
        );
    }

    /**
     * Update retention period.
     */
    @Transactional
    public void updateRetentionHours(int hours, String updatedBy) {
        if (hours < 1 || hours > 720) { // 1 hour to 30 days
            throw new IllegalArgumentException("Retention hours must be between 1 and 720");
        }

        settingsRepository.findBySettingKey(MonitoringSettings.KEY_RETENTION_HOURS)
                .ifPresentOrElse(
                        setting -> {
                            setting.updateValue(String.valueOf(hours), updatedBy);
                            settingsRepository.save(setting);
                        },
                        () -> settingsRepository.save(
                                MonitoringSettings.create(
                                        MonitoringSettings.KEY_RETENTION_HOURS,
                                        String.valueOf(hours),
                                        "Hours to retain detailed metrics",
                                        updatedBy
                                )
                        )
                );

        log.info("Retention period updated to {} hours by {}", hours, updatedBy);
    }

    private int getRetentionHours() {
        return settingsRepository.findBySettingKey(MonitoringSettings.KEY_RETENTION_HOURS)
                .map(s -> s.getIntValue(MonitoringSettings.DEFAULT_RETENTION_HOURS))
                .orElse(MonitoringSettings.DEFAULT_RETENTION_HOURS);
    }

    /**
     * Result of a cleanup operation.
     */
    public record CleanupResult(
            int deletedMetrics,
            int deletedEvents,
            LocalDateTime cutoff
    ) {}

    /**
     * Current retention statistics.
     */
    public record RetentionStats(
            long totalMetrics,
            long totalEvents,
            int retentionHours,
            LocalDateTime nextCleanupCutoff,
            LocalDateTime calculatedAt
    ) {}
}
