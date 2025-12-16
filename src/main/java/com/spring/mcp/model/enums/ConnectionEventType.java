package com.spring.mcp.model.enums;

/**
 * Types of MCP connection events for monitoring.
 *
 * @author Spring MCP Server
 * @version 1.4.4
 * @since 2025-12-16
 */
public enum ConnectionEventType {
    /**
     * Client successfully connected
     */
    CONNECTED("Connected", "success", "bi-plug"),

    /**
     * Client disconnected gracefully
     */
    DISCONNECTED("Disconnected", "secondary", "bi-plug"),

    /**
     * Connection error occurred
     */
    ERROR("Error", "danger", "bi-exclamation-triangle"),

    /**
     * Heartbeat received from client
     */
    HEARTBEAT("Heartbeat", "info", "bi-heart-pulse"),

    /**
     * Connection timed out
     */
    TIMEOUT("Timeout", "warning", "bi-hourglass"),

    /**
     * Session initialized (after MCP handshake)
     */
    INITIALIZED("Initialized", "primary", "bi-check-circle"),

    /**
     * Reconnection attempt
     */
    RECONNECT("Reconnect", "info", "bi-arrow-repeat");

    private final String displayName;
    private final String badgeClass;
    private final String iconClass;

    ConnectionEventType(String displayName, String badgeClass, String iconClass) {
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
     * Check if this is a positive event (connected, initialized, heartbeat)
     */
    public boolean isPositive() {
        return this == CONNECTED || this == INITIALIZED || this == HEARTBEAT;
    }

    /**
     * Check if this is a negative event (error, timeout, disconnected)
     */
    public boolean isNegative() {
        return this == ERROR || this == TIMEOUT;
    }

    /**
     * Parse event type from string (case-insensitive).
     */
    public static ConnectionEventType fromString(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return ConnectionEventType.valueOf(value.toUpperCase().trim());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
