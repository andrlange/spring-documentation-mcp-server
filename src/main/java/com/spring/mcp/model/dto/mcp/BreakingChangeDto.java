package com.spring.mcp.model.dto.mcp;

import com.spring.mcp.model.entity.MigrationTransformation;

/**
 * DTO for breaking change information in MCP responses.
 */
public record BreakingChangeDto(
    String type,
    String category,
    String oldPattern,
    String newPattern,
    String explanation,
    String codeExample,
    String severity,
    String filePattern
) {
    /**
     * Create from entity
     */
    public static BreakingChangeDto from(MigrationTransformation entity) {
        return new BreakingChangeDto(
            entity.getTransformationType() != null ? entity.getTransformationType().name() : null,
            entity.getCategory(),
            entity.getOldPattern(),
            entity.getNewPattern(),
            entity.getExplanation(),
            entity.getCodeExample(),
            entity.getSeverity() != null ? entity.getSeverity().name() : "ERROR",
            entity.getFilePattern()
        );
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String type;
        private String category;
        private String oldPattern;
        private String newPattern;
        private String explanation;
        private String codeExample;
        private String severity = "ERROR";
        private String filePattern;

        public Builder type(String type) { this.type = type; return this; }
        public Builder category(String category) { this.category = category; return this; }
        public Builder oldPattern(String oldPattern) { this.oldPattern = oldPattern; return this; }
        public Builder newPattern(String newPattern) { this.newPattern = newPattern; return this; }
        public Builder explanation(String explanation) { this.explanation = explanation; return this; }
        public Builder codeExample(String codeExample) { this.codeExample = codeExample; return this; }
        public Builder severity(String severity) { this.severity = severity; return this; }
        public Builder filePattern(String filePattern) { this.filePattern = filePattern; return this; }

        public BreakingChangeDto build() {
            return new BreakingChangeDto(type, category, oldPattern, newPattern,
                explanation, codeExample, severity, filePattern);
        }
    }
}
