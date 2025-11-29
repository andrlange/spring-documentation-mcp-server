package com.spring.mcp.model.entity;

import com.spring.mcp.model.enums.SyncFrequency;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Entity representing scheduler settings for language evolution sync.
 * Supports daily, weekly, and monthly scheduling with flexible configuration.
 *
 * @author Spring MCP Server
 * @version 1.2.0
 * @since 2025-11-29
 */
@Entity
@Table(name = "language_scheduler_settings")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LanguageSchedulerSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Whether automatic language sync is enabled
     */
    @Column(name = "sync_enabled", nullable = false)
    @Builder.Default
    private Boolean syncEnabled = false;

    /**
     * Time to run sync in HH:mm 24-hour format (e.g., "04:00")
     */
    @Column(name = "sync_time", nullable = false, length = 5)
    @Builder.Default
    private String syncTime = "04:00";

    /**
     * Display format preference: "12h" or "24h"
     */
    @Column(name = "time_format", nullable = false, length = 3)
    @Builder.Default
    private String timeFormat = "24h";

    /**
     * Sync frequency: DAILY, WEEKLY, MONTHLY
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "frequency", nullable = false, length = 10)
    @Builder.Default
    private SyncFrequency frequency = SyncFrequency.WEEKLY;

    /**
     * Comma-separated weekday codes for WEEKLY frequency (MON,TUE,WED,THU,FRI,SAT,SUN)
     */
    @Column(name = "weekdays", length = 30)
    @Builder.Default
    private String weekdays = "MON,TUE,WED,THU,FRI,SAT,SUN";

    /**
     * Whether all weekdays are selected
     */
    @Column(name = "all_weekdays")
    @Builder.Default
    private Boolean allWeekdays = true;

    /**
     * Day of month for MONTHLY frequency (1-31)
     */
    @Column(name = "day_of_month")
    @Builder.Default
    private Integer dayOfMonth = 1;

    /**
     * Comma-separated month codes for MONTHLY frequency (JAN,FEB,MAR,APR,MAY,JUN,JUL,AUG,SEP,OCT,NOV,DEC)
     */
    @Column(name = "months", length = 50)
    @Builder.Default
    private String months = "JAN,FEB,MAR,APR,MAY,JUN,JUL,AUG,SEP,OCT,NOV,DEC";

    /**
     * Whether all months are selected
     */
    @Column(name = "all_months")
    @Builder.Default
    private Boolean allMonths = true;

    /**
     * Timestamp of last automatic sync execution
     */
    @Column(name = "last_sync_run")
    private LocalDateTime lastSyncRun;

    /**
     * Calculated timestamp for next scheduled sync
     */
    @Column(name = "next_sync_run")
    private LocalDateTime nextSyncRun;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * Get selected weekdays as a Set
     */
    public Set<String> getWeekdaySet() {
        if (weekdays == null || weekdays.isBlank()) {
            return Set.of();
        }
        return Arrays.stream(weekdays.split(","))
                .map(String::trim)
                .collect(Collectors.toSet());
    }

    /**
     * Get selected months as a Set
     */
    public Set<String> getMonthSet() {
        if (months == null || months.isBlank()) {
            return Set.of();
        }
        return Arrays.stream(months.split(","))
                .map(String::trim)
                .collect(Collectors.toSet());
    }

    /**
     * Get effective day of month for a specific year-month.
     * Implements "last day fallback" for months with fewer days.
     *
     * @param yearMonth the target month
     * @return the effective day (capped at month's last day)
     */
    public int getEffectiveDay(YearMonth yearMonth) {
        int lastDay = yearMonth.lengthOfMonth();
        return Math.min(dayOfMonth, lastDay);
    }

    /**
     * Check if a specific weekday should run
     *
     * @param weekday weekday code (MON, TUE, etc.)
     * @return true if sync should run on this day
     */
    public boolean shouldRunOnWeekday(String weekday) {
        if (Boolean.TRUE.equals(allWeekdays)) {
            return true;
        }
        return getWeekdaySet().contains(weekday.toUpperCase());
    }

    /**
     * Check if a specific month should run
     *
     * @param month month code (JAN, FEB, etc.)
     * @return true if sync should run in this month
     */
    public boolean shouldRunInMonth(String month) {
        if (Boolean.TRUE.equals(allMonths)) {
            return true;
        }
        return getMonthSet().contains(month.toUpperCase());
    }

    /**
     * Get display string for schedule
     */
    public String getScheduleDescription() {
        StringBuilder sb = new StringBuilder();
        sb.append(frequency.getDisplayName());

        switch (frequency) {
            case DAILY:
                sb.append(" at ").append(syncTime);
                break;
            case WEEKLY:
                sb.append(" on ");
                if (Boolean.TRUE.equals(allWeekdays)) {
                    sb.append("all days");
                } else {
                    sb.append(weekdays);
                }
                sb.append(" at ").append(syncTime);
                break;
            case MONTHLY:
                sb.append(" on day ").append(dayOfMonth);
                if (!Boolean.TRUE.equals(allMonths)) {
                    sb.append(" in ").append(months);
                }
                sb.append(" at ").append(syncTime);
                break;
        }

        return sb.toString();
    }
}
