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
 * Entity representing scheduler settings for automatic synchronization.
 * Controls when and how automatic comprehensive syncs are executed.
 *
 * @author Spring MCP Server
 * @version 1.0
 * @since 2025-01-12
 */
@Entity
@Table(name = "scheduler_settings")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SchedulerSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Whether automatic synchronization is enabled
     */
    @Column(name = "sync_enabled", nullable = false)
    private Boolean syncEnabled;

    /**
     * Time to run sync in HH:mm 24-hour format (e.g., "03:00")
     */
    @Column(name = "sync_time", nullable = false, length = 5)
    private String syncTime;

    /**
     * Display format preference: "12h" or "24h"
     */
    @Column(name = "time_format", nullable = false, length = 3)
    private String timeFormat;

    /**
     * Sync frequency: DAILY, WEEKLY, MONTHLY
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "frequency", nullable = false, length = 10)
    @Builder.Default
    private SyncFrequency frequency = SyncFrequency.DAILY;

    /**
     * Day of month for MONTHLY frequency (1-31)
     */
    @Column(name = "day_of_month")
    @Builder.Default
    private Integer dayOfMonth = 1;

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

    /**
     * Comma-separated weekday codes (MON,TUE,WED,THU,FRI,SAT,SUN)
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
     * Check if sync should run on a specific weekday
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
     * Get effective day of month for a specific year-month.
     * Implements "last day fallback" for months with fewer days.
     *
     * @param yearMonth the target month
     * @return the effective day (capped at month's last day)
     */
    public int getEffectiveDay(YearMonth yearMonth) {
        int lastDay = yearMonth.lengthOfMonth();
        return Math.min(dayOfMonth != null ? dayOfMonth : 1, lastDay);
    }

    /**
     * Get display string for schedule
     */
    public String getScheduleDescription() {
        StringBuilder sb = new StringBuilder();
        sb.append(frequency != null ? frequency.getDisplayName() : "Daily");

        switch (frequency != null ? frequency : SyncFrequency.DAILY) {
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
                sb.append(" on day ").append(dayOfMonth != null ? dayOfMonth : 1);
                sb.append(" at ").append(syncTime);
                break;
        }

        return sb.toString();
    }
}
