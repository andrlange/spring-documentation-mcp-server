package com.spring.mcp.model.enums;

/**
 * Types of metrics tracked for MCP monitoring.
 *
 * @author Spring MCP Server
 * @version 1.4.4
 * @since 2025-12-16
 */
public enum MetricType {
    /**
     * Tool invocation metrics
     */
    TOOL_CALLS("Tool Calls", "primary", "bi-tools"),

    /**
     * Connection-related metrics
     */
    CONNECTION("Connections", "info", "bi-plug"),

    /**
     * Error metrics
     */
    ERROR("Errors", "danger", "bi-exclamation-circle"),

    /**
     * Latency/timing metrics
     */
    LATENCY("Latency", "warning", "bi-speedometer2"),

    /**
     * Throughput metrics (requests per second)
     */
    THROUGHPUT("Throughput", "success", "bi-graph-up");

    private final String displayName;
    private final String badgeClass;
    private final String iconClass;

    MetricType(String displayName, String badgeClass, String iconClass) {
        this.displayName = displayName;
        this.badgeClass = badgeClass;
        this.iconClass = iconClass;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getBadgeClass() {
        return badgeClass;
    }

    public String getIconClass() {
        return iconClass;
    }

    /**
     * Parse metric type from string (case-insensitive).
     */
    public static MetricType fromString(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return MetricType.valueOf(value.toUpperCase().trim().replace("-", "_"));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
