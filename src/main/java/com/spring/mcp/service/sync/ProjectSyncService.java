package com.spring.mcp.service.sync;

import com.spring.mcp.model.dto.springio.SpringInitializrMetadata;
import com.spring.mcp.model.entity.ProjectVersion;
import com.spring.mcp.model.entity.SpringProject;
import com.spring.mcp.model.enums.VersionState;
import com.spring.mcp.repository.ProjectVersionRepository;
import com.spring.mcp.repository.SpringProjectRepository;
import com.spring.mcp.util.VersionParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service for synchronizing Spring projects and versions from external sources.
 *
 * @author Spring MCP Server
 * @version 1.0
 * @since 2025-01-08
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProjectSyncService {

    private final SpringInitializrClient initializrClient;
    private final SpringProjectRepository springProjectRepository;
    private final ProjectVersionRepository projectVersionRepository;

    // Pattern to parse version numbers like "3.5.7", "4.0.0", etc.
    private static final Pattern VERSION_PATTERN = Pattern.compile("(\\d+)\\.(\\d+)\\.(\\d+)");

    /**
     * Synchronize Spring Boot project and versions.
     *
     * @return SyncResult with statistics
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public SyncResult syncSpringBoot() {
        log.info("Starting Spring Boot sync");
        SyncResult result = new SyncResult();

        try {
            // Ensure Spring Boot project exists
            SpringProject springBoot = ensureSpringBootProject();
            result.setProjectsCreated(result.getProjectsCreated() + 1);

            // Fetch metadata from Spring Initializr
            SpringInitializrMetadata metadata = initializrClient.fetchMetadata();

            if (metadata == null || metadata.getBootVersion() == null) {
                throw new RuntimeException("Failed to fetch Spring Initializr metadata");
            }

            // Process boot versions
            List<SpringInitializrMetadata.VersionValue> versions = metadata.getBootVersion().getValues();
            String defaultVersionId = metadata.getBootVersion().getDefaultVersion();

            log.info("Processing {} Spring Boot versions", versions.size());

            for (SpringInitializrMetadata.VersionValue versionValue : versions) {
                try {
                    ProjectVersion version = processVersion(springBoot, versionValue, defaultVersionId);
                    if (version != null) {
                        result.setVersionsCreated(result.getVersionsCreated() + 1);
                    }
                } catch (Exception e) {
                    log.error("Error processing version: {}", versionValue.getId(), e);
                    result.setErrorsEncountered(result.getErrorsEncountered() + 1);
                }
            }

            // Mark default version
            markDefaultVersion(springBoot, defaultVersionId);

            result.setSuccess(true);
            log.info("Spring Boot sync completed successfully. Created {} versions", result.getVersionsCreated());

        } catch (Exception e) {
            log.error("Error during Spring Boot sync", e);
            result.setSuccess(false);
            result.setErrorMessage(e.getMessage());
        }

        return result;
    }

    /**
     * Ensure Spring Boot project exists in the database.
     *
     * @return SpringProject entity
     */
    private SpringProject ensureSpringBootProject() {
        return springProjectRepository.findBySlug("spring-boot")
            .orElseGet(() -> {
                log.info("Creating Spring Boot project");
                SpringProject project = SpringProject.builder()
                    .name("Spring Boot")
                    .slug("spring-boot")
                    .description("Spring Boot makes it easy to create stand-alone, production-grade Spring based Applications")
                    .homepageUrl("https://spring.io/projects/spring-boot")
                    .githubUrl("https://github.com/spring-projects/spring-boot")
                    .active(true)
                    .build();
                return springProjectRepository.save(project);
            });
    }

    /**
     * Process a single version from Spring Initializr.
     *
     * @param project Spring Boot project
     * @param versionValue version info from API
     * @param defaultVersionId the default version ID
     * @return created or updated ProjectVersion
     */
    private ProjectVersion processVersion(SpringProject project,
                                         SpringInitializrMetadata.VersionValue versionValue,
                                         String defaultVersionId) {
        String versionId = versionValue.getId();
        String versionName = versionValue.getName();

        log.debug("Processing version: {} ({})", versionId, versionName);

        // Check if version already exists
        if (projectVersionRepository.existsByProjectAndVersion(project, versionId)) {
            log.debug("Version already exists: {}", versionId);
            return null;
        }

        // Parse version numbers using VersionParser
        VersionParser.ParsedVersion parsedVersion = VersionParser.parse(versionId);

        // Determine version state
        VersionState versionState = determineVersionState(versionId);

        // Determine if this is the latest version
        boolean isLatest = versionState == VersionState.GA && versionId.equals(defaultVersionId);

        // Create and save version
        ProjectVersion version = ProjectVersion.builder()
            .project(project)
            .version(versionId)
            .majorVersion(parsedVersion.getMajorVersion())
            .minorVersion(parsedVersion.getMinorVersion())
            .patchVersion(parsedVersion.getPatchVersion())
            .state(versionState)
            .isLatest(isLatest)
            .isDefault(versionId.equals(defaultVersionId))
            .releaseDate(LocalDate.now()) // We don't have real release date from this API
            .build();

        ProjectVersion saved = projectVersionRepository.save(version);
        log.info("Created version: {} (state: {}, latest: {})", versionId, versionState, isLatest);

        return saved;
    }

    /**
     * Parse version string into major, minor, patch components.
     *
     * @param versionString version string like "3.5.7.RELEASE"
     * @return VersionInfo with parsed components
     */
    private VersionInfo parseVersion(String versionString) {
        // Remove suffixes like .RELEASE, .BUILD-SNAPSHOT, .RC2, etc.
        String cleanVersion = versionString.replaceAll("\\.(RELEASE|BUILD-SNAPSHOT|RC\\d+|M\\d+)$", "");

        Matcher matcher = VERSION_PATTERN.matcher(cleanVersion);
        if (matcher.find()) {
            return new VersionInfo(
                Integer.parseInt(matcher.group(1)),
                Integer.parseInt(matcher.group(2)),
                Integer.parseInt(matcher.group(3))
            );
        }

        // Fallback to 0.0.0 if parsing fails
        log.warn("Failed to parse version: {}, using 0.0.0", versionString);
        return new VersionInfo(0, 0, 0);
    }

    /**
     * Determine version state from version string.
     *
     * @param versionString version string
     * @return VersionState enum value
     */
    private VersionState determineVersionState(String versionString) {
        if (versionString.contains("BUILD-SNAPSHOT") || versionString.contains("SNAPSHOT")) {
            return VersionState.SNAPSHOT;
        } else if (versionString.contains("RC")) {
            return VersionState.RC;
        } else if (versionString.contains("M")) {
            return VersionState.MILESTONE;
        } else if (versionString.contains("RELEASE") || versionString.matches("\\d+\\.\\d+\\.\\d+")) {
            return VersionState.GA;
        }
        return VersionState.GA;
    }

    /**
     * Mark the default/latest version for the project.
     *
     * @param project Spring project
     * @param defaultVersionId default version ID
     */
    private void markDefaultVersion(SpringProject project, String defaultVersionId) {
        // Reset all versions to not be latest
        List<ProjectVersion> allVersions = projectVersionRepository.findByProject(project);

        for (ProjectVersion version : allVersions) {
            if (version.getIsLatest() && !version.getVersion().equals(defaultVersionId)) {
                version.setIsLatest(false);
                projectVersionRepository.save(version);
            }
        }

        // Set the default version as latest
        projectVersionRepository.findByProjectAndVersion(project, defaultVersionId)
            .ifPresent(version -> {
                if (!version.getIsLatest()) {
                    version.setIsLatest(true);
                    projectVersionRepository.save(version);
                    log.info("Marked {} as latest version", defaultVersionId);
                }
            });
    }

    /**
     * DTO for version information.
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
     * Result DTO for sync operations.
     */
    public static class SyncResult {
        private boolean success;
        private int projectsCreated = 0;
        private int versionsCreated = 0;
        private int errorsEncountered = 0;
        private String errorMessage;

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public int getProjectsCreated() { return projectsCreated; }
        public void setProjectsCreated(int projectsCreated) { this.projectsCreated = projectsCreated; }

        public int getVersionsCreated() { return versionsCreated; }
        public void setVersionsCreated(int versionsCreated) { this.versionsCreated = versionsCreated; }

        public int getErrorsEncountered() { return errorsEncountered; }
        public void setErrorsEncountered(int errorsEncountered) { this.errorsEncountered = errorsEncountered; }

        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    }
}
