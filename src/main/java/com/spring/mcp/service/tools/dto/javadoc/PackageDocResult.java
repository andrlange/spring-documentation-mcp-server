package com.spring.mcp.service.tools.dto.javadoc;

import lombok.Builder;

import java.util.List;

/**
 * Result DTO for getPackageDoc MCP tool.
 * Contains package documentation with list of classes.
 */
@Builder
public record PackageDocResult(
        String library,
        String version,
        String packageName,
        String summary,
        String description,
        String sourceUrl,
        List<ClassEntry> classes,
        int totalClasses
) {
    /**
     * Class entry within a package.
     */
    @Builder
    public record ClassEntry(
            String simpleName,
            String fqcn,
            String kind,
            String summary,
            boolean deprecated
    ) {}

    /**
     * Create an empty result for not found cases.
     */
    public static PackageDocResult notFound(String library, String version, String packageName) {
        return PackageDocResult.builder()
                .library(library)
                .version(version)
                .packageName(packageName)
                .summary("Package not found: " + packageName)
                .classes(List.of())
                .totalClasses(0)
                .build();
    }
}
