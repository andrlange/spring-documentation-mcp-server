package com.spring.mcp.service.github;

import com.spring.mcp.config.GitHubProperties;
import com.spring.mcp.model.entity.*;
import com.spring.mcp.model.enums.VersionState;
import com.spring.mcp.repository.*;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Service for syncing documentation from GitHub repositories.
 *
 * This service provides a GitHub-first approach to documentation loading,
 * fetching AsciiDoc source files directly from Spring GitHub repositories
 * and converting them to Markdown for storage.
 *
 * It can be used alongside or as an alternative to the existing spring.io
 * based documentation sync.
 *
 * @author Spring MCP Server
 * @version 1.0.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GitHubDocumentationSyncService {

    private final GitHubProperties gitHubProperties;
    private final GitHubDocumentationDiscoveryService discoveryService;
    private final GitHubContentFetchService contentFetchService;
    private final AsciiDocToMarkdownConverter asciiDocConverter;
    private final GitHubCodeExampleService codeExampleService;

    private final SpringProjectRepository projectRepository;
    private final ProjectVersionRepository versionRepository;
    private final DocumentationLinkRepository linkRepository;
    private final DocumentationContentRepository contentRepository;
    private final DocumentationTypeRepository docTypeRepository;
    private final CodeExampleRepository codeExampleRepository;

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Documentation type slug for GitHub-sourced documentation.
     */
    private static final String GITHUB_DOC_TYPE_SLUG = "github-reference";
    private static final String GITHUB_DOC_TYPE_NAME = "GitHub Reference";

    private DocumentationType githubDocType;

    /**
     * Initialize the GitHub documentation type.
     */
    @PostConstruct
    public void init() {
        // Ensure the GitHub documentation type exists
        githubDocType = docTypeRepository.findBySlug(GITHUB_DOC_TYPE_SLUG)
            .orElseGet(() -> {
                DocumentationType docType = DocumentationType.builder()
                    .name(GITHUB_DOC_TYPE_NAME)
                    .slug(GITHUB_DOC_TYPE_SLUG)
                    .displayOrder(100)
                    .build();
                return docTypeRepository.save(docType);
            });
        log.debug("GitHub documentation type initialized: {}", githubDocType.getId());
    }

    /**
     * Sync documentation for all supported projects from GitHub.
     *
     * Note: This method intentionally does NOT have @Transactional annotation.
     * Each project sync runs in its own isolated transaction (REQUIRES_NEW)
     * so that one project failure doesn't roll back all other projects.
     *
     * @return sync result with statistics
     */
    public GitHubSyncResult syncAll() {
        log.info("=".repeat(60));
        log.info("GITHUB DOCUMENTATION SYNC STARTED");
        log.info("=".repeat(60));

        GitHubSyncResult result = new GitHubSyncResult();
        result.setStartTime(LocalDateTime.now());

        try {
            // Get all projects that are supported by GitHub discovery
            List<SpringProject> projects = projectRepository.findAll();
            Set<String> supportedProjects = discoveryService.getSupportedProjects();

            int processedCount = 0;

            for (SpringProject project : projects) {
                if (!supportedProjects.contains(project.getSlug())) {
                    log.debug("Project {} not supported for GitHub sync, skipping", project.getSlug());
                    continue;
                }

                try {
                    // Each project sync runs in its own transaction (REQUIRES_NEW)
                    // This isolates failures so one project doesn't affect others
                    ProjectSyncResult projectResult = syncProject(project);
                    result.addProjectResult(projectResult);
                    processedCount++;

                    if (processedCount % 5 == 0) {
                        log.info("Progress: {} projects processed", processedCount);
                    }

                } catch (Exception e) {
                    log.error("Error syncing project {}: {}", project.getSlug(), e.getMessage(), e);
                    result.addError(project.getSlug(), e.getMessage());

                    // Clear the entity manager to reset session state after a failure
                    // This prevents "null identifier" errors from corrupting subsequent operations
                    try {
                        entityManager.clear();
                        log.debug("Entity manager cleared after project sync failure");
                    } catch (Exception clearEx) {
                        log.warn("Failed to clear entity manager: {}", clearEx.getMessage());
                    }
                }
            }

            result.setSuccess(result.getTotalErrors() == 0 || result.getTotalErrors() < processedCount / 2);

        } catch (Exception e) {
            log.error("GitHub documentation sync failed", e);
            result.setSuccess(false);
            result.setErrorMessage(e.getMessage());
        } finally {
            result.setEndTime(LocalDateTime.now());

            log.info("=".repeat(60));
            log.info("GITHUB DOCUMENTATION SYNC COMPLETED");
            log.info("Duration: {} seconds",
                java.time.Duration.between(result.getStartTime(), result.getEndTime()).getSeconds());
            log.info("Projects Synced: {}", result.getProjectsSynced());
            log.info("Documentation Files: {}", result.getTotalDocumentationFiles());
            log.info("Code Examples: {}", result.getTotalCodeExamples());
            log.info("Versions Skipped (already synced): {}", result.getTotalVersionsSkipped());
            log.info("Errors: {}", result.getTotalErrors());
            log.info("Status: {}", result.isSuccess() ? "SUCCESS" : "FAILED");
            log.info("=".repeat(60));
        }

        return result;
    }

    /**
     * Sync documentation for a specific project.
     *
     * Uses REQUIRES_NEW propagation to ensure this project sync runs in its own
     * isolated transaction. If this sync fails, it won't affect other projects.
     *
     * @param project the project to sync
     * @return sync result for the project
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ProjectSyncResult syncProject(SpringProject project) {
        log.info("Syncing documentation for project: {}", project.getSlug());

        ProjectSyncResult result = new ProjectSyncResult();
        result.setProjectSlug(project.getSlug());

        // Get all versions for this project
        List<ProjectVersion> versions = versionRepository.findByProject(project);

        if (versions.isEmpty()) {
            log.debug("No versions found for project: {}", project.getSlug());
            return result;
        }

        // Only sync the latest version and maybe a few recent versions
        List<ProjectVersion> versionsToSync = selectVersionsToSync(versions);

        for (ProjectVersion version : versionsToSync) {
            try {
                VersionSyncResult versionResult = syncVersion(project, version);
                result.addVersionResult(versionResult);

            } catch (Exception e) {
                log.error("Error syncing version {} for project {}: {}",
                         version.getVersion(), project.getSlug(), e.getMessage());
                result.addError(version.getVersion(), e.getMessage());
            }
        }

        return result;
    }

    /**
     * Sync documentation for a specific project version.
     *
     * Note: This method is called from within syncProject() which already has
     * REQUIRES_NEW propagation, so we use the default REQUIRED propagation here
     * to join the project's transaction.
     *
     * For GA (stable) versions, documentation is static and won't change, so we skip
     * re-fetching if content already exists. For SNAPSHOT/RC/MILESTONE versions,
     * documentation may change, so we always re-sync.
     *
     * @param project the project
     * @param version the version to sync
     * @return sync result for the version
     */
    @Transactional
    public VersionSyncResult syncVersion(SpringProject project, ProjectVersion version) {
        String projectSlug = project.getSlug();
        String versionStr = version.getVersion();

        log.debug("Syncing GitHub documentation for {} version {}", projectSlug, versionStr);

        VersionSyncResult result = new VersionSyncResult();
        result.setVersion(versionStr);

        try {
            // Check if this is a stable (GA) version that already has documentation
            // For GA versions, documentation is static and doesn't change
            boolean isStableVersion = version.getState() == VersionState.GA;
            long existingDocCount = linkRepository.countByVersionAndDocTypeAndIsActiveTrue(version, githubDocType);
            long existingCodeExampleCount = codeExampleRepository.countByVersion(version);

            if (isStableVersion && existingDocCount > 0) {
                log.info("Skipping GitHub docs sync for {} {} - already has {} docs (GA version is static)",
                        projectSlug, versionStr, existingDocCount);
                result.setSkipped(true);
                result.setSkipReason("GA version already synced");
                result.setSuccess(true);

                // Still check code examples for GA versions
                if (existingCodeExampleCount == 0) {
                    log.debug("Syncing code examples for {} {} (no existing examples)", projectSlug, versionStr);
                    int codeExamples = codeExampleService.syncCodeExamples(version);
                    result.setCodeExamplesCreated(codeExamples);
                } else {
                    log.debug("Skipping code examples sync for {} {} - already has {} examples",
                            projectSlug, versionStr, existingCodeExampleCount);
                }

                return result;
            }

            // For non-GA versions or versions without docs, proceed with sync
            if (!isStableVersion && existingDocCount > 0) {
                log.info("Re-syncing {} {} ({} state) - {} existing docs may be outdated",
                        projectSlug, versionStr, version.getState(), existingDocCount);
            }

            // Discover and fetch documentation files
            Map<String, String> docFiles = contentFetchService.fetchAllDocumentation(
                projectSlug, versionStr, discoveryService);

            // Process each documentation file
            for (Map.Entry<String, String> entry : docFiles.entrySet()) {
                String filePath = entry.getKey();
                String adocContent = entry.getValue();

                try {
                    // Convert AsciiDoc to Markdown
                    String markdown = asciiDocConverter.convert(adocContent);

                    if (markdown != null && !markdown.isBlank()) {
                        // Save the documentation content
                        saveDocumentationContent(version, filePath, adocContent, markdown);
                        result.addDocumentationFile();
                    }

                } catch (Exception e) {
                    log.warn("Error converting file {}: {}", filePath, e.getMessage());
                    result.addConversionError();
                }
            }

            // Sync code examples (skip if GA version already has examples)
            if (isStableVersion && existingCodeExampleCount > 0) {
                log.debug("Skipping code examples sync for {} {} - already has {} examples",
                        projectSlug, versionStr, existingCodeExampleCount);
            } else {
                int codeExamples = codeExampleService.syncCodeExamples(version);
                result.setCodeExamplesCreated(codeExamples);
            }

            result.setSuccess(true);

        } catch (Exception e) {
            log.error("Error syncing version {}: {}", versionStr, e.getMessage());
            result.setSuccess(false);
            result.setErrorMessage(e.getMessage());
        }

        return result;
    }

    /**
     * Sync documentation for a specific project by slug.
     *
     * Uses REQUIRES_NEW propagation to ensure this sync runs in its own
     * isolated transaction.
     *
     * @param projectSlug the project slug
     * @return sync result
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ProjectSyncResult syncProjectBySlug(String projectSlug) {
        Optional<SpringProject> projectOpt = projectRepository.findBySlug(projectSlug);

        if (projectOpt.isEmpty()) {
            ProjectSyncResult result = new ProjectSyncResult();
            result.setProjectSlug(projectSlug);
            result.addError("Project not found", "No project found with slug: " + projectSlug);
            return result;
        }

        return syncProject(projectOpt.get());
    }

    /**
     * Select which versions to sync for a project.
     * By default, sync latest + n-2 minor versions.
     */
    private List<ProjectVersion> selectVersionsToSync(List<ProjectVersion> versions) {
        List<ProjectVersion> toSync = new ArrayList<>();

        // Find the latest version
        Optional<ProjectVersion> latestOpt = versions.stream()
            .filter(ProjectVersion::getIsLatest)
            .findFirst();

        if (latestOpt.isPresent()) {
            toSync.add(latestOpt.get());
        }

        // Add up to 2 more recent GA versions
        versions.stream()
            .filter(v -> v.getState() == VersionState.GA)
            .filter(v -> !v.getIsLatest())
            .sorted((a, b) -> compareVersions(b.getVersion(), a.getVersion()))
            .limit(2)
            .forEach(toSync::add);

        return toSync;
    }

    /**
     * Save documentation content to the database using the existing entity structure.
     */
    private void saveDocumentationContent(ProjectVersion version, String filePath,
                                          String originalContent, String markdownContent) {
        // Extract title from AsciiDoc
        String title = asciiDocConverter.extractTitle(originalContent);
        if (title == null) {
            title = extractTitleFromPath(filePath);
        }

        // Extract description
        String description = asciiDocConverter.extractDescription(originalContent);

        // Build the GitHub URL
        String githubUrl = buildGitHubUrl(version.getProject().getSlug(), filePath, version.getVersion());

        // Check if a link already exists for this URL
        Optional<DocumentationLink> existingLinkOpt = linkRepository.findByUrl(githubUrl);

        DocumentationLink link;
        if (existingLinkOpt.isPresent()) {
            // Update existing link
            link = existingLinkOpt.get();
            link.setTitle(title);
            link.setDescription(description);
            link.setLastFetched(LocalDateTime.now());
            link.setUpdatedAt(LocalDateTime.now());
        } else {
            // Create new link
            link = DocumentationLink.builder()
                .version(version)
                .docType(githubDocType)
                .title(title)
                .url(githubUrl)
                .description(description)
                .isActive(true)
                .lastFetched(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        }

        link = linkRepository.save(link);

        // Create or update content
        DocumentationContent content = link.getContent();
        if (content != null) {
            content.setContent(markdownContent);
            content.setContentType("text/markdown");
            content.setUpdatedAt(LocalDateTime.now());

            // Store metadata
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("source", "github");
            metadata.put("filePath", filePath);
            metadata.put("format", "asciidoc");
            content.setMetadata(metadata);
        } else {
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("source", "github");
            metadata.put("filePath", filePath);
            metadata.put("format", "asciidoc");

            content = DocumentationContent.builder()
                .link(link)
                .content(markdownContent)
                .contentType("text/markdown")
                .metadata(metadata)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

            link.setContent(content);
        }

        contentRepository.save(content);
        log.debug("Saved documentation content: {}", title);
    }

    /**
     * Extract title from file path.
     */
    private String extractTitleFromPath(String filePath) {
        String fileName = filePath;

        int lastSlash = fileName.lastIndexOf('/');
        if (lastSlash >= 0) {
            fileName = fileName.substring(lastSlash + 1);
        }

        // Remove extension
        int lastDot = fileName.lastIndexOf('.');
        if (lastDot >= 0) {
            fileName = fileName.substring(0, lastDot);
        }

        // Convert to title case
        String title = fileName.replaceAll("-", " ")
                               .replaceAll("_", " ")
                               .replaceAll("(.)([A-Z])", "$1 $2");

        // Capitalize first letter
        if (!title.isEmpty()) {
            title = Character.toUpperCase(title.charAt(0)) + title.substring(1);
        }

        return title;
    }

    /**
     * Build GitHub URL for source file.
     */
    private String buildGitHubUrl(String projectSlug, String filePath, String version) {
        String tag = discoveryService.getGitTag(projectSlug, version);
        String org = gitHubProperties.getApi().getOrganization();
        return String.format("https://github.com/%s/%s/blob/%s/%s", org, projectSlug, tag, filePath);
    }

    /**
     * Compare version strings.
     */
    private int compareVersions(String v1, String v2) {
        v1 = v1.replaceFirst("^[vV]", "");
        v2 = v2.replaceFirst("^[vV]", "");

        String[] parts1 = v1.split("[.\\-]");
        String[] parts2 = v2.split("[.\\-]");

        int maxLength = Math.max(parts1.length, parts2.length);

        for (int i = 0; i < maxLength; i++) {
            String part1 = i < parts1.length ? parts1[i] : "0";
            String part2 = i < parts2.length ? parts2[i] : "0";

            try {
                int num1 = Integer.parseInt(part1);
                int num2 = Integer.parseInt(part2);
                if (num1 != num2) {
                    return num1 - num2;
                }
            } catch (NumberFormatException e) {
                int cmp = part1.compareTo(part2);
                if (cmp != 0) {
                    return cmp;
                }
            }
        }

        return 0;
    }

    // ========== Result Classes ==========

    /**
     * Result of the overall GitHub sync operation.
     */
    @Data
    @NoArgsConstructor
    public static class GitHubSyncResult {
        private boolean success;
        private String errorMessage;
        private LocalDateTime startTime;
        private LocalDateTime endTime;

        private int projectsSynced;
        private int totalDocumentationFiles;
        private int totalCodeExamples;
        private int totalVersionsSkipped;
        private int totalErrors;

        private List<ProjectSyncResult> projectResults = new ArrayList<>();
        private List<String> errors = new ArrayList<>();

        public void addProjectResult(ProjectSyncResult result) {
            projectResults.add(result);
            if (result.isSuccess()) {
                projectsSynced++;
                totalDocumentationFiles += result.getDocumentationFilesCreated();
                totalCodeExamples += result.getCodeExamplesCreated();
                totalVersionsSkipped += result.getVersionsSkipped();
            } else {
                totalErrors++;
            }
        }

        public void addError(String projectSlug, String message) {
            errors.add(projectSlug + ": " + message);
            totalErrors++;
        }
    }

    /**
     * Result of syncing a single project.
     */
    @Data
    @NoArgsConstructor
    public static class ProjectSyncResult {
        private String projectSlug;
        private boolean success = true;

        private int documentationFilesCreated;
        private int codeExamplesCreated;
        private int versionsSynced;
        private int versionsSkipped;
        private int errorsEncountered;

        private List<VersionSyncResult> versionResults = new ArrayList<>();
        private List<String> errors = new ArrayList<>();

        public void addVersionResult(VersionSyncResult result) {
            versionResults.add(result);
            if (result.isSuccess()) {
                if (result.isSkipped()) {
                    versionsSkipped++;
                } else {
                    versionsSynced++;
                    documentationFilesCreated += result.getDocumentationFilesCreated();
                    codeExamplesCreated += result.getCodeExamplesCreated();
                }
            } else {
                errorsEncountered++;
            }
        }

        public void addError(String version, String message) {
            errors.add(version + ": " + message);
            errorsEncountered++;
            success = false;
        }
    }

    /**
     * Result of syncing a single version.
     */
    @Data
    @NoArgsConstructor
    public static class VersionSyncResult {
        private String version;
        private boolean success;
        private boolean skipped;
        private String skipReason;
        private String errorMessage;

        private int documentationFilesCreated;
        private int codeExamplesCreated;
        private int conversionErrors;

        public void addDocumentationFile() {
            documentationFilesCreated++;
        }

        public void addConversionError() {
            conversionErrors++;
        }
    }
}
