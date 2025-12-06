package com.spring.mcp.service.tools;

import com.spring.mcp.service.initializr.InitializrService;
import com.spring.mcp.service.initializr.dto.BootVersion;
import com.spring.mcp.service.initializr.dto.CompatibilityInfo;
import com.spring.mcp.service.initializr.dto.DependencyCategory;
import com.spring.mcp.service.initializr.dto.DependencyInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springaicommunity.mcp.annotation.McpTool;
import org.springaicommunity.mcp.annotation.McpToolParam;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * MCP Tools for Spring Boot Initializr integration.
 *
 * <p>Provides AI agents with access to Spring Boot dependency information
 * from start.spring.io, enabling intelligent project setup assistance.</p>
 *
 * <p>Available tools:</p>
 * <ul>
 *   <li>{@code initializrGetDependency} - Get a specific dependency with formatted snippet</li>
 *   <li>{@code initializrSearchDependencies} - Search for dependencies by name/description</li>
 *   <li>{@code initializrCheckCompatibility} - Check dependency version compatibility</li>
 *   <li>{@code initializrGetBootVersions} - List available Spring Boot versions</li>
 *   <li>{@code initializrGetDependencyCategories} - List all dependency categories</li>
 * </ul>
 *
 * @author Spring MCP Server
 * @version 1.4.0
 * @since 2025-12-06
 */
@Service
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "mcp.features.initializr", name = "enabled", havingValue = "true", matchIfMissing = true)
public class InitializrTools {

    private final InitializrService initializrService;

    @McpTool(description = """
        Get a Spring Boot dependency with formatted snippet for Maven or Gradle.
        Returns the dependency XML (Maven) or DSL notation (Gradle) that can be
        directly added to a project's build file.

        Example: Get "web" dependency for Gradle:
        - dependencyId: "web"
        - format: "gradle"
        - Returns: implementation 'org.springframework.boot:spring-boot-starter-web'
        """)
    public DependencyResult initializrGetDependency(
            @McpToolParam(description = "Dependency ID (e.g., 'web', 'data-jpa', 'security', 'actuator')")
            String dependencyId,
            @McpToolParam(description = "Spring Boot version for compatibility check (optional, defaults to current stable)")
            String bootVersion,
            @McpToolParam(description = "Output format: 'maven' (XML), 'gradle' (Groovy DSL), or 'gradle-kotlin' (Kotlin DSL). Defaults to 'gradle'")
            String format
    ) {
        log.info("Tool: initializrGetDependency - id={}, version={}, format={}", dependencyId, bootVersion, format);

        if (dependencyId == null || dependencyId.isBlank()) {
            return new DependencyResult(false, "Dependency ID is required", null, null, null);
        }

        String effectiveFormat = format != null && !format.isBlank() ? format : "gradle";
        InitializrService.DependencyResult result = initializrService.getDependency(dependencyId, bootVersion, effectiveFormat);

        if (!result.found()) {
            return new DependencyResult(false, result.message(), null, null, null);
        }

        if (!result.compatible()) {
            return new DependencyResult(false, result.message(),
                result.dependency() != null ? result.dependency().getName() : null,
                null, result.compatibilityInfo());
        }

        return new DependencyResult(true, "Success",
            result.dependency().getName(),
            result.snippet(),
            null);
    }

    @McpTool(description = """
        Search for Spring Boot dependencies by name, description, or ID.
        Useful for finding dependencies when the exact ID is unknown.

        Example: Search for "database" to find all database-related dependencies.
        Can filter by category (Web, SQL, NoSQL, Security, etc.) and Spring Boot version.
        """)
    public SearchResult initializrSearchDependencies(
            @McpToolParam(description = "Search query (matches dependency name, description, or ID)")
            String query,
            @McpToolParam(description = "Spring Boot version filter for compatibility (optional)")
            String bootVersion,
            @McpToolParam(description = "Category filter (e.g., 'Web', 'SQL', 'NoSQL', 'Security', 'Developer Tools'). Optional")
            String category,
            @McpToolParam(description = "Maximum number of results to return (default: 20, max: 50)")
            Integer limit
    ) {
        log.info("Tool: initializrSearchDependencies - query={}, version={}, category={}, limit={}",
            query, bootVersion, category, limit);

        if (query == null || query.isBlank()) {
            return new SearchResult(0, List.of(), "Search query is required");
        }

        int effectiveLimit = limit != null ? Math.min(Math.max(limit, 1), 50) : 20;
        InitializrService.SearchResult result = initializrService.searchDependencies(query, bootVersion, category, effectiveLimit);

        if (result.isEmpty()) {
            return new SearchResult(0, List.of(), "No dependencies found matching: " + query);
        }

        List<DependencySummary> summaries = result.results().stream()
            .map(dep -> new DependencySummary(
                dep.getId(),
                dep.getName(),
                dep.getDescription(),
                dep.getVersionRange()
            ))
            .collect(Collectors.toList());

        return new SearchResult(result.totalCount(), summaries, null);
    }

    @McpTool(description = """
        Check if a dependency is compatible with a specific Spring Boot version.
        Returns compatibility status with explanation and suggestions if incompatible.

        Useful before adding a dependency to ensure it works with your project's Spring Boot version.
        """)
    public CompatibilityResult initializrCheckCompatibility(
            @McpToolParam(description = "Dependency ID to check (e.g., 'web', 'data-jpa')")
            String dependencyId,
            @McpToolParam(description = "Spring Boot version to check against (e.g., '3.5.8', '4.0.0')")
            String bootVersion
    ) {
        log.info("Tool: initializrCheckCompatibility - dependencyId={}, bootVersion={}", dependencyId, bootVersion);

        if (dependencyId == null || dependencyId.isBlank()) {
            return new CompatibilityResult(false, "Dependency ID is required", null, null, null);
        }

        if (bootVersion == null || bootVersion.isBlank()) {
            return new CompatibilityResult(false, "Spring Boot version is required", null, null, null);
        }

        CompatibilityInfo info = initializrService.checkCompatibility(dependencyId, bootVersion);

        return new CompatibilityResult(
            info.isCompatible(),
            info.getReason(),
            info.getDependencyName(),
            info.getVersionRange(),
            info.getSuggestedBootVersion()
        );
    }

    @McpTool(description = """
        Get all available Spring Boot versions from Spring Initializr.
        Returns version list including stable releases, release candidates, and snapshots.
        The default version is marked for easy identification.
        """)
    public VersionsResult initializrGetBootVersions() {
        log.info("Tool: initializrGetBootVersions");

        List<BootVersion> versions = initializrService.getBootVersions();
        BootVersion defaultVersion = initializrService.getDefaultBootVersion();

        List<VersionSummary> summaries = versions.stream()
            .map(v -> new VersionSummary(
                v.getId(),
                v.getName(),
                v.isDefaultVersion(),
                v.getVersionType()
            ))
            .collect(Collectors.toList());

        return new VersionsResult(
            summaries.size(),
            defaultVersion != null ? defaultVersion.getId() : null,
            summaries
        );
    }

    @McpTool(description = """
        Get all dependency categories with their available dependencies.
        Categories include: Developer Tools, Web, SQL, NoSQL, Security, Cloud, AI, etc.
        Useful for browsing available dependencies by category.

        When bootVersion is specified, only returns dependencies compatible with that version.
        Categories with no compatible dependencies are excluded from the results.

        Example: With bootVersion "4.0.0", the AI category will be excluded since
        Spring AI dependencies are not yet compatible with Spring Boot 4.0.
        """)
    public CategoriesResult initializrGetDependencyCategories(
            @McpToolParam(description = "Spring Boot version to filter by compatibility (optional, e.g., '3.5.8', '4.0.0'). When provided, only compatible dependencies are returned.")
            String bootVersion
    ) {
        log.info("Tool: initializrGetDependencyCategories - bootVersion={}", bootVersion);

        List<DependencyCategory> categories;

        if (bootVersion != null && !bootVersion.isBlank()) {
            categories = initializrService.getDependencyCategoriesForVersion(bootVersion);
        } else {
            categories = initializrService.getDependencyCategories();
        }

        Map<String, List<String>> categoryMap = categories.stream()
            .filter(cat -> cat.getName() != null && cat.getContent() != null)
            .collect(Collectors.toMap(
                DependencyCategory::getName,
                cat -> cat.getContent().stream()
                    .map(DependencyInfo::getId)
                    .collect(Collectors.toList())
            ));

        return new CategoriesResult(
            categories.size(),
            categoryMap,
            bootVersion
        );
    }

    // Result DTOs for tool responses

    public record DependencyResult(
        boolean success,
        String message,
        String dependencyName,
        String snippet,
        CompatibilityInfo compatibilityInfo
    ) {}

    public record SearchResult(
        int count,
        List<DependencySummary> dependencies,
        String error
    ) {}

    public record DependencySummary(
        String id,
        String name,
        String description,
        String versionRange
    ) {}

    public record CompatibilityResult(
        boolean compatible,
        String reason,
        String dependencyName,
        String versionRange,
        String suggestedVersion
    ) {}

    public record VersionsResult(
        int count,
        String defaultVersion,
        List<VersionSummary> versions
    ) {}

    public record VersionSummary(
        String id,
        String name,
        boolean isDefault,
        String type
    ) {}

    public record CategoriesResult(
        int count,
        Map<String, List<String>> categories,
        String filteredByVersion
    ) {}
}
