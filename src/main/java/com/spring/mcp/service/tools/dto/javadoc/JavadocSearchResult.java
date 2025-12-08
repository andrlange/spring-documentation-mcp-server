package com.spring.mcp.service.tools.dto.javadoc;

import lombok.Builder;

/**
 * Result DTO for searchJavadocs MCP tool.
 * Represents a single search result entry.
 */
@Builder
public record JavadocSearchResult(
        String type,          // CLASS, INTERFACE, ENUM, METHOD, PACKAGE
        String library,
        String version,
        String fqcn,
        String packageName,
        String simpleName,
        String kind,
        String snippet,       // Matched text snippet
        String sourceUrl,
        boolean deprecated,
        double score          // Relevance score from tsvector ranking
) {
    /**
     * Create a search result from a class entity.
     */
    public static JavadocSearchResult fromClass(
            String library, String version, String fqcn, String simpleName,
            String kind, String snippet, String sourceUrl, boolean deprecated, double score) {
        String pkg = fqcn.contains(".") ? fqcn.substring(0, fqcn.lastIndexOf('.')) : "";
        return JavadocSearchResult.builder()
                .type("CLASS")
                .library(library)
                .version(version)
                .fqcn(fqcn)
                .packageName(pkg)
                .simpleName(simpleName)
                .kind(kind)
                .snippet(snippet)
                .sourceUrl(sourceUrl)
                .deprecated(deprecated)
                .score(score)
                .build();
    }

    /**
     * Create a search result from a package.
     */
    public static JavadocSearchResult fromPackage(
            String library, String version, String packageName,
            String snippet, String sourceUrl, double score) {
        return JavadocSearchResult.builder()
                .type("PACKAGE")
                .library(library)
                .version(version)
                .packageName(packageName)
                .simpleName(packageName)
                .snippet(snippet)
                .sourceUrl(sourceUrl)
                .deprecated(false)
                .score(score)
                .build();
    }
}
