package com.spring.mcp.service.javadoc.dto;

import java.util.List;

/**
 * DTO representing a parsed package from Javadoc.
 *
 * @param packageName Package name (e.g., org.springframework.ai.chat.client)
 * @param summary     Short description
 * @param description Full description
 * @param classes     List of class FQCNs in this package
 * @param sourceUrl   URL to the source documentation
 */
public record ParsedPackageDto(
        String packageName,
        String summary,
        String description,
        List<String> classes,
        String sourceUrl
) {
    /**
     * Create a builder for ParsedPackageDto.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Get class count.
     */
    public int classCount() {
        return classes != null ? classes.size() : 0;
    }

    /**
     * Get the parent package name.
     */
    public String parentPackage() {
        if (packageName == null) return null;
        int lastDot = packageName.lastIndexOf('.');
        return lastDot > 0 ? packageName.substring(0, lastDot) : null;
    }

    /**
     * Get the simple package name (last segment).
     */
    public String simplePackageName() {
        if (packageName == null) return null;
        int lastDot = packageName.lastIndexOf('.');
        return lastDot >= 0 ? packageName.substring(lastDot + 1) : packageName;
    }

    public static class Builder {
        private String packageName;
        private String summary;
        private String description;
        private List<String> classes = List.of();
        private String sourceUrl;

        public Builder packageName(String packageName) {
            this.packageName = packageName;
            return this;
        }

        public Builder summary(String summary) {
            this.summary = summary;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder classes(List<String> classes) {
            this.classes = classes != null ? classes : List.of();
            return this;
        }

        public Builder sourceUrl(String sourceUrl) {
            this.sourceUrl = sourceUrl;
            return this;
        }

        public ParsedPackageDto build() {
            return new ParsedPackageDto(packageName, summary, description, classes, sourceUrl);
        }
    }
}
