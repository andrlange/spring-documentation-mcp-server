package com.spring.mcp.controller.web;

import com.spring.mcp.config.JavadocsFeatureConfig;
import com.spring.mcp.model.entity.JavadocSyncStatus;
import com.spring.mcp.model.entity.ProjectVersion;
import com.spring.mcp.model.entity.SpringBootVersion;
import com.spring.mcp.model.entity.SpringProject;
import com.spring.mcp.repository.JavadocPackageRepository;
import com.spring.mcp.repository.JavadocSyncStatusRepository;
import com.spring.mcp.repository.ProjectVersionRepository;
import com.spring.mcp.repository.SpringBootVersionRepository;
import com.spring.mcp.repository.SpringProjectRepository;
import com.spring.mcp.service.SettingsService;
import com.spring.mcp.service.sync.JavadocSyncService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Controller for managing Spring projects.
 * Handles CRUD operations for Spring ecosystem projects.
 *
 * <p>This controller provides endpoints for:
 * <ul>
 *   <li>Listing all Spring projects</li>
 *   <li>Viewing individual project details</li>
 *   <li>Creating new projects (ADMIN only)</li>
 *   <li>Updating existing projects (ADMIN only)</li>
 *   <li>Deleting projects (ADMIN only)</li>
 * </ul>
 *
 * @author Spring MCP Server
 * @version 1.0
 * @since 2025-01-08
 */
@Controller
@RequestMapping("/projects")
@RequiredArgsConstructor
@Slf4j
public class ProjectsController {

    private final SpringProjectRepository springProjectRepository;
    private final ProjectVersionRepository projectVersionRepository;
    private final SpringBootVersionRepository springBootVersionRepository;
    private final SettingsService settingsService;
    private final JavadocSyncStatusRepository javadocSyncStatusRepository;
    private final JavadocsFeatureConfig javadocsFeatureConfig;
    private final Optional<JavadocSyncService> javadocSyncService;
    private final Optional<JavadocPackageRepository> javadocPackageRepository;

    /**
     * List all Spring projects.
     *
     * <p>Displays a paginated list of all projects in the system,
     * including both active and inactive projects. Projects are
     * ordered by name for easy navigation.
     *
     * <p>Optionally filters projects by Spring Boot version compatibility.
     * When a Spring Boot version ID is provided via the {@code springBootVersionId}
     * parameter, only projects that have at least one version compatible with
     * the selected Spring Boot version are shown.
     *
     * @param springBootVersionId optional Spring Boot version ID for filtering
     * @param model Spring MVC model to add attributes for the view
     * @return view name "projects/list" which renders the project list template
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public String listProjects(
            @RequestParam(required = false) Long springBootVersionId,
            Model model) {
        log.debug("Listing projects with Spring Boot version filter: {}", springBootVersionId);

        // Fetch all Spring Boot versions for the dropdown
        List<SpringBootVersion> springBootVersions = springBootVersionRepository.findAllOrderByVersionDesc();
        model.addAttribute("springBootVersions", springBootVersions);

        // Fetch projects - filtered if a Spring Boot version is selected
        List<SpringProject> projects;
        SpringBootVersion selectedVersion = null;

        if (springBootVersionId != null) {
            selectedVersion = springBootVersionRepository.findById(springBootVersionId).orElse(null);
            if (selectedVersion != null) {
                log.debug("Filtering by Spring Boot version: {}", selectedVersion.getVersion());
                projects = springProjectRepository.findAllCompatibleWithSpringBootVersion(springBootVersionId);
                model.addAttribute("selectedSpringBootVersionId", springBootVersionId);
                model.addAttribute("selectedSpringBootVersion", selectedVersion);

                // Create a map of filtered versions for each project
                java.util.Map<Long, List<ProjectVersion>> projectVersionsMap = new java.util.HashMap<>();
                for (SpringProject project : projects) {
                    List<ProjectVersion> filteredVersions = projectVersionRepository
                        .findByProjectIdAndSpringBootVersionId(project.getId(), springBootVersionId)
                        .stream()
                        .filter(ProjectVersion::getVisible)
                        .toList();
                    projectVersionsMap.put(project.getId(), filteredVersions);
                }
                model.addAttribute("projectVersionsMap", projectVersionsMap);
            } else {
                log.warn("Spring Boot version with id {} not found, showing all projects", springBootVersionId);
                projects = springProjectRepository.findAll();
            }
        } else {
            projects = springProjectRepository.findAll();
        }

        model.addAttribute("projects", projects);
        model.addAttribute("pageTitle", "Spring Projects");
        model.addAttribute("activePage", "projects");

        // Add Javadoc sync status information
        boolean javadocsEnabled = javadocsFeatureConfig.isEnabled();
        model.addAttribute("javadocsEnabled", javadocsEnabled);

        if (javadocsEnabled) {
            // Build a map of project ID -> JavadocSyncStatus
            Map<Long, JavadocSyncStatus> javadocSyncStatusMap = new HashMap<>();
            List<JavadocSyncStatus> allStatuses = javadocSyncStatusRepository.findAllWithProjects();
            for (JavadocSyncStatus status : allStatuses) {
                javadocSyncStatusMap.put(status.getProject().getId(), status);
            }
            model.addAttribute("javadocSyncStatusMap", javadocSyncStatusMap);

            // Build a map of project ID -> fetch status
            // Values: "all" (green), "partial" (orange), "none" (red), "no_urls" (gray)
            Map<Long, String> javadocFetchStatusMap = new HashMap<>();
            for (SpringProject project : projects) {
                String fetchStatus = computeFetchStatus(project.getId());
                javadocFetchStatusMap.put(project.getId(), fetchStatus);
            }
            model.addAttribute("javadocFetchStatusMap", javadocFetchStatusMap);

            // Add max failures for UI tooltip
            model.addAttribute("javadocMaxFailures", javadocsFeatureConfig.getSync().getMaxFailures());
        }

        log.info("Retrieved {} projects (filtered: {})", projects.size(), springBootVersionId != null);
        return "projects/list";
    }

    /**
     * Compute Javadoc fetch status for a project.
     *
     * @param projectId the project ID
     * @return status string: "all", "partial", "none", or "no_urls"
     */
    private String computeFetchStatus(Long projectId) {
        List<ProjectVersion> versionsWithUrls = projectVersionRepository.findByProjectIdWithApiDocUrl(projectId);

        if (versionsWithUrls.isEmpty()) {
            return "no_urls";
        }

        // Check sync status
        Optional<JavadocSyncStatus> statusOpt = javadocSyncStatusRepository.findByProjectId(projectId);
        if (statusOpt.isEmpty()) {
            return "none";
        }

        JavadocSyncStatus status = statusOpt.get();
        if (status.getLastSyncAt() != null && status.getFailureCount() == 0) {
            return "all";
        } else if (status.getLastSyncAt() != null && status.getFailureCount() > 0) {
            return "partial";
        }

        return "none";
    }

    /**
     * Display a single project's details.
     *
     * <p>Shows comprehensive information about a specific project including:
     * <ul>
     *   <li>Project metadata (name, slug, description)</li>
     *   <li>Associated versions</li>
     *   <li>Homepage and GitHub URLs</li>
     *   <li>Active status</li>
     * </ul>
     *
     * <p>Optionally filters project versions by Spring Boot version compatibility.
     * When a Spring Boot version ID is provided, only versions compatible with
     * the selected Spring Boot version are shown.
     *
     * @param id the project ID
     * @param springBootVersionId optional Spring Boot version ID for filtering
     * @param model Spring MVC model to add attributes for the view
     * @param redirectAttributes redirect attributes for flash messages
     * @return view name "projects/detail" or redirect to list if not found
     */
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public String viewProject(
            @PathVariable Long id,
            @RequestParam(required = false) Long springBootVersionId,
            Model model,
            RedirectAttributes redirectAttributes) {
        log.debug("Viewing project with id: {} and Spring Boot version filter: {}", id, springBootVersionId);

        return springProjectRepository.findById(id)
            .map(project -> {
                model.addAttribute("project", project);
                model.addAttribute("pageTitle", project.getName());
                model.addAttribute("activePage", "projects");

                // Fetch all Spring Boot versions for the dropdown
                List<SpringBootVersion> springBootVersions = springBootVersionRepository.findAllOrderByVersionDesc();
                model.addAttribute("springBootVersions", springBootVersions);

                // Fetch versions - filtered if a Spring Boot version is selected
                List<ProjectVersion> versions;
                SpringBootVersion selectedVersion = null;

                if (springBootVersionId != null) {
                    selectedVersion = springBootVersionRepository.findById(springBootVersionId).orElse(null);
                    if (selectedVersion != null) {
                        log.debug("Filtering versions by Spring Boot version: {}", selectedVersion.getVersion());
                        versions = projectVersionRepository.findByProjectIdAndSpringBootVersionId(
                                project.getId(), springBootVersionId);
                        model.addAttribute("selectedSpringBootVersionId", springBootVersionId);
                        model.addAttribute("selectedSpringBootVersion", selectedVersion);
                    } else {
                        log.warn("Spring Boot version with id {} not found, showing all versions", springBootVersionId);
                        versions = projectVersionRepository.findByProjectId(project.getId());
                    }
                } else {
                    versions = projectVersionRepository.findByProjectId(project.getId());
                }

                // Filter to show only visible versions (status != null)
                final List<ProjectVersion> visibleVersions =
                        versions.stream().filter(ProjectVersion::getVisible).toList();

                // Get enterprise subscription setting
                boolean enterpriseSubscriptionEnabled = settingsService.isEnterpriseSubscriptionEnabled();

                model.addAttribute("versions", visibleVersions);
                model.addAttribute("enterpriseSubscriptionEnabled", enterpriseSubscriptionEnabled);

                // Count versions with API doc URLs (always needed for stats)
                long versionsWithApiDocs = visibleVersions.stream()
                        .filter(v -> v.getApiDocUrl() != null && !v.getApiDocUrl().isBlank())
                        .count();
                model.addAttribute("versionsWithApiDocs", versionsWithApiDocs);

                // Count versions with reference doc URLs
                long versionsWithReferenceDocs = visibleVersions.stream()
                        .filter(v -> v.getReferenceDocUrl() != null && !v.getReferenceDocUrl().isBlank())
                        .count();
                model.addAttribute("versionsWithReferenceDocs", versionsWithReferenceDocs);

                // Add Javadoc status information
                boolean javadocsEnabled = javadocsFeatureConfig.isEnabled();
                model.addAttribute("javadocsEnabled", javadocsEnabled);

                if (javadocsEnabled && javadocPackageRepository.isPresent()) {
                    // Get sync status for this project
                    Optional<JavadocSyncStatus> syncStatusOpt = javadocSyncStatusRepository.findByProjectId(id);
                    model.addAttribute("javadocSyncStatus", syncStatusOpt.orElse(null));

                    // Build a map of version -> hasLocalJavadocs (check actual data in DB)
                    Map<String, Boolean> javadocAvailableByVersion = new HashMap<>();
                    var pkgRepo = javadocPackageRepository.get();
                    for (ProjectVersion pv : visibleVersions) {
                        if (pv.getApiDocUrl() != null && !pv.getApiDocUrl().isBlank()) {
                            // Check if we have Javadocs stored for this version (actual packages exist)
                            boolean hasLocalJavadocs = pkgRepo.existsByLibraryNameAndVersion(
                                    project.getSlug(), pv.getVersion());
                            javadocAvailableByVersion.put(pv.getVersion(), hasLocalJavadocs);
                        }
                    }
                    model.addAttribute("javadocAvailableByVersion", javadocAvailableByVersion);
                    model.addAttribute("projectSlug", project.getSlug());
                }

                log.info("Retrieved {} visible versions for project {} (filtered: {})",
                        visibleVersions.size(), project.getName(), springBootVersionId != null);

                return "projects/detail";
            })
            .orElseGet(() -> {
                log.warn("Project with id {} not found", id);
                redirectAttributes.addFlashAttribute("error", "Project not found");
                return "redirect:/projects";
            });
    }

    /**
     * Toggle Javadoc sync for a project (AJAX endpoint).
     *
     * <p>This endpoint toggles the enabled state of Javadoc synchronization
     * for a specific project. Only accessible by users with ADMIN role.
     *
     * @param id the project ID
     * @return JSON response with new enabled state and status message
     */
    @PostMapping("/{id}/javadoc-sync-toggle")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> toggleJavadocSync(@PathVariable Long id) {
        log.debug("Toggling Javadoc sync for project id: {}", id);

        Map<String, Object> response = new HashMap<>();

        if (!javadocsFeatureConfig.isEnabled()) {
            response.put("success", false);
            response.put("message", "Javadoc feature is disabled");
            return ResponseEntity.badRequest().body(response);
        }

        if (javadocSyncService.isEmpty()) {
            response.put("success", false);
            response.put("message", "Javadoc sync service not available");
            return ResponseEntity.badRequest().body(response);
        }

        // Check if project exists
        Optional<SpringProject> projectOpt = springProjectRepository.findById(id);
        if (projectOpt.isEmpty()) {
            response.put("success", false);
            response.put("message", "Project not found");
            return ResponseEntity.notFound().build();
        }

        // Check if project has any versions with API doc URLs
        List<ProjectVersion> versionsWithUrls = projectVersionRepository.findByProjectIdWithApiDocUrl(id);
        if (versionsWithUrls.isEmpty()) {
            response.put("success", false);
            response.put("message", "No versions with API documentation URLs configured");
            return ResponseEntity.badRequest().body(response);
        }

        // Toggle the sync
        Boolean newState = javadocSyncService.get().toggleSync(id);
        if (newState == null) {
            response.put("success", false);
            response.put("message", "Failed to toggle sync");
            return ResponseEntity.internalServerError().body(response);
        }

        response.put("success", true);
        response.put("enabled", newState);
        response.put("message", newState ? "Javadoc sync enabled" : "Javadoc sync disabled");
        response.put("projectId", id);
        response.put("projectName", projectOpt.get().getName());

        log.info("Toggled Javadoc sync for project {} to: {}", projectOpt.get().getSlug(), newState);
        return ResponseEntity.ok(response);
    }

}
