package com.spring.mcp.service.mcp;

import com.spring.mcp.model.entity.CodeExample;
import com.spring.mcp.model.entity.ProjectVersion;
import com.spring.mcp.model.entity.SpringBootVersion;
import com.spring.mcp.model.entity.SpringProject;
import com.spring.mcp.repository.CodeExampleRepository;
import com.spring.mcp.repository.ProjectVersionRepository;
import com.spring.mcp.repository.SpringBootVersionRepository;
import com.spring.mcp.repository.SpringProjectRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * MCP-specific version resolution service.
 *
 * This service provides version resolution and normalization specifically for MCP tools,
 * handling inconsistencies in version naming (e.g., "4.0.0" vs "4.0.0.RELEASE") without
 * modifying the underlying sync services, UI, or database schema.
 *
 * Key features:
 * - Resolves version queries with fallback to alternative naming conventions
 * - Aggregates data from multiple version records (e.g., docs from 4.0.0 + examples from 4.0.0.RELEASE)
 * - Provides null-safe URL handling for MCP responses
 *
 * @author Spring MCP Server
 * @version 1.0.0
 * @since 2025-12-01
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class McpVersionResolverService {

    private final SpringProjectRepository projectRepository;
    private final ProjectVersionRepository versionRepository;
    private final SpringBootVersionRepository springBootVersionRepository;
    private final CodeExampleRepository codeExampleRepository;

    /**
     * Version suffixes to try when resolving versions.
     * Order matters: most common patterns first.
     */
    private static final String[] VERSION_SUFFIXES = {
        "",           // Try exact match first
        ".RELEASE",   // Legacy Spring naming
        ".GA",        // Alternative GA suffix
    };

    /**
     * Resolve a project version, trying alternative naming conventions if exact match fails.
     *
     * @param project the Spring project
     * @param version the requested version string
     * @return Optional containing the resolved ProjectVersion
     */
    public Optional<ProjectVersion> resolveVersion(SpringProject project, String version) {
        if (version == null || version.isBlank()) {
            return Optional.empty();
        }

        // Try exact match first
        Optional<ProjectVersion> exactMatch = versionRepository.findByProjectAndVersion(project, version);
        if (exactMatch.isPresent()) {
            log.debug("Found exact version match: {} for project {}", version, project.getSlug());
            return exactMatch;
        }

        // Try alternative naming conventions
        for (String suffix : VERSION_SUFFIXES) {
            if (suffix.isEmpty()) continue; // Already tried exact match

            String alternativeVersion = version + suffix;
            Optional<ProjectVersion> altMatch = versionRepository.findByProjectAndVersion(project, alternativeVersion);
            if (altMatch.isPresent()) {
                log.debug("Resolved version {} -> {} for project {}", version, alternativeVersion, project.getSlug());
                return altMatch;
            }
        }

        // Try stripping suffixes if the requested version has one
        String strippedVersion = stripVersionSuffix(version);
        if (!strippedVersion.equals(version)) {
            Optional<ProjectVersion> strippedMatch = versionRepository.findByProjectAndVersion(project, strippedVersion);
            if (strippedMatch.isPresent()) {
                log.debug("Resolved version {} -> {} (stripped) for project {}", version, strippedVersion, project.getSlug());
                return strippedMatch;
            }
        }

        log.debug("Could not resolve version {} for project {}", version, project.getSlug());
        return Optional.empty();
    }

    /**
     * Find all related version records for a given version.
     * This handles cases where data is split across multiple version records
     * (e.g., docs under "4.0.0", examples under "4.0.0.RELEASE").
     *
     * @param project the Spring project
     * @param version the requested version string
     * @return list of all related ProjectVersion records
     */
    public List<ProjectVersion> findRelatedVersions(SpringProject project, String version) {
        List<ProjectVersion> relatedVersions = new ArrayList<>();

        String baseVersion = stripVersionSuffix(version);

        // Find all versions that match the base version pattern
        List<String> versionsToCheck = List.of(
            baseVersion,
            baseVersion + ".RELEASE",
            baseVersion + ".GA",
            version // Include original if different
        );

        for (String v : versionsToCheck) {
            versionRepository.findByProjectAndVersion(project, v)
                .ifPresent(pv -> {
                    if (!relatedVersions.contains(pv)) {
                        relatedVersions.add(pv);
                    }
                });
        }

        log.debug("Found {} related versions for {} version {}: {}",
            relatedVersions.size(), project.getSlug(), version,
            relatedVersions.stream().map(ProjectVersion::getVersion).collect(Collectors.joining(", ")));

        return relatedVersions;
    }

    /**
     * Get code examples from all related version records.
     * This aggregates examples that might be stored under different version naming conventions.
     *
     * @param project the Spring project
     * @param version the requested version string
     * @return aggregated list of code examples
     */
    public List<CodeExample> getCodeExamplesAggregated(SpringProject project, String version) {
        List<ProjectVersion> relatedVersions = findRelatedVersions(project, version);

        return relatedVersions.stream()
            .flatMap(pv -> codeExampleRepository.findByVersion(pv).stream())
            .distinct()
            .collect(Collectors.toList());
    }

    /**
     * Get code examples with language filter from all related version records.
     *
     * @param project the Spring project
     * @param version the requested version string
     * @param language the programming language filter
     * @return aggregated list of code examples
     */
    public List<CodeExample> getCodeExamplesAggregated(SpringProject project, String version, String language) {
        List<ProjectVersion> relatedVersions = findRelatedVersions(project, version);

        return relatedVersions.stream()
            .flatMap(pv -> codeExampleRepository.findByVersionAndLanguage(pv, language).stream())
            .distinct()
            .collect(Collectors.toList());
    }

    /**
     * Resolve a Spring Boot version, trying alternative naming conventions.
     *
     * @param version the requested version string
     * @return Optional containing the resolved SpringBootVersion
     */
    public Optional<SpringBootVersion> resolveSpringBootVersion(String version) {
        if (version == null || version.isBlank()) {
            return Optional.empty();
        }

        // Try exact match first
        Optional<SpringBootVersion> exactMatch = springBootVersionRepository.findByVersion(version);
        if (exactMatch.isPresent()) {
            return exactMatch;
        }

        // Try alternative naming conventions
        for (String suffix : VERSION_SUFFIXES) {
            if (suffix.isEmpty()) continue;

            String alternativeVersion = version + suffix;
            Optional<SpringBootVersion> altMatch = springBootVersionRepository.findByVersion(alternativeVersion);
            if (altMatch.isPresent()) {
                log.debug("Resolved Spring Boot version {} -> {}", version, alternativeVersion);
                return altMatch;
            }
        }

        // Try stripping suffixes
        String strippedVersion = stripVersionSuffix(version);
        if (!strippedVersion.equals(version)) {
            Optional<SpringBootVersion> strippedMatch = springBootVersionRepository.findByVersion(strippedVersion);
            if (strippedMatch.isPresent()) {
                log.debug("Resolved Spring Boot version {} -> {} (stripped)", version, strippedVersion);
                return strippedMatch;
            }
        }

        return Optional.empty();
    }

    /**
     * Get the canonical (preferred) version string for display.
     * Prefers versions without suffixes (e.g., "4.0.0" over "4.0.0.RELEASE").
     *
     * @param version the version string
     * @return canonical version string
     */
    public String getCanonicalVersion(String version) {
        return stripVersionSuffix(version);
    }

    /**
     * Normalize a URL, returning empty string if null.
     * Used for null-safe MCP responses.
     *
     * @param url the URL to normalize
     * @return the URL or empty string if null
     */
    public String safeUrl(String url) {
        return url != null ? url : "";
    }

    /**
     * Build a documentation URL with version placeholder replaced.
     * Returns empty string if the template URL is null.
     *
     * @param urlTemplate the URL template with {version} placeholder
     * @param version the version to insert
     * @return the resolved URL or empty string
     */
    public String buildDocUrl(String urlTemplate, String version) {
        if (urlTemplate == null || urlTemplate.isBlank()) {
            return "";
        }
        return urlTemplate.replace("{version}", version);
    }

    /**
     * Strip common version suffixes (.RELEASE, .GA, .BUILD-SNAPSHOT, etc.)
     */
    private String stripVersionSuffix(String version) {
        if (version == null) return "";
        return version
            .replaceAll("\\.(RELEASE|GA)$", "")
            .replaceAll("\\.BUILD-SNAPSHOT$", "-SNAPSHOT");
    }

    /**
     * Check if two version strings refer to the same logical version.
     * E.g., "4.0.0" and "4.0.0.RELEASE" are considered equivalent.
     *
     * @param version1 first version string
     * @param version2 second version string
     * @return true if versions are equivalent
     */
    public boolean areVersionsEquivalent(String version1, String version2) {
        if (version1 == null || version2 == null) {
            return false;
        }
        return stripVersionSuffix(version1).equals(stripVersionSuffix(version2));
    }
}
