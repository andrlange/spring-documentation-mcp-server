package com.spring.mcp.service.javadoc.dto;

/**
 * DTO representing a method or constructor parameter from Javadoc.
 *
 * @param name        Parameter name
 * @param type        Parameter type (may include generics)
 * @param description Parameter description from @param tag
 */
public record MethodParameterDto(
        String name,
        String type,
        String description
) {
    /**
     * Create a parameter with just name and type (no description).
     */
    public static MethodParameterDto of(String name, String type) {
        return new MethodParameterDto(name, type, null);
    }

    /**
     * Create a parameter with all fields.
     */
    public static MethodParameterDto of(String name, String type, String description) {
        return new MethodParameterDto(name, type, description);
    }

    /**
     * Get a display string for this parameter.
     */
    public String toDisplayString() {
        return type + " " + name;
    }
}
