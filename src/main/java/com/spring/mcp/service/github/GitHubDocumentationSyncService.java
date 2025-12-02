package com.spring.mcp.service.github;

import com.spring.mcp.config.GitHubProperties;
import com.spring.mcp.model.entity.*;
import com.spring.mcp.model.enums.VersionState;
import com.spring.mcp.repository.*;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.PostConstruct;
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
     * @return sync result with statistics
     */
    @Transactional
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
                    ProjectSyncResult projectResult = syncProject(project);
                    result.addProjectResult(projectResult);
                    processedCount++;

                    if (processedCount % 5 == 0) {
                        log.info("Progress: {} projects processed", processedCount);
                    }

                } catch (Exception e) {
                    log.error("Error syncing project {}: {}", project.getSlug(), e.getMessage());
                    result.addError(project.getSlug(), e.getMessage());
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
            log.info("Errors: {}", result.getTotalErrors());
            log.info("Status: {}", result.isSuccess() ? "SUCCESS" : "FAILED");
            log.info("=".repeat(60));
        }

        return result;
    }

    /**
     * Sync documentation for a specific project.
     *
     * @param project the project to sync
     * @return sync result for the project
     */
    @Transactional
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

            // Sync code examples
            int codeExamples = codeExampleService.syncCodeExamples(version);
            result.setCodeExamplesCreated(codeExamples);

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
     * @param projectSlug the project slug
     * @return sync result
     */
    @Transactional
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
        private int totalErrors;

        private List<ProjectSyncResult> projectResults = new ArrayList<>();
        private List<String> errors = new ArrayList<>();

        public void addProjectResult(ProjectSyncResult result) {
            projectResults.add(result);
            if (result.isSuccess()) {
                projectsSynced++;
                totalDocumentationFiles += result.getDocumentationFilesCreated();
                totalCodeExamples += result.getCodeExamplesCreated();
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
        private int errorsEncountered;

        private List<VersionSyncResult> versionResults = new ArrayList<>();
        private List<String> errors = new ArrayList<>();

        public void addVersionResult(VersionSyncResult result) {
            versionResults.add(result);
            if (result.isSuccess()) {
                versionsSynced++;
                documentationFilesCreated += result.getDocumentationFilesCreated();
                codeExamplesCreated += result.getCodeExamplesCreated();
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
