package com.spring.mcp.service.tools.dto.javadoc;

import lombok.Builder;

import java.util.List;
import java.util.Map;

/**
 * Result DTO for getClassDoc MCP tool.
 * Contains full class documentation with methods, fields, and constructors.
 */
@Builder
public record ClassDocResult(
        String library,
        String version,
        String fqcn,
        String simpleName,
        String kind,
        String modifiers,
        String summary,
        String description,
        String superClass,
        List<String> interfaces,
        boolean deprecated,
        String deprecatedMessage,
        String sourceUrl,
        List<MethodSummary> methods,
        List<FieldSummary> fields,
        List<ConstructorSummary> constructors,
        List<String> annotations
) {
    /**
     * Simplified method summary for listing.
     */
    @Builder
    public record MethodSummary(
            String name,
            String signature,
            String returnType,
            String summary,
            boolean deprecated,
            List<Map<String, String>> parameters
    ) {}

    /**
     * Simplified field summary for listing.
     */
    @Builder
    public record FieldSummary(
            String name,
            String type,
            String modifiers,
            String summary,
            boolean deprecated,
            String constantValue
    ) {}

    /**
     * Simplified constructor summary for listing.
     */
    @Builder
    public record ConstructorSummary(
            String signature,
            String summary,
            boolean deprecated,
            List<Map<String, String>> parameters
    ) {}

    /**
     * Create an empty result for not found cases.
     */
    public static ClassDocResult notFound(String library, String version, String fqcn) {
        return ClassDocResult.builder()
                .library(library)
                .version(version)
                .fqcn(fqcn)
                .summary("Class not found: " + fqcn)
                .methods(List.of())
                .fields(List.of())
                .constructors(List.of())
                .interfaces(List.of())
                .annotations(List.of())
                .build();
    }
}
