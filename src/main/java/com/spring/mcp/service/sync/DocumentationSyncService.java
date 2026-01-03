package com.spring.mcp.service.sync;

import com.spring.mcp.model.entity.*;
import com.spring.mcp.repository.*;
import com.spring.mcp.service.documentation.DocumentationFetchService;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Service responsible for syncing documentation content for Spring projects.
 * Fetches OVERVIEW content from spring.io/projects/{slug} and stores it as Markdown.
 *
 * @author Spring MCP Server
 * @version 1.0.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentationSyncService {

    private final SpringProjectRepository springProjectRepository;
    private final ProjectVersionRepository projectVersionRepository;
    private final DocumentationTypeRepository documentationTypeRepository;
    private final DocumentationLinkRepository documentationLinkRepository;
    private final DocumentationContentRepository documentationContentRepository;
    private final DocumentationFetchService documentationFetchService;

    /**
     * Syncs documentation for all Spring projects by fetching OVERVIEW content
     * from spring.io/projects/{slug} and converting to Markdown.
     *
     * @return SyncResult with statistics
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public SyncResult syncAllDocumentation() {
        log.info("Starting documentation sync for all Spring projects...");
        SyncResult result = new SyncResult();
        result.setStartTime(LocalDateTime.now());

        try {
            // Get the OVERVIEW documentation type
            DocumentationType overviewType = documentationTypeRepository.findBySlug("overview")
                .orElseGet(() -> {
                    log.info("Creating OVERVIEW documentation type");
                    DocumentationType type = DocumentationType.builder()
                        .name("Overview")
                        .slug("overview")
                        .displayOrder(1)
                        .build();
                    return documentationTypeRepository.save(type);
                });

            // Get all active Spring projects
            List<SpringProject> projects = springProjectRepository.findAll().stream()
                .filter(SpringProject::getActive)
                .toList();

            log.info("Found {} active projects to sync documentation for", projects.size());

            for (SpringProject project : projects) {
                try {
                    syncProjectDocumentation(project, overviewType, result);
                } catch (Exception e) {
                    log.error("Error syncing documentation for project {}: {}",
                        project.getSlug(), e.getMessage(), e);
                    result.addError();
                }
            }

            result.setSuccess(result.getErrorsEncountered() == 0);
            result.setSummaryMessage(String.format(
                "Synced documentation for %d projects: %d links created, %d updated, %d errors",
                result.getProjectsProcessed(),
                result.getLinksCreated(),
                result.getLinksUpdated(),
                result.getErrorsEncountered()
            ));

        } catch (Exception e) {
            log.error("Error during documentation sync", e);
            result.setSuccess(false);
            result.setSummaryMessage("Documentation sync failed: " + e.getMessage());
            result.addError();
        } finally {
            result.setEndTime(LocalDateTime.now());
        }

        log.info("Documentation sync completed: {}", result.getSummaryMessage());
        return result;
    }

    /**
     * Syncs documentation for a single project
     */
    private void syncProjectDocumentation(SpringProject project, DocumentationType overviewType, SyncResult result) {
        log.debug("Syncing documentation for project: {}", project.getSlug());
        result.addProjectProcessed();

        try {
            // Fetch OVERVIEW content as Markdown
            String markdown = documentationFetchService.fetchProjectOverviewAsMarkdown(project.getSlug());

            if (markdown == null || markdown.isBlank()) {
                log.warn("No OVERVIEW content found for project: {}", project.getSlug());
                return;
            }

            // Get the latest GA version (is_latest=true), or any version with CURRENT status,
            // or fall back to the most recent GA version
            // Use the safer method that handles multiple "latest" versions due to potential data issues
            ProjectVersion latestVersion = projectVersionRepository.findTopByProjectAndIsLatestTrueOrderByCreatedAtDesc(project)
                .orElseGet(() -> projectVersionRepository.findByProjectAndStatusOrderByVersionDesc(project, "CURRENT")
                    .stream().findFirst()
                    .orElseGet(() -> projectVersionRepository.findByProject(project).stream()
                        .filter(v -> v.getState() == com.spring.mcp.model.enums.VersionState.GA)
                        .max((v1, v2) -> {
                            // Compare by major, minor, patch
                            int majorCmp = Integer.compare(v1.getMajorVersion(), v2.getMajorVersion());
                            if (majorCmp != 0) return majorCmp;
                            int minorCmp = Integer.compare(v1.getMinorVersion(), v2.getMinorVersion());
                            if (minorCmp != 0) return minorCmp;
                            return Integer.compare(
                                v1.getPatchVersion() != null ? v1.getPatchVersion() : 0,
                                v2.getPatchVersion() != null ? v2.getPatchVersion() : 0
                            );
                        })
                        .orElse(null)));

            if (latestVersion == null) {
                log.warn("No versions found for project: {}, skipping documentation", project.getSlug());
                return;
            }

            log.debug("Using version {} for project {} documentation", latestVersion.getVersion(), project.getSlug());

            // Use reference doc URL from version if available, otherwise construct from spring.io
            final String docUrl;
            String referenceUrl = latestVersion.getReferenceDocUrl();
            if (referenceUrl != null && !referenceUrl.isBlank()) {
                docUrl = referenceUrl;
            } else {
                // Fallback: use spring.io project overview page as the doc URL
                docUrl = "https://spring.io/projects/" + project.getSlug();
            }
            DocumentationLink link = documentationLinkRepository.findByUrl(docUrl)
                .orElseGet(() -> {
                    log.info("Creating documentation link for: {}", project.getSlug());
                    DocumentationLink newLink = DocumentationLink.builder()
                        .version(latestVersion)
                        .docType(overviewType)
                        .title(project.getName() + " - Documentation")
                        .url(docUrl)
                        .description("Documentation from docs.spring.io")
                        .isActive(true)
                        .build();
                    result.addLinkCreated();
                    return documentationLinkRepository.save(newLink);
                });

            // Calculate content hash
            String contentHash = documentationFetchService.calculateContentHash(markdown);

            // Check if content has changed
            if (contentHash.equals(link.getContentHash())) {
                log.debug("Documentation content unchanged for: {}", project.getSlug());
                link.setLastFetched(LocalDateTime.now());
                documentationLinkRepository.save(link);
                return;
            }

            // Update link
            link.setContentHash(contentHash);
            link.setLastFetched(LocalDateTime.now());
            link.setIsActive(true);
            documentationLinkRepository.save(link);

            // Create or update documentation content
            DocumentationContent content = documentationContentRepository.findByLink(link)
                .orElseGet(() -> {
                    DocumentationContent newContent = DocumentationContent.builder()
                        .link(link)
                        .build();
                    return newContent;
                });

            content.setContentType("text/markdown");
            content.setContent(markdown);
            content.setMetadata(java.util.Map.of(
                "source", "docs.spring.io",
                "projectSlug", project.getSlug(),
                "section", "documentation",
                "fetchedAt", LocalDateTime.now().toString(),
                "contentLength", markdown.length()
            ));

            documentationContentRepository.save(content);
            result.addLinkUpdated();

            log.info("Successfully synced documentation for: {} - Hash: {}, Size: {} chars",
                project.getSlug(), contentHash.substring(0, 8), markdown.length());

        } catch (Exception e) {
            log.error("Error syncing documentation for project {}: {}",
                project.getSlug(), e.getMessage(), e);
            result.addError();
        }
    }

    /**
     * Fix documentation links that point to placeholder versions (e.g., "1.0.x") instead of actual versions.
     * This updates the version_id to point to the latest GA version for each project.
     *
     * @return number of links fixed
     */
    @Transactional
    public int fixPlaceholderVersionLinks() {
        log.info("Starting fix for placeholder version documentation links...");
        int fixedCount = 0;

        List<DocumentationLink> allLinks = documentationLinkRepository.findAll();
        log.info("Found {} documentation links to check", allLinks.size());

        for (DocumentationLink link : allLinks) {
            if (link.getVersion() == null) {
                log.debug("Link {} has no version, skipping", link.getId());
                continue;
            }

            String version = link.getVersion().getVersion();
            log.debug("Checking link {} with version: {}", link.getId(), version);

            // Check if it's a placeholder version (ends with .x)
            if (version != null && version.endsWith(".x")) {
                SpringProject project = link.getVersion().getProject();
                if (project == null) {
                    log.warn("Link {} has version {} but project is null, skipping", link.getId(), version);
                    continue;
                }

                log.info("Found placeholder version link {} for project {}: {}",
                    link.getId(), project.getSlug(), version);

                // Find the actual latest version
                // Use the safer method that handles multiple "latest" versions due to potential data issues
                ProjectVersion latestVersion = projectVersionRepository.findTopByProjectAndIsLatestTrueOrderByCreatedAtDesc(project)
                    .orElseGet(() -> projectVersionRepository.findByProjectAndStatusOrderByVersionDesc(project, "CURRENT")
                        .stream().findFirst()
                        .orElse(null));

                if (latestVersion == null) {
                    log.warn("No latest version found for project {}", project.getSlug());
                    continue;
                }

                log.info("Found latest version for {}: {} (is_latest={}, status={})",
                    project.getSlug(), latestVersion.getVersion(),
                    latestVersion.getIsLatest(), latestVersion.getStatus());

                if (!latestVersion.getVersion().equals(version)) {
                    log.info("Fixing link {} for project {}: {} -> {}",
                        link.getId(), project.getSlug(), version, latestVersion.getVersion());

                    // Update version
                    link.setVersion(latestVersion);

                    // Update URL to use actual doc URL if available
                    if (latestVersion.getReferenceDocUrl() != null && !latestVersion.getReferenceDocUrl().isBlank()) {
                        link.setUrl(latestVersion.getReferenceDocUrl());
                    } else {
                        // Fallback to spring.io project page
                        link.setUrl("https://spring.io/projects/" + project.getSlug());
                    }

                    documentationLinkRepository.save(link);
                    fixedCount++;
                }
            }
        }

        log.info("Fixed {} documentation links with placeholder versions", fixedCount);
        return fixedCount;
    }

    /**
     * Result object for documentation sync operations
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SyncResult {
        private boolean success;
        private String summaryMessage;
        private String errorMessage;
        private LocalDateTime startTime;
        private LocalDateTime endTime;

        @Builder.Default
        private int projectsProcessed = 0;
        @Builder.Default
        private int linksCreated = 0;
        @Builder.Default
        private int linksUpdated = 0;
        @Builder.Default
        private int errorsEncountered = 0;

        public void addProjectProcessed() {
            this.projectsProcessed++;
        }

        public void addLinkCreated() {
            this.linksCreated++;
        }

        public void addLinkUpdated() {
            this.linksUpdated++;
        }

        public void addError() {
            this.errorsEncountered++;
        }
    }
}
