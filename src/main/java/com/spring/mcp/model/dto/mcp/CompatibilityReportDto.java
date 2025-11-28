package com.spring.mcp.model.dto.mcp;

import java.util.List;

/**
 * DTO for dependency compatibility report in MCP responses.
 */
public record CompatibilityReportDto(
    String springBootVersion,
    List<DependencyCompatibility> dependencies,
    boolean allCompatible,
    List<String> warnings
) {
    /**
     * Individual dependency compatibility info
     */
    public record DependencyCompatibility(
        String dependency,
        String compatibleVersion,
        boolean verified,
        String notes
    ) {}

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String springBootVersion;
        private List<DependencyCompatibility> dependencies = List.of();
        private boolean allCompatible = true;
        private List<String> warnings = List.of();

        public Builder springBootVersion(String springBootVersion) {
            this.springBootVersion = springBootVersion;
            return this;
        }
        public Builder dependencies(List<DependencyCompatibility> dependencies) {
            this.dependencies = dependencies;
            return this;
        }
        public Builder allCompatible(boolean allCompatible) {
            this.allCompatible = allCompatible;
            return this;
        }
        public Builder warnings(List<String> warnings) {
            this.warnings = warnings;
            return this;
        }

        public CompatibilityReportDto build() {
            return new CompatibilityReportDto(springBootVersion, dependencies, allCompatible, warnings);
        }
    }
}
