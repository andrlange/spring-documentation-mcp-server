package com.spring.mcp.service.javadoc.dto;

/**
 * DTO representing a parsed field from Javadoc.
 *
 * @param name          Field name
 * @param type          Field type
 * @param modifiers     Field modifiers (e.g., "public static final")
 * @param summary       Short description
 * @param deprecated    Whether the field is deprecated
 * @param constantValue Compile-time constant value if applicable
 */
public record ParsedFieldDto(
        String name,
        String type,
        String modifiers,
        String summary,
        boolean deprecated,
        String constantValue
) {
    /**
     * Create a builder for ParsedFieldDto.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Check if this field is static.
     */
    public boolean isStatic() {
        return modifiers != null && modifiers.contains("static");
    }

    /**
     * Check if this field is final.
     */
    public boolean isFinal() {
        return modifiers != null && modifiers.contains("final");
    }

    /**
     * Check if this is a constant (static final).
     */
    public boolean isConstant() {
        return isStatic() && isFinal();
    }

    public static class Builder {
        private String name;
        private String type;
        private String modifiers;
        private String summary;
        private boolean deprecated;
        private String constantValue;

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder type(String type) {
            this.type = type;
            return this;
        }

        public Builder modifiers(String modifiers) {
            this.modifiers = modifiers;
            return this;
        }

        public Builder summary(String summary) {
            this.summary = summary;
            return this;
        }

        public Builder deprecated(boolean deprecated) {
            this.deprecated = deprecated;
            return this;
        }

        public Builder constantValue(String constantValue) {
            this.constantValue = constantValue;
            return this;
        }

        public ParsedFieldDto build() {
            return new ParsedFieldDto(name, type, modifiers, summary, deprecated, constantValue);
        }
    }
}
