package com.spring.mcp.model.enums;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * Time bucket types for metrics aggregation.
 *
 * @author Spring MCP Server
 * @version 1.4.4
 * @since 2025-12-16
 */
public enum BucketType {
    /**
     * 5-minute aggregation bucket
     */
    FIVE_MIN("5 min", Duration.ofMinutes(5), "primary", "bi-clock"),

    /**
     * 1-hour aggregation bucket
     */
    ONE_HOUR("1 hour", Duration.ofHours(1), "info", "bi-clock-history"),

    /**
     * 24-hour aggregation bucket
     */
    TWENTY_FOUR_HOUR("24 hours", Duration.ofHours(24), "secondary", "bi-calendar");

    private final String displayName;
    private final Duration duration;
    private final String badgeClass;
    private final String iconClass;

    BucketType(String displayName, Duration duration, String badgeClass, String iconClass) {
        this.displayName = displayName;
        this.duration = duration;
        this.badgeClass = badgeClass;
        this.iconClass = iconClass;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Duration getDuration() {
        return duration;
    }

    public String getBadgeClass() {
        return badgeClass;
    }

    public String getIconClass() {
        return iconClass;
    }

    /**
     * Calculate the bucket start time for a given timestamp.
     */
    public LocalDateTime getBucketStart(LocalDateTime timestamp) {
        return switch (this) {
            case FIVE_MIN -> timestamp.truncatedTo(ChronoUnit.HOURS)
                    .plusMinutes((timestamp.getMinute() / 5) * 5);
            case ONE_HOUR -> timestamp.truncatedTo(ChronoUnit.HOURS);
            case TWENTY_FOUR_HOUR -> timestamp.truncatedTo(ChronoUnit.DAYS);
        };
    }

    /**
     * Calculate the bucket end time for a given bucket start.
     */
    public LocalDateTime getBucketEnd(LocalDateTime bucketStart) {
        return bucketStart.plus(duration);
    }

    /**
     * Get the lookback time for displaying metrics of this bucket type.
     */
    public LocalDateTime getLookbackTime() {
        return LocalDateTime.now().minus(duration);
    }

    /**
     * Parse bucket type from string (case-insensitive).
     */
    public static BucketType fromString(String value) {
        if (value == null || value.isBlank()) {
            return FIVE_MIN;
        }
        String normalized = value.toUpperCase().trim()
                .replace("-", "_")
                .replace(" ", "_");
        return switch (normalized) {
            case "5MIN", "5_MIN", "FIVE_MIN", "5M" -> FIVE_MIN;
            case "1HOUR", "1_HOUR", "ONE_HOUR", "1H" -> ONE_HOUR;
            case "24HOUR", "24_HOUR", "TWENTY_FOUR_HOUR", "24H" -> TWENTY_FOUR_HOUR;
            default -> {
                try {
                    yield BucketType.valueOf(normalized);
                } catch (IllegalArgumentException e) {
                    yield FIVE_MIN;
                }
            }
        };
    }
}
