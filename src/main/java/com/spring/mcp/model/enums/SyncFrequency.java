package com.spring.mcp.model.enums;

/**
 * Frequency options for scheduled language evolution sync.
 *
 * @author Spring MCP Server
 * @version 1.2.0
 * @since 2025-11-29
 */
public enum SyncFrequency {
    /**
     * Run every day at the specified time
     */
    DAILY("Daily"),

    /**
     * Run on selected weekdays at the specified time
     */
    WEEKLY("Weekly"),

    /**
     * Run on selected day of month in selected months
     */
    MONTHLY("Monthly");

    private final String displayName;

    SyncFrequency(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * Parse frequency from string (case-insensitive).
     */
    public static SyncFrequency fromString(String value) {
        if (value == null || value.isBlank()) {
            return WEEKLY; // default
        }
        try {
            return SyncFrequency.valueOf(value.toUpperCase().trim());
        } catch (IllegalArgumentException e) {
            return WEEKLY; // default
        }
    }
}
