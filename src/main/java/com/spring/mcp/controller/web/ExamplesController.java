package com.spring.mcp.controller.web;

import com.spring.mcp.model.entity.CodeExample;
import com.spring.mcp.model.entity.SpringProject;
import com.spring.mcp.repository.CodeExampleRepository;
import com.spring.mcp.repository.ProjectVersionRepository;
import com.spring.mcp.repository.SpringProjectRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Controller for managing code examples.
 * Handles operations for Spring code examples and samples.
 *
 * @author Spring MCP Server
 * @version 1.0
 * @since 2025-01-08
 */
@Controller
@RequestMapping("/examples")
@RequiredArgsConstructor
@Slf4j
public class ExamplesController {

    private final CodeExampleRepository codeExampleRepository;
    private final ProjectVersionRepository projectVersionRepository;
    private final SpringProjectRepository springProjectRepository;

    /**
     * List all code examples with advanced filtering and full-text search.
     * Supports filtering by project, version, category, and free text search.
     *
     * @param projectSlug the project slug filter (optional)
     * @param version the version string filter (optional)
     * @param category the category filter (optional)
     * @param search free text search query (optional)
     * @param model Spring MVC model to add attributes for the view
     * @return view name "examples/list"
     */
    @GetMapping
    public String listExamples(
            @RequestParam(required = false) String projectSlug,
            @RequestParam(required = false) String version,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String search,
            Model model) {

        log.debug("Listing code examples - projectSlug: {}, version: {}, category: {}, search: '{}'",
            projectSlug, version, category, search);

        // Convert empty strings to null for proper SQL filtering
        String normalizedProjectSlug = (projectSlug != null && projectSlug.trim().isEmpty()) ? null : projectSlug;
        String normalizedVersion = (version != null && version.trim().isEmpty()) ? null : version;
        String normalizedCategory = (category != null && category.trim().isEmpty()) ? null : category;
        String normalizedSearch = (search != null && search.trim().isEmpty()) ? null : search;

        try {
            // Use the advanced filtering query
            var examples = codeExampleRepository.findWithFilters(
                normalizedProjectSlug,
                normalizedVersion,
                normalizedCategory,
                normalizedSearch
            );

            log.debug("Found {} code examples matching filters", examples.size());

            // Prepare filter data for dropdowns
            var allProjects = springProjectRepository.findAll(Sort.by(Sort.Direction.ASC, "name")).stream()
                .filter(SpringProject::getActive)
                .map(SpringProject::getSlug)
                .distinct()
                .toList();

            var allCategories = codeExampleRepository.findDistinctCategories();

            // Add attributes to model
            model.addAttribute("examples", examples);
            model.addAttribute("allProjects", allProjects);
            model.addAttribute("allCategories", allCategories);
            model.addAttribute("activePage", "examples");
            model.addAttribute("pageTitle", "Code Examples");
            model.addAttribute("totalElements", examples.size());

            // Preserve filter values
            model.addAttribute("selectedProject", projectSlug != null ? projectSlug : "");
            model.addAttribute("selectedCategory", category != null ? category : "");
            model.addAttribute("searchQuery", search != null ? search : "");

            return "examples/list";

        } catch (Exception e) {
            log.error("Error listing code examples", e);
            model.addAttribute("error", "Error loading code examples: " + e.getMessage());
            model.addAttribute("examples", java.util.List.of());
            model.addAttribute("allProjects", java.util.List.of());
            model.addAttribute("allCategories", java.util.List.of());
            model.addAttribute("activePage", "examples");
            model.addAttribute("pageTitle", "Code Examples");
            model.addAttribute("totalElements", 0);
            return "examples/list";
        }
    }

    /**
     * Show details of a specific code example.
     *
     * @param id the example ID
     * @param model Spring MVC model to add attributes for the view
     * @param redirectAttributes for flash messages on redirect
     * @return view name "examples/detail" or redirect to list
     */
    @GetMapping("/{id}")
    public String showExample(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        log.debug("Showing code example with ID: {}", id);

        return codeExampleRepository.findById(id)
            .map(example -> {
                model.addAttribute("example", example);
                model.addAttribute("activePage", "examples");
                model.addAttribute("pageTitle", example.getTitle());
                return "examples/detail";
            })
            .orElseGet(() -> {
                log.warn("Code example not found with ID: {}", id);
                redirectAttributes.addFlashAttribute("error", "Code example not found");
                return "redirect:/examples";
            });
    }

    /**
     * Show form to create a new code example.
     *
     * @param model Spring MVC model to add attributes for the view
     * @return view name "examples/form"
     */
    @GetMapping("/new")
    @PreAuthorize("hasRole('ADMIN')")
    public String showCreateForm(Model model) {
        log.debug("Showing create code example form");

        var example = CodeExample.builder()
            .language("java")
            .build();

        model.addAttribute("example", example);

        // Load projects with their versions for grouped select
        var projects = springProjectRepository.findAll(Sort.by(Sort.Direction.ASC, "name"));
        model.addAttribute("projects", projects);
        model.addAttribute("pageTitle", "Create New Code Example");
        model.addAttribute("activePage", "examples");

        return "examples/form";
    }

    /**
     * Create a new code example.
     *
     * @param example the code example to create
     * @param bindingResult validation result
     * @param versionId the version ID to associate with
     * @param redirectAttributes for flash messages
     * @param model Spring MVC model
     * @return redirect to example detail or form on error
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public String createExample(
            @Valid @ModelAttribute("example") CodeExample example,
            BindingResult bindingResult,
            @RequestParam("version.id") Long versionId,
            @RequestParam(value = "tagsInput", required = false) String tagsInput,
            RedirectAttributes redirectAttributes,
            Model model) {

        log.debug("Creating new code example: {}", example.getTitle());

        // Set the version
        projectVersionRepository.findById(versionId).ifPresentOrElse(
            example::setVersion,
            () -> bindingResult.rejectValue("version", "invalid", "Invalid version selected")
        );

        // Convert tags input to array
        if (tagsInput != null && !tagsInput.trim().isEmpty()) {
            String[] tagsArray = tagsInput.split(",");
            for (int i = 0; i < tagsArray.length; i++) {
                tagsArray[i] = tagsArray[i].trim();
            }
            example.setTags(tagsArray);
        } else {
            example.setTags(null);
        }

        if (bindingResult.hasErrors()) {
            log.warn("Validation errors creating code example: {}", bindingResult.getAllErrors());
            // Re-populate form data
            var projects = springProjectRepository.findAll(Sort.by(Sort.Direction.ASC, "name"));
            model.addAttribute("projects", projects);
            model.addAttribute("pageTitle", "Create New Code Example");
            model.addAttribute("activePage", "examples");
            return "examples/form";
        }

        try {
            CodeExample savedExample = codeExampleRepository.save(example);
            log.info("Code example created successfully with ID: {}", savedExample.getId());
            redirectAttributes.addFlashAttribute("success", "Code example created successfully");
            return "redirect:/examples/" + savedExample.getId();
        } catch (Exception e) {
            log.error("Error creating code example", e);
            model.addAttribute("error", "Failed to create code example: " + e.getMessage());
            var projects = springProjectRepository.findAll(Sort.by(Sort.Direction.ASC, "name"));
            model.addAttribute("projects", projects);
            model.addAttribute("pageTitle", "Create New Code Example");
            model.addAttribute("activePage", "examples");
            return "examples/form";
        }
    }

    /**
     * Show form to edit an existing code example.
     *
     * @param id the example ID
     * @param model Spring MVC model to add attributes for the view
     * @param redirectAttributes for flash messages on redirect
     * @return view name "examples/form" or redirect to list
     */
    @GetMapping("/{id}/edit")
    @PreAuthorize("hasRole('ADMIN')")
    public String showEditForm(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        log.debug("Showing edit form for code example with ID: {}", id);

        return codeExampleRepository.findById(id)
            .map(example -> {
                model.addAttribute("example", example);
                var projects = springProjectRepository.findAll(Sort.by(Sort.Direction.ASC, "name"));
                model.addAttribute("projects", projects);
                model.addAttribute("pageTitle", "Edit " + example.getTitle());
                model.addAttribute("activePage", "examples");
                return "examples/form";
            })
            .orElseGet(() -> {
                log.warn("Code example not found with ID: {}", id);
                redirectAttributes.addFlashAttribute("error", "Code example not found");
                return "redirect:/examples";
            });
    }

    /**
     * Update an existing code example.
     *
     * @param id the example ID
     * @param example the updated example data
     * @param bindingResult validation result
     * @param versionId the version ID to associate with
     * @param redirectAttributes for flash messages
     * @param model Spring MVC model
     * @return redirect to example detail or form on error
     */
    @PostMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public String updateExample(
            @PathVariable Long id,
            @Valid @ModelAttribute("example") CodeExample example,
            BindingResult bindingResult,
            @RequestParam("version.id") Long versionId,
            @RequestParam(value = "tagsInput", required = false) String tagsInput,
            RedirectAttributes redirectAttributes,
            Model model) {

        log.debug("Updating code example with ID: {}", id);

        // Set the version
        projectVersionRepository.findById(versionId).ifPresentOrElse(
            example::setVersion,
            () -> bindingResult.rejectValue("version", "invalid", "Invalid version selected")
        );

        // Convert tags input to array
        if (tagsInput != null && !tagsInput.trim().isEmpty()) {
            String[] tagsArray = tagsInput.split(",");
            for (int i = 0; i < tagsArray.length; i++) {
                tagsArray[i] = tagsArray[i].trim();
            }
            example.setTags(tagsArray);
        } else {
            example.setTags(null);
        }

        if (bindingResult.hasErrors()) {
            log.warn("Validation errors updating code example: {}", bindingResult.getAllErrors());
            // Re-populate form data
            model.addAttribute("example", example);
            var projects = springProjectRepository.findAll(Sort.by(Sort.Direction.ASC, "name"));
            model.addAttribute("projects", projects);
            model.addAttribute("pageTitle", "Edit Code Example");
            model.addAttribute("activePage", "examples");
            return "examples/form";
        }

        return codeExampleRepository.findById(id)
            .map(existingExample -> {
                // Update fields
                existingExample.setTitle(example.getTitle());
                existingExample.setDescription(example.getDescription());
                existingExample.setCodeSnippet(example.getCodeSnippet());
                existingExample.setLanguage(example.getLanguage());
                existingExample.setCategory(example.getCategory());
                existingExample.setTags(example.getTags());
                existingExample.setSourceUrl(example.getSourceUrl());
                existingExample.setVersion(example.getVersion());

                try {
                    CodeExample updatedExample = codeExampleRepository.save(existingExample);
                    log.info("Code example updated successfully with ID: {}", updatedExample.getId());
                    redirectAttributes.addFlashAttribute("success", "Code example updated successfully");
                    return "redirect:/examples/" + updatedExample.getId();
                } catch (Exception e) {
                    log.error("Error updating code example", e);
                    model.addAttribute("error", "Failed to update code example: " + e.getMessage());
                    model.addAttribute("example", existingExample);
                    var projects = springProjectRepository.findAll(Sort.by(Sort.Direction.ASC, "name"));
                    model.addAttribute("projects", projects);
                    model.addAttribute("pageTitle", "Edit Code Example");
                    model.addAttribute("activePage", "examples");
                    return "examples/form";
                }
            })
            .orElseGet(() -> {
                log.warn("Code example not found with ID: {}", id);
                redirectAttributes.addFlashAttribute("error", "Code example not found");
                return "redirect:/examples";
            });
    }

    /**
     * Delete a code example.
     *
     * @param id the example ID
     * @param redirectAttributes for flash messages
     * @return redirect to examples list
     */
    @PostMapping("/{id}/delete")
    @PreAuthorize("hasRole('ADMIN')")
    public String deleteExample(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        log.debug("Deleting code example with ID: {}", id);

        return codeExampleRepository.findById(id)
            .map(example -> {
                try {
                    String exampleTitle = example.getTitle();
                    codeExampleRepository.delete(example);
                    log.info("Code example deleted successfully: {}", exampleTitle);
                    redirectAttributes.addFlashAttribute("success",
                        "Code example '" + exampleTitle + "' deleted successfully");
                } catch (Exception e) {
                    log.error("Error deleting code example with ID: {}", id, e);
                    redirectAttributes.addFlashAttribute("error",
                        "Failed to delete code example: " + e.getMessage());
                }
                return "redirect:/examples";
            })
            .orElseGet(() -> {
                log.warn("Code example not found with ID: {}", id);
                redirectAttributes.addFlashAttribute("error", "Code example not found");
                return "redirect:/examples";
            });
    }
}
