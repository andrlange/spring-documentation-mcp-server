package com.spring.mcp.controller.web;

import com.spring.mcp.model.entity.DocumentationLink;
import com.spring.mcp.model.entity.ProjectVersion;
import com.spring.mcp.repository.DocumentationContentRepository;
import com.spring.mcp.repository.DocumentationLinkRepository;
import com.spring.mcp.repository.DocumentationTypeRepository;
import com.spring.mcp.repository.ProjectVersionRepository;
import com.spring.mcp.repository.SpringProjectRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Objects;

/**
 * Controller for managing documentation links.
 * Handles display and navigation of Spring documentation across projects and versions.
 *
 * <p>This controller provides endpoints for:
 * <ul>
 *   <li>Listing all documentation links with pagination</li>
 *   <li>Viewing individual documentation link details</li>
 *   <li>Searching documentation by title or URL</li>
 *   <li>Filtering documentation by project, version, or type</li>
 * </ul>
 *
 * <p>Documentation links represent external Spring documentation resources
 * including reference guides, API docs, tutorials, and samples.
 *
 * @author Spring MCP Server
 * @version 1.0
 * @since 2025-01-08
 */
@Controller
@RequestMapping("/documentation")
@RequiredArgsConstructor
@Slf4j
public class DocumentationController {

    private final DocumentationLinkRepository documentationLinkRepository;
    private final DocumentationContentRepository documentationContentRepository;
    private final ProjectVersionRepository projectVersionRepository;
    private final DocumentationTypeRepository documentationTypeRepository;
    private final SpringProjectRepository springProjectRepository;
    private final com.spring.mcp.service.SettingsService settingsService;

    /**
     * List all documentation with advanced filtering and full-text search.
     * Supports filtering by project, version, type, status, and free text search.
     *
     * @param page the page number (0-indexed, default: 0)
     * @param size the page size (default: 50)
     * @param projectSlug the project slug filter (optional)
     * @param versionId the version ID filter (optional)
     * @param docTypeSlug the documentation type slug filter (optional)
     * @param activeOnly filter to show only active links (default: true)
     * @param search free text search query (optional)
     * @param model Spring MVC model to add attributes for the view
     * @return view name "documentation/list"
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public String listDocumentation(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(required = false) String projectSlug,
            @RequestParam(required = false) Long versionId,
            @RequestParam(required = false) String docTypeSlug,
            @RequestParam(required = false) String active,
            @RequestParam(required = false) String search,
            Model model) {

        log.debug("Listing documentation - page: {}, size: {}, projectSlug: {}, active: {}, search: '{}'",
            page, size, projectSlug, active, search);

        // Determine if we should filter to active only
        boolean activeOnly = active == null || active.isEmpty() || "true".equals(active);

        try {
            List<DocumentationLink> documentationLinks;
            long totalElements = 0;

            // Determine version string from versionId
            String versionString = null;
            if (versionId != null) {
                versionString = projectVersionRepository.findById(versionId)
                    .map(ProjectVersion::getVersion)
                    .orElse(null);
            }

            // If there's a search query, use full-text search
            if (search != null && !search.isBlank()) {
                log.debug("Performing full-text search with query: '{}'", search);

                // Convert empty strings to null for SQL queries
                String projectSlugOrNull = (projectSlug != null && !projectSlug.isEmpty()) ? projectSlug : null;
                String docTypeSlugOrNull = (docTypeSlug != null && !docTypeSlug.isEmpty()) ? docTypeSlug : null;

                // Use DocumentationContentRepository for full-text search
                int offset = page * size;
                List<Long> linkIds = documentationContentRepository.advancedFullTextSearch(
                    search,
                    projectSlugOrNull,
                    versionString,
                    docTypeSlugOrNull,
                    size,
                    offset
                );

                // Count total results for pagination
                Long count = documentationContentRepository.countAdvancedSearch(
                    search,
                    projectSlugOrNull,
                    versionString,
                    docTypeSlugOrNull
                );
                totalElements = count != null ? count : 0;

                // Convert search results to DocumentationLink objects
                documentationLinks = linkIds.stream()
                    .map(linkId -> documentationLinkRepository.findById(linkId).orElse(null))
                    .filter(Objects::nonNull)
                    .filter(link -> !activeOnly || link.getIsActive())
                    .toList();

            } else {
                // No search query - filter by other criteria
                List<DocumentationLink> allLinks;

                // Check if any filters are actually set (not null and not empty)
                boolean hasProjectFilter = projectSlug != null && !projectSlug.isEmpty();
                boolean hasVersionFilter = versionId != null;
                boolean hasDocTypeFilter = docTypeSlug != null && !docTypeSlug.isEmpty();

                if (hasProjectFilter || hasVersionFilter || hasDocTypeFilter) {
                    // Apply filters
                    allLinks = documentationLinkRepository.findAll().stream()
                        .filter(link -> {
                            if (activeOnly && !link.getIsActive()) return false;
                            if (hasProjectFilter && !link.getVersion().getProject().getSlug().equals(projectSlug)) return false;
                            if (hasVersionFilter && !link.getVersion().getId().equals(versionId)) return false;
                            if (hasDocTypeFilter && !link.getDocType().getSlug().equals(docTypeSlug)) return false;
                            return true;
                        })
                        .toList();
                } else {
                    // No filters - get all (or only active)
                    if (activeOnly) {
                        allLinks = documentationLinkRepository.findAll().stream()
                            .filter(DocumentationLink::getIsActive)
                            .toList();
                    } else {
                        allLinks = documentationLinkRepository.findAll();
                    }
                }

                totalElements = allLinks.size();

                // Manual pagination
                int start = Math.min(page * size, allLinks.size());
                int end = Math.min(start + size, allLinks.size());
                documentationLinks = allLinks.subList(start, end);
            }

            int totalPages = (int) Math.ceil((double) totalElements / size);

            model.addAttribute("documentationLinks", documentationLinks);
            model.addAttribute("currentPage", page);
            model.addAttribute("totalPages", totalPages);
            model.addAttribute("totalElements", totalElements);

            // Add filter data for dropdowns
            model.addAttribute("projects", springProjectRepository.findAll());
            model.addAttribute("versions", versionId != null ?
                projectVersionRepository.findAll() : List.of());
            model.addAttribute("docTypes", documentationTypeRepository.findAll());

            // Add current filter values
            model.addAttribute("projectSlug", projectSlug != null ? projectSlug : "");
            model.addAttribute("versionId", versionId != null ? versionId : "");
            model.addAttribute("docTypeSlug", docTypeSlug != null ? docTypeSlug : "");
            model.addAttribute("search", search != null ? search : "");
            model.addAttribute("active", active != null ? active : "");

            model.addAttribute("pageSize", size);
            model.addAttribute("pageTitle", "Documentation");
            model.addAttribute("activePage", "documentation");

            // Add enterprise subscription setting for header
            boolean enterpriseSubscriptionEnabled = settingsService.isEnterpriseSubscriptionEnabled();
            model.addAttribute("enterpriseSubscriptionEnabled", enterpriseSubscriptionEnabled);

            log.info("Retrieved {} documentation links (page {}/{})",
                documentationLinks.size(), page + 1, totalPages);

            return "documentation/list";
        } catch (Exception e) {
            log.error("Error listing documentation", e);
            model.addAttribute("error", "Failed to load documentation links: " + e.getMessage());
            return "error/general";
        }
    }

    /**
     * Display details for a specific documentation link.
     *
     * <p>Shows comprehensive information about a documentation link including:
     * <ul>
     *   <li>Title and description</li>
     *   <li>URL to the documentation resource</li>
     *   <li>Associated project and version</li>
     *   <li>Documentation type (Overview, Learn, Support, Samples)</li>
     *   <li>Content hash for change detection</li>
     *   <li>Last fetched timestamp</li>
     *   <li>Active status</li>
     * </ul>
     *
     * @param id the documentation link ID
     * @param model Spring MVC model to add attributes for the view
     * @param redirectAttributes redirect attributes for flash messages
     * @return view name "documentation/detail" or redirect if not found
     */
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public String viewDocumentation(
            @PathVariable Long id,
            Model model,
            RedirectAttributes redirectAttributes) {

        log.debug("Viewing documentation link with id: {}", id);

        return documentationLinkRepository.findById(id)
            .map(link -> {
                model.addAttribute("documentation", link);
                model.addAttribute("version", link.getVersion());
                model.addAttribute("project", link.getVersion().getProject());
                model.addAttribute("docType", link.getDocType());
                model.addAttribute("pageTitle", link.getTitle());

                log.info("Displaying documentation: {} (id: {}, url: {})",
                    link.getTitle(), link.getId(), link.getUrl());

                model.addAttribute("activePage", "documentation");
                return "documentation/detail";
            })
            .orElseGet(() -> {
                log.warn("Documentation link with id {} not found", id);
                redirectAttributes.addFlashAttribute("error", "Documentation link not found");
                return "redirect:/documentation";
            });
    }

    /**
     * Search documentation links by title or content.
     *
     * <p>Performs a case-insensitive search across documentation link titles.
     * Future enhancements could include full-text search across cached content.
     *
     * @param query the search query
     * @param model Spring MVC model to add attributes for the view
     * @return view name "documentation/search-results"
     */
    @GetMapping("/search")
    @PreAuthorize("isAuthenticated()")
    public String searchDocumentation(
            @RequestParam(required = false) String query,
            Model model) {

        log.debug("Searching documentation with query: {}", query);

        if (query == null || query.trim().isEmpty()) {
            model.addAttribute("documentation", List.of());
            model.addAttribute("query", "");
            model.addAttribute("pageTitle", "Search Documentation");
            model.addAttribute("activePage", "documentation");
            return "documentation/search-results";
        }

        try {
            List<DocumentationLink> results = documentationLinkRepository.searchByTitle(query);

            model.addAttribute("documentation", results);
            model.addAttribute("query", query);
            model.addAttribute("resultCount", results.size());
            model.addAttribute("pageTitle", "Search Results");

            log.info("Found {} documentation links matching query: {}", results.size(), query);

            model.addAttribute("activePage", "documentation");
            return "documentation/search-results";
        } catch (Exception e) {
            log.error("Error searching documentation", e);
            model.addAttribute("error", "Failed to search documentation");
            return "error/general";
        }
    }

    /**
     * List documentation links for a specific version.
     *
     * <p>Displays all documentation links associated with a particular
     * project version, grouped by documentation type.
     *
     * @param versionId the version ID
     * @param model Spring MVC model to add attributes for the view
     * @param redirectAttributes redirect attributes for flash messages
     * @return view name "documentation/version-docs" or redirect if not found
     */
    @GetMapping("/version/{versionId}")
    @PreAuthorize("isAuthenticated()")
    public String listDocumentationByVersion(
            @PathVariable Long versionId,
            Model model,
            RedirectAttributes redirectAttributes) {

        log.debug("Listing documentation for version id: {}", versionId);

        return projectVersionRepository.findById(versionId)
            .map(version -> {
                List<DocumentationLink> links = documentationLinkRepository
                    .findByVersionAndIsActiveTrue(version);

                model.addAttribute("version", version);
                model.addAttribute("project", version.getProject());
                model.addAttribute("documentation", links);
                model.addAttribute("pageTitle",
                    version.getProject().getName() + " " + version.getVersion() + " Documentation");

                // Group by documentation type
                var groupedDocs = links.stream()
                    .collect(java.util.stream.Collectors.groupingBy(
                        DocumentationLink::getDocType));
                model.addAttribute("groupedDocumentation", groupedDocs);

                log.info("Retrieved {} documentation links for version: {} {}",
                    links.size(), version.getProject().getName(), version.getVersion());

                model.addAttribute("activePage", "documentation");
                return "documentation/version-docs";
            })
            .orElseGet(() -> {
                log.warn("Version with id {} not found", versionId);
                redirectAttributes.addFlashAttribute("error", "Version not found");
                return "redirect:/versions";
            });
    }

    /**
     * List all available documentation types.
     *
     * <p>Displays all documentation type categories available in the system
     * (e.g., Overview, Learn, Support, Samples).
     *
     * @param model Spring MVC model to add attributes for the view
     * @return view name "documentation/types"
     */
    @GetMapping("/types")
    @PreAuthorize("isAuthenticated()")
    public String listDocumentationTypes(Model model) {
        log.debug("Listing documentation types");

        try {
            var types = documentationTypeRepository.findAll(
                Sort.by("displayOrder").ascending());

            model.addAttribute("documentationTypes", types);
            model.addAttribute("pageTitle", "Documentation Types");

            log.info("Retrieved {} documentation types", types.size());

            model.addAttribute("activePage", "documentation");
            return "documentation/types";
        } catch (Exception e) {
            log.error("Error listing documentation types", e);
            model.addAttribute("error", "Failed to load documentation types");
            return "error/general";
        }
    }

    /**
     * Show form for creating a new documentation link.
     *
     * <p>Security: Only users with ADMIN role can access this endpoint.
     *
     * @param model Spring MVC model to add attributes for the view
     * @return view name "documentation/form" which renders the documentation creation form
     */
    @GetMapping("/new")
    @PreAuthorize("hasRole('ADMIN')")
    public String showCreateForm(Model model) {
        log.debug("Showing create documentation form");

        model.addAttribute("documentation", new DocumentationLink());

        // Load projects with their versions for grouped select
        var projects = springProjectRepository.findAll(Sort.by(Sort.Direction.ASC, "name"));
        model.addAttribute("projects", projects);
        model.addAttribute("docTypes", documentationTypeRepository.findAll(Sort.by("displayOrder").ascending()));
        model.addAttribute("pageTitle", "Create New Documentation");
        model.addAttribute("activePage", "documentation");

        return "documentation/form";
    }

    /**
     * Process the creation of a new documentation link.
     *
     * <p>Validates the documentation data and saves it to the database.
     *
     * <p>Security: Only users with ADMIN role can access this endpoint.
     *
     * @param documentation the documentation to create
     * @param bindingResult validation result
     * @param versionId the version ID from the form
     * @param docTypeId the documentation type ID from the form
     * @param redirectAttributes redirect attributes for flash messages
     * @param model Spring MVC model
     * @return redirect to documentation detail on success, or back to form on error
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public String createDocumentation(
            @Valid @ModelAttribute("documentation") DocumentationLink documentation,
            BindingResult bindingResult,
            @RequestParam("version.id") Long versionId,
            @RequestParam("docType.id") Long docTypeId,
            RedirectAttributes redirectAttributes,
            Model model) {

        log.debug("Creating new documentation: {}", documentation.getTitle());

        // Set the version and doc type
        projectVersionRepository.findById(versionId).ifPresentOrElse(
            documentation::setVersion,
            () -> bindingResult.rejectValue("version", "invalid", "Invalid version selected")
        );

        documentationTypeRepository.findById(docTypeId).ifPresentOrElse(
            documentation::setDocType,
            () -> bindingResult.rejectValue("docType", "invalid", "Invalid documentation type selected")
        );

        if (bindingResult.hasErrors()) {
            log.warn("Validation errors while creating documentation: {}", bindingResult.getAllErrors());
            var projects = springProjectRepository.findAll(Sort.by(Sort.Direction.ASC, "name"));
            model.addAttribute("projects", projects);
            model.addAttribute("docTypes", documentationTypeRepository.findAll(Sort.by("displayOrder").ascending()));
            model.addAttribute("pageTitle", "Create New Documentation");
            model.addAttribute("activePage", "documentation");
            return "documentation/form";
        }

        try {
            DocumentationLink savedDoc = documentationLinkRepository.save(documentation);
            log.info("Successfully created documentation: {} (id: {})", savedDoc.getTitle(), savedDoc.getId());
            redirectAttributes.addFlashAttribute("success", "Documentation link created successfully");
            return "redirect:/documentation/" + savedDoc.getId();
        } catch (Exception e) {
            log.error("Error creating documentation", e);
            redirectAttributes.addFlashAttribute("error", "Failed to create documentation link");
            return "redirect:/documentation";
        }
    }

    /**
     * Show form for editing an existing documentation link.
     *
     * <p>Security: Only users with ADMIN role can access this endpoint.
     *
     * @param id the documentation link ID to edit
     * @param model Spring MVC model to add attributes for the view
     * @param redirectAttributes redirect attributes for flash messages
     * @return view name "documentation/form" or redirect if not found
     */
    @GetMapping("/{id}/edit")
    @PreAuthorize("hasRole('ADMIN')")
    public String showEditForm(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        log.debug("Showing edit form for documentation link id: {}", id);

        return documentationLinkRepository.findById(id)
            .map(documentation -> {
                model.addAttribute("documentation", documentation);
                var projects = springProjectRepository.findAll(Sort.by(Sort.Direction.ASC, "name"));
                model.addAttribute("projects", projects);
                model.addAttribute("docTypes", documentationTypeRepository.findAll(Sort.by("displayOrder").ascending()));
                model.addAttribute("pageTitle", "Edit " + documentation.getTitle());
                model.addAttribute("activePage", "documentation");
                return "documentation/form";
            })
            .orElseGet(() -> {
                log.warn("Documentation link with id {} not found for editing", id);
                redirectAttributes.addFlashAttribute("error", "Documentation link not found");
                return "redirect:/documentation";
            });
    }

    /**
     * Process the update of an existing documentation link.
     *
     * <p>Validates and updates the documentation information.
     *
     * <p>Security: Only users with ADMIN role can access this endpoint.
     *
     * @param id the documentation link ID to update
     * @param documentation the updated documentation data
     * @param bindingResult validation result
     * @param versionId the version ID from the form
     * @param docTypeId the documentation type ID from the form
     * @param redirectAttributes redirect attributes for flash messages
     * @param model Spring MVC model
     * @return redirect to documentation detail on success, or back to form on error
     */
    @PostMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public String updateDocumentation(
            @PathVariable Long id,
            @Valid @ModelAttribute("documentation") DocumentationLink documentation,
            BindingResult bindingResult,
            @RequestParam("version.id") Long versionId,
            @RequestParam("docType.id") Long docTypeId,
            RedirectAttributes redirectAttributes,
            Model model) {

        log.debug("Updating documentation link with id: {}", id);

        // Set the version and doc type
        projectVersionRepository.findById(versionId).ifPresentOrElse(
            documentation::setVersion,
            () -> bindingResult.rejectValue("version", "invalid", "Invalid version selected")
        );

        documentationTypeRepository.findById(docTypeId).ifPresentOrElse(
            documentation::setDocType,
            () -> bindingResult.rejectValue("docType", "invalid", "Invalid documentation type selected")
        );

        if (bindingResult.hasErrors()) {
            log.warn("Validation errors while updating documentation: {}", bindingResult.getAllErrors());
            var projects = springProjectRepository.findAll(Sort.by(Sort.Direction.ASC, "name"));
            model.addAttribute("projects", projects);
            model.addAttribute("docTypes", documentationTypeRepository.findAll(Sort.by("displayOrder").ascending()));
            model.addAttribute("pageTitle", "Edit Documentation");
            model.addAttribute("activePage", "documentation");
            return "documentation/form";
        }

        return documentationLinkRepository.findById(id)
            .map(existingDoc -> {
                // Update fields
                existingDoc.setTitle(documentation.getTitle());
                existingDoc.setUrl(documentation.getUrl());
                existingDoc.setDescription(documentation.getDescription());
                existingDoc.setIsActive(documentation.getIsActive());
                existingDoc.setVersion(documentation.getVersion());
                existingDoc.setDocType(documentation.getDocType());

                try {
                    DocumentationLink updatedDoc = documentationLinkRepository.save(existingDoc);
                    log.info("Successfully updated documentation: {} (id: {})",
                        updatedDoc.getTitle(), updatedDoc.getId());
                    redirectAttributes.addFlashAttribute("success", "Documentation link updated successfully");
                    return "redirect:/documentation/" + updatedDoc.getId();
                } catch (Exception e) {
                    log.error("Error updating documentation", e);
                    redirectAttributes.addFlashAttribute("error", "Failed to update documentation link");
                    return "redirect:/documentation";
                }
            })
            .orElseGet(() -> {
                log.warn("Documentation link with id {} not found for update", id);
                redirectAttributes.addFlashAttribute("error", "Documentation link not found");
                return "redirect:/documentation";
            });
    }

    /**
     * Delete a documentation link.
     *
     * <p>Permanently removes a documentation link and associated content
     * from the database. This operation cannot be undone.
     *
     * <p>Security: Only users with ADMIN role can access this endpoint.
     *
     * @param id the documentation link ID to delete
     * @param redirectAttributes redirect attributes for flash messages
     * @return redirect to documentation list
     */
    @PostMapping("/{id}/delete")
    @PreAuthorize("hasRole('ADMIN')")
    public String deleteDocumentation(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        log.debug("Attempting to delete documentation link with id: {}", id);

        return documentationLinkRepository.findById(id)
            .map(documentation -> {
                try {
                    String docTitle = documentation.getTitle();
                    documentationLinkRepository.delete(documentation);
                    log.info("Successfully deleted documentation: {} (id: {})", docTitle, id);
                    redirectAttributes.addFlashAttribute("success",
                        "Documentation link '" + docTitle + "' deleted successfully");
                } catch (Exception e) {
                    log.error("Error deleting documentation link with id: {}", id, e);
                    redirectAttributes.addFlashAttribute("error",
                        "Failed to delete documentation link.");
                }
                return "redirect:/documentation";
            })
            .orElseGet(() -> {
                log.warn("Documentation link with id {} not found for deletion", id);
                redirectAttributes.addFlashAttribute("error", "Documentation link not found");
                return "redirect:/documentation";
            });
    }
}
