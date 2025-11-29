package com.spring.mcp.model.enums;

/**
 * Status of a language feature in its lifecycle.
 *
 * @author Spring MCP Server
 * @version 1.2.0
 * @since 2025-11-29
 */
public enum FeatureStatus {
    /**
     * New feature introduced in this version
     */
    NEW("New", "success", "bi-plus-circle"),

    /**
     * Feature marked as deprecated, scheduled for future removal
     */
    DEPRECATED("Deprecated", "warning", "bi-exclamation-triangle"),

    /**
     * Feature has been removed from the language
     */
    REMOVED("Removed", "danger", "bi-x-circle"),

    /**
     * Preview feature (Java) - not yet stable
     */
    PREVIEW("Preview", "info", "bi-eye"),

    /**
     * Incubating feature (Java) - experimental API
     */
    INCUBATING("Incubating", "secondary", "bi-flask");

    private final String displayName;
    private final String badgeClass;
    private final String iconClass;

    FeatureStatus(String displayName, String badgeClass, String iconClass) {
        this.displayName = displayName;
        this.badgeClass = badgeClass;
        this.iconClass = iconClass;
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
     * Bootstrap icon class for UI.
     */
    public String getIconClass() {
        return iconClass;
    }

    /**
     * Parse status from string (case-insensitive).
     */
    public static FeatureStatus fromString(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return FeatureStatus.valueOf(value.toUpperCase().trim());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
