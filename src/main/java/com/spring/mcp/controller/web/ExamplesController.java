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

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

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
     * Pattern to extract topic from example title.
     * Matches titles like "Messaging with RabbitMQ - Example 1"
     */
    private static final Pattern EXAMPLE_TITLE_PATTERN = Pattern.compile("^(.+?)\\s*-\\s*Example\\s*\\d+$");

    /**
     * Pattern to extract example number from title for natural sorting.
     * Matches "Example 1", "Example 10", etc.
     */
    private static final Pattern EXAMPLE_NUMBER_PATTERN = Pattern.compile("Example\\s*(\\d+)$");

    /**
     * DTO for grouped examples by topic.
     */
    public record TopicGroup(
        String topic,
        List<CodeExample> examples,
        String projectName,
        String version
    ) {}

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

            // Group examples by topic
            List<TopicGroup> topicGroups = groupExamplesByTopic(examples);
            log.debug("Grouped into {} topics", topicGroups.size());

            // Prepare filter data for dropdowns
            var allProjects = springProjectRepository.findAll(Sort.by(Sort.Direction.ASC, "name")).stream()
                .filter(SpringProject::getActive)
                .map(SpringProject::getSlug)
                .distinct()
                .toList();

            var allCategories = codeExampleRepository.findDistinctCategories();

            // Add attributes to model
            model.addAttribute("topicGroups", topicGroups);
            model.addAttribute("allProjects", allProjects);
            model.addAttribute("allCategories", allCategories);
            model.addAttribute("activePage", "examples");
            model.addAttribute("pageTitle", "Code Examples");
            model.addAttribute("totalElements", examples.size());
            model.addAttribute("totalTopics", topicGroups.size());

            // Preserve filter values
            model.addAttribute("selectedProject", projectSlug != null ? projectSlug : "");
            model.addAttribute("selectedCategory", category != null ? category : "");
            model.addAttribute("searchQuery", search != null ? search : "");

            return "examples/list";

        } catch (Exception e) {
            log.error("Error listing code examples", e);
            model.addAttribute("error", "Error loading code examples: " + e.getMessage());
            model.addAttribute("topicGroups", java.util.List.of());
            model.addAttribute("allProjects", java.util.List.of());
            model.addAttribute("allCategories", java.util.List.of());
            model.addAttribute("activePage", "examples");
            model.addAttribute("pageTitle", "Code Examples");
            model.addAttribute("totalElements", 0);
            model.addAttribute("totalTopics", 0);
            return "examples/list";
        }
    }

    /**
     * Group examples by topic extracted from their titles.
     * Titles like "Messaging with RabbitMQ - Example 1" become group "Messaging with RabbitMQ".
     */
    private List<TopicGroup> groupExamplesByTopic(List<CodeExample> examples) {
        // Group by topic
        Map<String, List<CodeExample>> grouped = new LinkedHashMap<>();

        for (CodeExample example : examples) {
            String topic = extractTopic(example.getTitle());
            grouped.computeIfAbsent(topic, k -> new ArrayList<>()).add(example);
        }

        // Convert to TopicGroup list
        return grouped.entrySet().stream()
            .map(entry -> {
                List<CodeExample> topicExamples = entry.getValue();
                // Sort examples by example number (natural/numeric sorting)
                topicExamples.sort(this::compareExamplesByNumber);

                // Get project/version from first example
                String projectName = null;
                String version = null;
                if (!topicExamples.isEmpty() && topicExamples.get(0).getVersion() != null) {
                    var v = topicExamples.get(0).getVersion();
                    if (v.getProject() != null) {
                        projectName = v.getProject().getName();
                    }
                    version = v.getVersion();
                }

                return new TopicGroup(entry.getKey(), topicExamples, projectName, version);
            })
            .sorted(Comparator.comparing(TopicGroup::topic))
            .collect(Collectors.toList());
    }

    /**
     * Extract topic from example title by removing " - Example N" suffix.
     */
    private String extractTopic(String title) {
        if (title == null) {
            return "Unknown";
        }

        Matcher matcher = EXAMPLE_TITLE_PATTERN.matcher(title);
        if (matcher.matches()) {
            return matcher.group(1).trim();
        }

        // If no pattern match, use the whole title
        return title;
    }

    /**
     * Compare two code examples by their example number for natural sorting.
     * Extracts the numeric part from titles like "Topic - Example 10" and sorts numerically.
     * Falls back to alphabetical comparison if no number is found.
     */
    private int compareExamplesByNumber(CodeExample e1, CodeExample e2) {
        int num1 = extractExampleNumber(e1.getTitle());
        int num2 = extractExampleNumber(e2.getTitle());

        // If both have numbers, compare numerically
        if (num1 >= 0 && num2 >= 0) {
            return Integer.compare(num1, num2);
        }

        // If only one has a number, numbered examples come first
        if (num1 >= 0) return -1;
        if (num2 >= 0) return 1;

        // Neither has a number, fall back to alphabetical
        String title1 = e1.getTitle() != null ? e1.getTitle() : "";
        String title2 = e2.getTitle() != null ? e2.getTitle() : "";
        return title1.compareTo(title2);
    }

    /**
     * Extract the example number from a title.
     * Returns -1 if no number is found.
     */
    private int extractExampleNumber(String title) {
        if (title == null) {
            return -1;
        }

        Matcher matcher = EXAMPLE_NUMBER_PATTERN.matcher(title);
        if (matcher.find()) {
            try {
                return Integer.parseInt(matcher.group(1));
            } catch (NumberFormatException e) {
                return -1;
            }
        }
        return -1;
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
