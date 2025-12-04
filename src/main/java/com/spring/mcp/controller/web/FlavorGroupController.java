package com.spring.mcp.controller.web;

import com.spring.mcp.model.entity.FlavorGroup;
import com.spring.mcp.repository.ApiKeyRepository;
import com.spring.mcp.repository.FlavorRepository;
import com.spring.mcp.repository.UserRepository;
import com.spring.mcp.service.FlavorGroupService;
import com.spring.mcp.service.FlavorGroupService.GroupStatistics;
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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Map;

/**
 * Controller for Flavor Groups management.
 * Handles CRUD operations for flavor groups, membership management,
 * and flavor assignments.
 *
 * @author Spring MCP Server
 * @version 1.3.3
 * @since 2025-12-04
 */
@Controller
@RequestMapping("/groups")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('ADMIN')")
@ConditionalOnProperty(name = "mcp.features.flavors.enabled", havingValue = "true", matchIfMissing = true)
public class FlavorGroupController {

    private final FlavorGroupService groupService;
    private final UserRepository userRepository;
    private final ApiKeyRepository apiKeyRepository;
    private final FlavorRepository flavorRepository;

    /**
     * List all flavor groups with statistics and filtering.
     */
    @GetMapping
    public String listGroups(
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) Boolean isPublic,
            @RequestParam(required = false) String search,
            Model model) {

        log.debug("Listing groups: active={}, isPublic={}, search={}", active, isPublic, search);

        model.addAttribute("activePage", "groups");
        model.addAttribute("pageTitle", "Flavor Groups");

        // Get all groups (admin sees all)
        List<FlavorGroup> groups = groupService.findAllGroups();

        // Filter by active status
        if (active != null) {
            groups = groups.stream()
                    .filter(g -> active.equals(g.getIsActive()))
                    .toList();
        }

        // Filter by visibility (public/private)
        if (isPublic != null) {
            groups = groups.stream()
                    .filter(g -> isPublic.equals(g.isPublic()))
                    .toList();
        }

        // Filter by search term
        if (search != null && !search.isBlank()) {
            String searchLower = search.toLowerCase();
            groups = groups.stream()
                    .filter(g -> g.getDisplayName().toLowerCase().contains(searchLower) ||
                            g.getUniqueName().toLowerCase().contains(searchLower) ||
                            (g.getDescription() != null && g.getDescription().toLowerCase().contains(searchLower)))
                    .toList();
        }

        model.addAttribute("groups", groups);

        // Statistics
        GroupStatistics stats = groupService.getGroupStatistics();
        model.addAttribute("stats", stats);

        // Current filter values
        model.addAttribute("selectedActive", active);
        model.addAttribute("selectedPublic", isPublic);
        model.addAttribute("searchTerm", search);

        return "groups/list";
    }

    /**
     * Display new group form.
     */
    @GetMapping("/new")
    public String newGroupForm(Model model) {

        FlavorGroup group = FlavorGroup.builder()
                .isActive(true)
                .build();

        model.addAttribute("activePage", "groups");
        model.addAttribute("pageTitle", "New Flavor Group");
        model.addAttribute("group", group);
        model.addAttribute("isNew", true);

        // Available users, API keys, and flavors for selection
        model.addAttribute("availableUsers", userRepository.findByEnabledTrue());
        model.addAttribute("availableApiKeys", apiKeyRepository.findByIsActiveTrue());
        model.addAttribute("availableFlavors", flavorRepository.findByIsActiveTrue());

        // Currently selected (empty for new)
        model.addAttribute("selectedUserIds", List.of());
        model.addAttribute("selectedApiKeyIds", List.of());
        model.addAttribute("selectedFlavorIds", List.of());

        return "groups/form";
    }

    /**
     * Display edit group form.
     */
    @GetMapping("/{id}/edit")
    public String editGroupForm(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {

        return groupService.findById(id)
                .map(group -> {
                    model.addAttribute("activePage", "groups");
                    model.addAttribute("pageTitle", "Edit " + group.getDisplayName());
                    model.addAttribute("group", group);
                    model.addAttribute("isNew", false);

                    // Available users, API keys, and flavors
                    model.addAttribute("availableUsers", userRepository.findByEnabledTrue());
                    model.addAttribute("availableApiKeys", apiKeyRepository.findByIsActiveTrue());
                    model.addAttribute("availableFlavors", flavorRepository.findByIsActiveTrue());

                    // Currently selected members and flavors
                    List<Long> selectedUserIds = group.getUserMembers().stream()
                            .map(m -> m.getUser().getId())
                            .toList();
                    List<Long> selectedApiKeyIds = group.getApiKeyMembers().stream()
                            .map(m -> m.getApiKey().getId())
                            .toList();
                    List<Long> selectedFlavorIds = group.getGroupFlavors().stream()
                            .map(gf -> gf.getFlavor().getId())
                            .toList();

                    model.addAttribute("selectedUserIds", selectedUserIds);
                    model.addAttribute("selectedApiKeyIds", selectedApiKeyIds);
                    model.addAttribute("selectedFlavorIds", selectedFlavorIds);

                    return "groups/form";
                })
                .orElseGet(() -> {
                    redirectAttributes.addFlashAttribute("errorMessage", "Group not found");
                    return "redirect:/groups";
                });
    }

    /**
     * View group details (read-only).
     */
    @GetMapping("/{id}")
    public String viewGroup(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {

        return groupService.findById(id)
                .map(group -> {
                    model.addAttribute("activePage", "groups");
                    model.addAttribute("pageTitle", group.getDisplayName());
                    model.addAttribute("group", group);
                    return "groups/view";
                })
                .orElseGet(() -> {
                    redirectAttributes.addFlashAttribute("errorMessage", "Group not found");
                    return "redirect:/groups";
                });
    }

    /**
     * Create new group.
     */
    @PostMapping
    public String createGroup(
            @RequestParam String uniqueName,
            @RequestParam String displayName,
            @RequestParam(required = false) String description,
            @RequestParam(defaultValue = "true") Boolean isActive,
            @RequestParam(required = false) List<Long> userIds,
            @RequestParam(required = false) List<Long> apiKeyIds,
            @RequestParam(required = false) List<Long> flavorIds,
            @AuthenticationPrincipal UserDetails user,
            RedirectAttributes redirectAttributes,
            Model model) {

        log.info("Creating new group: {} by user: {}", uniqueName, user.getUsername());

        try {
            // Validate unique name
            if (uniqueName == null || uniqueName.isBlank() || uniqueName.length() < 3) {
                throw new IllegalArgumentException("Unique name must be at least 3 characters");
            }

            // Normalize unique name (lowercase, dashes instead of spaces)
            String normalizedUniqueName = uniqueName.toLowerCase()
                    .replaceAll("\\s+", "-")
                    .replaceAll("[^a-z0-9-]", "");

            FlavorGroup group = FlavorGroup.builder()
                    .uniqueName(normalizedUniqueName)
                    .displayName(displayName)
                    .description(description)
                    .isActive(isActive)
                    .createdBy(user.getUsername())
                    .updatedBy(user.getUsername())
                    .build();

            FlavorGroup created = groupService.createGroup(group);

            // Add members
            if (userIds != null) {
                for (Long userId : userIds) {
                    groupService.addUserToGroup(created.getId(), userId, user.getUsername());
                }
            }
            if (apiKeyIds != null) {
                for (Long apiKeyId : apiKeyIds) {
                    groupService.addApiKeyToGroup(created.getId(), apiKeyId, user.getUsername());
                }
            }

            // Add flavors
            if (flavorIds != null) {
                for (Long flavorId : flavorIds) {
                    groupService.addFlavorToGroup(created.getId(), flavorId, user.getUsername());
                }
            }

            redirectAttributes.addFlashAttribute("successMessage",
                    "Group '" + created.getDisplayName() + "' created successfully!");
            return "redirect:/groups";

        } catch (IllegalArgumentException e) {
            log.error("Error creating group", e);
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/groups/new";
        }
    }

    /**
     * Update existing group.
     */
    @PostMapping("/{id}")
    public String updateGroup(
            @PathVariable Long id,
            @RequestParam String displayName,
            @RequestParam(required = false) String description,
            @RequestParam(defaultValue = "true") Boolean isActive,
            @RequestParam(required = false) List<Long> userIds,
            @RequestParam(required = false) List<Long> apiKeyIds,
            @RequestParam(required = false) List<Long> flavorIds,
            @AuthenticationPrincipal UserDetails user,
            RedirectAttributes redirectAttributes) {

        log.info("Updating group id: {} by user: {}", id, user.getUsername());

        try {
            FlavorGroup updates = FlavorGroup.builder()
                    .displayName(displayName)
                    .description(description)
                    .isActive(isActive)
                    .updatedBy(user.getUsername())
                    .build();

            FlavorGroup updated = groupService.updateGroup(id, updates);

            // Update user members
            updateGroupMembers(id, userIds, user.getUsername(), "user");

            // Update API key members
            updateGroupMembers(id, apiKeyIds, user.getUsername(), "apikey");

            // Update flavors
            updateGroupFlavors(id, flavorIds, user.getUsername());

            redirectAttributes.addFlashAttribute("successMessage",
                    "Group '" + updated.getDisplayName() + "' updated successfully!");
            return "redirect:/groups";

        } catch (Exception e) {
            log.error("Error updating group", e);
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/groups/" + id + "/edit";
        }
    }

    /**
     * Toggle group active status (AJAX).
     */
    @PostMapping("/{id}/toggle-active")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> toggleActive(
            @PathVariable Long id,
            @RequestParam boolean active,
            @AuthenticationPrincipal UserDetails user) {

        log.info("Toggling group id: {} to active: {} by user: {}", id, active, user.getUsername());

        try {
            FlavorGroup updates = FlavorGroup.builder()
                    .isActive(active)
                    .updatedBy(user.getUsername())
                    .build();

            groupService.updateGroup(id, updates);
            return ResponseEntity.ok(Map.of("success", true, "active", active));
        } catch (Exception e) {
            log.error("Error toggling group active status", e);
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    /**
     * Delete group (AJAX).
     */
    @DeleteMapping("/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> deleteGroup(@PathVariable Long id) {

        log.info("Deleting group id: {}", id);

        try {
            groupService.deleteGroup(id);
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            log.error("Error deleting group", e);
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    // ==================== Helper Methods ====================

    private void updateGroupMembers(Long groupId, List<Long> newIds, String updatedBy, String memberType) {
        FlavorGroup group = groupService.findById(groupId).orElseThrow();

        List<Long> currentIds;
        if ("user".equals(memberType)) {
            currentIds = group.getUserMembers().stream()
                    .map(m -> m.getUser().getId())
                    .toList();
        } else {
            currentIds = group.getApiKeyMembers().stream()
                    .map(m -> m.getApiKey().getId())
                    .toList();
        }

        // Remove members not in new list
        for (Long currentId : currentIds) {
            if (newIds == null || !newIds.contains(currentId)) {
                if ("user".equals(memberType)) {
                    groupService.removeUserFromGroup(groupId, currentId);
                } else {
                    groupService.removeApiKeyFromGroup(groupId, currentId);
                }
            }
        }

        // Add new members
        if (newIds != null) {
            for (Long newId : newIds) {
                if (!currentIds.contains(newId)) {
                    if ("user".equals(memberType)) {
                        groupService.addUserToGroup(groupId, newId, updatedBy);
                    } else {
                        groupService.addApiKeyToGroup(groupId, newId, updatedBy);
                    }
                }
            }
        }
    }

    private void updateGroupFlavors(Long groupId, List<Long> newFlavorIds, String updatedBy) {
        FlavorGroup group = groupService.findById(groupId).orElseThrow();

        List<Long> currentIds = group.getGroupFlavors().stream()
                .map(gf -> gf.getFlavor().getId())
                .toList();

        // Remove flavors not in new list
        for (Long currentId : currentIds) {
            if (newFlavorIds == null || !newFlavorIds.contains(currentId)) {
                groupService.removeFlavorFromGroup(groupId, currentId);
            }
        }

        // Add new flavors
        if (newFlavorIds != null) {
            for (Long newId : newFlavorIds) {
                if (!currentIds.contains(newId)) {
                    groupService.addFlavorToGroup(groupId, newId, updatedBy);
                }
            }
        }
    }
}
