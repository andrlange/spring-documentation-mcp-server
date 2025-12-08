package com.spring.mcp.service.scheduler;

import com.spring.mcp.model.entity.SchedulerSettings;
import com.spring.mcp.model.enums.SyncFrequency;
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
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Set;
import java.util.concurrent.ScheduledFuture;

import org.springframework.context.annotation.Profile;

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
@Profile("!test")
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
        return updateSettings(syncEnabled, syncTime, timeFormat, weekdays, allWeekdays, null, null);
    }

    /**
     * Update scheduler settings with full frequency support and reschedule task
     *
     * @param syncEnabled whether sync is enabled
     * @param syncTime time in HH:mm format
     * @param timeFormat display format (12h or 24h)
     * @param weekdays comma-separated weekday codes (MON,TUE,WED,THU,FRI,SAT,SUN)
     * @param allWeekdays whether all weekdays are selected
     * @param frequency sync frequency (DAILY, WEEKLY, MONTHLY)
     * @param dayOfMonth day of month for MONTHLY frequency (1-31)
     * @return updated settings
     */
    @Transactional
    public SchedulerSettings updateSettings(Boolean syncEnabled, String syncTime, String timeFormat,
                                           String weekdays, Boolean allWeekdays,
                                           SyncFrequency frequency, Integer dayOfMonth) {
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
        if (frequency != null) {
            settings.setFrequency(frequency);
        }
        if (dayOfMonth != null) {
            settings.setDayOfMonth(dayOfMonth);
        }

        settings.setNextSyncRun(calculateNextRun(settings));

        settings = schedulerSettingsRepository.save(settings);
        log.info("Scheduler settings updated: enabled={}, time={}, format={}, frequency={}, weekdays={}, allWeekdays={}, dayOfMonth={}",
            syncEnabled, syncTime, timeFormat, settings.getFrequency(), settings.getWeekdays(),
            settings.getAllWeekdays(), settings.getDayOfMonth());

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
     * Check every minute if it's time to run the scheduled sync
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

            // Check frequency-based restrictions
            SyncFrequency frequency = settings.getFrequency() != null ? settings.getFrequency() : SyncFrequency.DAILY;

            switch (frequency) {
                case DAILY:
                    // For DAILY, run every day at the scheduled time
                    break;

                case WEEKLY:
                    // For WEEKLY, check weekday restriction
                    String todayCode = now.getDayOfWeek().toString().substring(0, 3); // MON, TUE, etc.
                    if (!settings.shouldRunOnWeekday(todayCode)) {
                        log.debug("Skipping sync - not scheduled for {} (weekly mode)", todayCode);
                        return;
                    }
                    break;

                case MONTHLY:
                    // For MONTHLY, check day of month with fallback
                    YearMonth currentMonth = YearMonth.from(now);
                    int effectiveDay = settings.getEffectiveDay(currentMonth);
                    if (now.getDayOfMonth() != effectiveDay) {
                        log.debug("Skipping sync - not scheduled for day {} (monthly mode, effective day: {})",
                            now.getDayOfMonth(), effectiveDay);
                        return;
                    }
                    break;
            }

            // Check if we haven't run in this period yet
            if (!hasRunInCurrentPeriod(settings, now)) {
                log.info("=".repeat(80));
                log.info("SCHEDULED SYNC TRIGGERED - Running comprehensive sync ({})", frequency);
                log.info("=".repeat(80));

                runScheduledSync();
            }
        }
    }

    /**
     * Check if sync has already run in the current scheduling period
     */
    private boolean hasRunInCurrentPeriod(SchedulerSettings settings, LocalDateTime now) {
        if (settings.getLastSyncRun() == null) {
            return false;
        }

        LocalDateTime lastRun = settings.getLastSyncRun();
        SyncFrequency frequency = settings.getFrequency() != null ? settings.getFrequency() : SyncFrequency.DAILY;

        switch (frequency) {
            case DAILY:
                // Check if we ran today
                return lastRun.toLocalDate().equals(now.toLocalDate());

            case WEEKLY:
                // Check if we ran on this exact day (same date)
                return lastRun.toLocalDate().equals(now.toLocalDate());

            case MONTHLY:
                // Check if we ran this month
                return YearMonth.from(lastRun).equals(YearMonth.from(now));

            default:
                return lastRun.toLocalDate().equals(now.toLocalDate());
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
     * Calculate next run time based on settings (including frequency and weekday restrictions)
     */
    private LocalDateTime calculateNextRun(SchedulerSettings settings) {
        LocalTime time = LocalTime.parse(settings.getSyncTime(), TIME_FORMATTER);
        LocalDateTime now = LocalDateTime.now();
        SyncFrequency frequency = settings.getFrequency() != null ? settings.getFrequency() : SyncFrequency.DAILY;

        switch (frequency) {
            case DAILY:
                return calculateNextDailyRun(time, now);

            case WEEKLY:
                return calculateNextWeeklyRun(time, now, settings);

            case MONTHLY:
                return calculateNextMonthlyRun(time, now, settings);

            default:
                return calculateNextDailyRun(time, now);
        }
    }

    /**
     * Calculate next daily run time
     */
    private LocalDateTime calculateNextDailyRun(LocalTime time, LocalDateTime now) {
        LocalDateTime nextRun = LocalDateTime.of(now.toLocalDate(), time);

        // If time has passed today, schedule for tomorrow
        if (nextRun.isBefore(now) || nextRun.isEqual(now)) {
            nextRun = nextRun.plusDays(1);
        }

        return nextRun;
    }

    /**
     * Calculate next weekly run time based on allowed weekdays
     */
    private LocalDateTime calculateNextWeeklyRun(LocalTime time, LocalDateTime now, SchedulerSettings settings) {
        LocalDateTime nextRun = LocalDateTime.of(now.toLocalDate(), time);

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
     * Calculate next monthly run time based on day of month with fallback
     */
    private LocalDateTime calculateNextMonthlyRun(LocalTime time, LocalDateTime now, SchedulerSettings settings) {
        int targetDay = settings.getDayOfMonth() != null ? settings.getDayOfMonth() : 1;
        YearMonth currentMonth = YearMonth.from(now);

        // Get effective day for current month (handles February, etc.)
        int effectiveDay = settings.getEffectiveDay(currentMonth);
        LocalDateTime nextRun = LocalDateTime.of(
            now.getYear(), now.getMonth(), effectiveDay, time.getHour(), time.getMinute()
        );

        // If the scheduled time has passed this month, move to next month
        if (nextRun.isBefore(now) || nextRun.isEqual(now)) {
            YearMonth nextMonth = currentMonth.plusMonths(1);
            effectiveDay = Math.min(targetDay, nextMonth.lengthOfMonth());
            nextRun = LocalDateTime.of(
                nextMonth.getYear(), nextMonth.getMonth(), effectiveDay, time.getHour(), time.getMinute()
            );
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
            .syncEnabled(false)  // Disabled by default
            .syncTime("03:00")
            .timeFormat("24h")
            .frequency(SyncFrequency.DAILY)
            .dayOfMonth(1)
            .weekdays("MON,TUE,WED,THU,FRI,SAT,SUN")
            .allWeekdays(true)
            .build();

        // Calculate next run after building (to avoid circular dependency)
        settings.setNextSyncRun(calculateNextRun(settings));

        return schedulerSettingsRepository.save(settings);
    }
}
