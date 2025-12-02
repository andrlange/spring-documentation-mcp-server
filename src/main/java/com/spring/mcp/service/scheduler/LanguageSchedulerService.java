package com.spring.mcp.service.scheduler;

import com.spring.mcp.model.entity.LanguageSchedulerSettings;
import com.spring.mcp.model.enums.SyncFrequency;
import com.spring.mcp.repository.LanguageSchedulerSettingsRepository;
import com.spring.mcp.service.language.LanguageSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.PostConstruct;
import java.time.*;
import java.time.format.DateTimeFormatter;

import org.springframework.context.annotation.Profile;

/**
 * Service for managing scheduled automatic language synchronizations.
 * Supports daily, weekly, and monthly sync frequencies with configurable times.
 *
 * @author Spring MCP Server
 * @version 1.2.0
 * @since 2025-11-29
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Profile("!test")
public class LanguageSchedulerService {

    private final LanguageSchedulerSettingsRepository settingsRepository;
    private final LanguageSyncService languageSyncService;

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    /**
     * Initialize scheduler on application startup
     */
    @PostConstruct
    public void init() {
        log.info("Initializing language scheduler service...");
        LanguageSchedulerSettings settings = getSettings();
        log.info("Language scheduler: enabled={}, frequency={}, time={}",
                settings.getSyncEnabled(), settings.getFrequency(), settings.getSyncTime());

        if (settings.getSyncEnabled()) {
            LocalDateTime nextRun = calculateNextRun(settings);
            log.info("Next language sync scheduled for: {}", nextRun);
        }
    }

    /**
     * Get current scheduler settings
     *
     * @return LanguageSchedulerSettings
     */
    public LanguageSchedulerSettings getSettings() {
        return settingsRepository.findFirstByOrderByIdAsc()
                .orElseGet(this::createDefaultSettings);
    }

    /**
     * Update scheduler settings
     *
     * @param syncEnabled whether sync is enabled
     * @param frequency sync frequency (DAILY, WEEKLY, MONTHLY)
     * @param syncTime time in HH:mm format
     * @param weekdays comma-separated weekday codes (MON,TUE, etc.) for WEEKLY
     * @param dayOfMonth day of month for monthly syncs (1-31)
     * @param timeFormat display format (12h or 24h)
     * @return updated settings
     */
    @Transactional
    public LanguageSchedulerSettings updateSettings(
            Boolean syncEnabled,
            SyncFrequency frequency,
            String syncTime,
            String weekdays,
            Integer dayOfMonth,
            String timeFormat) {

        LanguageSchedulerSettings settings = getSettings();

        settings.setSyncEnabled(syncEnabled);
        settings.setFrequency(frequency);
        settings.setSyncTime(syncTime);
        if (weekdays != null) {
            settings.setWeekdays(weekdays);
            settings.setAllWeekdays("MON,TUE,WED,THU,FRI,SAT,SUN".equals(weekdays));
        }
        if (dayOfMonth != null) {
            settings.setDayOfMonth(dayOfMonth);
        }
        settings.setTimeFormat(timeFormat);
        settings.setNextSyncRun(calculateNextRun(settings));

        settings = settingsRepository.save(settings);
        log.info("Language scheduler settings updated: enabled={}, frequency={}, time={}",
                syncEnabled, frequency, syncTime);

        return settings;
    }

    /**
     * Update only the time format setting
     *
     * @param timeFormat display format (12h or 24h)
     * @return updated settings
     */
    @Transactional
    public LanguageSchedulerSettings updateTimeFormat(String timeFormat) {
        LanguageSchedulerSettings settings = getSettings();
        settings.setTimeFormat(timeFormat);
        settings = settingsRepository.save(settings);
        log.info("Language scheduler time format updated to: {}", timeFormat);
        return settings;
    }

    /**
     * Check every minute if it's time to run the scheduled sync
     */
    @Scheduled(cron = "0 * * * * *") // Run every minute
    public void checkAndRunScheduledSync() {
        LanguageSchedulerSettings settings = getSettings();

        if (!settings.getSyncEnabled()) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        LocalTime scheduledTime = LocalTime.parse(settings.getSyncTime(), TIME_FORMATTER);
        LocalTime currentTime = now.toLocalTime();

        // Check if current minute matches scheduled time
        if (currentTime.getHour() != scheduledTime.getHour() ||
            currentTime.getMinute() != scheduledTime.getMinute()) {
            return;
        }

        // Check if we should run based on frequency
        if (!shouldRunToday(settings, now.toLocalDate())) {
            return;
        }

        // Check if we haven't run today yet
        if (settings.getLastSyncRun() != null &&
            settings.getLastSyncRun().toLocalDate().equals(now.toLocalDate())) {
            return;
        }

        log.info("=".repeat(80));
        log.info("LANGUAGE SYNC TRIGGERED - Running language evolution sync");
        log.info("=".repeat(80));

        runScheduledSync();
    }

    /**
     * Execute the scheduled sync
     */
    @Transactional
    public void runScheduledSync() {
        try {
            LanguageSchedulerSettings settings = getSettings();

            // Update last run time
            settings.setLastSyncRun(LocalDateTime.now());
            settings.setNextSyncRun(calculateNextRun(settings));
            settingsRepository.save(settings);

            // Execute language sync
            log.info("Starting scheduled language sync...");
            LanguageSyncService.SyncResult result = languageSyncService.syncAll();

            if (result.isSuccess()) {
                log.info("Scheduled language sync completed successfully!");
            } else {
                log.warn("Scheduled language sync completed with errors: {}", result.getErrorMessage());
            }

        } catch (Exception e) {
            log.error("Error during scheduled language sync", e);
        }
    }

    /**
     * Manually trigger a sync
     *
     * @return sync result
     */
    @Transactional
    public LanguageSyncService.SyncResult triggerManualSync() {
        log.info("Manual language sync triggered");

        LanguageSchedulerSettings settings = getSettings();
        settings.setLastSyncRun(LocalDateTime.now());
        settings.setNextSyncRun(calculateNextRun(settings));
        settingsRepository.save(settings);

        return languageSyncService.syncAll();
    }

    /**
     * Check if sync should run today based on frequency settings
     *
     * @param settings the scheduler settings
     * @param today today's date
     * @return true if sync should run today
     */
    private boolean shouldRunToday(LanguageSchedulerSettings settings, LocalDate today) {
        switch (settings.getFrequency()) {
            case DAILY:
                return true;

            case WEEKLY:
                // Check if today's weekday is in the allowed weekdays
                String todayCode = today.getDayOfWeek().toString().substring(0, 3); // MON, TUE, etc.
                return settings.shouldRunOnWeekday(todayCode);

            case MONTHLY:
                // Use effective day to handle months with fewer days
                int effectiveDay = settings.getEffectiveDay(YearMonth.from(today));
                return today.getDayOfMonth() == effectiveDay;

            default:
                return false;
        }
    }

    /**
     * Calculate next run time based on settings
     *
     * @param settings the scheduler settings
     * @return next scheduled run time
     */
    private LocalDateTime calculateNextRun(LanguageSchedulerSettings settings) {
        if (!settings.getSyncEnabled()) {
            return null;
        }

        LocalTime time = LocalTime.parse(settings.getSyncTime(), TIME_FORMATTER);
        LocalDate today = LocalDate.now();
        LocalDateTime now = LocalDateTime.now();

        switch (settings.getFrequency()) {
            case DAILY:
                LocalDateTime nextDaily = LocalDateTime.of(today, time);
                if (nextDaily.isBefore(now) || nextDaily.isEqual(now)) {
                    nextDaily = nextDaily.plusDays(1);
                }
                return nextDaily;

            case WEEKLY:
                return calculateNextWeeklyRun(settings, today, time, now);

            case MONTHLY:
                return calculateNextMonthlyRun(settings, today, time, now);

            default:
                return null;
        }
    }

    /**
     * Calculate next weekly run time
     */
    private LocalDateTime calculateNextWeeklyRun(
            LanguageSchedulerSettings settings,
            LocalDate today,
            LocalTime time,
            LocalDateTime now) {

        // Find next allowed weekday
        LocalDate nextDate = today;
        int attempts = 0;

        while (attempts < 7) {
            String dayCode = nextDate.getDayOfWeek().toString().substring(0, 3);
            LocalDateTime candidateRun = LocalDateTime.of(nextDate, time);

            if (settings.shouldRunOnWeekday(dayCode)) {
                // If it's today but time has passed, skip to next occurrence
                if (nextDate.equals(today) && (candidateRun.isBefore(now) || candidateRun.isEqual(now))) {
                    nextDate = nextDate.plusDays(1);
                    attempts++;
                    continue;
                }
                return candidateRun;
            }

            nextDate = nextDate.plusDays(1);
            attempts++;
        }

        // Fallback - shouldn't happen if at least one weekday is selected
        return LocalDateTime.of(today.plusDays(7), time);
    }

    /**
     * Calculate next monthly run time with last-day fallback
     */
    private LocalDateTime calculateNextMonthlyRun(
            LanguageSchedulerSettings settings,
            LocalDate today,
            LocalTime time,
            LocalDateTime now) {

        YearMonth thisMonth = YearMonth.from(today);
        int effectiveDay = settings.getEffectiveDay(thisMonth);

        LocalDate targetDate = thisMonth.atDay(effectiveDay);
        LocalDateTime nextRun = LocalDateTime.of(targetDate, time);

        // If the date has passed this month, schedule for next month
        if (nextRun.isBefore(now) || nextRun.isEqual(now)) {
            YearMonth nextMonth = thisMonth.plusMonths(1);
            effectiveDay = settings.getEffectiveDay(nextMonth);
            targetDate = nextMonth.atDay(effectiveDay);
            nextRun = LocalDateTime.of(targetDate, time);
        }

        return nextRun;
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
     * Get day of week name
     *
     * @param dayOfWeek day number (1-7)
     * @return day name
     */
    public String getDayOfWeekName(int dayOfWeek) {
        return DayOfWeek.of(dayOfWeek).toString();
    }

    /**
     * Create default settings if none exist
     */
    private LanguageSchedulerSettings createDefaultSettings() {
        LanguageSchedulerSettings settings = LanguageSchedulerSettings.builder()
                .syncEnabled(false) // Disabled by default
                .frequency(SyncFrequency.WEEKLY)
                .syncTime("04:00")
                .weekdays("MON") // Monday only by default
                .allWeekdays(false)
                .dayOfMonth(1) // First of month
                .timeFormat("24h")
                .build();

        // Don't calculate next run if disabled
        settings = settingsRepository.save(settings);
        log.info("Created default language scheduler settings (disabled)");
        return settings;
    }
}
