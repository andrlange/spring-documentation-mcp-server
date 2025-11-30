package com.spring.mcp.model.enums;

/**
 * Category classification for Flavors (company guidelines, patterns, rules).
 *
 * @author Spring MCP Server
 * @version 1.2.0
 * @since 2025-11-30
 */
public enum FlavorCategory {
    ARCHITECTURE("Architecture", "bi-building", "purple"),
    COMPLIANCE("Compliance", "bi-shield-check", "pink"),
    AGENTS("Agents / Subagent", "bi-robot", "cyan"),
    INITIALIZATION("Initialization", "bi-rocket-takeoff", "green"),
    GENERAL("General Flavor", "bi-bookmark", "amber");

    private final String displayName;
    private final String iconClass;
    private final String colorClass;

    FlavorCategory(String displayName, String iconClass, String colorClass) {
        this.displayName = displayName;
        this.iconClass = iconClass;
        this.colorClass = colorClass;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getIconClass() {
        return iconClass;
    }

    public String getColorClass() {
        return colorClass;
    }

    /**
     * Parse category from string (case-insensitive).
     *
     * @param value the string value
     * @return the FlavorCategory or null if not found
     */
    public static FlavorCategory fromString(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return FlavorCategory.valueOf(value.toUpperCase().trim());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
