package com.spring.mcp.controller.web;

import com.spring.mcp.model.entity.ApiKey;
import com.spring.mcp.model.entity.Settings;
import com.spring.mcp.service.ApiKeyService;
import com.spring.mcp.service.SettingsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller for system settings.
 * Handles MCP server configuration and settings (Admin only).
 *
 * @author Spring MCP Server
 * @version 1.0
 * @since 2025-01-08
 */
@Controller
@RequestMapping("/settings")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('ADMIN')")
public class SettingsController {

    private final SettingsService settingsService;
    private final ApiKeyService apiKeyService;

    /**
     * Display settings page.
     *
     * @param model Spring MVC model to add attributes for the view
     * @return view name "settings/index"
     */
    @GetMapping
    public String showSettings(Model model) {
        log.debug("Showing settings page");

        // Set active page for sidebar navigation
        model.addAttribute("activePage", "settings");
        model.addAttribute("pageTitle", "Settings");

        // Load actual settings
        Settings settings = settingsService.getSettings();
        model.addAttribute("settings", settings);
        model.addAttribute("mcpServerStatus", "Running");
        model.addAttribute("mcpServerPort", 8080);
        model.addAttribute("databaseStatus", "Connected");

        // Load API keys
        List<ApiKey> apiKeys = apiKeyService.getAllApiKeys();
        model.addAttribute("apiKeys", apiKeys);
        model.addAttribute("apiKeyStats", apiKeyService.getStatistics());

        return "settings/index";
    }

    /**
     * Update settings.
     *
     * @param enterpriseSubscriptionEnabled the enterprise subscription checkbox value
     * @param redirectAttributes for flash messages
     * @return redirect to settings page
     */
    @PostMapping
    public String updateSettings(
            @RequestParam(value = "enterpriseSubscriptionEnabled", defaultValue = "false") boolean enterpriseSubscriptionEnabled,
            RedirectAttributes redirectAttributes) {

        log.debug("Updating settings: enterpriseSubscriptionEnabled={}", enterpriseSubscriptionEnabled);

        try {
            settingsService.updateEnterpriseSubscription(enterpriseSubscriptionEnabled);
            redirectAttributes.addFlashAttribute("success",
                "Settings updated successfully. Enterprise Subscription is now " +
                (enterpriseSubscriptionEnabled ? "enabled" : "disabled") + ".");
            log.info("Settings updated successfully");
        } catch (Exception e) {
            log.error("Error updating settings", e);
            redirectAttributes.addFlashAttribute("error",
                "Failed to update settings: " + e.getMessage());
        }

        return "redirect:/settings";
    }

    // ==================== API Key Management ====================

    /**
     * Create a new API key
     */
    @PostMapping("/api-keys/create")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> createApiKey(
            @RequestParam String name,
            @RequestParam(required = false) String description,
            Authentication authentication) {

        log.info("Creating API key: name={}", name);

        try {
            // Validate name length
            if (name == null || name.trim().length() < 3) {
                return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "error", "Name must be at least 3 characters"));
            }

            String username = authentication.getName();
            Map<String, Object> result = apiKeyService.createApiKey(name, username, description);

            ApiKey apiKey = (ApiKey) result.get("apiKey");
            String plainTextKey = (String) result.get("plainTextKey");

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "API key created successfully");
            response.put("apiKey", Map.of(
                "id", apiKey.getId(),
                "name", apiKey.getName(),
                "createdAt", apiKey.getCreatedAt().toString(),
                "isActive", apiKey.getIsActive()
            ));
            response.put("plainTextKey", plainTextKey); // Show only once!
            response.put("warning", "IMPORTANT: Copy this key now. It will not be shown again!");

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.warn("Failed to create API key: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(Map.of("success", false, "error", e.getMessage()));
        } catch (Exception e) {
            log.error("Error creating API key", e);
            return ResponseEntity.internalServerError()
                .body(Map.of("success", false, "error", "Failed to create API key: " + e.getMessage()));
        }
    }

    /**
     * Generate a secure random API key (for preview)
     */
    @GetMapping("/api-keys/generate")
    @ResponseBody
    public ResponseEntity<Map<String, String>> generateApiKey() {
        String key = apiKeyService.generateSecureKey();
        return ResponseEntity.ok(Map.of("key", key));
    }

    /**
     * Deactivate an API key
     */
    @PostMapping("/api-keys/{id}/deactivate")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> deactivateApiKey(@PathVariable Long id) {
        log.info("Deactivating API key: id={}", id);

        try {
            apiKeyService.deactivateApiKey(id);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "API key deactivated successfully"
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                .body(Map.of("success", false, "error", e.getMessage()));
        } catch (Exception e) {
            log.error("Error deactivating API key", e);
            return ResponseEntity.internalServerError()
                .body(Map.of("success", false, "error", "Failed to deactivate API key"));
        }
    }

    /**
     * Reactivate an API key
     */
    @PostMapping("/api-keys/{id}/activate")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> reactivateApiKey(@PathVariable Long id) {
        log.info("Reactivating API key: id={}", id);

        try {
            apiKeyService.reactivateApiKey(id);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "API key reactivated successfully"
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                .body(Map.of("success", false, "error", e.getMessage()));
        } catch (Exception e) {
            log.error("Error reactivating API key", e);
            return ResponseEntity.internalServerError()
                .body(Map.of("success", false, "error", "Failed to reactivate API key"));
        }
    }

    /**
     * Delete an API key permanently
     */
    @DeleteMapping("/api-keys/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> deleteApiKey(@PathVariable Long id) {
        log.info("Deleting API key: id={}", id);

        try {
            apiKeyService.deleteApiKey(id);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "API key deleted successfully"
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                .body(Map.of("success", false, "error", e.getMessage()));
        } catch (Exception e) {
            log.error("Error deleting API key", e);
            return ResponseEntity.internalServerError()
                .body(Map.of("success", false, "error", "Failed to delete API key"));
        }
    }
}
