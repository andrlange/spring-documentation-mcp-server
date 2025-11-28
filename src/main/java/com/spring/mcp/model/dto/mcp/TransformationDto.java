package com.spring.mcp.model.dto.mcp;

import com.spring.mcp.model.entity.MigrationTransformation;

import java.util.List;

/**
 * DTO for individual transformation in MCP responses.
 */
public record TransformationDto(
    String type,
    String category,
    String subcategory,
    String oldPattern,
    String newPattern,
    String filePattern,
    String explanation,
    String codeExample,
    String additionalSteps,
    boolean breakingChange,
    String severity,
    List<String> tags
) {
    /**
     * Create from entity
     */
    public static TransformationDto from(MigrationTransformation entity) {
        return new TransformationDto(
            entity.getTransformationType() != null ? entity.getTransformationType().name() : null,
            entity.getCategory(),
            entity.getSubcategory(),
            entity.getOldPattern(),
            entity.getNewPattern(),
            entity.getFilePattern(),
            entity.getExplanation(),
            entity.getCodeExample(),
            entity.getAdditionalSteps(),
            Boolean.TRUE.equals(entity.getBreakingChange()),
            entity.getSeverity() != null ? entity.getSeverity().name() : "INFO",
            entity.getTags()
        );
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String type;
        private String category;
        private String subcategory;
        private String oldPattern;
        private String newPattern;
        private String filePattern;
        private String explanation;
        private String codeExample;
        private String additionalSteps;
        private boolean breakingChange;
        private String severity = "INFO";
        private List<String> tags = List.of();

        public Builder type(String type) { this.type = type; return this; }
        public Builder category(String category) { this.category = category; return this; }
        public Builder subcategory(String subcategory) { this.subcategory = subcategory; return this; }
        public Builder oldPattern(String oldPattern) { this.oldPattern = oldPattern; return this; }
        public Builder newPattern(String newPattern) { this.newPattern = newPattern; return this; }
        public Builder filePattern(String filePattern) { this.filePattern = filePattern; return this; }
        public Builder explanation(String explanation) { this.explanation = explanation; return this; }
        public Builder codeExample(String codeExample) { this.codeExample = codeExample; return this; }
        public Builder additionalSteps(String additionalSteps) { this.additionalSteps = additionalSteps; return this; }
        public Builder breakingChange(boolean breakingChange) { this.breakingChange = breakingChange; return this; }
        public Builder severity(String severity) { this.severity = severity; return this; }
        public Builder tags(List<String> tags) { this.tags = tags; return this; }

        public TransformationDto build() {
            return new TransformationDto(type, category, subcategory, oldPattern, newPattern,
                filePattern, explanation, codeExample, additionalSteps, breakingChange, severity, tags);
        }
    }
}
