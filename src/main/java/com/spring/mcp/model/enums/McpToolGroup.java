package com.spring.mcp.model.enums;

/**
 * Group classification for MCP tools.
 * Used for organizing tools in the UI and for bulk enable/disable operations.
 *
 * @author Spring MCP Server
 * @version 1.6.2
 * @since 2026-01-03
 */
public enum McpToolGroup {
    DOCUMENTATION("Documentation", "bi-book", "blue", 10),
    MIGRATION("Migration", "bi-arrow-repeat", "orange", 7),
    LANGUAGE("Language Evolution", "bi-braces", "yellow", 7),
    FLAVORS("Flavors", "bi-palette", "purple", 8),
    FLAVOR_GROUPS("Flavor Groups", "bi-people", "violet", 3),
    INITIALIZR("Initializr", "bi-box-seam", "green", 5),
    JAVADOC("Javadoc", "bi-file-code", "cyan", 4);

    private final String displayName;
    private final String iconClass;
    private final String colorClass;
    private final int expectedCount;

    McpToolGroup(String displayName, String iconClass, String colorClass, int expectedCount) {
        this.displayName = displayName;
        this.iconClass = iconClass;
        this.colorClass = colorClass;
        this.expectedCount = expectedCount;
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

    public int getExpectedCount() {
        return expectedCount;
    }

    /**
     * Parse group from string (case-insensitive).
     *
     * @param value the string value
     * @return the McpToolGroup or null if not found
     */
    public static McpToolGroup fromString(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        String normalized = value.trim();

        // Try exact enum name match (case-insensitive)
        try {
            return McpToolGroup.valueOf(normalized.toUpperCase().replace(" ", "_"));
        } catch (IllegalArgumentException ignored) {
            // Continue to display name matching
        }

        // Try display name match (case-insensitive)
        for (McpToolGroup group : values()) {
            if (group.displayName.equalsIgnoreCase(normalized)) {
                return group;
            }
        }

        return null;
    }

    /**
     * Get the total expected tool count across all groups.
     *
     * @return total expected tools
     */
    public static int getTotalExpectedCount() {
        int total = 0;
        for (McpToolGroup group : values()) {
            total += group.expectedCount;
        }
        return total;
    }
}
