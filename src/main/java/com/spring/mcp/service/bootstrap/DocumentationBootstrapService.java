package com.spring.mcp.service.bootstrap;

import com.spring.mcp.model.entity.DocumentationLink;
import com.spring.mcp.model.entity.DocumentationType;
import com.spring.mcp.model.entity.ProjectVersion;
import com.spring.mcp.model.entity.SpringProject;
import com.spring.mcp.model.enums.VersionState;
import com.spring.mcp.repository.DocumentationLinkRepository;
import com.spring.mcp.repository.DocumentationTypeRepository;
import com.spring.mcp.repository.ProjectVersionRepository;
import com.spring.mcp.repository.SpringProjectRepository;
import com.spring.mcp.service.documentation.DocumentationFetchService;
import com.spring.mcp.service.indexing.DocumentationIndexer;
import com.spring.mcp.service.version.VersionDetectionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Service responsible for bootstrapping initial documentation data for the Spring MCP Server.
 * <p>
 * This service initializes the database with core Spring projects, their versions,
 * and documentation links. It supports both manual bootstrap via API/command and
 * optional automatic bootstrap on application startup.
 * <p>
 * Key features:
 * <ul>
 *   <li>Creates core Spring projects (Boot, Framework, Data, Security, Cloud)</li>
 *   <li>Detects and stores available versions for each project</li>
 *   <li>Creates documentation links following Spring documentation URL patterns</li>
 *   <li>Optionally fetches and indexes documentation content</li>
 *   <li>Tracks bootstrap progress and status</li>
 *   <li>Supports partial bootstrap (individual projects)</li>
 *   <li>Idempotent operations (safe to run multiple times)</li>
 * </ul>
 * <p>
 * Configuration properties:
 * <ul>
 *   <li>mcp.documentation.bootstrap.enabled - Enable/disable bootstrap functionality</li>
 *   <li>mcp.documentation.bootstrap.on-startup - Auto-bootstrap on application startup</li>
 *   <li>mcp.documentation.bootstrap.projects - List of projects to bootstrap</li>
 * </ul>
 *
 * @author Spring MCP Server
 * @version 1.0.0
 * @since Phase 3
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class DocumentationBootstrapService {

    private final SpringProjectRepository springProjectRepository;
    private final ProjectVersionRepository projectVersionRepository;
    private final DocumentationTypeRepository documentationTypeRepository;
    private final DocumentationLinkRepository documentationLinkRepository;
    private final VersionDetectionService versionDetectionService;
    private final DocumentationFetchService documentationFetchService;
    private final DocumentationIndexer documentationIndexer;

    @Value("${mcp.documentation.bootstrap.enabled:false}")
    private boolean bootstrapEnabled;

    @Value("${mcp.documentation.bootstrap.on-startup:false}")
    private boolean bootstrapOnStartup;

    @Value("#{'${mcp.documentation.bootstrap.projects:spring-boot,spring-framework,spring-data,spring-security,spring-cloud}'.split(',')}")
    private List<String> bootstrapProjects;

    @Value("${mcp.documentation.base-urls.spring-docs:https://docs.spring.io}")
    private String springDocsBaseUrl;

    // Bootstrap state tracking
    private final AtomicBoolean bootstrapInProgress = new AtomicBoolean(false);
    private final AtomicBoolean bootstrapCompleted = new AtomicBoolean(false);
    private final Map<String, BootstrapStatus> projectStatus = new ConcurrentHashMap<>();
    private LocalDateTime bootstrapStartTime;
    private LocalDateTime bootstrapEndTime;

    // Statistics
    private final AtomicInteger totalProjects = new AtomicInteger(0);
    private final AtomicInteger totalVersions = new AtomicInteger(0);
    private final AtomicInteger totalLinks = new AtomicInteger(0);
    private final List<String> errors = Collections.synchronizedList(new ArrayList<>());

    /**
     * Core Spring project definitions with metadata for bootstrapping.
     */
    private static final Map<String, ProjectDefinition> PROJECT_DEFINITIONS = Map.of(
        "spring-boot", new ProjectDefinition(
            "Spring Boot",
            "Spring Boot makes it easy to create stand-alone, production-grade Spring based Applications",
            "https://spring.io/projects/spring-boot",
            "https://github.com/spring-projects/spring-boot",
            List.of("3.5.7", "3.4.0", "3.3.0"),
            List.of("4.0.0-RC2")
        ),
        "spring-framework", new ProjectDefinition(
            "Spring Framework",
            "The Spring Framework provides a comprehensive programming and configuration model",
            "https://spring.io/projects/spring-framework",
            "https://github.com/spring-projects/spring-framework",
            List.of("6.2.0", "6.1.0", "6.0.0"),
            List.of("7.0.0-SNAPSHOT")
        ),
        "spring-data", new ProjectDefinition(
            "Spring Data",
            "Spring Data's mission is to provide a familiar and consistent Spring-based programming model for data access",
            "https://spring.io/projects/spring-data",
            "https://github.com/spring-projects/spring-data",
            List.of("2024.1.0", "2024.0.0", "2023.1.0"),
            Collections.emptyList()
        ),
        "spring-security", new ProjectDefinition(
            "Spring Security",
            "Spring Security is a framework that focuses on providing authentication and authorization to Java applications",
            "https://spring.io/projects/spring-security",
            "https://github.com/spring-projects/spring-security",
            List.of("6.4.0", "6.3.0", "6.2.0"),
            Collections.emptyList()
        ),
        "spring-cloud", new ProjectDefinition(
            "Spring Cloud",
            "Spring Cloud provides tools for developers to quickly build common patterns in distributed systems",
            "https://spring.io/projects/spring-cloud",
            "https://github.com/spring-projects/spring-cloud",
            List.of("2024.0.0", "2023.0.0"),
            Collections.emptyList()
        )
    );

    /**
     * Documentation types to create (Overview, Learn, Support, Samples, API, Reference).
     */
    private static final List<DocTypeDefinition> DOC_TYPE_DEFINITIONS = List.of(
        new DocTypeDefinition("Overview", "overview", 1),
        new DocTypeDefinition("Learn", "learn", 2),
        new DocTypeDefinition("Reference", "reference", 3),
        new DocTypeDefinition("API Documentation", "api", 4),
        new DocTypeDefinition("Samples", "samples", 5),
        new DocTypeDefinition("Support", "support", 6)
    );

    /**
     * Event listener that triggers bootstrap on application startup if configured.
     * This runs after the application context is fully initialized and ready.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        if (bootstrapEnabled && bootstrapOnStartup) {
            log.info("Bootstrap on startup enabled - starting automatic bootstrap");
            try {
                bootstrapAllProjects();
            } catch (Exception e) {
                log.error("Error during automatic bootstrap on startup", e);
            }
        } else {
            log.info("Bootstrap on startup disabled (enabled: {}, on-startup: {})",
                bootstrapEnabled, bootstrapOnStartup);
        }
    }

    /**
     * Bootstraps all configured Spring projects.
     * <p>
     * This method performs a complete bootstrap workflow:
     * <ol>
     *   <li>Validates bootstrap is not already in progress</li>
     *   <li>Creates documentation types if they don't exist</li>
     *   <li>Creates each configured project</li>
     *   <li>Detects and stores available versions</li>
     *   <li>Creates documentation links for each version</li>
     *   <li>Optionally fetches and indexes content</li>
     * </ol>
     * <p>
     * The operation is idempotent - existing data will not be duplicated.
     *
     * @throws IllegalStateException if bootstrap is already in progress
     */
    @Transactional
    public void bootstrapAllProjects() {
        if (!bootstrapInProgress.compareAndSet(false, true)) {
            throw new IllegalStateException("Bootstrap is already in progress");
        }

        try {
            log.info("=".repeat(80));
            log.info("Starting documentation bootstrap for {} projects", bootstrapProjects.size());
            log.info("=".repeat(80));

            bootstrapStartTime = LocalDateTime.now();
            resetStatistics();

            // Step 1: Create documentation types
            log.info("Step 1: Creating documentation types");
            createDocumentationTypes();

            // Step 2: Bootstrap each project
            log.info("Step 2: Bootstrapping projects: {}", bootstrapProjects);
            for (String projectSlug : bootstrapProjects) {
                try {
                    bootstrapProject(projectSlug);
                    totalProjects.incrementAndGet();
                } catch (Exception e) {
                    String error = String.format("Failed to bootstrap project %s: %s",
                        projectSlug, e.getMessage());
                    errors.add(error);
                    log.error(error, e);
                }
            }

            bootstrapEndTime = LocalDateTime.now();
            bootstrapCompleted.set(true);

            // Print summary
            printBootstrapSummary();

        } finally {
            bootstrapInProgress.set(false);
        }
    }

    /**
     * Asynchronously bootstraps all configured Spring projects.
     * <p>
     * This method runs on a virtual thread and delegates to {@link #bootstrapAllProjects()}.
     * It provides non-blocking execution for long-running bootstrap operations.
     * <p>
     * Use this method when triggering bootstrap from REST endpoints or other
     * contexts where blocking is undesirable.
     * <p>
     * Progress can be monitored via {@link #getBootstrapStatus()}.
     *
     * @see #bootstrapAllProjects()
     */
    @Async("bootstrapExecutor")
    public void bootstrapAllProjectsAsync() {
        log.info("Starting async bootstrap on virtual thread: {}", Thread.currentThread());
        try {
            bootstrapAllProjects();
        } catch (Exception e) {
            log.error("Error during async bootstrap: {}", e.getMessage(), e);
        }
    }

    /**
     * Bootstraps a single Spring project.
     * <p>
     * This method:
     * <ol>
     *   <li>Creates or retrieves the project entity</li>
     *   <li>Detects available versions (or uses predefined versions)</li>
     *   <li>Stores version information</li>
     *   <li>Creates documentation links for each version</li>
     * </ol>
     *
     * @param projectSlug the project slug (e.g., "spring-boot")
     * @throws IllegalArgumentException if project slug is unknown
     */
    @Transactional
    public void bootstrapProject(String projectSlug) {
        log.info("Bootstrapping project: {}", projectSlug);
        long startTime = System.currentTimeMillis();

        BootstrapStatus status = new BootstrapStatus();
        status.setStatus("IN_PROGRESS");
        status.setStartTime(LocalDateTime.now());
        projectStatus.put(projectSlug, status);

        try {
            // Get project definition
            ProjectDefinition definition = PROJECT_DEFINITIONS.get(projectSlug);
            if (definition == null) {
                throw new IllegalArgumentException("Unknown project: " + projectSlug);
            }

            // Step 1: Create or get project
            SpringProject project = createOrGetProject(projectSlug, definition);
            status.setProjectId(project.getId());
            log.info("Project created/retrieved: {} (ID: {})", project.getName(), project.getId());

            // Step 2: Create versions
            List<ProjectVersion> versions = createProjectVersions(project, definition);
            status.setVersionsCreated(versions.size());
            totalVersions.addAndGet(versions.size());
            log.info("Created {} versions for project: {}", versions.size(), projectSlug);

            // Step 3: Create documentation links for each version
            int linksCreated = 0;
            for (ProjectVersion version : versions) {
                int links = createDocumentationLinksForVersion(version);
                linksCreated += links;
                totalLinks.addAndGet(links);
            }
            status.setLinksCreated(linksCreated);
            log.info("Created {} documentation links for project: {}", linksCreated, projectSlug);

            // Mark as completed
            status.setStatus("COMPLETED");
            status.setEndTime(LocalDateTime.now());
            status.setDurationMs(System.currentTimeMillis() - startTime);

            log.info("Successfully bootstrapped project: {} in {}ms",
                projectSlug, status.getDurationMs());

        } catch (Exception e) {
            status.setStatus("FAILED");
            status.setEndTime(LocalDateTime.now());
            status.setError(e.getMessage());
            String error = String.format("Error bootstrapping project %s: %s",
                projectSlug, e.getMessage());
            errors.add(error);
            log.error(error, e);
            throw new RuntimeException(error, e);
        }
    }

    /**
     * Creates documentation links for a specific project version.
     * <p>
     * This method generates appropriate documentation URLs based on the project
     * and version, following Spring documentation URL patterns. Links are created
     * for different documentation types (Reference, API, Learn, etc.).
     * <p>
     * URL patterns:
     * <ul>
     *   <li>Spring Boot: https://docs.spring.io/spring-boot/reference/{version}/</li>
     *   <li>Spring Boot API: https://docs.spring.io/spring-boot/api/java/</li>
     *   <li>Spring Framework: https://docs.spring.io/spring-framework/reference/{version}/</li>
     *   <li>Spring Framework API: https://docs.spring.io/spring-framework/docs/{version}/javadoc-api/</li>
     * </ul>
     *
     * @param version the ProjectVersion to create links for
     * @return the number of links created
     */
    @Transactional
    public int createDocumentationLinksForVersion(ProjectVersion version) {
        if (version == null) {
            throw new IllegalArgumentException("ProjectVersion cannot be null");
        }

        log.debug("Creating documentation links for version: {} - {}",
            version.getProject().getSlug(), version.getVersion());

        String projectSlug = version.getProject().getSlug();
        String versionString = version.getVersion();
        int linksCreated = 0;

        try {
            // Get documentation types
            DocumentationType referenceType = documentationTypeRepository.findBySlug("reference")
                .orElseThrow(() -> new IllegalStateException("Reference doc type not found"));
            DocumentationType apiType = documentationTypeRepository.findBySlug("api")
                .orElseThrow(() -> new IllegalStateException("API doc type not found"));
            DocumentationType learnType = documentationTypeRepository.findBySlug("learn")
                .orElseThrow(() -> new IllegalStateException("Learn doc type not found"));

            // Check if links already exist
            List<DocumentationLink> existingLinks = documentationLinkRepository
                .findByVersionId(version.getId());

            if (!existingLinks.isEmpty()) {
                log.debug("Documentation links already exist for version: {} - skipping",
                    version.getId());
                return 0;
            }

            // Create links based on project type
            List<DocumentationLink> links = new ArrayList<>();

            switch (projectSlug) {
                case "spring-boot":
                    links.addAll(createSpringBootLinks(version, referenceType, apiType, learnType));
                    break;
                case "spring-framework":
                    links.addAll(createSpringFrameworkLinks(version, referenceType, apiType, learnType));
                    break;
                case "spring-data":
                    links.addAll(createSpringDataLinks(version, referenceType, apiType));
                    break;
                case "spring-security":
                    links.addAll(createSpringSecurityLinks(version, referenceType, apiType));
                    break;
                case "spring-cloud":
                    links.addAll(createSpringCloudLinks(version, referenceType, apiType));
                    break;
                default:
                    log.warn("Unknown project type for link creation: {}", projectSlug);
            }

            if (!links.isEmpty()) {
                documentationLinkRepository.saveAll(links);
                linksCreated = links.size();
                log.debug("Created {} documentation links for version: {}", linksCreated, version.getId());
            }

            return linksCreated;

        } catch (Exception e) {
            log.error("Error creating documentation links for version: {} - Error: {}",
                version.getId(), e.getMessage(), e);
            throw new RuntimeException("Failed to create documentation links", e);
        }
    }

    /**
     * Returns the current bootstrap status for all projects.
     * <p>
     * The status includes:
     * <ul>
     *   <li>enabled - whether bootstrap is enabled</li>
     *   <li>inProgress - whether bootstrap is currently running</li>
     *   <li>completed - whether bootstrap has completed successfully</li>
     *   <li>startTime - when bootstrap started</li>
     *   <li>endTime - when bootstrap completed</li>
     *   <li>statistics - counts of projects, versions, links created</li>
     *   <li>projectStatus - detailed status for each project</li>
     *   <li>errors - list of errors encountered</li>
     * </ul>
     *
     * @return a Map containing bootstrap status information
     */
    public Map<String, Object> getBootstrapStatus() {
        Map<String, Object> status = new LinkedHashMap<>();

        status.put("enabled", bootstrapEnabled);
        status.put("inProgress", bootstrapInProgress.get());
        status.put("completed", bootstrapCompleted.get());
        status.put("startTime", bootstrapStartTime);
        status.put("endTime", bootstrapEndTime);

        if (bootstrapStartTime != null && bootstrapEndTime != null) {
            status.put("durationMs",
                java.time.Duration.between(bootstrapStartTime, bootstrapEndTime).toMillis());
        }

        Map<String, Object> statistics = new LinkedHashMap<>();
        statistics.put("totalProjects", totalProjects.get());
        statistics.put("totalVersions", totalVersions.get());
        statistics.put("totalLinks", totalLinks.get());
        statistics.put("errors", errors.size());
        status.put("statistics", statistics);

        Map<String, Map<String, Object>> projectStatusMap = new LinkedHashMap<>();
        projectStatus.forEach((slug, bootstrapStatus) ->
            projectStatusMap.put(slug, bootstrapStatus.toMap()));
        status.put("projectStatus", projectStatusMap);

        if (!errors.isEmpty()) {
            status.put("errors", new ArrayList<>(errors));
        }

        return status;
    }

    /**
     * Checks if the bootstrap process has completed successfully.
     *
     * @return true if bootstrap has completed, false otherwise
     */
    public boolean isBootstrapComplete() {
        return bootstrapCompleted.get() && !bootstrapInProgress.get();
    }

    // ==================== Private Helper Methods ====================

    /**
     * Creates documentation types in the database if they don't exist.
     */
    @Transactional
    protected void createDocumentationTypes() {
        log.info("Creating documentation types");

        for (DocTypeDefinition definition : DOC_TYPE_DEFINITIONS) {
            documentationTypeRepository.findBySlug(definition.slug())
                .orElseGet(() -> {
                    DocumentationType docType = DocumentationType.builder()
                        .name(definition.name())
                        .slug(definition.slug())
                        .displayOrder(definition.displayOrder())
                        .build();
                    documentationTypeRepository.save(docType);
                    log.info("Created documentation type: {} ({})", definition.name(), definition.slug());
                    return docType;
                });
        }

        log.info("Documentation types created/verified");
    }

    /**
     * Creates or retrieves an existing Spring project.
     */
    @Transactional
    protected SpringProject createOrGetProject(String slug, ProjectDefinition definition) {
        return springProjectRepository.findBySlug(slug)
            .orElseGet(() -> {
                SpringProject project = SpringProject.builder()
                    .name(definition.name())
                    .slug(slug)
                    .description(definition.description())
                    .homepageUrl(definition.homepageUrl())
                    .githubUrl(definition.githubUrl())
                    .active(true)
                    .build();
                springProjectRepository.save(project);
                log.info("Created new project: {} (slug: {})", definition.name(), slug);
                return project;
            });
    }

    /**
     * Creates project versions from the definition.
     */
    @Transactional
    protected List<ProjectVersion> createProjectVersions(SpringProject project, ProjectDefinition definition) {
        List<ProjectVersion> versions = new ArrayList<>();

        // Create stable versions
        for (String versionString : definition.stableVersions()) {
            ProjectVersion version = createVersionIfNotExists(project, versionString, VersionState.GA);
            if (version != null) {
                versions.add(version);
            }
        }

        // Create pre-release versions
        for (String versionString : definition.preReleaseVersions()) {
            VersionState versionState = determineVersionState(versionString);
            ProjectVersion version = createVersionIfNotExists(project, versionString, versionState);
            if (version != null) {
                versions.add(version);
            }
        }

        // Update latest flag
        if (!versions.isEmpty()) {
            versionDetectionService.updateLatestStableFlag(project);
        }

        return versions;
    }

    /**
     * Creates a version if it doesn't already exist.
     */
    @Transactional
    protected ProjectVersion createVersionIfNotExists(SpringProject project, String versionString, VersionState versionState) {
        // Check if version already exists
        Optional<ProjectVersion> existing = projectVersionRepository
            .findByProjectAndVersion(project, versionString);

        if (existing.isPresent()) {
            log.debug("Version already exists: {} - {}", project.getSlug(), versionString);
            return existing.get();
        }

        // Parse version
        ProjectVersion version = versionDetectionService.parseVersion(versionString);
        if (version == null) {
            log.warn("Failed to parse version: {}", versionString);
            return null;
        }

        version.setProject(project);
        version.setState(versionState);
        version.setReleaseDate(LocalDate.now());

        projectVersionRepository.save(version);
        log.info("Created version: {} - {}", project.getSlug(), versionString);

        return version;
    }

    /**
     * Determines version state from version string.
     */
    private VersionState determineVersionState(String versionString) {
        if (versionString.contains("SNAPSHOT")) {
            return VersionState.SNAPSHOT;
        } else if (versionString.contains("RC")) {
            return VersionState.RC;
        } else if (versionString.contains("M")) {
            return VersionState.MILESTONE;
        }
        return VersionState.GA;
    }

    /**
     * Creates Spring Boot documentation links.
     */
    private List<DocumentationLink> createSpringBootLinks(
        ProjectVersion version,
        DocumentationType referenceType,
        DocumentationType apiType,
        DocumentationType learnType) {

        List<DocumentationLink> links = new ArrayList<>();
        String versionString = version.getVersion();

        // Reference documentation
        links.add(DocumentationLink.builder()
            .version(version)
            .docType(referenceType)
            .title("Spring Boot Reference Documentation")
            .url(String.format("%s/spring-boot/reference/%s/", springDocsBaseUrl, versionString))
            .description("Comprehensive reference documentation for Spring Boot " + versionString)
            .isActive(true)
            .build());

        // API documentation
        links.add(DocumentationLink.builder()
            .version(version)
            .docType(apiType)
            .title("Spring Boot API Documentation")
            .url(String.format("%s/spring-boot/api/java/", springDocsBaseUrl))
            .description("Java API documentation for Spring Boot")
            .isActive(true)
            .build());

        return links;
    }

    /**
     * Creates Spring Framework documentation links.
     */
    private List<DocumentationLink> createSpringFrameworkLinks(
        ProjectVersion version,
        DocumentationType referenceType,
        DocumentationType apiType,
        DocumentationType learnType) {

        List<DocumentationLink> links = new ArrayList<>();
        String versionString = version.getVersion();

        // Reference documentation
        links.add(DocumentationLink.builder()
            .version(version)
            .docType(referenceType)
            .title("Spring Framework Reference Documentation")
            .url(String.format("%s/spring-framework/reference/%s/", springDocsBaseUrl, versionString))
            .description("Comprehensive reference documentation for Spring Framework " + versionString)
            .isActive(true)
            .build());

        // API documentation
        links.add(DocumentationLink.builder()
            .version(version)
            .docType(apiType)
            .title("Spring Framework API Documentation")
            .url(String.format("%s/spring-framework/docs/%s/javadoc-api/", springDocsBaseUrl, versionString))
            .description("Java API documentation for Spring Framework " + versionString)
            .isActive(true)
            .build());

        return links;
    }

    /**
     * Creates Spring Data documentation links.
     */
    private List<DocumentationLink> createSpringDataLinks(
        ProjectVersion version,
        DocumentationType referenceType,
        DocumentationType apiType) {

        List<DocumentationLink> links = new ArrayList<>();
        String versionString = version.getVersion();

        // Reference documentation
        links.add(DocumentationLink.builder()
            .version(version)
            .docType(referenceType)
            .title("Spring Data Reference Documentation")
            .url(String.format("%s/spring-data/reference/%s/", springDocsBaseUrl, versionString))
            .description("Reference documentation for Spring Data " + versionString)
            .isActive(true)
            .build());

        return links;
    }

    /**
     * Creates Spring Security documentation links.
     */
    private List<DocumentationLink> createSpringSecurityLinks(
        ProjectVersion version,
        DocumentationType referenceType,
        DocumentationType apiType) {

        List<DocumentationLink> links = new ArrayList<>();
        String versionString = version.getVersion();

        // Reference documentation
        links.add(DocumentationLink.builder()
            .version(version)
            .docType(referenceType)
            .title("Spring Security Reference Documentation")
            .url(String.format("%s/spring-security/reference/%s/", springDocsBaseUrl, versionString))
            .description("Reference documentation for Spring Security " + versionString)
            .isActive(true)
            .build());

        return links;
    }

    /**
     * Creates Spring Cloud documentation links.
     */
    private List<DocumentationLink> createSpringCloudLinks(
        ProjectVersion version,
        DocumentationType referenceType,
        DocumentationType apiType) {

        List<DocumentationLink> links = new ArrayList<>();
        String versionString = version.getVersion();

        // Reference documentation
        links.add(DocumentationLink.builder()
            .version(version)
            .docType(referenceType)
            .title("Spring Cloud Reference Documentation")
            .url(String.format("%s/spring-cloud/reference/%s/", springDocsBaseUrl, versionString))
            .description("Reference documentation for Spring Cloud " + versionString)
            .isActive(true)
            .build());

        return links;
    }

    /**
     * Resets bootstrap statistics.
     */
    private void resetStatistics() {
        totalProjects.set(0);
        totalVersions.set(0);
        totalLinks.set(0);
        errors.clear();
        projectStatus.clear();
    }

    /**
     * Prints a summary of the bootstrap operation.
     */
    private void printBootstrapSummary() {
        log.info("=".repeat(80));
        log.info("Bootstrap Summary");
        log.info("=".repeat(80));
        log.info("Status: {}", bootstrapCompleted.get() ? "COMPLETED" : "FAILED");
        log.info("Projects bootstrapped: {}", totalProjects.get());
        log.info("Versions created: {}", totalVersions.get());
        log.info("Documentation links created: {}", totalLinks.get());
        log.info("Errors encountered: {}", errors.size());

        if (bootstrapStartTime != null && bootstrapEndTime != null) {
            long durationMs = java.time.Duration.between(bootstrapStartTime, bootstrapEndTime).toMillis();
            log.info("Total duration: {}ms ({}s)", durationMs, durationMs / 1000.0);
        }

        if (!errors.isEmpty()) {
            log.error("Errors during bootstrap:");
            errors.forEach(error -> log.error("  - {}", error));
        }

        log.info("=".repeat(80));
    }

    // ==================== Inner Classes ====================

    /**
     * Definition of a Spring project for bootstrapping.
     */
    protected record ProjectDefinition(
        String name,
        String description,
        String homepageUrl,
        String githubUrl,
        List<String> stableVersions,
        List<String> preReleaseVersions
    ) {}

    /**
     * Definition of a documentation type.
     */
    protected record DocTypeDefinition(
        String name,
        String slug,
        int displayOrder
    ) {}

    /**
     * Status tracking for individual project bootstrap.
     */
    @lombok.Data
    private static class BootstrapStatus {
        private String status;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private Long durationMs;
        private Long projectId;
        private int versionsCreated;
        private int linksCreated;
        private String error;

        public Map<String, Object> toMap() {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("status", status);
            map.put("startTime", startTime);
            map.put("endTime", endTime);
            map.put("durationMs", durationMs);
            map.put("projectId", projectId);
            map.put("versionsCreated", versionsCreated);
            map.put("linksCreated", linksCreated);
            if (error != null) {
                map.put("error", error);
            }
            return map;
        }
    }
}
