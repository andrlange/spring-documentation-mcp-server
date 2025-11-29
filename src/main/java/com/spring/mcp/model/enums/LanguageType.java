package com.spring.mcp.model.enums;

/**
 * Supported programming languages for language evolution tracking.
 *
 * @author Spring MCP Server
 * @version 1.2.0
 * @since 2025-11-29
 */
public enum LanguageType {
    JAVA("Java"),
    KOTLIN("Kotlin");

    private final String displayName;

    LanguageType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * Parse language type from string (case-insensitive).
     *
     * @param value the string value
     * @return the LanguageType or null if not found
     */
    public static LanguageType fromString(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LanguageType.valueOf(value.toUpperCase().trim());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
