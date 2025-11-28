package com.spring.mcp.model.dto.mcp;

import java.util.List;

/**
 * DTO for comprehensive migration guide response from MCP tools.
 */
public record MigrationGuideDto(
    String project,
    String fromVersion,
    String toVersion,
    int totalChanges,
    long breakingChanges,
    List<TransformationDto> importChanges,
    List<TransformationDto> dependencyChanges,
    List<TransformationDto> propertyChanges,
    List<TransformationDto> codeChanges,
    List<TransformationDto> buildChanges,
    List<TransformationDto> templateChanges,
    List<TransformationDto> annotationChanges,
    List<TransformationDto> configChanges,
    String sourceUrl,
    String license
) {
    public static MigrationGuideDto empty(String project, String from, String to) {
        return new MigrationGuideDto(project, from, to, 0, 0,
            List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(),
            null, null);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String project;
        private String fromVersion;
        private String toVersion;
        private int totalChanges;
        private long breakingChanges;
        private List<TransformationDto> importChanges = List.of();
        private List<TransformationDto> dependencyChanges = List.of();
        private List<TransformationDto> propertyChanges = List.of();
        private List<TransformationDto> codeChanges = List.of();
        private List<TransformationDto> buildChanges = List.of();
        private List<TransformationDto> templateChanges = List.of();
        private List<TransformationDto> annotationChanges = List.of();
        private List<TransformationDto> configChanges = List.of();
        private String sourceUrl;
        private String license;

        public Builder project(String project) { this.project = project; return this; }
        public Builder fromVersion(String fromVersion) { this.fromVersion = fromVersion; return this; }
        public Builder toVersion(String toVersion) { this.toVersion = toVersion; return this; }
        public Builder totalChanges(int totalChanges) { this.totalChanges = totalChanges; return this; }
        public Builder breakingChanges(long breakingChanges) { this.breakingChanges = breakingChanges; return this; }
        public Builder importChanges(List<TransformationDto> importChanges) { this.importChanges = importChanges; return this; }
        public Builder dependencyChanges(List<TransformationDto> dependencyChanges) { this.dependencyChanges = dependencyChanges; return this; }
        public Builder propertyChanges(List<TransformationDto> propertyChanges) { this.propertyChanges = propertyChanges; return this; }
        public Builder codeChanges(List<TransformationDto> codeChanges) { this.codeChanges = codeChanges; return this; }
        public Builder buildChanges(List<TransformationDto> buildChanges) { this.buildChanges = buildChanges; return this; }
        public Builder templateChanges(List<TransformationDto> templateChanges) { this.templateChanges = templateChanges; return this; }
        public Builder annotationChanges(List<TransformationDto> annotationChanges) { this.annotationChanges = annotationChanges; return this; }
        public Builder configChanges(List<TransformationDto> configChanges) { this.configChanges = configChanges; return this; }
        public Builder sourceUrl(String sourceUrl) { this.sourceUrl = sourceUrl; return this; }
        public Builder license(String license) { this.license = license; return this; }

        public MigrationGuideDto build() {
            return new MigrationGuideDto(project, fromVersion, toVersion, totalChanges, breakingChanges,
                importChanges, dependencyChanges, propertyChanges, codeChanges, buildChanges,
                templateChanges, annotationChanges, configChanges, sourceUrl, license);
        }
    }
}
