package com.spring.mcp.service.initializr;

import com.spring.mcp.config.InitializrProperties;
import com.spring.mcp.service.initializr.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Facade service for Spring Boot Initializr integration.
 *
 * <p>This service provides high-level operations for interacting with the
 * Spring Initializr API, coordinating between the metadata service and
 * cache service. It is designed to be used by MCP tools and controllers.</p>
 *
 * <p>Key features:</p>
 * <ul>
 *   <li>Dependency search and retrieval</li>
 *   <li>Version compatibility checking</li>
 *   <li>Dependency snippet generation (Maven/Gradle)</li>
 *   <li>Project configuration assistance</li>
 * </ul>
 *
 * @author Spring MCP Server
 * @version 1.4.0
 * @since 2025-12-06
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "mcp.features.initializr", name = "enabled", havingValue = "true", matchIfMissing = true)
public class InitializrService {

    private final InitializrMetadataService metadataService;
    private final InitializrCacheService cacheService;
    private final InitializrProperties properties;

    /**
     * Get a dependency by ID and format it for the specified build tool.
     *
     * @param dependencyId the dependency ID (e.g., "web", "data-jpa")
     * @param bootVersion optional Spring Boot version for compatibility check
     * @param format the output format: "maven", "gradle", or "gradle-kotlin"
     * @return formatted dependency snippet or error message
     */
    public DependencyResult getDependency(String dependencyId, String bootVersion, String format) {
        log.debug("Getting dependency: {}, version: {}, format: {}", dependencyId, bootVersion, format);

        Optional<DependencyInfo> depOpt = metadataService.getDependencyById(dependencyId);

        if (depOpt.isEmpty()) {
            return DependencyResult.notFound(dependencyId);
        }

        DependencyInfo dep = depOpt.get();
        String effectiveVersion = bootVersion != null ? bootVersion : properties.getDefaults().getBootVersion();

        // Check compatibility
        if (!dep.isCompatibleWith(effectiveVersion)) {
            CompatibilityInfo compat = metadataService.checkCompatibility(dependencyId, effectiveVersion);
            return DependencyResult.incompatible(dep, compat);
        }

        // Generate the formatted snippet
        String snippet = switch (format.toLowerCase()) {
            case "maven", "xml" -> dep.toMavenDependency();
            case "gradle-kotlin", "kotlin", "kts" -> dep.toGradleKotlinDependency();
            default -> dep.toGradleDependency();
        };

        return DependencyResult.success(dep, snippet, format);
    }

    /**
     * Search for dependencies matching a query.
     *
     * @param query search term (matches name, description, or ID)
     * @param bootVersion optional version filter for compatibility
     * @param category optional category filter
     * @param limit maximum results to return (default 20)
     * @return list of matching dependencies with metadata
     */
    public SearchResult searchDependencies(String query, String bootVersion, String category, int limit) {
        log.debug("Searching dependencies: query={}, version={}, category={}, limit={}",
            query, bootVersion, category, limit);

        List<DependencyInfo> results;

        if (category != null && !category.isBlank()) {
            results = metadataService.searchDependencies(query, category);
        } else {
            results = metadataService.searchDependencies(query);
        }

        // Always filter by version compatibility when bootVersion is provided
        if (bootVersion != null && !bootVersion.isBlank()) {
            String effectiveVersion = bootVersion;
            results = results.stream()
                .filter(dep -> dep.isCompatibleWith(effectiveVersion))
                .collect(Collectors.toList());
        }

        // Apply limit
        if (limit > 0 && results.size() > limit) {
            results = results.stream().limit(limit).collect(Collectors.toList());
        }

        // Group by category for better organization
        Map<String, List<DependencyInfo>> byCategory = results.stream()
            .collect(Collectors.groupingBy(dep -> findCategoryForDependency(dep.getId())));

        return new SearchResult(query, results.size(), results, byCategory);
    }

    /**
     * Check if a dependency is compatible with a Spring Boot version.
     *
     * @param dependencyId the dependency ID
     * @param bootVersion the Spring Boot version to check
     * @return compatibility information with recommendations
     */
    public CompatibilityInfo checkCompatibility(String dependencyId, String bootVersion) {
        return metadataService.checkCompatibility(dependencyId, bootVersion);
    }

    /**
     * Get all available Spring Boot versions.
     *
     * @return list of boot versions sorted by recency
     */
    public List<BootVersion> getBootVersions() {
        return cacheService.getCachedBootVersions();
    }

    /**
     * Get the default Spring Boot version.
     *
     * @return the default boot version
     */
    public BootVersion getDefaultBootVersion() {
        return metadataService.getDefaultBootVersion();
    }

    /**
     * Get all dependency categories with their dependencies.
     *
     * @return list of categories
     */
    public List<DependencyCategory> getDependencyCategories() {
        return cacheService.getCachedDependencyCategories();
    }

    /**
     * Get dependency categories filtered by Spring Boot version compatibility.
     *
     * <p>Returns only categories that have at least one compatible dependency,
     * with each category's content filtered to only include compatible dependencies.</p>
     *
     * @param bootVersion the Spring Boot version to filter by (e.g., "3.5.8", "4.0.0")
     * @return list of categories with compatible dependencies only
     */
    public List<DependencyCategory> getDependencyCategoriesForVersion(String bootVersion) {
        if (bootVersion == null || bootVersion.isBlank()) {
            return getDependencyCategories();
        }

        List<DependencyCategory> allCategories = getDependencyCategories();
        List<DependencyCategory> filteredCategories = new java.util.ArrayList<>();

        for (DependencyCategory category : allCategories) {
            if (category.getContent() == null) {
                continue;
            }

            // Filter dependencies by version compatibility
            List<DependencyInfo> compatibleDeps = category.getContent().stream()
                .filter(dep -> dep.isCompatibleWith(bootVersion))
                .collect(Collectors.toList());

            // Only include categories that have at least one compatible dependency
            if (!compatibleDeps.isEmpty()) {
                // Create a new category with only compatible dependencies
                DependencyCategory filteredCategory = DependencyCategory.builder()
                    .name(category.getName())
                    .content(compatibleDeps)
                    .build();
                filteredCategories.add(filteredCategory);
            }
        }

        log.debug("Filtered {} categories to {} for Spring Boot {}",
            allCategories.size(), filteredCategories.size(), bootVersion);

        return filteredCategories;
    }

    /**
     * Get available Java versions.
     *
     * @return list of Java versions
     */
    public List<InitializrMetadata.JavaVersion> getJavaVersions() {
        return metadataService.getJavaVersions();
    }

    /**
     * Get available languages.
     *
     * @return list of languages
     */
    public List<InitializrMetadata.LanguageOption> getLanguages() {
        return metadataService.getLanguages();
    }

    /**
     * Get available project types.
     *
     * @return list of project types
     */
    public List<InitializrMetadata.TypeOption> getProjectTypes() {
        return metadataService.getProjectTypes();
    }

    /**
     * Get available packaging types.
     *
     * @return list of packaging types
     */
    public List<InitializrMetadata.PackagingOption> getPackagingTypes() {
        return metadataService.getPackagingTypes();
    }

    /**
     * Generate a project configuration summary.
     *
     * @param metadata the project metadata
     * @return formatted summary string
     */
    public String generateProjectSummary(ProjectMetadata metadata) {
        StringBuilder sb = new StringBuilder();
        sb.append("Project Configuration:\n");
        sb.append("  Group ID: ").append(metadata.getGroupId()).append("\n");
        sb.append("  Artifact ID: ").append(metadata.getArtifactId()).append("\n");
        sb.append("  Name: ").append(metadata.getName()).append("\n");
        sb.append("  Spring Boot: ").append(metadata.getBootVersion()).append("\n");
        sb.append("  Java: ").append(metadata.getJavaVersion()).append("\n");
        sb.append("  Language: ").append(metadata.getLanguage()).append("\n");
        sb.append("  Packaging: ").append(metadata.getPackaging()).append("\n");
        sb.append("  Build Tool: ").append(metadata.getBuildType()).append("\n");

        if (metadata.getDependencies() != null && !metadata.getDependencies().isEmpty()) {
            sb.append("  Dependencies: ").append(String.join(", ", metadata.getDependencies())).append("\n");
        }

        return sb.toString();
    }

    /**
     * Refresh the cache.
     */
    public void refreshCache() {
        cacheService.invalidateAndRefresh(true);
    }

    /**
     * Get cache statistics summary.
     *
     * @return cache summary
     */
    public InitializrCacheService.CacheSummary getCacheStatus() {
        return cacheService.getCacheSummary();
    }

    // Private helper methods

    private String findCategoryForDependency(String dependencyId) {
        return metadataService.getDependencyCategories().stream()
            .filter(cat -> cat.getContent() != null)
            .filter(cat -> cat.getContent().stream()
                .anyMatch(dep -> dep.getId() != null && dep.getId().equals(dependencyId)))
            .map(DependencyCategory::getName)
            .findFirst()
            .orElse("Other");
    }

    /**
     * Result of a dependency lookup.
     */
    public record DependencyResult(
        boolean found,
        boolean compatible,
        DependencyInfo dependency,
        String snippet,
        String format,
        String message,
        CompatibilityInfo compatibilityInfo
    ) {
        public static DependencyResult success(DependencyInfo dep, String snippet, String format) {
            return new DependencyResult(true, true, dep, snippet, format,
                "Dependency found: " + dep.getName(), null);
        }

        public static DependencyResult notFound(String dependencyId) {
            return new DependencyResult(false, false, null, null, null,
                "Dependency not found: " + dependencyId, null);
        }

        public static DependencyResult incompatible(DependencyInfo dep, CompatibilityInfo compat) {
            return new DependencyResult(true, false, dep, null, null,
                compat.getReason(), compat);
        }
    }

    /**
     * Result of a dependency search.
     */
    public record SearchResult(
        String query,
        int totalCount,
        List<DependencyInfo> results,
        Map<String, List<DependencyInfo>> byCategory
    ) {
        public boolean isEmpty() {
            return results == null || results.isEmpty();
        }
    }
}
