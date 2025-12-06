package com.spring.mcp.service.initializr;

import com.spring.mcp.config.CacheConfig;
import com.spring.mcp.config.InitializrProperties;
import com.spring.mcp.service.initializr.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for fetching and processing Spring Initializr metadata.
 *
 * <p>This service communicates with the Spring Initializr API (start.spring.io)
 * to retrieve metadata about available dependencies, Spring Boot versions,
 * Java versions, and other project configuration options.</p>
 *
 * <p>Results are cached using Caffeine with configurable TTL to minimize
 * API calls and improve response times.</p>
 *
 * @author Spring MCP Server
 * @version 1.4.0
 * @since 2025-12-06
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "mcp.features.initializr", name = "enabled", havingValue = "true", matchIfMissing = true)
public class InitializrMetadataService {

    @Qualifier("initializrWebClient")
    private final WebClient webClient;

    private final InitializrProperties properties;

    /**
     * Fetch the complete metadata from Spring Initializr.
     *
     * <p>This method retrieves all available options from the /metadata/client
     * endpoint, including dependencies, versions, languages, and packaging types.</p>
     *
     * <p>Results are cached for improved performance.</p>
     *
     * @return InitializrMetadata containing all available options
     * @throws InitializrServiceException if the API call fails
     */
    @Cacheable(value = CacheConfig.CACHE_INITIALIZR_METADATA, key = "'metadata'")
    public InitializrMetadata fetchMetadata() {
        log.debug("Fetching metadata from Spring Initializr: {}", properties.getMetadataUrl());

        try {
            InitializrMetadata metadata = webClient.get()
                .uri("/metadata/client")
                .retrieve()
                .bodyToMono(InitializrMetadata.class)
                .timeout(Duration.ofMillis(properties.getApi().getTimeout()))
                .block();

            if (metadata != null) {
                log.info("Successfully fetched Initializr metadata with {} dependency categories",
                    metadata.getDependencies() != null && metadata.getDependencies().getValues() != null
                        ? metadata.getDependencies().getValues().size() : 0);
            }

            return metadata;
        } catch (WebClientResponseException e) {
            log.error("Failed to fetch Initializr metadata: {} - {}", e.getStatusCode(), e.getMessage());
            throw new InitializrServiceException("Failed to fetch metadata from Spring Initializr", e);
        } catch (Exception e) {
            log.error("Error fetching Initializr metadata", e);
            throw new InitializrServiceException("Error communicating with Spring Initializr", e);
        }
    }

    /**
     * Get all available Spring Boot versions.
     *
     * @return list of available boot versions, sorted with default first
     */
    @Cacheable(value = CacheConfig.CACHE_INITIALIZR_METADATA, key = "'bootVersions'")
    public List<BootVersion> getBootVersions() {
        InitializrMetadata metadata = fetchMetadata();

        if (metadata == null || metadata.getBootVersion() == null
            || metadata.getBootVersion().getValues() == null) {
            log.warn("No boot versions available in metadata");
            return Collections.emptyList();
        }

        List<BootVersion> versions = new ArrayList<>(metadata.getBootVersion().getValues());

        // Sort with default version first, then by version descending
        versions.sort((a, b) -> {
            if (a.isDefaultVersion() && !b.isDefaultVersion()) return -1;
            if (!a.isDefaultVersion() && b.isDefaultVersion()) return 1;
            return VersionRangeUtil.compareVersions(b.getId(), a.getId());
        });

        log.debug("Found {} boot versions, default: {}",
            versions.size(),
            versions.stream().filter(BootVersion::isDefaultVersion).findFirst()
                .map(BootVersion::getId).orElse("none"));

        return versions;
    }

    /**
     * Get the default Spring Boot version.
     *
     * @return the default boot version, or the latest stable if no default
     */
    public BootVersion getDefaultBootVersion() {
        List<BootVersion> versions = getBootVersions();

        return versions.stream()
            .filter(BootVersion::isDefaultVersion)
            .findFirst()
            .orElseGet(() -> versions.stream()
                .filter(BootVersion::isStable)
                .findFirst()
                .orElse(versions.isEmpty() ? null : versions.get(0)));
    }

    /**
     * Get all dependency categories.
     *
     * @return list of dependency categories with their dependencies
     */
    @Cacheable(value = CacheConfig.CACHE_INITIALIZR_CATEGORIES, key = "'categories'")
    public List<DependencyCategory> getDependencyCategories() {
        InitializrMetadata metadata = fetchMetadata();

        if (metadata == null || metadata.getDependencies() == null
            || metadata.getDependencies().getValues() == null) {
            log.warn("No dependency categories available in metadata");
            return Collections.emptyList();
        }

        return metadata.getDependencies().getValues();
    }

    /**
     * Get all dependencies across all categories.
     *
     * @return flat list of all available dependencies
     */
    @Cacheable(value = CacheConfig.CACHE_INITIALIZR_DEPENDENCIES, key = "'allDependencies'")
    public List<DependencyInfo> getAllDependencies() {
        return getDependencyCategories().stream()
            .filter(cat -> cat.getContent() != null)
            .flatMap(cat -> cat.getContent().stream())
            .collect(Collectors.toList());
    }

    /**
     * Find a dependency by its ID.
     *
     * @param id the dependency ID (e.g., "web", "data-jpa", "security")
     * @return Optional containing the dependency if found
     */
    public Optional<DependencyInfo> getDependencyById(String id) {
        if (id == null || id.isBlank()) {
            return Optional.empty();
        }

        String searchId = id.trim().toLowerCase();

        return getAllDependencies().stream()
            .filter(dep -> dep.getId() != null && dep.getId().toLowerCase().equals(searchId))
            .findFirst();
    }

    /**
     * Search dependencies by name, description, or ID.
     *
     * <p>Performs a case-insensitive search across dependency metadata.</p>
     *
     * @param query the search query
     * @return list of matching dependencies
     */
    public List<DependencyInfo> searchDependencies(String query) {
        if (query == null || query.isBlank()) {
            return Collections.emptyList();
        }

        String searchTerm = query.trim().toLowerCase();

        return getAllDependencies().stream()
            .filter(dep -> matchesDependency(dep, searchTerm))
            .collect(Collectors.toList());
    }

    /**
     * Search dependencies by name, description, or ID with category filter.
     *
     * @param query the search query
     * @param category optional category name filter
     * @return list of matching dependencies
     */
    public List<DependencyInfo> searchDependencies(String query, String category) {
        if (category == null || category.isBlank()) {
            return searchDependencies(query);
        }

        String searchTerm = query != null ? query.trim().toLowerCase() : "";
        String categoryFilter = category.trim().toLowerCase();

        return getDependencyCategories().stream()
            .filter(cat -> cat.getName() != null
                && cat.getName().toLowerCase().contains(categoryFilter))
            .filter(cat -> cat.getContent() != null)
            .flatMap(cat -> cat.getContent().stream())
            .filter(dep -> searchTerm.isEmpty() || matchesDependency(dep, searchTerm))
            .collect(Collectors.toList());
    }

    /**
     * Search dependencies compatible with a specific Spring Boot version.
     *
     * @param query the search query
     * @param bootVersion the Spring Boot version to filter by
     * @return list of matching and compatible dependencies
     */
    public List<DependencyInfo> searchDependenciesForVersion(String query, String bootVersion) {
        return searchDependencies(query).stream()
            .filter(dep -> dep.isCompatibleWith(bootVersion))
            .collect(Collectors.toList());
    }

    /**
     * Check if a dependency is compatible with a Spring Boot version.
     *
     * @param dependencyId the dependency ID
     * @param bootVersion the Spring Boot version
     * @return CompatibilityInfo with compatibility status and details
     */
    public CompatibilityInfo checkCompatibility(String dependencyId, String bootVersion) {
        Optional<DependencyInfo> depOpt = getDependencyById(dependencyId);

        if (depOpt.isEmpty()) {
            return CompatibilityInfo.notFound(dependencyId, bootVersion);
        }

        DependencyInfo dep = depOpt.get();
        boolean compatible = dep.isCompatibleWith(bootVersion);

        if (compatible) {
            return CompatibilityInfo.compatible(
                dep.getId(),
                dep.getName(),
                bootVersion,
                dep.getVersionRange()
            );
        } else {
            // Find a suggested compatible version
            String suggestedVersion = findSuggestedVersion(dep.getVersionRange());

            return CompatibilityInfo.incompatible(
                dep.getId(),
                dep.getName(),
                bootVersion,
                dep.getVersionRange(),
                suggestedVersion
            );
        }
    }

    /**
     * Get dependencies grouped by category name.
     *
     * @return map of category name to list of dependencies
     */
    public Map<String, List<DependencyInfo>> getDependenciesByCategory() {
        return getDependencyCategories().stream()
            .filter(cat -> cat.getName() != null && cat.getContent() != null)
            .collect(Collectors.toMap(
                DependencyCategory::getName,
                DependencyCategory::getContent,
                (a, b) -> a, // In case of duplicates, keep first
                LinkedHashMap::new
            ));
    }

    /**
     * Get available Java versions.
     *
     * @return list of available Java versions
     */
    public List<InitializrMetadata.JavaVersion> getJavaVersions() {
        InitializrMetadata metadata = fetchMetadata();

        if (metadata == null || metadata.getJavaVersion() == null
            || metadata.getJavaVersion().getValues() == null) {
            return Collections.emptyList();
        }

        return metadata.getJavaVersion().getValues();
    }

    /**
     * Get available languages.
     *
     * @return list of available languages (java, kotlin, groovy)
     */
    public List<InitializrMetadata.LanguageOption> getLanguages() {
        InitializrMetadata metadata = fetchMetadata();

        if (metadata == null || metadata.getLanguage() == null
            || metadata.getLanguage().getValues() == null) {
            return Collections.emptyList();
        }

        return metadata.getLanguage().getValues();
    }

    /**
     * Get available project types.
     *
     * <p>Returns only full project types (gradle-project, gradle-project-kotlin, maven-project),
     * filtering out build-only types (gradle-build, maven-build) which only generate build files.</p>
     *
     * @return list of available project types for full project generation
     */
    public List<InitializrMetadata.TypeOption> getProjectTypes() {
        InitializrMetadata metadata = fetchMetadata();

        if (metadata == null || metadata.getType() == null
            || metadata.getType().getValues() == null) {
            return Collections.emptyList();
        }

        // Filter to only show project types (not build-only types)
        // start.spring.io returns: gradle-project, gradle-project-kotlin, gradle-build, maven-project, maven-build
        // We want: gradle-project, gradle-project-kotlin, maven-project (contain "project" but not "build")
        return metadata.getType().getValues().stream()
            .filter(type -> type.getId() != null
                && type.getId().contains("-project")
                && !type.getId().contains("-build"))
            .collect(Collectors.toList());
    }

    /**
     * Get available packaging types.
     *
     * @return list of available packaging types (jar, war)
     */
    public List<InitializrMetadata.PackagingOption> getPackagingTypes() {
        InitializrMetadata metadata = fetchMetadata();

        if (metadata == null || metadata.getPackaging() == null
            || metadata.getPackaging().getValues() == null) {
            return Collections.emptyList();
        }

        return metadata.getPackaging().getValues();
    }

    // Private helper methods

    private boolean matchesDependency(DependencyInfo dep, String searchTerm) {
        if (dep.getId() != null && dep.getId().toLowerCase().contains(searchTerm)) {
            return true;
        }
        if (dep.getName() != null && dep.getName().toLowerCase().contains(searchTerm)) {
            return true;
        }
        if (dep.getDescription() != null && dep.getDescription().toLowerCase().contains(searchTerm)) {
            return true;
        }
        return false;
    }

    private String findSuggestedVersion(String versionRange) {
        if (versionRange == null || versionRange.isBlank()) {
            return properties.getDefaults().getBootVersion();
        }

        // Try to get the lower bound of the range as a suggestion
        String lowerBound = VersionRangeUtil.getLowerBound(versionRange);
        if (lowerBound != null) {
            return lowerBound;
        }

        // Fallback to default
        return properties.getDefaults().getBootVersion();
    }

    /**
     * Exception thrown when Initializr API calls fail.
     */
    public static class InitializrServiceException extends RuntimeException {
        public InitializrServiceException(String message) {
            super(message);
        }

        public InitializrServiceException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
