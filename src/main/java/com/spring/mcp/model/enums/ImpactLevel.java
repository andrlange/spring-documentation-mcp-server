package com.spring.mcp.model.enums;

/**
 * Impact level of a language feature change on existing code.
 *
 * @author Spring MCP Server
 * @version 1.2.0
 * @since 2025-11-29
 */
public enum ImpactLevel {
    /**
     * High impact - breaking changes, significant code modifications required
     */
    HIGH("High", "danger", 3),

    /**
     * Medium impact - notable changes but manageable migration
     */
    MEDIUM("Medium", "warning", 2),

    /**
     * Low impact - minor changes, easy adoption
     */
    LOW("Low", "success", 1);

    private final String displayName;
    private final String badgeClass;
    private final int priority;

    ImpactLevel(String displayName, String badgeClass, int priority) {
        this.displayName = displayName;
        this.badgeClass = badgeClass;
        this.priority = priority;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * Bootstrap badge class for UI styling.
     */
    public String getBadgeClass() {
        return badgeClass;
    }

    /**
     * Priority for sorting (higher = more impactful).
     */
    public int getPriority() {
        return priority;
    }

    /**
     * Parse impact level from string (case-insensitive).
     */
    public static ImpactLevel fromString(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return ImpactLevel.valueOf(value.toUpperCase().trim());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
