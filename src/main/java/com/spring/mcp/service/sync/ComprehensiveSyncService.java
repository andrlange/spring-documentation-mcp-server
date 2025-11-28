package com.spring.mcp.service.sync;

import com.spring.mcp.config.OpenRewriteFeatureConfig;
import com.spring.mcp.model.entity.SpringProject;
import com.spring.mcp.model.event.SyncProgressEvent;
import com.spring.mcp.repository.SpringProjectRepository;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Comprehensive sync service that orchestrates all project and version synchronization.
 * Ensures ALL Spring projects and their versions are loaded and updated from multiple sources.
 *
 * @author Spring MCP Server
 * @version 1.0
 * @since 2025-01-08
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ComprehensiveSyncService {

    private final SpringGenerationsSyncService generationsSyncService;
    private final ProjectSyncService projectSyncService;
    private final SpringProjectPageCrawlerService crawlerService;
    private final SpringBootVersionSyncService springBootVersionSyncService;
    private final ProjectRelationshipSyncService projectRelationshipSyncService;
    private final DocumentationSyncService documentationSyncService;
    private final CodeExamplesSyncService codeExamplesSyncService;
    private final SpringProjectRepository springProjectRepository;
    private final SyncProgressTracker progressTracker;
    private final OpenRewriteFeatureConfig openRewriteFeatureConfig;
    private final Optional<RecipeSyncService> recipeSyncService;

    /**
     * Synchronize ALL Spring projects and versions from all available sources.
     * This is the master sync method that should be called to ensure complete data.
     *
     * @return ComprehensiveSyncResult with aggregated statistics
     */
    @Transactional
    public ComprehensiveSyncResult syncAll() {
        log.info("=".repeat(80));
        log.info("COMPREHENSIVE SYNC STARTED - Syncing ALL Spring Projects and Versions");
        log.info("=".repeat(80));

        ComprehensiveSyncResult result = new ComprehensiveSyncResult();
        result.setStartTime(LocalDateTime.now());

        try {
            // PHASE 0: Sync Spring Boot versions to spring_boot_versions table (PRIMARY TABLE)
            log.info("Phase 0/8: Syncing Spring Boot versions to spring_boot_versions table...");
            publishProgress(0, 8, "Syncing Spring Boot versions", "running", 0);
            SpringBootVersionSyncService.SyncResult bootVersionsResult = springBootVersionSyncService.syncSpringBootVersions();
            result.setBootVersionsResult(bootVersionsResult);
            result.addVersionsCreated(bootVersionsResult.getVersionsCreated());
            result.addVersionsUpdated(bootVersionsResult.getVersionsUpdated());
            result.addErrorsEncountered(bootVersionsResult.getErrorsEncountered());

            if (bootVersionsResult.isSuccess()) {
                log.info("✓ Phase 0 completed: {} Spring Boot versions created, {} updated",
                    bootVersionsResult.getVersionsCreated(),
                    bootVersionsResult.getVersionsUpdated());
            } else {
                log.warn("✗ Phase 0 completed with errors: {}", bootVersionsResult.getErrorMessage());
            }

            // PHASE 1: Sync from Spring Generations API (Spring Boot with support dates)
            log.info("Phase 1/8: Syncing from Spring Generations API...");
            publishProgress(1, 8, "Syncing from Spring Generations API", "running", 12);
            SpringGenerationsSyncService.SyncResult generationsResult = generationsSyncService.syncAllGenerations();
            result.setGenerationsResult(generationsResult);
            result.addProjectsCreated(generationsResult.getProjectsCreated());
            result.addVersionsCreated(generationsResult.getVersionsCreated());
            result.addErrorsEncountered(generationsResult.getErrorsEncountered());

            if (generationsResult.isSuccess()) {
                log.info("✓ Phase 1 completed: {} projects, {} versions created",
                    generationsResult.getProjectsCreated(),
                    generationsResult.getVersionsCreated());
            } else {
                log.warn("✗ Phase 1 completed with errors: {}", generationsResult.getErrorMessage());
            }

            // PHASE 2: Sync from Spring Initializr (additional Spring Boot versions)
            log.info("Phase 2/8: Syncing from Spring Initializr API...");
            publishProgress(2, 8, "Syncing from Spring Initializr API", "running", 25);
            ProjectSyncService.SyncResult initializrResult = projectSyncService.syncSpringBoot();
            result.setInitializrResult(initializrResult);
            result.addProjectsCreated(initializrResult.getProjectsCreated());
            result.addVersionsCreated(initializrResult.getVersionsCreated());
            result.addErrorsEncountered(initializrResult.getErrorsEncountered());

            if (initializrResult.isSuccess()) {
                log.info("✓ Phase 2 completed: {} projects, {} versions created",
                    initializrResult.getProjectsCreated(),
                    initializrResult.getVersionsCreated());
            } else {
                log.warn("✗ Phase 2 completed with errors: {}", initializrResult.getErrorMessage());
            }

            // PHASE 3: Crawl project pages to enrich version data
            log.info("Phase 3/8: Crawling Spring project pages for documentation links and support dates...");
            publishProgress(3, 8, "Crawling Spring project pages", "running", 37);
            CrawlerResult crawlerResult = crawlAllProjects();
            result.setCrawlerResult(crawlerResult);
            result.addVersionsUpdated(crawlerResult.getTotalVersionsUpdated());
            result.addErrorsEncountered(crawlerResult.getTotalErrors());

            if (crawlerResult.isSuccess()) {
                log.info("✓ Phase 3 completed: {} projects crawled, {} versions updated",
                    crawlerResult.getProjectsCrawled(),
                    crawlerResult.getTotalVersionsUpdated());
            } else {
                log.warn("✗ Phase 3 completed with errors: {} failures out of {} projects",
                    crawlerResult.getTotalErrors(),
                    crawlerResult.getProjectsCrawled());
            }

            // PHASE 4: Sync project relationships (parent/child hierarchies)
            log.info("Phase 4/8: Syncing project relationships (parent/child hierarchies)...");
            publishProgress(4, 8, "Syncing project relationships", "running", 50);
            ProjectRelationshipSyncService.SyncResult relationshipsResult = projectRelationshipSyncService.syncProjectRelationships();
            result.setRelationshipsResult(relationshipsResult);
            result.addRelationshipsCreated(relationshipsResult.getRelationshipsCreated());
            result.addErrorsEncountered(relationshipsResult.getErrorsEncountered());

            if (relationshipsResult.isSuccess()) {
                log.info("✓ Phase 4 completed: {} relationships created, {} skipped",
                    relationshipsResult.getRelationshipsCreated(),
                    relationshipsResult.getRelationshipsSkipped());
            } else {
                log.warn("✗ Phase 4 completed with errors: {}", relationshipsResult.getErrorMessage());
            }

            // PHASE 5: Sync documentation content (fetch OVERVIEW from spring.io and convert to Markdown)
            log.info("Phase 5/8: Syncing documentation content (OVERVIEW from spring.io)...");
            publishProgress(5, 8, "Syncing documentation content", "running", 62);
            DocumentationSyncService.SyncResult documentationResult = documentationSyncService.syncAllDocumentation();
            result.setDocumentationResult(documentationResult);
            result.addDocumentationLinksCreated(documentationResult.getLinksCreated());
            result.addDocumentationLinksUpdated(documentationResult.getLinksUpdated());
            result.addErrorsEncountered(documentationResult.getErrorsEncountered());

            if (documentationResult.isSuccess()) {
                log.info("✓ Phase 5 completed: {} documentation links created, {} updated",
                    documentationResult.getLinksCreated(),
                    documentationResult.getLinksUpdated());
            } else {
                log.warn("✗ Phase 5 completed with errors: {}", documentationResult.getErrorMessage());
            }

            // PHASE 6: Sync code examples (extract samples from spring.io project pages)
            log.info("Phase 6/8: Syncing code examples (samples from spring.io)...");
            publishProgress(6, 8, "Syncing code examples", "running", 75);
            CodeExamplesSyncService.SyncResult codeExamplesResult = codeExamplesSyncService.syncCodeExamples();
            result.setCodeExamplesResult(codeExamplesResult);
            result.addCodeExamplesCreated(codeExamplesResult.getExamplesCreated());
            result.addCodeExamplesUpdated(codeExamplesResult.getExamplesUpdated());
            result.addErrorsEncountered(codeExamplesResult.getErrorsEncountered());

            if (codeExamplesResult.isSuccess()) {
                log.info("✓ Phase 6 completed: {} code examples created, {} updated",
                    codeExamplesResult.getExamplesCreated(),
                    codeExamplesResult.getExamplesUpdated());
            } else {
                log.warn("✗ Phase 6 completed with errors: {}", codeExamplesResult.getErrorMessage());
            }

            // PHASE 7: Sync OpenRewrite migration recipes (conditional on feature flag)
            RecipeSyncService.RecipeSyncResult recipeSyncResult = null;
            if (openRewriteFeatureConfig.isEnabled() && recipeSyncService.isPresent()) {
                log.info("Phase 7/8: Syncing OpenRewrite migration recipes...");
                publishProgress(7, 8, "Syncing migration recipes", "running", 87);
                try {
                    recipeSyncService.get().syncRecipes();
                    RecipeSyncService.RecipeSyncStatus status = recipeSyncService.get().getSyncStatus();
                    recipeSyncResult = new RecipeSyncService.RecipeSyncResult(
                        true,
                        (int) status.recipeCount(),
                        (int) status.transformationCount(),
                        0,
                        null
                    );
                    result.setRecipeSyncResult(recipeSyncResult);
                    log.info("✓ Phase 7 completed: {} recipes, {} transformations synced",
                        status.recipeCount(), status.transformationCount());
                } catch (Exception e) {
                    log.warn("✗ Phase 7 completed with errors: {}", e.getMessage());
                    recipeSyncResult = new RecipeSyncService.RecipeSyncResult(false, 0, 0, 1, e.getMessage());
                    result.setRecipeSyncResult(recipeSyncResult);
                    result.addErrorsEncountered(1);
                }
            } else {
                log.info("Phase 7/8: Skipping OpenRewrite recipe sync (feature disabled or service unavailable)");
                publishProgress(7, 8, "Recipe sync skipped", "running", 87);
                recipeSyncResult = new RecipeSyncService.RecipeSyncResult(true, 0, 0, 0, "Feature disabled");
                result.setRecipeSyncResult(recipeSyncResult);
            }

            // Determine overall success
            result.setSuccess(bootVersionsResult.isSuccess() &&
                            generationsResult.isSuccess() &&
                            initializrResult.isSuccess() &&
                            crawlerResult.isSuccess() &&
                            relationshipsResult.isSuccess() &&
                            documentationResult.isSuccess() &&
                            codeExamplesResult.isSuccess() &&
                            (recipeSyncResult == null || recipeSyncResult.success()));

            // Build summary message
            StringBuilder summary = new StringBuilder();
            if (result.isSuccess()) {
                summary.append("All phases completed successfully!");
            } else {
                summary.append("Completed with some errors.");
            }
            result.setSummaryMessage(summary.toString());

        } catch (Exception e) {
            log.error("Error during comprehensive sync", e);
            result.setSuccess(false);
            result.setSummaryMessage("Comprehensive sync failed: " + e.getMessage());
            result.addErrorsEncountered(1);
        } finally {
            result.setEndTime(LocalDateTime.now());

            // Publish final completion event
            publishCompletion(result.isSuccess(), result.getSummaryMessage());

            log.info("=".repeat(80));
            log.info("COMPREHENSIVE SYNC COMPLETED");
            log.info("Duration: {} seconds",
                java.time.Duration.between(result.getStartTime(), result.getEndTime()).getSeconds());
            log.info("Total Projects Created: {}", result.getTotalProjectsCreated());
            log.info("Total Versions Created: {}", result.getTotalVersionsCreated());
            log.info("Total Versions Updated: {}", result.getTotalVersionsUpdated());
            log.info("Total Errors: {}", result.getTotalErrors());
            log.info("Status: {}", result.isSuccess() ? "SUCCESS" : "FAILED");
            log.info("=".repeat(80));
        }

        return result;
    }

    /**
     * Crawl all Spring project pages to extract documentation links and support dates
     *
     * @return CrawlerResult with aggregated crawl statistics
     */
    private CrawlerResult crawlAllProjects() {
        CrawlerResult result = new CrawlerResult();
        List<SpringProject> projects = springProjectRepository.findAll();

        log.debug("Found {} projects to crawl", projects.size());

        for (SpringProject project : projects) {
            try {
                log.debug("Crawling project: {}", project.getSlug());
                SpringProjectPageCrawlerService.CrawlResult crawlResult =
                    crawlerService.crawlProject(project.getSlug());

                result.addProjectCrawled();
                result.addVersionsParsed(crawlResult.getVersionsParsed());
                result.addVersionsUpdated(crawlResult.getVersionsUpdated());

                if (!crawlResult.isSuccess()) {
                    result.addError();
                    log.warn("Crawl failed for project {}: {}",
                        project.getSlug(), crawlResult.getErrorMessage());
                }

            } catch (Exception e) {
                result.addError();
                log.error("Error crawling project: {}", project.getSlug(), e);
            }
        }

        // Determine success (at least 50% of projects crawled successfully)
        result.setSuccess(result.getTotalErrors() <= (result.getProjectsCrawled() / 2));

        return result;
    }

    /**
     * Result object for crawler operation.
     */
    @Data
    @NoArgsConstructor
    public static class CrawlerResult {
        private boolean success;
        private int projectsCrawled = 0;
        private int totalVersionsParsed = 0;
        private int totalVersionsUpdated = 0;
        private int totalErrors = 0;

        public void addProjectCrawled() {
            this.projectsCrawled++;
        }

        public void addVersionsParsed(int count) {
            this.totalVersionsParsed += count;
        }

        public void addVersionsUpdated(int count) {
            this.totalVersionsUpdated += count;
        }

        public void addError() {
            this.totalErrors++;
        }
    }

    /**
     * Result object for comprehensive sync operation.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ComprehensiveSyncResult {
        private boolean success;
        private String summaryMessage;
        private LocalDateTime startTime;
        private LocalDateTime endTime;

        // Aggregated statistics
        private int totalProjectsCreated;
        private int totalVersionsCreated;
        private int totalVersionsUpdated;
        private int totalRelationshipsCreated;
        private int totalDocumentationLinksCreated;
        private int totalDocumentationLinksUpdated;
        private int totalCodeExamplesCreated;
        private int totalCodeExamplesUpdated;
        private int totalErrors;

        // Individual phase results
        private SpringBootVersionSyncService.SyncResult bootVersionsResult;
        private SpringGenerationsSyncService.SyncResult generationsResult;
        private ProjectSyncService.SyncResult initializrResult;
        private CrawlerResult crawlerResult;
        private ProjectRelationshipSyncService.SyncResult relationshipsResult;
        private DocumentationSyncService.SyncResult documentationResult;
        private CodeExamplesSyncService.SyncResult codeExamplesResult;
        private RecipeSyncService.RecipeSyncResult recipeSyncResult;

        // Helper methods to aggregate statistics
        public void addProjectsCreated(int count) {
            this.totalProjectsCreated += count;
        }

        public void addVersionsCreated(int count) {
            this.totalVersionsCreated += count;
        }

        public void addVersionsUpdated(int count) {
            this.totalVersionsUpdated += count;
        }

        public void addRelationshipsCreated(int count) {
            this.totalRelationshipsCreated += count;
        }

        public void addDocumentationLinksCreated(int count) {
            this.totalDocumentationLinksCreated += count;
        }

        public void addDocumentationLinksUpdated(int count) {
            this.totalDocumentationLinksUpdated += count;
        }

        public void addCodeExamplesCreated(int count) {
            this.totalCodeExamplesCreated += count;
        }

        public void addCodeExamplesUpdated(int count) {
            this.totalCodeExamplesUpdated += count;
        }

        public void addErrorsEncountered(int count) {
            this.totalErrors += count;
        }
    }

    /**
     * Helper method to publish progress updates to connected SSE clients.
     *
     * @param currentPhase current phase number (0-5)
     * @param totalPhases total number of phases (6)
     * @param description phase description
     * @param status status ("running", "completed", "error")
     * @param progressPercent progress percentage (0-100)
     */
    private void publishProgress(int currentPhase, int totalPhases, String description, String status, int progressPercent) {
        SyncProgressEvent event = SyncProgressEvent.builder()
            .currentPhase(currentPhase)
            .totalPhases(totalPhases)
            .phaseDescription(description)
            .status(status)
            .progressPercent(progressPercent)
            .completed(false)
            .build();

        progressTracker.publishProgress(event);
    }

    /**
     * Helper method to publish final completion event.
     *
     * @param success whether the sync was successful
     * @param message summary message
     */
    private void publishCompletion(boolean success, String message) {
        SyncProgressEvent event = SyncProgressEvent.builder()
            .currentPhase(8)
            .totalPhases(8)
            .phaseDescription("Sync Complete")
            .status(success ? "completed" : "error")
            .progressPercent(100)
            .message(message)
            .completed(true)
            .errorMessage(success ? null : message)
            .build();

        progressTracker.publishProgress(event);
    }
}
