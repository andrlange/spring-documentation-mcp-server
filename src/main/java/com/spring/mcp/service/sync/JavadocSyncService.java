package com.spring.mcp.service.sync;

import com.spring.mcp.config.JavadocsFeatureConfig;
import com.spring.mcp.model.entity.JavadocSyncStatus;
import com.spring.mcp.model.entity.ProjectVersion;
import com.spring.mcp.model.entity.SpringProject;
import com.spring.mcp.repository.JavadocSyncStatusRepository;
import com.spring.mcp.repository.ProjectVersionRepository;
import com.spring.mcp.repository.SpringProjectRepository;
import com.spring.mcp.service.SettingsService;
import com.spring.mcp.service.javadoc.JavadocCrawlService;
import com.spring.mcp.service.javadoc.JavadocStorageService;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Service for synchronizing Javadoc documentation.
 * Coordinates crawling, parsing, and storage of Javadocs for enabled Spring projects.
 */
@Service
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "mcp.features.javadocs", name = "enabled", havingValue = "true", matchIfMissing = true)
public class JavadocSyncService {

    private final JavadocCrawlService crawlService;
    private final JavadocStorageService storageService;
    private final JavadocSyncStatusRepository syncStatusRepository;
    private final ProjectVersionRepository versionRepository;
    private final SpringProjectRepository projectRepository;
    private final JavadocsFeatureConfig config;
    private final SettingsService settingsService;

    /**
     * Sync Javadocs for all enabled projects.
     * Note: No @Transactional here - each package/class save has its own transaction
     * to prevent rollback of the entire sync operation if one item fails.
     *
     * @return SyncResult with statistics
     */
    public SyncResult syncAll() {
        Instant startTime = Instant.now();
        log.info("Starting Javadoc sync for all enabled projects");

        SyncResult result = new SyncResult();

        // Find all projects with enabled Javadoc sync
        List<JavadocSyncStatus> enabledStatuses = syncStatusRepository.findByEnabledTrue();
        log.info("Found {} projects with Javadoc sync enabled", enabledStatuses.size());

        for (JavadocSyncStatus status : enabledStatuses) {
            try {
                SyncResult projectResult = syncProject(status.getProject().getId());
                result.add(projectResult);
            } catch (Exception e) {
                log.error("Failed to sync Javadocs for project {}: {}",
                        status.getProject().getSlug(), e.getMessage());
                result.addError(e.getMessage());
            }
        }

        result.setDuration(Duration.between(startTime, Instant.now()));
        log.info("Javadoc sync completed: {} projects, {} versions, {} packages skipped, {} classes ({} skipped), {} errors in {}s",
                result.projectsSynced, result.versionsProcessed, result.packagesSkipped, result.classesStored,
                result.classesSkipped, result.errors.size(), result.duration.getSeconds());

        return result;
    }

    /**
     * Sync Javadocs for a single project.
     * Note: No @Transactional here - each package/class save has its own transaction
     * to prevent rollback of the entire sync operation if one item fails.
     *
     * @param projectId the project ID
     * @return SyncResult with statistics
     */
    public SyncResult syncProject(Long projectId) {
        Instant startTime = Instant.now();
        SyncResult result = new SyncResult();

        Optional<SpringProject> projectOpt = projectRepository.findById(projectId);
        if (projectOpt.isEmpty()) {
            result.addError("Project not found: " + projectId);
            return result;
        }

        SpringProject project = projectOpt.get();
        log.info("Syncing Javadocs for project: {}", project.getSlug());

        // Find or create sync status
        JavadocSyncStatus status = syncStatusRepository.findByProjectId(projectId)
                .orElseGet(() -> {
                    JavadocSyncStatus newStatus = new JavadocSyncStatus();
                    newStatus.setProject(project);
                    newStatus.setEnabled(true);
                    return syncStatusRepository.save(newStatus);
                });

        if (!status.getEnabled()) {
            log.info("Javadoc sync disabled for project: {}", project.getSlug());
            result.addError("Sync disabled for project: " + project.getSlug());
            return result;
        }

        try {
            // Find versions with API doc URLs
            List<ProjectVersion> versions = versionRepository.findByProjectIdWithApiDocUrl(projectId);
            log.info("Found {} versions with API doc URLs for {}", versions.size(), project.getSlug());

            // Filter versions based on settings (Release 1.4.3)
            List<ProjectVersion> filteredVersions = filterVersionsBySettings(versions);
            if (filteredVersions.size() < versions.size()) {
                log.info("Filtered from {} to {} versions based on Javadoc sync settings (SNAPSHOT={}, RC={}, Milestone={})",
                        versions.size(), filteredVersions.size(),
                        settingsService.isJavadocSyncSnapshotEnabled(),
                        settingsService.isJavadocSyncRcEnabled(),
                        settingsService.isJavadocSyncMilestoneEnabled());
            }

            for (ProjectVersion version : filteredVersions) {
                try {
                    // Crawl the Javadocs (will skip already-synced classes at the class level)
                    String apiDocUrl = version.getApiDocUrl();
                    log.info("Crawling Javadocs for {}/{} from {}", project.getSlug(), version.getVersion(), apiDocUrl);

                    JavadocCrawlService.CrawlResult crawlResult =
                            crawlService.crawlJavadoc(apiDocUrl, project.getSlug(), version.getVersion());

                    result.versionsProcessed++;
                    result.packagesSkipped += crawlResult.packagesSkipped;
                    result.classesStored += crawlResult.classesProcessed;
                    result.classesSkipped += crawlResult.classesSkipped;
                    result.methodsStored += crawlResult.methodsStored;

                    // Log resume info if packages/classes were skipped
                    if (crawlResult.packagesSkipped > 0 || crawlResult.classesSkipped > 0) {
                        log.info("Resumed sync for {}/{}: {} packages skipped, {} classes skipped, {} new classes processed",
                                project.getSlug(), version.getVersion(),
                                crawlResult.packagesSkipped, crawlResult.classesSkipped, crawlResult.classesProcessed);
                    }

                    if (crawlResult.hasErrors()) {
                        result.errors.addAll(crawlResult.errors.subList(0,
                                Math.min(crawlResult.errors.size(), 10))); // Limit errors
                    }

                } catch (Exception e) {
                    log.error("Failed to sync version {}/{}: {}",
                            project.getSlug(), version.getVersion(), e.getMessage());
                    result.addError("Version " + version.getVersion() + ": " + e.getMessage());
                }
            }

            result.projectsSynced++;
            status.recordSuccess();
            syncStatusRepository.save(status);

        } catch (Exception e) {
            log.error("Failed to sync project {}: {}", project.getSlug(), e.getMessage());
            status.recordFailure(e.getMessage(), config.getSync().getMaxFailures());
            syncStatusRepository.save(status);
            result.addError("Project " + project.getSlug() + ": " + e.getMessage());
        }

        result.setDuration(Duration.between(startTime, Instant.now()));
        return result;
    }

    /**
     * Enable Javadoc sync for a project.
     *
     * @param projectId the project ID
     * @return true if successful
     */
    @Transactional
    public boolean enableSync(Long projectId) {
        Optional<SpringProject> projectOpt = projectRepository.findById(projectId);
        if (projectOpt.isEmpty()) {
            return false;
        }

        JavadocSyncStatus status = syncStatusRepository.findByProjectId(projectId)
                .orElseGet(() -> {
                    JavadocSyncStatus newStatus = new JavadocSyncStatus();
                    newStatus.setProject(projectOpt.get());
                    return newStatus;
                });

        status.setEnabled(true);
        status.setFailureCount(0);
        status.setLastError(null);
        syncStatusRepository.save(status);

        log.info("Enabled Javadoc sync for project: {}", projectOpt.get().getSlug());
        return true;
    }

    /**
     * Disable Javadoc sync for a project.
     *
     * @param projectId the project ID
     * @return true if successful
     */
    @Transactional
    public boolean disableSync(Long projectId) {
        Optional<JavadocSyncStatus> statusOpt = syncStatusRepository.findByProjectId(projectId);
        if (statusOpt.isEmpty()) {
            return false;
        }

        JavadocSyncStatus status = statusOpt.get();
        status.setEnabled(false);
        syncStatusRepository.save(status);

        log.info("Disabled Javadoc sync for project ID: {}", projectId);
        return true;
    }

    /**
     * Toggle Javadoc sync for a project.
     *
     * @param projectId the project ID
     * @return the new enabled state, or null if project not found
     */
    @Transactional
    public Boolean toggleSync(Long projectId) {
        Optional<SpringProject> projectOpt = projectRepository.findById(projectId);
        if (projectOpt.isEmpty()) {
            return null;
        }

        JavadocSyncStatus status = syncStatusRepository.findByProjectId(projectId)
                .orElseGet(() -> {
                    JavadocSyncStatus newStatus = new JavadocSyncStatus();
                    newStatus.setProject(projectOpt.get());
                    newStatus.setEnabled(false);
                    return newStatus;
                });

        boolean newState = !status.getEnabled();
        status.setEnabled(newState);

        if (newState) {
            // Reset failure count when enabling
            status.setFailureCount(0);
            status.setLastError(null);
        }

        syncStatusRepository.save(status);
        log.info("Toggled Javadoc sync for project {} to: {}", projectOpt.get().getSlug(), newState);

        return newState;
    }

    /**
     * Get sync status for a project.
     *
     * @param projectId the project ID
     * @return sync status, or empty if not found
     */
    public Optional<JavadocSyncStatus> getSyncStatus(Long projectId) {
        return syncStatusRepository.findByProjectId(projectId);
    }

    /**
     * Get all sync statuses.
     *
     * @return list of all sync statuses
     */
    public List<JavadocSyncStatus> getAllSyncStatuses() {
        return syncStatusRepository.findAll();
    }

    /**
     * Clear stored Javadocs for a project version.
     *
     * @param projectSlug the project slug
     * @param version     the version
     * @return number of packages deleted
     */
    @Transactional
    public int clearVersion(String projectSlug, String version) {
        return storageService.clearLibraryVersion(projectSlug, version);
    }

    /**
     * Filter versions based on Javadoc sync settings.
     * By default, only GA/CURRENT versions are included. SNAPSHOT, RC, and Milestone
     * versions are included only if their respective settings are enabled.
     * <p>
     * Added in Release 1.4.3.
     *
     * @param versions list of versions to filter
     * @return filtered list of versions
     */
    private List<ProjectVersion> filterVersionsBySettings(List<ProjectVersion> versions) {
        return versions.stream()
                .filter(v -> settingsService.shouldSyncJavadocVersion(v.getVersion()))
                .toList();
    }

    /**
     * Result of a sync operation.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SyncResult {
        private int projectsSynced;
        private int versionsProcessed;
        private int packagesSkipped;  // Packages already synced and skipped (resume support)
        private int classesStored;
        private int classesSkipped;  // Classes already synced and skipped (resume support)
        private int methodsStored;
        private Duration duration;
        @Builder.Default
        private List<String> errors = new ArrayList<>();

        public void addError(String error) {
            if (errors.size() < 100) { // Limit error list size
                errors.add(error);
            }
        }

        public boolean isSuccess() {
            return errors.isEmpty() || projectsSynced > 0;
        }

        public void add(SyncResult other) {
            this.projectsSynced += other.projectsSynced;
            this.versionsProcessed += other.versionsProcessed;
            this.packagesSkipped += other.packagesSkipped;
            this.classesStored += other.classesStored;
            this.classesSkipped += other.classesSkipped;
            this.methodsStored += other.methodsStored;
            this.errors.addAll(other.errors.subList(0, Math.min(other.errors.size(), 10)));
        }
    }
}
