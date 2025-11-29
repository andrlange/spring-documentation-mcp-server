package com.spring.mcp.service.scheduler;

import com.spring.mcp.model.entity.SchedulerSettings;
import com.spring.mcp.repository.SchedulerSettingsRepository;
import com.spring.mcp.service.sync.ComprehensiveSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.PostConstruct;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Set;
import java.util.concurrent.ScheduledFuture;

/**
 * Service for managing scheduled automatic synchronizations.
 * Runs comprehensive sync at configured time daily.
 *
 * @author Spring MCP Server
 * @version 1.0
 * @since 2025-01-12
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SchedulerService {

    private final SchedulerSettingsRepository schedulerSettingsRepository;
    private final ComprehensiveSyncService comprehensiveSyncService;
    private final TaskScheduler taskScheduler;

    private ScheduledFuture<?> scheduledTask;
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    /**
     * Initialize scheduler on application startup
     */
    @PostConstruct
    public void init() {
        log.info("Initializing scheduler service...");
        rescheduleTask();
    }

    /**
     * Get current scheduler settings
     *
     * @return SchedulerSettings
     */
    public SchedulerSettings getSettings() {
        return schedulerSettingsRepository.findFirstByOrderByIdAsc()
            .orElseGet(this::createDefaultSettings);
    }

    /**
     * Update scheduler settings and reschedule task
     *
     * @param syncEnabled whether sync is enabled
     * @param syncTime time in HH:mm format
     * @param timeFormat display format (12h or 24h)
     * @return updated settings
     */
    @Transactional
    public SchedulerSettings updateSettings(Boolean syncEnabled, String syncTime, String timeFormat) {
        return updateSettings(syncEnabled, syncTime, timeFormat, null, null);
    }

    /**
     * Update scheduler settings with weekday support and reschedule task
     *
     * @param syncEnabled whether sync is enabled
     * @param syncTime time in HH:mm format
     * @param timeFormat display format (12h or 24h)
     * @param weekdays comma-separated weekday codes (MON,TUE,WED,THU,FRI,SAT,SUN)
     * @param allWeekdays whether all weekdays are selected
     * @return updated settings
     */
    @Transactional
    public SchedulerSettings updateSettings(Boolean syncEnabled, String syncTime, String timeFormat,
                                           String weekdays, Boolean allWeekdays) {
        SchedulerSettings settings = getSettings();

        settings.setSyncEnabled(syncEnabled);
        settings.setSyncTime(syncTime);
        settings.setTimeFormat(timeFormat);

        if (weekdays != null) {
            settings.setWeekdays(weekdays);
        }
        if (allWeekdays != null) {
            settings.setAllWeekdays(allWeekdays);
        }

        settings.setNextSyncRun(calculateNextRun(settings));

        settings = schedulerSettingsRepository.save(settings);
        log.info("Scheduler settings updated: enabled={}, time={}, format={}, weekdays={}, allWeekdays={}",
            syncEnabled, syncTime, timeFormat, settings.getWeekdays(), settings.getAllWeekdays());

        // Reschedule task with new settings
        rescheduleTask();

        return settings;
    }

    /**
     * Update only the time format setting (immediate update)
     *
     * @param timeFormat display format (12h or 24h)
     * @return updated settings
     */
    @Transactional
    public SchedulerSettings updateTimeFormat(String timeFormat) {
        SchedulerSettings settings = getSettings();
        settings.setTimeFormat(timeFormat);
        settings = schedulerSettingsRepository.save(settings);
        log.info("Time format updated to: {}", timeFormat);
        return settings;
    }

    /**
     * Check every hour if it's time to run the scheduled sync
     */
    @Scheduled(cron = "0 * * * * *") // Run every minute to check
    public void checkAndRunScheduledSync() {
        SchedulerSettings settings = getSettings();

        if (!settings.getSyncEnabled()) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        LocalTime scheduledTime = LocalTime.parse(settings.getSyncTime(), TIME_FORMATTER);
        LocalTime currentTime = now.toLocalTime();

        // Check if current minute matches scheduled time (with 1-minute window)
        if (currentTime.getHour() == scheduledTime.getHour() &&
            currentTime.getMinute() == scheduledTime.getMinute()) {

            // Check weekday restriction
            String todayCode = now.getDayOfWeek().toString().substring(0, 3); // MON, TUE, etc.
            if (!settings.shouldRunOnWeekday(todayCode)) {
                log.debug("Skipping sync - not scheduled for {}", todayCode);
                return;
            }

            // Check if we haven't run today yet
            if (settings.getLastSyncRun() == null ||
                !settings.getLastSyncRun().toLocalDate().equals(now.toLocalDate())) {

                log.info("=".repeat(80));
                log.info("SCHEDULED SYNC TRIGGERED - Running comprehensive sync");
                log.info("=".repeat(80));

                runScheduledSync();
            }
        }
    }

    /**
     * Execute the scheduled sync
     */
    @Transactional
    public void runScheduledSync() {
        try {
            SchedulerSettings settings = getSettings();

            // Update last run time
            settings.setLastSyncRun(LocalDateTime.now());
            settings.setNextSyncRun(calculateNextRun(settings.getSyncTime()));
            schedulerSettingsRepository.save(settings);

            // Execute comprehensive sync
            log.info("Starting scheduled comprehensive sync...");
            ComprehensiveSyncService.ComprehensiveSyncResult result = comprehensiveSyncService.syncAll();

            if (result.isSuccess()) {
                log.info("Scheduled sync completed successfully!");
            } else {
                log.warn("Scheduled sync completed with errors: {}", result.getSummaryMessage());
            }

        } catch (Exception e) {
            log.error("Error during scheduled sync", e);
        }
    }

    /**
     * Format time for display based on user preference
     *
     * @param time24h time in HH:mm format
     * @param format "12h" or "24h"
     * @return formatted time string
     */
    public String formatTimeForDisplay(String time24h, String format) {
        if ("12h".equals(format)) {
            LocalTime time = LocalTime.parse(time24h, TIME_FORMATTER);
            int hour = time.getHour();
            int minute = time.getMinute();
            String amPm = hour >= 12 ? "PM" : "AM";
            int hour12 = hour % 12;
            if (hour12 == 0) hour12 = 12;
            return String.format("%02d:%02d %s", hour12, minute, amPm);
        }
        return time24h;
    }

    /**
     * Calculate next run time based on sync time
     */
    private LocalDateTime calculateNextRun(String syncTime) {
        SchedulerSettings settings = getSettings();
        return calculateNextRun(settings);
    }

    /**
     * Calculate next run time based on settings (including weekday restrictions)
     */
    private LocalDateTime calculateNextRun(SchedulerSettings settings) {
        LocalTime time = LocalTime.parse(settings.getSyncTime(), TIME_FORMATTER);
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime nextRun = LocalDateTime.of(LocalDate.now(), time);

        // If time has passed today, start checking from tomorrow
        if (nextRun.isBefore(now) || nextRun.isEqual(now)) {
            nextRun = nextRun.plusDays(1);
        }

        // Find next allowed weekday
        if (!Boolean.TRUE.equals(settings.getAllWeekdays())) {
            Set<String> allowedDays = settings.getWeekdaySet();
            if (!allowedDays.isEmpty()) {
                int attempts = 0;
                while (attempts < 7) {
                    String dayCode = nextRun.getDayOfWeek().toString().substring(0, 3);
                    if (allowedDays.contains(dayCode)) {
                        break;
                    }
                    nextRun = nextRun.plusDays(1);
                    attempts++;
                }
            }
        }

        return nextRun;
    }

    /**
     * Reschedule the task with current settings
     */
    private void rescheduleTask() {
        // Cancel existing task if any
        if (scheduledTask != null && !scheduledTask.isCancelled()) {
            scheduledTask.cancel(false);
            log.debug("Cancelled existing scheduled task");
        }

        SchedulerSettings settings = getSettings();

        if (!settings.getSyncEnabled()) {
            log.info("Scheduled sync is disabled");
            return;
        }

        // Create cron expression for daily execution at specified time
        LocalTime time = LocalTime.parse(settings.getSyncTime(), TIME_FORMATTER);
        String cronExpression = String.format("0 %d %d * * *", time.getMinute(), time.getHour());

        log.info("Scheduling sync for: {} (cron: {})", settings.getSyncTime(), cronExpression);
    }

    /**
     * Create default settings if none exist
     */
    private SchedulerSettings createDefaultSettings() {
        SchedulerSettings settings = SchedulerSettings.builder()
            .syncEnabled(true)
            .syncTime("03:00")
            .timeFormat("24h")
            .nextSyncRun(calculateNextRun("03:00"))
            .build();

        return schedulerSettingsRepository.save(settings);
    }
}
