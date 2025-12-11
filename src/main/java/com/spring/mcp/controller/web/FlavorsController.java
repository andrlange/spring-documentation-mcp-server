package com.spring.mcp.controller.web;

import com.spring.mcp.model.dto.flavor.CategoryStatsDto;
import com.spring.mcp.model.dto.flavor.FlavorDto;
import com.spring.mcp.model.entity.FlavorGroup;
import com.spring.mcp.model.entity.User;
import com.spring.mcp.model.enums.FlavorCategory;
import com.spring.mcp.model.enums.UserRole;
import com.spring.mcp.repository.UserRepository;
import com.spring.mcp.service.FlavorGroupService;
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
    private final FlavorGroupService flavorGroupService;
    private final UserRepository userRepository;

    /**
     * Display flavors list page with filters.
     * Shows groups at the top (public first, then private), then ungrouped flavors.
     * Admins see all groups; regular users only see public groups + groups they're members of.
     * When a specific group is selected via filter, shows only that group's flavors.
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public String listFlavors(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long groupId,
            @AuthenticationPrincipal UserDetails userDetails,
            Model model) {

        log.debug("Flavors page: category={}, active={}, search={}, groupId={}", category, active, search, groupId);

        model.addAttribute("activePage", "flavors");
        model.addAttribute("pageTitle", "Flavors");

        // Get current user and check if admin
        User currentUser = userRepository.findByUsername(userDetails.getUsername()).orElse(null);
        boolean isAdmin = currentUser != null && currentUser.getRole() == UserRole.ADMIN;

        // Get accessible groups for the current user
        List<FlavorGroup> accessibleGroups;
        if (isAdmin) {
            // Admins see all active groups
            accessibleGroups = flavorGroupService.findAllActiveGroups();
        } else {
            // Regular users see public groups + groups they're members of
            Long userId = currentUser != null ? currentUser.getId() : null;
            accessibleGroups = flavorGroupService.findAccessibleGroupsForUser(userId);
        }

        // Sort groups: public first, then private, both alphabetically by display name
        List<FlavorGroup> sortedGroups = accessibleGroups.stream()
            .sorted(Comparator
                .comparing(FlavorGroup::isPublic).reversed() // public (true) first
                .thenComparing(FlavorGroup::getDisplayName, String.CASE_INSENSITIVE_ORDER))
            .collect(Collectors.toList());

        // Pass accessible groups for the dropdown filter
        model.addAttribute("availableGroups", sortedGroups);

        // Parse category filter
        FlavorCategory catFilter = null;
        if (category != null && !category.isBlank()) {
            catFilter = FlavorCategory.fromString(category);
        }

        // Check if filtering by specific group
        if (groupId != null) {
            // Filter by specific group - show only flavors from this group
            FlavorGroup selectedGroup = sortedGroups.stream()
                .filter(g -> g.getId().equals(groupId))
                .findFirst()
                .orElse(null);

            if (selectedGroup != null) {
                // Get flavors from the selected group using service method (avoids lazy loading issues)
                List<FlavorDto> groupFlavors = flavorGroupService.getFlavorsInGroupById(groupId).stream()
                    .map(f -> flavorService.findById(f.getId()).orElse(null))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

                // Apply additional filters (category, active, search)
                if (catFilter != null) {
                    final FlavorCategory finalCatFilter = catFilter;
                    groupFlavors = groupFlavors.stream()
                        .filter(f -> f.getCategory() == finalCatFilter)
                        .collect(Collectors.toList());
                }
                if (active != null) {
                    final Boolean activeFilter = active;
                    groupFlavors = groupFlavors.stream()
                        .filter(f -> Objects.equals(f.getIsActive(), activeFilter))
                        .collect(Collectors.toList());
                }
                if (search != null && !search.isBlank()) {
                    final String searchLower = search.toLowerCase();
                    groupFlavors = groupFlavors.stream()
                        .filter(f -> f.getDisplayName().toLowerCase().contains(searchLower) ||
                                     f.getUniqueName().toLowerCase().contains(searchLower) ||
                                     (f.getDescription() != null && f.getDescription().toLowerCase().contains(searchLower)))
                        .collect(Collectors.toList());
                }

                // When filtering by group, don't show expandable groups section
                model.addAttribute("groups", List.of());
                model.addAttribute("flavors", groupFlavors);
                model.addAttribute("selectedGroupName", selectedGroup.getDisplayName());
            } else {
                // Group not accessible or not found
                model.addAttribute("groups", List.of());
                model.addAttribute("flavors", List.of());
            }
        } else {
            // Normal view - show expandable groups and ungrouped flavors

            // Get IDs of flavors that are in groups the user can access (to exclude from ungrouped list)
            Set<Long> groupedFlavorIds = sortedGroups.stream()
                .flatMap(g -> g.getGroupFlavors().stream())
                .map(gf -> gf.getFlavor().getId())
                .collect(Collectors.toSet());

            // SECURITY FIX: Get the set of flavor IDs accessible to this user
            // This includes: unassigned flavors + public group flavors + member private group flavors
            // Flavors in private groups where the user is NOT a member must be hidden
            Long userId = currentUser != null ? currentUser.getId() : null;
            Set<Long> accessibleFlavorIds = flavorGroupService.getAccessibleFlavorIdsForUser(userId);

            // Get flavors based on filters
            List<FlavorDto> allFlavors;
            if (search != null && !search.isBlank()) {
                // Search mode
                allFlavors = flavorService.search(search, catFilter, null, 100).stream()
                    .map(summary -> flavorService.findById(summary.getId()).orElse(null))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            } else if (catFilter != null) {
                allFlavors = flavorService.findByCategory(catFilter);
            } else {
                allFlavors = flavorService.findAll();
            }

            // SECURITY FIX: Filter to only include accessible flavors (unless admin)
            if (!isAdmin) {
                allFlavors = allFlavors.stream()
                    .filter(f -> accessibleFlavorIds.contains(f.getId()))
                    .collect(Collectors.toList());
            }

            // Filter by active status if specified
            if (active != null) {
                final Boolean activeFilter = active;
                allFlavors = allFlavors.stream()
                    .filter(f -> Objects.equals(f.getIsActive(), activeFilter))
                    .collect(Collectors.toList());
            }

            // Get IDs of all matching flavors for filtering groups
            Set<Long> matchingFlavorIds = allFlavors.stream()
                .map(FlavorDto::getId)
                .collect(Collectors.toSet());

            // Filter groups to only include those with matching flavors (when search/filters applied)
            List<FlavorGroup> filteredGroups;
            Map<Long, Integer> groupFilteredCounts = new HashMap<>();
            if (search != null && !search.isBlank() || catFilter != null || active != null) {
                // When filters are active, filter groups to show only those with matching flavors
                filteredGroups = sortedGroups.stream()
                    .filter(g -> g.getGroupFlavors().stream()
                        .anyMatch(gf -> matchingFlavorIds.contains(gf.getFlavor().getId())))
                    .collect(Collectors.toList());

                // Calculate filtered counts for each group
                for (FlavorGroup group : filteredGroups) {
                    int filteredCount = (int) group.getGroupFlavors().stream()
                        .filter(gf -> matchingFlavorIds.contains(gf.getFlavor().getId()))
                        .count();
                    groupFilteredCounts.put(group.getId(), filteredCount);
                }

                // Also filter the flavors within each group by creating filtered view DTOs
                model.addAttribute("filteredFlavorIds", matchingFlavorIds);
                model.addAttribute("groupFilteredCounts", groupFilteredCounts);
            } else {
                filteredGroups = sortedGroups;
                model.addAttribute("filteredFlavorIds", null);
                model.addAttribute("groupFilteredCounts", null);
            }

            // Separate ungrouped flavors (not in any accessible group)
            List<FlavorDto> ungroupedFlavors = allFlavors.stream()
                .filter(f -> !groupedFlavorIds.contains(f.getId()))
                .collect(Collectors.toList());

            model.addAttribute("groups", filteredGroups);
            model.addAttribute("flavors", ungroupedFlavors); // Now only ungrouped flavors
        }

        model.addAttribute("categories", FlavorCategory.values());

        // Statistics
        CategoryStatsDto stats = flavorService.getStatistics();
        model.addAttribute("stats", stats);

        // Group statistics
        FlavorGroupService.GroupStatistics groupStats = flavorGroupService.getGroupStatistics();
        model.addAttribute("groupStats", groupStats);

        // Current filter values
        model.addAttribute("selectedCategory", category);
        model.addAttribute("selectedActive", active);
        model.addAttribute("searchTerm", search);
        model.addAttribute("selectedGroupId", groupId);

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

        // Available groups for assignment
        List<FlavorGroup> availableGroups = flavorGroupService.findAllActiveGroups().stream()
            .sorted(Comparator
                .comparing(FlavorGroup::isPublic).reversed()
                .thenComparing(FlavorGroup::getDisplayName, String.CASE_INSENSITIVE_ORDER))
            .collect(Collectors.toList());
        model.addAttribute("availableGroups", availableGroups);
        model.addAttribute("selectedGroupIds", List.of());

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

        // Available groups for assignment
        List<FlavorGroup> availableGroups = flavorGroupService.findAllActiveGroups().stream()
            .sorted(Comparator
                .comparing(FlavorGroup::isPublic).reversed()
                .thenComparing(FlavorGroup::getDisplayName, String.CASE_INSENSITIVE_ORDER))
            .collect(Collectors.toList());
        model.addAttribute("availableGroups", availableGroups);

        // Get IDs of groups this flavor is currently in
        List<Long> selectedGroupIds = availableGroups.stream()
            .filter(g -> g.getGroupFlavors().stream()
                .anyMatch(gf -> gf.getFlavor().getId().equals(id)))
            .map(FlavorGroup::getId)
            .collect(Collectors.toList());
        model.addAttribute("selectedGroupIds", selectedGroupIds);

        return "flavors/form";
    }

    /**
     * View flavor details (read-only).
     * SECURITY: Non-admin users can only view flavors they have access to
     * (unassigned, public group, or member of private group).
     */
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public String viewFlavor(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails,
            Model model) {

        FlavorDto flavor = flavorService.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Flavor not found: " + id));

        // SECURITY CHECK: Verify user has access to this flavor
        User currentUser = userRepository.findByUsername(userDetails.getUsername()).orElse(null);
        boolean isAdmin = currentUser != null && currentUser.getRole() == UserRole.ADMIN;

        if (!isAdmin) {
            Long userId = currentUser != null ? currentUser.getId() : null;
            Set<Long> accessibleFlavorIds = flavorGroupService.getAccessibleFlavorIdsForUser(userId);
            if (!accessibleFlavorIds.contains(id)) {
                throw new IllegalArgumentException("Flavor not found: " + id); // Don't reveal it exists
            }
        }

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
            @RequestParam(required = false) List<Long> groupIds,
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

            // Add to selected groups
            if (groupIds != null && !groupIds.isEmpty()) {
                for (Long groupId : groupIds) {
                    try {
                        flavorGroupService.addFlavorToGroup(groupId, created.getId(), user.getUsername());
                    } catch (Exception e) {
                        log.warn("Failed to add flavor {} to group {}: {}", created.getId(), groupId, e.getMessage());
                    }
                }
            }

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

            // Re-populate groups for form
            List<FlavorGroup> availableGroups = flavorGroupService.findAllActiveGroups().stream()
                .sorted(Comparator
                    .comparing(FlavorGroup::isPublic).reversed()
                    .thenComparing(FlavorGroup::getDisplayName, String.CASE_INSENSITIVE_ORDER))
                .collect(Collectors.toList());
            model.addAttribute("availableGroups", availableGroups);
            model.addAttribute("selectedGroupIds", groupIds != null ? groupIds : List.of());

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
            @RequestParam(required = false) List<Long> groupIds,
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

            // Update group assignments
            updateFlavorGroups(id, groupIds, user.getUsername());

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

            // Re-populate groups for form
            List<FlavorGroup> availableGroups = flavorGroupService.findAllActiveGroups().stream()
                .sorted(Comparator
                    .comparing(FlavorGroup::isPublic).reversed()
                    .thenComparing(FlavorGroup::getDisplayName, String.CASE_INSENSITIVE_ORDER))
                .collect(Collectors.toList());
            model.addAttribute("availableGroups", availableGroups);
            model.addAttribute("selectedGroupIds", groupIds != null ? groupIds : List.of());

            return "flavors/form";
        }
    }

    /**
     * Helper method to update flavor group assignments.
     */
    private void updateFlavorGroups(Long flavorId, List<Long> newGroupIds, String updatedBy) {
        // Get all active groups
        List<FlavorGroup> allGroups = flavorGroupService.findAllActiveGroups();

        // Find current group IDs for this flavor
        List<Long> currentGroupIds = allGroups.stream()
            .filter(g -> g.getGroupFlavors().stream()
                .anyMatch(gf -> gf.getFlavor().getId().equals(flavorId)))
            .map(FlavorGroup::getId)
            .collect(Collectors.toList());

        // Remove from groups not in new list
        for (Long currentGroupId : currentGroupIds) {
            if (newGroupIds == null || !newGroupIds.contains(currentGroupId)) {
                try {
                    flavorGroupService.removeFlavorFromGroup(currentGroupId, flavorId);
                } catch (Exception e) {
                    log.warn("Failed to remove flavor {} from group {}: {}", flavorId, currentGroupId, e.getMessage());
                }
            }
        }

        // Add to new groups
        if (newGroupIds != null) {
            for (Long newGroupId : newGroupIds) {
                if (!currentGroupIds.contains(newGroupId)) {
                    try {
                        flavorGroupService.addFlavorToGroup(newGroupId, flavorId, updatedBy);
                    } catch (Exception e) {
                        log.warn("Failed to add flavor {} to group {}: {}", flavorId, newGroupId, e.getMessage());
                    }
                }
            }
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
     * SECURITY: Non-admin users can only export flavors they have access to.
     *
     * @param id the flavor ID
     * @param includeMetadata if true, includes YAML front matter header (default: true)
     */
    @GetMapping("/{id}/export")
    @PreAuthorize("isAuthenticated()")
    @ResponseBody
    public ResponseEntity<String> exportMarkdown(
            @PathVariable Long id,
            @RequestParam(defaultValue = "true") boolean includeMetadata,
            @AuthenticationPrincipal UserDetails userDetails) {

        log.info("Exporting flavor id: {} as markdown (includeMetadata: {})", id, includeMetadata);

        // SECURITY CHECK: Verify user has access to this flavor
        User currentUser = userRepository.findByUsername(userDetails.getUsername()).orElse(null);
        boolean isAdmin = currentUser != null && currentUser.getRole() == UserRole.ADMIN;

        if (!isAdmin) {
            Long userId = currentUser != null ? currentUser.getId() : null;
            Set<Long> accessibleFlavorIds = flavorGroupService.getAccessibleFlavorIdsForUser(userId);
            if (!accessibleFlavorIds.contains(id)) {
                throw new IllegalArgumentException("Flavor not found: " + id);
            }
        }

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
     * SECURITY: Non-admin users can only access flavors they have access to.
     */
    @GetMapping("/{id}/content")
    @PreAuthorize("isAuthenticated()")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getFlavorContent(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {

        // SECURITY CHECK: Verify user has access to this flavor
        User currentUser = userRepository.findByUsername(userDetails.getUsername()).orElse(null);
        boolean isAdmin = currentUser != null && currentUser.getRole() == UserRole.ADMIN;

        if (!isAdmin) {
            Long userId = currentUser != null ? currentUser.getId() : null;
            Set<Long> accessibleFlavorIds = flavorGroupService.getAccessibleFlavorIdsForUser(userId);
            if (!accessibleFlavorIds.contains(id)) {
                return ResponseEntity.notFound().build();
            }
        }

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
