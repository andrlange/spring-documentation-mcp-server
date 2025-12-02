package com.spring.mcp.controller.web;

import com.spring.mcp.model.dto.flavor.CategoryStatsDto;
import com.spring.mcp.model.dto.flavor.FlavorDto;
import com.spring.mcp.model.enums.FlavorCategory;
import com.spring.mcp.service.FlavorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Controller for Flavors page.
 * Manages company guidelines, architecture patterns, compliance rules,
 * agent configurations, and project initialization templates.
 *
 * @author Spring MCP Server
 * @version 1.2.0
 * @since 2025-11-30
 */
@Controller
@RequestMapping("/flavors")
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "mcp.features.flavors.enabled", havingValue = "true", matchIfMissing = true)
public class FlavorsController {

    private final FlavorService flavorService;

    /**
     * Display flavors list page with filters.
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public String listFlavors(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) String search,
            Model model) {

        log.debug("Flavors page: category={}, active={}, search={}", category, active, search);

        model.addAttribute("activePage", "flavors");
        model.addAttribute("pageTitle", "Flavors");

        // Parse category filter
        FlavorCategory catFilter = null;
        if (category != null && !category.isBlank()) {
            catFilter = FlavorCategory.fromString(category);
        }

        // Get flavors based on filters
        List<FlavorDto> flavors;
        if (search != null && !search.isBlank()) {
            // Search mode
            flavors = flavorService.search(search, catFilter, null, 100).stream()
                .map(summary -> flavorService.findById(summary.getId()).orElse(null))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        } else if (catFilter != null) {
            flavors = flavorService.findByCategory(catFilter);
        } else {
            flavors = flavorService.findAll();
        }

        // Filter by active status if specified
        if (active != null) {
            final Boolean activeFilter = active;
            flavors = flavors.stream()
                .filter(f -> Objects.equals(f.getIsActive(), activeFilter))
                .collect(Collectors.toList());
        }

        model.addAttribute("flavors", flavors);
        model.addAttribute("categories", FlavorCategory.values());

        // Statistics
        CategoryStatsDto stats = flavorService.getStatistics();
        model.addAttribute("stats", stats);

        // Current filter values
        model.addAttribute("selectedCategory", category);
        model.addAttribute("selectedActive", active);
        model.addAttribute("searchTerm", search);

        return "flavors/list";
    }

    /**
     * Display new flavor form.
     */
    @GetMapping("/new")
    @PreAuthorize("hasRole('ADMIN')")
    public String newFlavorForm(
            @RequestParam(required = false) String category,
            Model model) {

        FlavorDto flavor = FlavorDto.builder()
            .isActive(false) // Default to inactive (draft)
            .tags(new ArrayList<>())
            .metadata(new HashMap<>())
            .build();

        // Pre-select category if provided
        if (category != null && !category.isBlank()) {
            FlavorCategory cat = FlavorCategory.fromString(category);
            if (cat != null) {
                flavor.setCategory(cat);
            }
        }

        model.addAttribute("activePage", "flavors");
        model.addAttribute("pageTitle", "New Flavor");
        model.addAttribute("flavor", flavor);
        model.addAttribute("categories", FlavorCategory.values());
        model.addAttribute("isNew", true);

        return "flavors/form";
    }

    /**
     * Display edit flavor form.
     */
    @GetMapping("/{id}/edit")
    @PreAuthorize("hasRole('ADMIN')")
    public String editFlavorForm(@PathVariable Long id, Model model) {

        FlavorDto flavor = flavorService.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Flavor not found: " + id));

        model.addAttribute("activePage", "flavors");
        model.addAttribute("pageTitle", "Edit Flavor");
        model.addAttribute("flavor", flavor);
        model.addAttribute("categories", FlavorCategory.values());
        model.addAttribute("isNew", false);

        return "flavors/form";
    }

    /**
     * View flavor details (read-only).
     */
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public String viewFlavor(@PathVariable Long id, Model model) {

        FlavorDto flavor = flavorService.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Flavor not found: " + id));

        model.addAttribute("activePage", "flavors");
        model.addAttribute("pageTitle", flavor.getDisplayName());
        model.addAttribute("flavor", flavor);

        return "flavors/view";
    }

    /**
     * Create new flavor.
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public String createFlavor(
            @ModelAttribute FlavorDto flavor,
            @RequestParam(required = false) String tagsInput,
            @AuthenticationPrincipal UserDetails user,
            RedirectAttributes redirectAttributes,
            Model model) {

        log.info("Creating new flavor: {} by user: {}", flavor.getUniqueName(), user.getUsername());

        try {
            // Parse tags from comma-separated input
            if (tagsInput != null && !tagsInput.isBlank()) {
                List<String> tags = Arrays.stream(tagsInput.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toList());
                flavor.setTags(tags);
            }

            FlavorDto created = flavorService.create(flavor, user.getUsername());
            redirectAttributes.addFlashAttribute("successMessage",
                "Flavor '" + created.getDisplayName() + "' created successfully!");
            return "redirect:/flavors";

        } catch (IllegalArgumentException e) {
            log.error("Error creating flavor", e);
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("flavor", flavor);
            model.addAttribute("categories", FlavorCategory.values());
            model.addAttribute("isNew", true);
            model.addAttribute("tagsInput", tagsInput);
            return "flavors/form";
        }
    }

    /**
     * Update existing flavor.
     */
    @PostMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public String updateFlavor(
            @PathVariable Long id,
            @ModelAttribute FlavorDto flavor,
            @RequestParam(required = false) String tagsInput,
            @AuthenticationPrincipal UserDetails user,
            RedirectAttributes redirectAttributes,
            Model model) {

        log.info("Updating flavor id: {} by user: {}", id, user.getUsername());

        try {
            // Parse tags from comma-separated input
            if (tagsInput != null && !tagsInput.isBlank()) {
                List<String> tags = Arrays.stream(tagsInput.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toList());
                flavor.setTags(tags);
            } else {
                flavor.setTags(new ArrayList<>());
            }

            FlavorDto updated = flavorService.update(id, flavor, user.getUsername());
            redirectAttributes.addFlashAttribute("successMessage",
                "Flavor '" + updated.getDisplayName() + "' updated successfully!");
            return "redirect:/flavors";

        } catch (IllegalArgumentException e) {
            log.error("Error updating flavor", e);
            model.addAttribute("errorMessage", e.getMessage());
            flavor.setId(id);
            model.addAttribute("flavor", flavor);
            model.addAttribute("categories", FlavorCategory.values());
            model.addAttribute("isNew", false);
            model.addAttribute("tagsInput", tagsInput);
            return "flavors/form";
        }
    }

    /**
     * Toggle flavor active status (AJAX).
     */
    @PostMapping("/{id}/toggle-active")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> toggleActive(
            @PathVariable Long id,
            @RequestParam boolean active,
            @AuthenticationPrincipal UserDetails user) {

        log.info("Toggling flavor id: {} to active: {} by user: {}", id, active, user.getUsername());

        try {
            flavorService.toggleActive(id, active, user.getUsername());
            return ResponseEntity.ok(Map.of("success", true, "active", active));
        } catch (Exception e) {
            log.error("Error toggling flavor active status", e);
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    /**
     * Delete flavor (AJAX).
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> deleteFlavor(@PathVariable Long id) {

        log.info("Deleting flavor id: {}", id);

        try {
            flavorService.delete(id);
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            log.error("Error deleting flavor", e);
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    /**
     * Import markdown file.
     * Parses YAML front matter header if present to extract metadata.
     */
    @PostMapping("/import")
    @PreAuthorize("hasRole('ADMIN')")
    public String importMarkdown(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal UserDetails user,
            RedirectAttributes redirectAttributes) {

        log.info("Importing markdown file by user: {}", user.getUsername());

        try {
            String content = new String(file.getBytes(), StandardCharsets.UTF_8);
            FlavorService.ImportResult result = flavorService.importFromMarkdown(content, user.getUsername());

            // Build success message
            StringBuilder successMsg = new StringBuilder();
            successMsg.append("Imported '").append(result.flavor().getDisplayName()).append("' successfully!");

            // Add warning if name was auto-renamed
            if (result.warningMessage() != null) {
                redirectAttributes.addFlashAttribute("warningMessage", result.warningMessage());
            }

            // Add info about missing category if needed
            if (result.flavor().getCategory() == null) {
                successMsg.append(" Please select a category.");
            } else {
                successMsg.append(" Review and activate when ready.");
            }

            redirectAttributes.addFlashAttribute("successMessage", successMsg.toString());
            return "redirect:/flavors/" + result.flavor().getId() + "/edit";

        } catch (IOException e) {
            log.error("Error reading imported file", e);
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to read file: " + e.getMessage());
            return "redirect:/flavors";
        }
    }

    /**
     * Export flavor as markdown.
     *
     * @param id the flavor ID
     * @param includeMetadata if true, includes YAML front matter header (default: true)
     */
    @GetMapping("/{id}/export")
    @PreAuthorize("isAuthenticated()")
    @ResponseBody
    public ResponseEntity<String> exportMarkdown(
            @PathVariable Long id,
            @RequestParam(defaultValue = "true") boolean includeMetadata) {

        log.info("Exporting flavor id: {} as markdown (includeMetadata: {})", id, includeMetadata);

        FlavorDto flavor = flavorService.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Flavor not found: " + id));

        String content = flavorService.exportToMarkdown(id, includeMetadata);

        return ResponseEntity.ok()
            .header("Content-Type", "text/markdown; charset=UTF-8")
            .header("Content-Disposition", "attachment; filename=\"" + flavor.getUniqueName() + ".md\"")
            .body(content);
    }

    /**
     * Check if unique name is available (AJAX).
     */
    @GetMapping("/check-name")
    @PreAuthorize("isAuthenticated()")
    @ResponseBody
    public ResponseEntity<Map<String, Boolean>> checkUniqueName(
            @RequestParam String name,
            @RequestParam(required = false) Long excludeId) {

        boolean available = flavorService.isUniqueNameAvailable(name, excludeId);
        return ResponseEntity.ok(Map.of("available", available));
    }

    /**
     * Get flavor content (AJAX for preview).
     */
    @GetMapping("/{id}/content")
    @PreAuthorize("isAuthenticated()")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getFlavorContent(@PathVariable Long id) {

        return flavorService.findById(id)
            .map(flavor -> {
                Map<String, Object> response = new HashMap<>();
                response.put("id", flavor.getId());
                response.put("uniqueName", flavor.getUniqueName());
                response.put("displayName", flavor.getDisplayName());
                response.put("category", flavor.getCategory().name());
                response.put("categoryDisplayName", flavor.getCategoryDisplayName());
                response.put("patternName", flavor.getPatternName());
                response.put("content", flavor.getContent());
                response.put("description", flavor.getDescription());
                response.put("tags", flavor.getTags());
                response.put("isActive", flavor.getIsActive());
                response.put("updatedAt", flavor.getUpdatedAt());
                return ResponseEntity.ok(response);
            })
            .orElse(ResponseEntity.notFound().build());
    }
}
