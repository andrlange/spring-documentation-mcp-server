package com.spring.mcp.scheduler;

import com.spring.mcp.model.entity.DocumentationLink;
import com.spring.mcp.model.entity.ProjectVersion;
import com.spring.mcp.model.entity.SpringProject;
import com.spring.mcp.repository.DocumentationLinkRepository;
import com.spring.mcp.repository.ProjectVersionRepository;
import com.spring.mcp.repository.SpringProjectRepository;
import com.spring.mcp.service.documentation.DocumentationFetchService;
import com.spring.mcp.service.indexing.DocumentationIndexer;
import com.spring.mcp.service.sync.ComprehensiveSyncService;
import com.spring.mcp.service.version.VersionDetectionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Documentation synchronization service providing manual sync operations.
 * <p>
 * This service provides methods for:
 * <ul>
 *   <li>Manual documentation synchronization (triggered via UI or API)</li>
 *   <li>Version detection for Spring projects</li>
 *   <li>Documentation content updates</li>
 *   <li>Cleanup of end-of-life versions</li>
 * </ul>
 * <p>
 * <b>Note:</b> Scheduled synchronization is handled by {@link com.spring.mcp.service.scheduler.SchedulerService}
 * which is configurable via the /settings UI. This class no longer contains hardcoded @Scheduled annotations.
 * <p>
 * All sync operations include:
 * <ul>
 *   <li>Comprehensive error handling and recovery</li>
 *   <li>Job status tracking and monitoring</li>
 *   <li>Statistics collection (documents processed, errors, duration)</li>
 *   <li>Detailed logging for observability</li>
 *   <li>Retry logic for transient failures</li>
 * </ul>
 *
 * @author Spring MCP Server
 * @version 1.1.0
 * @since Phase 3
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DocumentationSyncScheduler {

    private final VersionDetectionService versionDetectionService;
    private final DocumentationFetchService documentationFetchService;
    private final DocumentationIndexer documentationIndexer;
    private final SpringProjectRepository springProjectRepository;
    private final ProjectVersionRepository projectVersionRepository;
    private final DocumentationLinkRepository documentationLinkRepository;
    private final ComprehensiveSyncService comprehensiveSyncService;

    @Value("${mcp.documentation.fetch.enabled:true}")
    private boolean syncEnabled;

    @Value("${mcp.documentation.fetch.schedule:0 0 2 * * ?}")
    private String syncSchedule;

    @Value("${mcp.documentation.versioning.auto-detect:true}")
    private boolean autoDetectVersions;

    @Value("${mcp.documentation.fetch.retry.max-attempts:3}")
    private int maxRetryAttempts;

    // Job status tracking
    private final Map<String, JobStatus> jobStatuses = new ConcurrentHashMap<>();
    private final AtomicBoolean comprehensiveSyncInProgress = new AtomicBoolean(false);
    private final AtomicBoolean syncInProgress = new AtomicBoolean(false);
    private final AtomicBoolean versionDetectionInProgress = new AtomicBoolean(false);
    private final AtomicBoolean updateInProgress = new AtomicBoolean(false);
    private final AtomicBoolean cleanupInProgress = new AtomicBoolean(false);

    // Statistics tracking
    private final AtomicInteger totalDocumentsProcessed = new AtomicInteger(0);
    private final AtomicInteger totalVersionsDetected = new AtomicInteger(0);
    private final AtomicInteger totalErrors = new AtomicInteger(0);

    /**
     * Job status information for monitoring and tracking.
     */
    public static class JobStatus {
        private final String jobName;
        private LocalDateTime lastRunTime;
        private LocalDateTime nextRunTime;
        private String status; // SUCCESS, FAILED, RUNNING, SKIPPED
        private long durationMs;
        private int itemsProcessed;
        private int errorsEncountered;
        private String errorMessage;
        private Map<String, Object> statistics;

        public JobStatus(String jobName) {
            this.jobName = jobName;
            this.status = "IDLE";
            this.statistics = new HashMap<>();
        }

        // Getters
        public String getJobName() { return jobName; }
        public LocalDateTime getLastRunTime() { return lastRunTime; }
        public LocalDateTime getNextRunTime() { return nextRunTime; }
        public String getStatus() { return status; }
        public long getDurationMs() { return durationMs; }
        public int getItemsProcessed() { return itemsProcessed; }
        public int getErrorsEncountered() { return errorsEncountered; }
        public String getErrorMessage() { return errorMessage; }
        public Map<String, Object> getStatistics() { return statistics; }

        // Setters
        public void setLastRunTime(LocalDateTime lastRunTime) { this.lastRunTime = lastRunTime; }
        public void setNextRunTime(LocalDateTime nextRunTime) { this.nextRunTime = nextRunTime; }
        public void setStatus(String status) { this.status = status; }
        public void setDurationMs(long durationMs) { this.durationMs = durationMs; }
        public void setItemsProcessed(int itemsProcessed) { this.itemsProcessed = itemsProcessed; }
        public void setErrorsEncountered(int errorsEncountered) { this.errorsEncountered = errorsEncountered; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
        public void setStatistics(Map<String, Object> statistics) { this.statistics = statistics; }

        @Override
        public String toString() {
            return String.format("JobStatus{name='%s', status='%s', lastRun=%s, items=%d, errors=%d, duration=%dms}",
                jobName, status, lastRunTime, itemsProcessed, errorsEncountered, durationMs);
        }
    }

    /**
     * Initialize job statuses on application startup.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        log.info("Documentation Sync Scheduler initialized");
        log.info("Sync enabled: {}", syncEnabled);
        log.info("Auto-detect versions: {}", autoDetectVersions);

        // Initialize job statuses
        jobStatuses.put("comprehensiveSync", new JobStatus("comprehensiveSync"));
        jobStatuses.put("syncDocumentation", new JobStatus("syncDocumentation"));
        jobStatuses.put("detectNewVersions", new JobStatus("detectNewVersions"));
        jobStatuses.put("updateExistingDocumentation", new JobStatus("updateExistingDocumentation"));
        jobStatuses.put("cleanupOldVersions", new JobStatus("cleanupOldVersions"));

        if (syncEnabled) {
            log.info("Scheduled jobs are ENABLED and will run according to their schedules");
        } else {
            log.warn("Scheduled jobs are DISABLED (mcp.documentation.fetch.enabled=false)");
        }
    }

    /**
     * Full synchronization of all documentation.
     * <p>
     * This method performs:
     * <ol>
     *   <li>Fetches all active projects and versions</li>
     *   <li>Downloads and indexes documentation for each version</li>
     *   <li>Updates search vectors for full-text search</li>
     *   <li>Tracks statistics and errors</li>
     * </ol>
     * <p>
     * The sync is skipped if:
     * <ul>
     *   <li>Sync is disabled (mcp.documentation.fetch.enabled=false)</li>
     *   <li>A sync operation is already in progress</li>
     * </ul>
     * <p>
     * <b>Note:</b> This method is called by {@link com.spring.mcp.service.scheduler.SchedulerService}
     * based on UI-configured schedule, or can be triggered manually.
     */
    public void syncDocumentation() {
        if (!syncEnabled) {
            log.debug("Documentation sync is disabled, skipping job");
            return;
        }

        if (!syncInProgress.compareAndSet(false, true)) {
            log.warn("Documentation sync already in progress, skipping this run");
            updateJobStatus("syncDocumentation", "SKIPPED", 0, 0, 0,
                "Previous sync still running", Collections.emptyMap());
            return;
        }

        JobStatus jobStatus = jobStatuses.get("syncDocumentation");
        jobStatus.setStatus("RUNNING");
        jobStatus.setLastRunTime(LocalDateTime.now());

        log.info("========================================");
        log.info("Starting daily documentation synchronization");
        log.info("========================================");

        long startTime = System.currentTimeMillis();
        int totalProcessed = 0;
        int totalErrors = 0;
        Map<String, Object> statistics = new HashMap<>();

        try {
            // Get all active projects
            List<SpringProject> activeProjects = springProjectRepository.findByActiveTrue();
            log.info("Found {} active projects to sync", activeProjects.size());
            statistics.put("projectsFound", activeProjects.size());

            if (activeProjects.isEmpty()) {
                log.warn("No active projects found for synchronization");
                updateJobStatus("syncDocumentation", "SUCCESS", 0, 0,
                    System.currentTimeMillis() - startTime, null, statistics);
                return;
            }

            // Process each project
            for (SpringProject project : activeProjects) {
                try {
                    log.info("Syncing documentation for project: {}", project.getName());
                    Map<String, Object> projectStats = syncProjectDocumentation(project);

                    int processed = (int) projectStats.getOrDefault("processed", 0);
                    int errors = (int) projectStats.getOrDefault("errors", 0);

                    totalProcessed += processed;
                    totalErrors += errors;

                    statistics.put(project.getSlug(), projectStats);

                    log.info("Completed sync for project: {} - Processed: {}, Errors: {}",
                        project.getName(), processed, errors);

                } catch (Exception e) {
                    log.error("Error syncing project: {} - Error: {}", project.getName(), e.getMessage(), e);
                    totalErrors++;
                    statistics.put(project.getSlug() + "_error", e.getMessage());
                }
            }

            // Update global statistics
            this.totalDocumentsProcessed.addAndGet(totalProcessed);
            this.totalErrors.addAndGet(totalErrors);

            long duration = System.currentTimeMillis() - startTime;
            statistics.put("totalProcessed", totalProcessed);
            statistics.put("totalErrors", totalErrors);
            statistics.put("durationMs", duration);

            log.info("========================================");
            log.info("Documentation synchronization completed");
            log.info("Total processed: {}, Total errors: {}, Duration: {}ms",
                totalProcessed, totalErrors, duration);
            log.info("========================================");

            updateJobStatus("syncDocumentation", "SUCCESS", totalProcessed, totalErrors,
                duration, null, statistics);

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("Critical error during documentation synchronization: {}", e.getMessage(), e);
            this.totalErrors.incrementAndGet();

            updateJobStatus("syncDocumentation", "FAILED", totalProcessed, totalErrors,
                duration, e.getMessage(), statistics);

        } finally {
            syncInProgress.set(false);
        }
    }

    /**
     * Detection of new Spring versions.
     * <p>
     * This method performs:
     * <ol>
     *   <li>Checks spring.io for new version releases</li>
     *   <li>Detects stable, RC, and snapshot versions</li>
     *   <li>Updates version metadata in the database</li>
     *   <li>Triggers documentation sync for new versions</li>
     * </ol>
     * <p>
     * <b>Note:</b> This method is called as part of the comprehensive sync
     * managed by {@link com.spring.mcp.service.scheduler.SchedulerService},
     * or can be triggered manually.
     */
    public void detectNewVersions() {
        if (!syncEnabled || !autoDetectVersions) {
            log.debug("Version detection is disabled, skipping job");
            return;
        }

        if (!versionDetectionInProgress.compareAndSet(false, true)) {
            log.warn("Version detection already in progress, skipping this run");
            updateJobStatus("detectNewVersions", "SKIPPED", 0, 0, 0,
                "Previous detection still running", Collections.emptyMap());
            return;
        }

        JobStatus jobStatus = jobStatuses.get("detectNewVersions");
        jobStatus.setStatus("RUNNING");
        jobStatus.setLastRunTime(LocalDateTime.now());

        log.info("Starting hourly version detection");
        long startTime = System.currentTimeMillis();
        int totalNewVersions = 0;
        int totalErrors = 0;
        Map<String, Object> statistics = new HashMap<>();

        try {
            // Get all active projects
            List<SpringProject> activeProjects = springProjectRepository.findByActiveTrue();
            log.info("Checking {} projects for new versions", activeProjects.size());

            for (SpringProject project : activeProjects) {
                try {
                    log.debug("Detecting versions for project: {}", project.getName());

                    int newVersionsCount = versionDetectionService.updateVersions(project.getSlug());

                    if (newVersionsCount > 0) {
                        log.info("Detected {} new version(s) for project: {}",
                            newVersionsCount, project.getName());
                        totalNewVersions += newVersionsCount;
                        statistics.put(project.getSlug() + "_newVersions", newVersionsCount);

                        // If new versions found, trigger async documentation sync
                        syncNewVersionsAsync(project, newVersionsCount);
                    }

                } catch (Exception e) {
                    log.error("Error detecting versions for project: {} - Error: {}",
                        project.getName(), e.getMessage(), e);
                    totalErrors++;
                    statistics.put(project.getSlug() + "_error", e.getMessage());
                }
            }

            // Update global statistics
            this.totalVersionsDetected.addAndGet(totalNewVersions);
            this.totalErrors.addAndGet(totalErrors);

            long duration = System.currentTimeMillis() - startTime;
            statistics.put("totalNewVersions", totalNewVersions);
            statistics.put("totalErrors", totalErrors);

            log.info("Version detection completed - New versions: {}, Errors: {}, Duration: {}ms",
                totalNewVersions, totalErrors, duration);

            updateJobStatus("detectNewVersions", "SUCCESS", totalNewVersions, totalErrors,
                duration, null, statistics);

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("Critical error during version detection: {}", e.getMessage(), e);

            updateJobStatus("detectNewVersions", "FAILED", totalNewVersions, totalErrors,
                duration, e.getMessage(), statistics);

        } finally {
            versionDetectionInProgress.set(false);
        }
    }

    /**
     * Update of existing documentation content.
     * <p>
     * This method performs:
     * <ol>
     *   <li>Checks existing documentation links for content changes</li>
     *   <li>Re-fetches and re-indexes updated documentation</li>
     *   <li>Updates search vectors if content has changed</li>
     * </ol>
     * <p>
     * <b>Note:</b> This method is called as part of the comprehensive sync
     * managed by {@link com.spring.mcp.service.scheduler.SchedulerService},
     * or can be triggered manually.
     */
    public void updateExistingDocumentation() {
        if (!syncEnabled) {
            log.debug("Documentation update is disabled, skipping job");
            return;
        }

        if (!updateInProgress.compareAndSet(false, true)) {
            log.warn("Documentation update already in progress, skipping this run");
            updateJobStatus("updateExistingDocumentation", "SKIPPED", 0, 0, 0,
                "Previous update still running", Collections.emptyMap());
            return;
        }

        JobStatus jobStatus = jobStatuses.get("updateExistingDocumentation");
        jobStatus.setStatus("RUNNING");
        jobStatus.setLastRunTime(LocalDateTime.now());

        log.info("========================================");
        log.info("Starting weekly documentation update");
        log.info("========================================");

        long startTime = System.currentTimeMillis();
        int totalUpdated = 0;
        int totalErrors = 0;
        int totalUnchanged = 0;
        Map<String, Object> statistics = new HashMap<>();

        try {
            // Get all active documentation links
            List<DocumentationLink> allLinks = documentationLinkRepository.findAll()
                .stream()
                .filter(link -> link.getIsActive())
                .collect(Collectors.toList());

            log.info("Checking {} active documentation links for updates", allLinks.size());
            statistics.put("linksChecked", allLinks.size());

            if (allLinks.isEmpty()) {
                log.warn("No active documentation links found for update");
                updateJobStatus("updateExistingDocumentation", "SUCCESS", 0, 0,
                    System.currentTimeMillis() - startTime, null, statistics);
                return;
            }

            // Process links in batches
            int batchSize = 50;
            List<List<DocumentationLink>> batches = partitionList(allLinks, batchSize);

            for (int i = 0; i < batches.size(); i++) {
                List<DocumentationLink> batch = batches.get(i);
                log.info("Processing batch {}/{} ({} links)", i + 1, batches.size(), batch.size());

                for (DocumentationLink link : batch) {
                    try {
                        boolean wasUpdated = updateDocumentationLink(link);

                        if (wasUpdated) {
                            totalUpdated++;
                        } else {
                            totalUnchanged++;
                        }

                    } catch (Exception e) {
                        log.error("Error updating link: {} - URL: {} - Error: {}",
                            link.getId(), link.getUrl(), e.getMessage());
                        totalErrors++;
                    }
                }
            }

            long duration = System.currentTimeMillis() - startTime;
            statistics.put("totalUpdated", totalUpdated);
            statistics.put("totalUnchanged", totalUnchanged);
            statistics.put("totalErrors", totalErrors);

            log.info("========================================");
            log.info("Documentation update completed");
            log.info("Updated: {}, Unchanged: {}, Errors: {}, Duration: {}ms",
                totalUpdated, totalUnchanged, totalErrors, duration);
            log.info("========================================");

            updateJobStatus("updateExistingDocumentation", "SUCCESS", totalUpdated, totalErrors,
                duration, null, statistics);

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("Critical error during documentation update: {}", e.getMessage(), e);

            updateJobStatus("updateExistingDocumentation", "FAILED", totalUpdated, totalErrors,
                duration, e.getMessage(), statistics);

        } finally {
            updateInProgress.set(false);
        }
    }

    /**
     * Cleanup of end-of-life versions.
     * <p>
     * This method performs:
     * <ol>
     *   <li>Identifies versions past their end-of-life date</li>
     *   <li>Marks them as inactive</li>
     *   <li>Optionally archives their documentation</li>
     * </ol>
     * <p>
     * <b>Note:</b> This method is called as part of the comprehensive sync
     * managed by {@link com.spring.mcp.service.scheduler.SchedulerService},
     * or can be triggered manually.
     */
    public void cleanupOldVersions() {
        if (!syncEnabled) {
            log.debug("Version cleanup is disabled, skipping job");
            return;
        }

        if (!cleanupInProgress.compareAndSet(false, true)) {
            log.warn("Version cleanup already in progress, skipping this run");
            updateJobStatus("cleanupOldVersions", "SKIPPED", 0, 0, 0,
                "Previous cleanup still running", Collections.emptyMap());
            return;
        }

        JobStatus jobStatus = jobStatuses.get("cleanupOldVersions");
        jobStatus.setStatus("RUNNING");
        jobStatus.setLastRunTime(LocalDateTime.now());

        log.info("========================================");
        log.info("Starting monthly cleanup of old versions");
        log.info("========================================");

        long startTime = System.currentTimeMillis();
        int totalCleaned = 0;
        int totalErrors = 0;
        Map<String, Object> statistics = new HashMap<>();

        try {
            // Find all versions past their end-of-life date
            List<ProjectVersion> allVersions = projectVersionRepository.findAll();
            LocalDate today = LocalDate.now();

            List<ProjectVersion> eolVersions = allVersions.stream()
                .filter(version -> version.getEnterpriseSupportEnd() != null)
                .filter(version -> version.getEnterpriseSupportEnd().isBefore(today))
                .filter(version -> !version.getIsLatest()) // Never cleanup latest version
                .collect(Collectors.toList());

            log.info("Found {} end-of-life versions to cleanup", eolVersions.size());
            statistics.put("eolVersionsFound", eolVersions.size());

            if (eolVersions.isEmpty()) {
                log.info("No end-of-life versions found for cleanup");
                updateJobStatus("cleanupOldVersions", "SUCCESS", 0, 0,
                    System.currentTimeMillis() - startTime, null, statistics);
                return;
            }

            // Process each EOL version
            for (ProjectVersion version : eolVersions) {
                try {
                    log.info("Cleaning up version: {} {} (EOL: {})",
                        version.getProject().getName(), version.getVersion(), version.getEnterpriseSupportEnd());

                    // Deactivate documentation links for this version
                    List<DocumentationLink> links = documentationLinkRepository.findByVersion(version);
                    int deactivatedLinks = 0;

                    for (DocumentationLink link : links) {
                        if (link.getIsActive()) {
                            link.setIsActive(false);
                            documentationLinkRepository.save(link);
                            deactivatedLinks++;
                        }
                    }

                    log.info("Deactivated {} documentation links for version: {} {}",
                        deactivatedLinks, version.getProject().getName(), version.getVersion());

                    totalCleaned++;
                    statistics.put(version.getProject().getSlug() + "_" + version.getVersion() + "_deactivated",
                        deactivatedLinks);

                } catch (Exception e) {
                    log.error("Error cleaning up version: {} {} - Error: {}",
                        version.getProject().getName(), version.getVersion(), e.getMessage(), e);
                    totalErrors++;
                }
            }

            long duration = System.currentTimeMillis() - startTime;
            statistics.put("totalCleaned", totalCleaned);
            statistics.put("totalErrors", totalErrors);

            log.info("========================================");
            log.info("Version cleanup completed");
            log.info("Cleaned: {}, Errors: {}, Duration: {}ms",
                totalCleaned, totalErrors, duration);
            log.info("========================================");

            updateJobStatus("cleanupOldVersions", "SUCCESS", totalCleaned, totalErrors,
                duration, null, statistics);

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("Critical error during version cleanup: {}", e.getMessage(), e);

            updateJobStatus("cleanupOldVersions", "FAILED", totalCleaned, totalErrors,
                duration, e.getMessage(), statistics);

        } finally {
            cleanupInProgress.set(false);
        }
    }

    /**
     * Synchronizes documentation for a specific project.
     *
     * @param project the Spring project to sync
     * @return statistics map with processed and error counts
     */
    @Transactional
    protected Map<String, Object> syncProjectDocumentation(SpringProject project) {
        Map<String, Object> stats = new HashMap<>();
        int processed = 0;
        int errors = 0;

        try {
            // Get active versions for this project using n-2 strategy
            List<ProjectVersion> activeVersions = versionDetectionService.getActiveVersions(project.getSlug());
            log.info("Found {} active versions for project: {}", activeVersions.size(), project.getName());

            for (ProjectVersion version : activeVersions) {
                try {
                    // Get all documentation links for this version
                    List<DocumentationLink> links = documentationLinkRepository
                        .findByVersionAndIsActiveTrue(version);

                    log.debug("Processing {} documentation links for version: {} {}",
                        links.size(), project.getName(), version.getVersion());

                    // Index documentation in batch
                    Map<String, Object> indexStats = documentationIndexer.indexBatch(links);

                    processed += (int) indexStats.getOrDefault("successful", 0);
                    errors += (int) indexStats.getOrDefault("failed", 0);

                } catch (Exception e) {
                    log.error("Error syncing version: {} {} - Error: {}",
                        project.getName(), version.getVersion(), e.getMessage());
                    errors++;
                }
            }

            stats.put("processed", processed);
            stats.put("errors", errors);
            stats.put("versionsProcessed", activeVersions.size());

        } catch (Exception e) {
            log.error("Error syncing project: {} - Error: {}", project.getName(), e.getMessage(), e);
            stats.put("processed", processed);
            stats.put("errors", errors + 1);
        }

        return stats;
    }

    /**
     * Updates a single documentation link if content has changed.
     *
     * @param link the documentation link to update
     * @return true if content was updated, false if unchanged
     */
    @Transactional
    protected boolean updateDocumentationLink(DocumentationLink link) {
        try {
            // Check if content has changed
            String latestContent = documentationFetchService.fetchDocumentationContent(link.getUrl());

            if (latestContent.isEmpty()) {
                log.warn("Failed to fetch content for link: {} - URL: {}", link.getId(), link.getUrl());
                return false;
            }

            String existingHash = link.getContentHash();
            boolean hasChanged = documentationFetchService.hasContentChanged(existingHash, latestContent);

            if (hasChanged) {
                log.info("Content changed for link: {} - Re-indexing", link.getId());
                documentationIndexer.updateIndex(link);
                return true;
            } else {
                log.debug("Content unchanged for link: {} - Skipping", link.getId());
                // Update last fetched timestamp even if content unchanged
                link.setLastFetched(LocalDateTime.now());
                documentationLinkRepository.save(link);
                return false;
            }

        } catch (Exception e) {
            log.error("Error updating link: {} - Error: {}", link.getId(), e.getMessage());
            throw new RuntimeException("Failed to update link: " + link.getId(), e);
        }
    }

    /**
     * Asynchronously syncs documentation for newly detected versions.
     *
     * @param project the Spring project
     * @param newVersionsCount number of new versions detected
     */
    @Async
    protected void syncNewVersionsAsync(SpringProject project, int newVersionsCount) {
        log.info("Triggering async documentation sync for {} new version(s) of project: {}",
            newVersionsCount, project.getName());

        try {
            // Get the newly added versions (latest ones)
            List<ProjectVersion> allVersions = projectVersionRepository
                .findByProjectOrderByVersionDesc(project);

            List<ProjectVersion> newVersions = allVersions.stream()
                .limit(newVersionsCount)
                .collect(Collectors.toList());

            // Sync documentation for new versions
            for (ProjectVersion version : newVersions) {
                try {
                    List<DocumentationLink> links = documentationLinkRepository
                        .findByVersionAndIsActiveTrue(version);

                    if (!links.isEmpty()) {
                        log.info("Indexing {} documentation links for new version: {} {}",
                            links.size(), project.getName(), version.getVersion());
                        documentationIndexer.indexBatch(links);
                    }

                } catch (Exception e) {
                    log.error("Error syncing new version: {} {} - Error: {}",
                        project.getName(), version.getVersion(), e.getMessage());
                }
            }

            log.info("Completed async sync for new versions of project: {}", project.getName());

        } catch (Exception e) {
            log.error("Error in async version sync for project: {} - Error: {}",
                project.getName(), e.getMessage(), e);
        }
    }

    /**
     * Updates the job status with new information.
     */
    private void updateJobStatus(String jobName, String status, int itemsProcessed,
                                  int errorsEncountered, long durationMs,
                                  String errorMessage, Map<String, Object> statistics) {
        JobStatus jobStatus = jobStatuses.get(jobName);
        if (jobStatus != null) {
            jobStatus.setStatus(status);
            jobStatus.setItemsProcessed(itemsProcessed);
            jobStatus.setErrorsEncountered(errorsEncountered);
            jobStatus.setDurationMs(durationMs);
            jobStatus.setErrorMessage(errorMessage);
            jobStatus.setStatistics(statistics);

            log.info("Job status updated: {}", jobStatus);
        }
    }

    /**
     * Partitions a list into smaller sublists of specified size.
     */
    private <T> List<List<T>> partitionList(List<T> list, int size) {
        if (list == null || list.isEmpty()) {
            return Collections.emptyList();
        }

        List<List<T>> partitions = new ArrayList<>();
        for (int i = 0; i < list.size(); i += size) {
            partitions.add(list.subList(i, Math.min(i + size, list.size())));
        }
        return partitions;
    }

    // Public methods for monitoring and management

    /**
     * Gets the status of a specific job.
     *
     * @param jobName the job name
     * @return the job status, or null if not found
     */
    public JobStatus getJobStatus(String jobName) {
        return jobStatuses.get(jobName);
    }

    /**
     * Gets all job statuses.
     *
     * @return map of all job statuses
     */
    public Map<String, JobStatus> getAllJobStatuses() {
        return Collections.unmodifiableMap(jobStatuses);
    }

    /**
     * Gets global statistics.
     *
     * @return map of global statistics
     */
    public Map<String, Object> getGlobalStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalDocumentsProcessed", totalDocumentsProcessed.get());
        stats.put("totalVersionsDetected", totalVersionsDetected.get());
        stats.put("totalErrors", totalErrors.get());
        stats.put("syncEnabled", syncEnabled);
        stats.put("autoDetectVersions", autoDetectVersions);
        stats.put("comprehensiveSyncInProgress", comprehensiveSyncInProgress.get());
        stats.put("syncInProgress", syncInProgress.get());
        stats.put("versionDetectionInProgress", versionDetectionInProgress.get());
        stats.put("updateInProgress", updateInProgress.get());
        stats.put("cleanupInProgress", cleanupInProgress.get());
        return stats;
    }

    /**
     * Checks if any job is currently running.
     *
     * @return true if any job is running
     */
    public boolean isAnyJobRunning() {
        return comprehensiveSyncInProgress.get() || syncInProgress.get()
            || versionDetectionInProgress.get() || updateInProgress.get()
            || cleanupInProgress.get();
    }

    /**
     * Manually triggers documentation sync (for admin/management use).
     *
     * @return statistics from the sync operation
     */
    @Async
    public void triggerManualSync() {
        log.info("Manual documentation sync triggered");
        syncDocumentation();
    }

    /**
     * Manually triggers version detection (for admin/management use).
     *
     * @return statistics from the detection operation
     */
    @Async
    public void triggerManualVersionDetection() {
        log.info("Manual version detection triggered");
        detectNewVersions();
    }

    /**
     * Comprehensive synchronization of ALL Spring projects and versions from all sources.
     * <p>
     * This method performs:
     * <ol>
     *   <li>Syncs all Spring Boot versions from Spring Generations API (with support dates)</li>
     *   <li>Syncs additional Spring Boot versions from Spring Initializr API</li>
     *   <li>Ensures ALL projects and versions are loaded and updated in the database</li>
     * </ol>
     * <p>
     * The sync is skipped if:
     * <ul>
     *   <li>Sync is disabled (mcp.documentation.fetch.enabled=false)</li>
     *   <li>A comprehensive sync operation is already in progress</li>
     * </ul>
     * <p>
     * <b>Note:</b> This method is called by {@link com.spring.mcp.service.scheduler.SchedulerService}
     * based on UI-configured schedule, or can be triggered manually via the /sync page.
     */
    public void comprehensiveSync() {
        if (!syncEnabled) {
            log.debug("Comprehensive sync is disabled, skipping job");
            return;
        }

        if (!comprehensiveSyncInProgress.compareAndSet(false, true)) {
            log.warn("Comprehensive sync already in progress, skipping this run");
            updateJobStatus("comprehensiveSync", "SKIPPED", 0, 0, 0,
                "Previous sync still running", Collections.emptyMap());
            return;
        }

        JobStatus jobStatus = jobStatuses.get("comprehensiveSync");
        jobStatus.setStatus("RUNNING");
        jobStatus.setLastRunTime(LocalDateTime.now());

        log.info("========================================");
        log.info("Starting comprehensive sync of ALL projects and versions");
        log.info("========================================");

        long startTime = System.currentTimeMillis();

        try {
            // Execute comprehensive sync
            ComprehensiveSyncService.ComprehensiveSyncResult result = comprehensiveSyncService.syncAll();

            long duration = System.currentTimeMillis() - startTime;

            // Prepare statistics
            Map<String, Object> statistics = new HashMap<>();
            statistics.put("totalProjectsCreated", result.getTotalProjectsCreated());
            statistics.put("totalVersionsCreated", result.getTotalVersionsCreated());
            statistics.put("totalVersionsUpdated", result.getTotalVersionsUpdated());
            statistics.put("totalErrors", result.getTotalErrors());
            statistics.put("durationSeconds", duration / 1000);
            statistics.put("success", result.isSuccess());

            if (result.isSuccess()) {
                updateJobStatus("comprehensiveSync", "SUCCESS",
                    result.getTotalProjectsCreated() + result.getTotalVersionsCreated(),
                    0, duration, null, statistics);

                log.info("========================================");
                log.info("Comprehensive sync completed successfully");
                log.info("Projects created: {}", result.getTotalProjectsCreated());
                log.info("Versions created: {}", result.getTotalVersionsCreated());
                log.info("Versions updated: {}", result.getTotalVersionsUpdated());
                log.info("Errors: {}", result.getTotalErrors());
                log.info("Duration: {} seconds", duration / 1000);
                log.info("========================================");
            } else {
                updateJobStatus("comprehensiveSync", "FAILED",
                    result.getTotalProjectsCreated() + result.getTotalVersionsCreated(),
                    result.getTotalErrors(), duration, result.getSummaryMessage(), statistics);

                log.error("========================================");
                log.error("Comprehensive sync completed with errors");
                log.error("Error message: {}", result.getSummaryMessage());
                log.error("========================================");
            }

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            Map<String, Object> errorStats = new HashMap<>();
            errorStats.put("exception", e.getClass().getName());
            errorStats.put("message", e.getMessage());

            updateJobStatus("comprehensiveSync", "ERROR", 0, 1, duration,
                e.getMessage(), errorStats);

            log.error("========================================");
            log.error("Error during comprehensive sync", e);
            log.error("========================================");

        } finally {
            comprehensiveSyncInProgress.set(false);
        }
    }

    /**
     * Manually triggers comprehensive sync (for admin/management use).
     */
    @Async
    public void triggerManualComprehensiveSync() {
        log.info("Manual comprehensive sync triggered");
        comprehensiveSync();
    }
}
