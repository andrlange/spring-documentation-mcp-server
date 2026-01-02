package com.spring.mcp.service.sync;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.spring.mcp.model.dto.springio.SpringGenerationsResponse;
import com.spring.mcp.model.entity.ProjectVersion;
import com.spring.mcp.model.entity.SpringBootCompatibility;
import com.spring.mcp.model.entity.SpringBootVersion;
import com.spring.mcp.model.entity.SpringProject;
import com.spring.mcp.model.enums.VersionState;
import com.spring.mcp.repository.ProjectVersionRepository;
import com.spring.mcp.repository.SpringBootCompatibilityRepository;
import com.spring.mcp.repository.SpringBootVersionRepository;
import com.spring.mcp.repository.SpringProjectRepository;
import com.spring.mcp.util.VersionParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service for synchronizing all Spring projects and versions from Spring Generations API.
 * Handles the complex structure of project mappings including release trains.
 *
 * @author Spring MCP Server
 * @version 1.0
 * @since 2025-01-08
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SpringGenerationsSyncService {

    private final SpringGenerationsClient generationsClient;
    private final SpringProjectRepository springProjectRepository;
    private final ProjectVersionRepository projectVersionRepository;
    private final SpringBootCompatibilityRepository compatibilityRepository;
    private final SpringBootVersionRepository springBootVersionRepository;
    private final ObjectMapper objectMapper;

    private static final Pattern VERSION_PATTERN = Pattern.compile("(\\d+)\\.(\\d+)\\.(\\d+)");

    /**
     * Synchronize all Spring projects and versions from the Generations API.
     *
     * @return SyncResult with statistics
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public SyncResult syncAllGenerations() {
        log.info("Starting Spring Generations sync");
        SyncResult result = new SyncResult();

        try {
            // Fetch generations data
            SpringGenerationsResponse response = generationsClient.fetchGenerations();

            if (response == null || response.getResult() == null ||
                response.getResult().getData() == null ||
                response.getResult().getData().getAllSpringBootGeneration() == null) {
                throw new RuntimeException("Invalid response from Spring Generations API");
            }

            List<SpringGenerationsResponse.SpringBootGenerationNode> nodes =
                response.getResult().getData().getAllSpringBootGeneration().getNodes();

            log.info("Processing {} Spring Boot versions", nodes.size());

            // Build support dates map from API
            Map<String, SupportDates> supportDatesMap = buildSupportDatesMap(response);
            log.info("Loaded support dates for {} Spring Boot versions", supportDatesMap.size());

            // Ensure Spring Boot project exists
            SpringProject springBootProject = ensureProject("spring-boot", "Spring Boot",
                "Spring Boot makes it easy to create stand-alone, production-grade Spring based Applications");
            result.setProjectsCreated(result.getProjectsCreated() + 1);

            // Process each Spring Boot version
            for (SpringGenerationsResponse.SpringBootGenerationNode node : nodes) {
                try {
                    processBootGeneration(node, springBootProject, supportDatesMap, result);
                } catch (Exception e) {
                    log.error("Error processing Spring Boot version: {}", node.getVersion(), e);
                    result.setErrorsEncountered(result.getErrorsEncountered() + 1);
                }
            }

            result.setSuccess(true);
            log.info("Spring Generations sync completed successfully. " +
                "Projects: {}, Versions: {}, Compatibility Mappings: {}, Errors: {}",
                result.getProjectsCreated(), result.getVersionsCreated(),
                result.getCompatibilityMappingsCreated(), result.getErrorsEncountered());

        } catch (Exception e) {
            log.error("Error during Spring Generations sync", e);
            result.setSuccess(false);
            result.setErrorMessage(e.getMessage());
        }

        return result;
    }

    /**
     * Process a single Spring Boot generation (version) and its project mappings.
     */
    private void processBootGeneration(SpringGenerationsResponse.SpringBootGenerationNode node,
                                      SpringProject springBootProject,
                                      Map<String, SupportDates> supportDatesMap,
                                      SyncResult result) {
        String bootVersion = node.getVersion();
        log.debug("Processing Spring Boot version: {}", bootVersion);

        // Look up Spring Boot version from spring_boot_versions table (populated by Phase 0)
        // Use smart matching to handle version patterns like "3.5.x" -> "3.5.7"
        Optional<SpringBootVersion> springBootVersionOpt = findSpringBootVersion(bootVersion);

        if (springBootVersionOpt.isEmpty()) {
            log.warn("Spring Boot version {} not found in spring_boot_versions table. " +
                "Skipping compatibility mappings for this version. " +
                "This version should have been created by Phase 0 (SpringBootVersionSyncService).", bootVersion);
            result.setErrorsEncountered(result.getErrorsEncountered() + 1);
            return; // Skip this version
        }

        SpringBootVersion springBootVersion = springBootVersionOpt.get();
        log.debug("Matched version pattern '{}' to actual version '{}'", bootVersion, springBootVersion.getVersion());

        // Process project mappings
        Map<String, Object> projects = node.getGenerationsMapping().getProjects();
        Map<String, String> projectLookup = node.getProjectLookup();

        if (projects != null) {
            for (Map.Entry<String, Object> entry : projects.entrySet()) {
                String projectSlug = entry.getKey();
                Object versionData = entry.getValue();

                try {
                    processProjectMapping(projectSlug, versionData, projectLookup,
                        springBootVersion, result);
                } catch (Exception e) {
                    log.error("Error processing project: {} for Boot version: {}",
                        projectSlug, bootVersion, e);
                    result.setErrorsEncountered(result.getErrorsEncountered() + 1);
                }
            }
        }
    }

    /**
     * Process project mapping - handles both simple arrays and release train structures.
     */
    @SuppressWarnings("unchecked")
    private void processProjectMapping(String projectSlug, Object versionData,
                                      Map<String, String> projectLookup,
                                      SpringBootVersion springBootVersion, SyncResult result) {
        // Get project display name from lookup
        String projectName = projectLookup.getOrDefault(projectSlug, formatProjectName(projectSlug));

        if (versionData instanceof List) {
            // Simple case: ["6.2.x", "6.1.x"]
            List<String> versions = (List<String>) versionData;
            for (String version : versions) {
                createProjectAndVersion(projectSlug, projectName, version, springBootVersion, result);
            }
        } else if (versionData instanceof Map) {
            // Release train case: {"2025.1.x": {"spring-data-jpa": ["3.6.x"], ...}}
            Map<String, Object> releaseTrains = (Map<String, Object>) versionData;

            for (Map.Entry<String, Object> trainEntry : releaseTrains.entrySet()) {
                String trainVersion = trainEntry.getKey();
                Object trainProjects = trainEntry.getValue();

                // Create the release train project and version
                createProjectAndVersion(projectSlug, projectName, trainVersion, springBootVersion, result);

                // Process sub-projects in the release train
                if (trainProjects instanceof Map) {
                    Map<String, Object> subProjects = (Map<String, Object>) trainProjects;
                    for (Map.Entry<String, Object> subEntry : subProjects.entrySet()) {
                        String subProjectSlug = subEntry.getKey();
                        Object subVersionData = subEntry.getValue();

                        String subProjectName = projectLookup.getOrDefault(subProjectSlug,
                            formatProjectName(subProjectSlug));

                        if (subVersionData instanceof List) {
                            List<String> subVersions = (List<String>) subVersionData;
                            for (String subVersion : subVersions) {
                                createProjectAndVersion(subProjectSlug, subProjectName,
                                    subVersion, springBootVersion, result);
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Create or get a Spring project and its version, and create compatibility mapping.
     */
    private void createProjectAndVersion(String slug, String name, String version,
                                         SpringBootVersion springBootVersion, SyncResult result) {
        // Ensure project exists
        SpringProject project = ensureProject(slug, name, null);
        if (!springProjectRepository.findBySlug(slug).isPresent()) {
            result.setProjectsCreated(result.getProjectsCreated() + 1);
        }

        // Check if version already exists
        boolean versionExists = projectVersionRepository.existsByProjectAndVersion(project, version);
        ProjectVersion projectVersion;

        if (versionExists) {
            // Get existing version
            projectVersion = projectVersionRepository.findByProjectAndVersion(project, version)
                .orElseThrow(() -> new RuntimeException("Version not found: " + version));
        } else {
            // Parse version numbers using VersionParser
            VersionParser.ParsedVersion parsedVersion = VersionParser.parse(version);

            // Determine version state
            VersionState versionState = determineVersionState(version);

            // Create version
            projectVersion = ProjectVersion.builder()
                .project(project)
                .version(version)
                .majorVersion(parsedVersion.getMajorVersion())
                .minorVersion(parsedVersion.getMinorVersion())
                .patchVersion(parsedVersion.getPatchVersion())
                .state(versionState)
                .isLatest(false) // Will be determined later
                .isDefault(false)
                .releaseDate(LocalDate.now()) // We don't have real release dates from this API
                .build();

            projectVersionRepository.save(projectVersion);
            result.setVersionsCreated(result.getVersionsCreated() + 1);

            log.debug("Created version: {} for project: {}", version, name);
        }

        // Create compatibility mapping
        createCompatibilityMapping(springBootVersion, projectVersion, result);
    }

    /**
     * Create Spring Boot version.
     */
    private void createBootVersion(SpringProject project, String version, String support, SupportDates supportDates) {
        VersionParser.ParsedVersion parsedVersion = VersionParser.parse(version);
        VersionState versionState = determineVersionState(version);

        ProjectVersion.ProjectVersionBuilder builder = ProjectVersion.builder()
            .project(project)
            .version(version)
            .majorVersion(parsedVersion.getMajorVersion())
            .minorVersion(parsedVersion.getMinorVersion())
            .patchVersion(parsedVersion.getPatchVersion())
            .state(versionState)
            .isLatest("oss".equals(support)) // Mark OSS versions as latest
            .isDefault("oss".equals(support));

        // Add support dates if available
        if (supportDates != null) {
            builder.releaseDate(supportDates.getInitialRelease())
                   .ossSupportEnd(supportDates.getOssSupportEnd())
                   .enterpriseSupportEnd(supportDates.getEnterpriseSupportEnd());
        } else {
            builder.releaseDate(LocalDate.now());
        }

        ProjectVersion projectVersion = builder.build();
        projectVersionRepository.save(projectVersion);
        log.info("Created Spring Boot version: {} (support: {}, release: {}, oss end: {}, enterprise end: {})",
            version, support,
            supportDates != null ? supportDates.getInitialRelease() : "N/A",
            supportDates != null ? supportDates.getOssSupportEnd() : "N/A",
            supportDates != null ? supportDates.getEnterpriseSupportEnd() : "N/A");
    }

    /**
     * Create compatibility mappings between Spring Boot versions and project versions.
     * Matches on major.minor versions, ignoring patch numbers.
     *
     * For example, if Spring Boot 3.5.x is compatible with Spring Batch 5.2.x:
     * - Find all Spring Boot 3.5.* versions (3.5.0, 3.5.1, 3.5.7, etc.)
     * - Find all Spring Batch 5.2.* versions (5.2.0, 5.2.1, 5.2.2, etc.)
     * - Create compatibility mappings for all combinations
     */
    private void createCompatibilityMapping(SpringBootVersion springBootVersion,
                                           ProjectVersion projectVersion,
                                           SyncResult result) {
        // Find all Spring Boot versions with same major.minor
        List<SpringBootVersion> allBootVersions = springBootVersionRepository
            .findByMajorVersionAndMinorVersionOrderByPatchVersionDesc(
                springBootVersion.getMajorVersion(),
                springBootVersion.getMinorVersion()
            );

        // Find all project versions with same major.minor
        List<ProjectVersion> allProjectVersions = projectVersionRepository
            .findByProjectAndMajorVersionAndMinorVersion(
                projectVersion.getProject(),
                projectVersion.getMajorVersion(),
                projectVersion.getMinorVersion()
            );

        int mappingsCreated = 0;

        // Create compatibility mappings for all combinations
        for (SpringBootVersion bootVer : allBootVersions) {
            for (ProjectVersion projVer : allProjectVersions) {
                // Skip if mapping already exists
                if (compatibilityRepository.existsBySpringBootVersionAndCompatibleProjectVersion(
                        bootVer, projVer)) {
                    continue;
                }

                SpringBootCompatibility compatibility = SpringBootCompatibility.builder()
                    .springBootVersion(bootVer)
                    .compatibleProjectVersion(projVer)
                    .build();

                compatibilityRepository.save(compatibility);
                mappingsCreated++;
            }
        }

        if (mappingsCreated > 0) {
            result.setCompatibilityMappingsCreated(result.getCompatibilityMappingsCreated() + mappingsCreated);
            log.debug("Created {} compatibility mappings: Spring Boot {}.{}.x -> {} {}.{}.x",
                mappingsCreated,
                springBootVersion.getMajorVersion(),
                springBootVersion.getMinorVersion(),
                projectVersion.getProject().getName(),
                projectVersion.getMajorVersion(),
                projectVersion.getMinorVersion());
        }
    }

    /**
     * Ensure a Spring project exists, create if not.
     */
    private SpringProject ensureProject(String slug, String name, String description) {
        return springProjectRepository.findBySlug(slug)
            .orElseGet(() -> {
                log.info("Creating Spring project: {} ({})", name, slug);
                SpringProject project = SpringProject.builder()
                    .name(name)
                    .slug(slug)
                    .description(description != null ? description : name)
                    .homepageUrl("https://spring.io/projects/" + slug)
                    .githubUrl("https://github.com/spring-projects/" + slug)
                    .active(true)
                    .build();
                return springProjectRepository.save(project);
            });
    }

    /**
     * Format project name from slug (spring-data-jpa -> Spring Data JPA).
     */
    private String formatProjectName(String slug) {
        if (slug == null) return "Unknown";

        String[] parts = slug.split("-");
        StringBuilder name = new StringBuilder();

        for (String part : parts) {
            if (name.length() > 0) {
                name.append(" ");
            }
            name.append(Character.toUpperCase(part.charAt(0)))
                .append(part.substring(1));
        }

        return name.toString();
    }

    /**
     * Parse version string into components.
     */
    private VersionInfo parseVersion(String versionString) {
        String cleanVersion = versionString.replaceAll("\\.(x|RELEASE|BUILD-SNAPSHOT|RC\\d+|M\\d+)$", "");
        Matcher matcher = VERSION_PATTERN.matcher(cleanVersion);

        if (matcher.find()) {
            return new VersionInfo(
                Integer.parseInt(matcher.group(1)),
                Integer.parseInt(matcher.group(2)),
                matcher.groupCount() >= 3 ? Integer.parseInt(matcher.group(3)) : 0
            );
        }

        log.warn("Failed to parse version: {}, using 0.0.0", versionString);
        return new VersionInfo(0, 0, 0);
    }

    /**
     * Determine version state from version string.
     */
    private VersionState determineVersionState(String versionString) {
        if (versionString.contains("BUILD-SNAPSHOT") || versionString.contains("SNAPSHOT")) {
            return VersionState.SNAPSHOT;
        } else if (versionString.contains("RC")) {
            return VersionState.RC;
        } else if (versionString.contains("M")) {
            return VersionState.MILESTONE;
        } else if (versionString.endsWith(".x") || versionString.contains("RELEASE")) {
            return VersionState.GA;
        }
        return VersionState.GA;
    }

    /**
     * Build a map of Spring Boot version to support dates from API response.
     */
    private Map<String, SupportDates> buildSupportDatesMap(SpringGenerationsResponse response) {
        Map<String, SupportDates> map = new HashMap<>();

        try {
            if (response.getResult() != null &&
                response.getResult().getData() != null &&
                response.getResult().getData().getSpringBootProject() != null &&
                response.getResult().getData().getSpringBootProject().getFields() != null &&
                response.getResult().getData().getSpringBootProject().getFields().getSupport() != null) {

                List<SpringGenerationsResponse.Generation> generations =
                    response.getResult().getData().getSpringBootProject()
                           .getFields().getSupport().getGenerations();

                if (generations != null) {
                    for (SpringGenerationsResponse.Generation gen : generations) {
                        try {
                            SupportDates supportDates = new SupportDates(
                                parseYearMonth(gen.getInitialRelease()),
                                parseYearMonth(gen.getOssSupportEnd()),
                                parseYearMonth(gen.getEnterpriseSupportEnd())
                            );
                            map.put(gen.getGeneration(), supportDates);
                        } catch (Exception e) {
                            log.warn("Failed to parse support dates for version: {}", gen.getGeneration(), e);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error building support dates map", e);
        }

        return map;
    }

    /**
     * Parse YYYY-MM format to LocalDate (first day of month).
     */
    private LocalDate parseYearMonth(String yearMonth) {
        if (yearMonth == null || yearMonth.isBlank()) {
            return null;
        }

        try {
            YearMonth ym = YearMonth.parse(yearMonth, DateTimeFormatter.ofPattern("yyyy-MM"));
            return ym.atDay(1);
        } catch (DateTimeParseException e) {
            log.warn("Failed to parse year-month: {}", yearMonth, e);
            return null;
        }
    }

    /**
     * Find Spring Boot version with smart matching.
     * Handles version patterns like "3.5.x" by matching to actual versions like "3.5.7".
     *
     * Strategy:
     * 1. Try exact match first (e.g., "3.5.7" → "3.5.7")
     * 2. If not found and version ends with ".x", extract major.minor and find matching versions
     * 3. Prefer current/GA versions over snapshots
     *
     * @param versionPattern Version string which may be exact (e.g., "3.5.7") or pattern (e.g., "3.5.x")
     * @return Optional containing the matched SpringBootVersion, or empty if no match found
     */
    private Optional<SpringBootVersion> findSpringBootVersion(String versionPattern) {
        // Try exact match first
        Optional<SpringBootVersion> exactMatch = springBootVersionRepository.findByVersion(versionPattern);
        if (exactMatch.isPresent()) {
            return exactMatch;
        }

        // If version ends with ".x", try to find by major.minor
        if (versionPattern != null && versionPattern.endsWith(".x")) {
            // Extract major.minor from pattern (e.g., "3.5" from "3.5.x")
            String[] parts = versionPattern.split("\\.");
            if (parts.length >= 2) {
                try {
                    int major = Integer.parseInt(parts[0]);
                    int minor = Integer.parseInt(parts[1]);

                    // Find all versions with this major.minor
                    List<SpringBootVersion> candidates = springBootVersionRepository
                        .findByMajorVersionAndMinorVersionOrderByPatchVersionDesc(major, minor);

                    if (!candidates.isEmpty()) {
                        // Prefer current version, then GA, then latest
                        Optional<SpringBootVersion> current = candidates.stream()
                            .filter(v -> Boolean.TRUE.equals(v.getIsCurrent()))
                            .findFirst();
                        if (current.isPresent()) {
                            return current;
                        }

                        // Prefer GA over other states
                        Optional<SpringBootVersion> ga = candidates.stream()
                            .filter(v -> v.getState() == VersionState.GA)
                            .findFirst();
                        if (ga.isPresent()) {
                            return ga;
                        }

                        // Return the latest (highest patch version)
                        return Optional.of(candidates.get(0));
                    }
                } catch (NumberFormatException e) {
                    log.warn("Failed to parse major/minor from version pattern: {}", versionPattern, e);
                }
            }
        }

        return Optional.empty();
    }

    /**
     * Support dates holder.
     */
    private static class SupportDates {
        private final LocalDate initialRelease;
        private final LocalDate ossSupportEnd;
        private final LocalDate enterpriseSupportEnd;

        public SupportDates(LocalDate initialRelease, LocalDate ossSupportEnd, LocalDate enterpriseSupportEnd) {
            this.initialRelease = initialRelease;
            this.ossSupportEnd = ossSupportEnd;
            this.enterpriseSupportEnd = enterpriseSupportEnd;
        }

        public LocalDate getInitialRelease() { return initialRelease; }
        public LocalDate getOssSupportEnd() { return ossSupportEnd; }
        public LocalDate getEnterpriseSupportEnd() { return enterpriseSupportEnd; }
    }

    /**
     * Version info holder.
     */
    private static class VersionInfo {
        private final int major;
        private final int minor;
        private final int patch;

        public VersionInfo(int major, int minor, int patch) {
            this.major = major;
            this.minor = minor;
            this.patch = patch;
        }

        public int getMajor() { return major; }
        public int getMinor() { return minor; }
        public int getPatch() { return patch; }
    }

    /**
     * Sync result holder.
     */
    public static class SyncResult {
        private boolean success;
        private int projectsCreated = 0;
        private int versionsCreated = 0;
        private int compatibilityMappingsCreated = 0;
        private int errorsEncountered = 0;
        private String errorMessage;

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public int getProjectsCreated() { return projectsCreated; }
        public void setProjectsCreated(int projectsCreated) { this.projectsCreated = projectsCreated; }

        public int getVersionsCreated() { return versionsCreated; }
        public void setVersionsCreated(int versionsCreated) { this.versionsCreated = versionsCreated; }

        public int getCompatibilityMappingsCreated() { return compatibilityMappingsCreated; }
        public void setCompatibilityMappingsCreated(int compatibilityMappingsCreated) {
            this.compatibilityMappingsCreated = compatibilityMappingsCreated;
        }

        public int getErrorsEncountered() { return errorsEncountered; }
        public void setErrorsEncountered(int errorsEncountered) { this.errorsEncountered = errorsEncountered; }

        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    }
}
