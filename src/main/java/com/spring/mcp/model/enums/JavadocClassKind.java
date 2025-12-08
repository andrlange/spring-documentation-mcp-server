package com.spring.mcp.model.enums;

/**
 * Enumeration of Javadoc class types.
 * Represents the different kinds of Java types that can be documented.
 */
public enum JavadocClassKind {
    /**
     * Regular Java class
     */
    CLASS("Class"),

    /**
     * Java interface
     */
    INTERFACE("Interface"),

    /**
     * Java enumeration
     */
    ENUM("Enum"),

    /**
     * Java annotation type
     */
    ANNOTATION("Annotation"),

    /**
     * Java record (Java 14+)
     */
    RECORD("Record");

    private final String displayName;

    JavadocClassKind(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * Parse a kind from a string (case-insensitive).
     * @param value the string value to parse
     * @return the corresponding JavadocClassKind, or CLASS as default
     */
    public static JavadocClassKind fromString(String value) {
        if (value == null || value.isBlank()) {
            return CLASS;
        }
        String normalized = value.trim().toUpperCase();
        // Handle common variations
        if (normalized.contains("INTERFACE")) {
            return INTERFACE;
        } else if (normalized.contains("ENUM")) {
            return ENUM;
        } else if (normalized.contains("ANNOTATION") || normalized.startsWith("@")) {
            return ANNOTATION;
        } else if (normalized.contains("RECORD")) {
            return RECORD;
        }
        return CLASS;
    }
}
