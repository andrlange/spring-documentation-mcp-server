package com.spring.mcp.controller.web;

import com.spring.mcp.model.entity.ApiKey;
import com.spring.mcp.model.entity.LanguageSchedulerSettings;
import com.spring.mcp.model.entity.SchedulerSettings;
import com.spring.mcp.model.entity.Settings;
import com.spring.mcp.model.enums.SyncFrequency;
import com.spring.mcp.service.ApiKeyService;
import com.spring.mcp.service.SettingsService;
import com.spring.mcp.service.scheduler.LanguageSchedulerService;
import com.spring.mcp.service.scheduler.SchedulerService;
import com.spring.mcp.service.settings.GlobalSettingsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringBootVersion;
import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext;
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
import java.util.Optional;

/**
 * Controller for system settings.
 * Handles MCP server configuration and settings (Admin only).
 *
 * @author Spring MCP Server
 * @version 1.1
 * @since 2025-01-08
 */
@Controller
@RequestMapping("/settings")
@Slf4j
@PreAuthorize("hasRole('ADMIN')")
public class SettingsController {

    private final SettingsService settingsService;
    private final ApiKeyService apiKeyService;
    private final GlobalSettingsService globalSettingsService;
    private final Optional<SchedulerService> schedulerService;
    private final Optional<LanguageSchedulerService> languageSchedulerService;

    // Optional - may not be available in test contexts
    private final ServletWebServerApplicationContext webServerAppContext;

    @Value("${info.app.version:1.1.0}")
    private String appVersion;

    @Value("${server.port:8080}")
    private int serverPort;

    @Autowired
    public SettingsController(
            SettingsService settingsService,
            ApiKeyService apiKeyService,
            GlobalSettingsService globalSettingsService,
            Optional<SchedulerService> schedulerService,
            Optional<LanguageSchedulerService> languageSchedulerService,
            @Autowired(required = false) ServletWebServerApplicationContext webServerAppContext) {
        this.settingsService = settingsService;
        this.apiKeyService = apiKeyService;
        this.globalSettingsService = globalSettingsService;
        this.schedulerService = schedulerService;
        this.languageSchedulerService = languageSchedulerService;
        this.webServerAppContext = webServerAppContext;
    }

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
        // Use actual server port if available, otherwise use configured port
        int port = webServerAppContext != null ? webServerAppContext.getWebServer().getPort() : serverPort;
        model.addAttribute("mcpServerPort", port);
        model.addAttribute("databaseStatus", "Connected");

        // System information
        model.addAttribute("appVersion", appVersion);
        model.addAttribute("springBootVersion", SpringBootVersion.getVersion());
        model.addAttribute("javaVersion", System.getProperty("java.version"));

        // Load API keys
        List<ApiKey> apiKeys = apiKeyService.getAllApiKeys();
        model.addAttribute("apiKeys", apiKeys);
        model.addAttribute("apiKeyStats", apiKeyService.getStatistics());

        // Load global time format
        String globalTimeFormat = globalSettingsService.getTimeFormat();
        model.addAttribute("globalTimeFormat", globalTimeFormat);

        // Load sync frequencies for all schedulers
        model.addAttribute("syncFrequencies", SyncFrequency.values());

        // Load scheduler settings (if available)
        if (schedulerService.isPresent()) {
            SchedulerSettings schedulerSettings = schedulerService.get().getSettings();
            model.addAttribute("schedulerSettings", schedulerSettings);

            // Format time for display using global time format
            String displayTime = schedulerService.get().formatTimeForDisplay(
                schedulerSettings.getSyncTime(),
                globalTimeFormat
            );
            model.addAttribute("displayTime", displayTime);
        }

        // Load language scheduler settings (if available)
        if (languageSchedulerService.isPresent()) {
            LanguageSchedulerSettings languageSchedulerSettings = languageSchedulerService.get().getSettings();
            model.addAttribute("languageSchedulerSettings", languageSchedulerSettings);

            // Format language scheduler time for display using global time format
            String languageDisplayTime = languageSchedulerService.get().formatTimeForDisplay(
                languageSchedulerSettings.getSyncTime(),
                globalTimeFormat
            );
            model.addAttribute("languageDisplayTime", languageDisplayTime);
        }

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

    // ==================== Global Settings ====================

    /**
     * Update global time format (12h/24h toggle) - AJAX endpoint
     * This applies to all scheduler displays
     */
    @PostMapping("/global/time-format")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> updateGlobalTimeFormat(@RequestParam String timeFormat) {
        log.debug("Updating global time format to: {}", timeFormat);

        try {
            globalSettingsService.updateTimeFormat(timeFormat);

            // Prepare display times for both schedulers
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Time format updated to " + timeFormat);
            response.put("timeFormat", timeFormat);

            // Update display times for schedulers
            if (schedulerService.isPresent()) {
                SchedulerSettings settings = schedulerService.get().getSettings();
                String displayTime = schedulerService.get().formatTimeForDisplay(
                    settings.getSyncTime(), timeFormat
                );
                response.put("displayTime", displayTime);
            }

            if (languageSchedulerService.isPresent()) {
                LanguageSchedulerSettings settings = languageSchedulerService.get().getSettings();
                String languageDisplayTime = languageSchedulerService.get().formatTimeForDisplay(
                    settings.getSyncTime(), timeFormat
                );
                response.put("languageDisplayTime", languageDisplayTime);
            }

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                .body(Map.of("success", false, "error", e.getMessage()));
        } catch (Exception e) {
            log.error("Error updating global time format", e);
            return ResponseEntity.internalServerError()
                .body(Map.of("success", false, "error", "Failed to update time format"));
        }
    }

    // ==================== Scheduler Configuration ====================

    /**
     * Update scheduler settings with frequency support
     */
    @PostMapping("/scheduler")
    public String updateSchedulerSettings(
            @RequestParam(value = "syncEnabled", defaultValue = "false") boolean syncEnabled,
            @RequestParam String frequency,
            @RequestParam String syncTime,
            @RequestParam(required = false) String weekdays,
            @RequestParam(required = false) Integer dayOfMonth,
            RedirectAttributes redirectAttributes) {

        log.debug("Updating scheduler settings: syncEnabled={}, frequency={}, syncTime={}",
            syncEnabled, frequency, syncTime);

        if (schedulerService.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Scheduler service not available");
            return "redirect:/settings";
        }

        try {
            SyncFrequency syncFrequency = SyncFrequency.valueOf(frequency.toUpperCase());
            String globalTimeFormat = globalSettingsService.getTimeFormat();

            schedulerService.get().updateSettings(
                syncEnabled, syncTime, globalTimeFormat, weekdays,
                weekdays != null && !weekdays.isEmpty(),
                syncFrequency, dayOfMonth != null ? dayOfMonth : 1
            );

            redirectAttributes.addFlashAttribute("success",
                "Scheduler settings updated successfully. " +
                (syncEnabled ? "Automatic sync enabled (" + syncFrequency.getDisplayName() + ")" : "Automatic sync disabled"));

            log.info("Scheduler settings updated: syncEnabled={}, frequency={}, syncTime={}",
                syncEnabled, frequency, syncTime);
        } catch (Exception e) {
            log.error("Error updating scheduler settings", e);
            redirectAttributes.addFlashAttribute("error",
                "Failed to update scheduler settings: " + e.getMessage());
        }

        return "redirect:/settings";
    }

    /**
     * Update time format immediately (12h/24h toggle) - AJAX endpoint
     * @deprecated Use /settings/global/time-format instead
     */
    @PostMapping("/scheduler/time-format")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> updateTimeFormat(@RequestParam String timeFormat) {
        log.debug("Updating time format to: {} (delegating to global)", timeFormat);
        return updateGlobalTimeFormat(timeFormat);
    }

    // ==================== Language Scheduler Configuration ====================

    /**
     * Update language scheduler settings
     */
    @PostMapping("/language-scheduler")
    public String updateLanguageSchedulerSettings(
            @RequestParam(value = "syncEnabled", defaultValue = "false") boolean syncEnabled,
            @RequestParam String frequency,
            @RequestParam String syncTime,
            @RequestParam(required = false) String weekdays,
            @RequestParam(required = false) Integer dayOfMonth,
            RedirectAttributes redirectAttributes) {

        log.debug("Updating language scheduler settings: syncEnabled={}, frequency={}, syncTime={}",
                syncEnabled, frequency, syncTime);

        if (languageSchedulerService.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Language scheduler service not available");
            return "redirect:/settings";
        }

        try {
            SyncFrequency syncFrequency = SyncFrequency.valueOf(frequency.toUpperCase());
            String globalTimeFormat = globalSettingsService.getTimeFormat();

            languageSchedulerService.get().updateSettings(
                    syncEnabled, syncFrequency, syncTime, weekdays,
                    dayOfMonth != null ? dayOfMonth : 1,
                    globalTimeFormat);

            redirectAttributes.addFlashAttribute("success",
                    "Language scheduler settings updated successfully. " +
                    (syncEnabled ? "Automatic language sync enabled (" + syncFrequency.getDisplayName() + ")" : "Automatic language sync disabled"));

            log.info("Language scheduler settings updated: syncEnabled={}, frequency={}, syncTime={}",
                    syncEnabled, frequency, syncTime);
        } catch (Exception e) {
            log.error("Error updating language scheduler settings", e);
            redirectAttributes.addFlashAttribute("error",
                    "Failed to update language scheduler settings: " + e.getMessage());
        }

        return "redirect:/settings";
    }

    /**
     * Update language scheduler time format (AJAX endpoint)
     * @deprecated Use /settings/global/time-format instead
     */
    @PostMapping("/language-scheduler/time-format")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> updateLanguageTimeFormat(@RequestParam String timeFormat) {
        log.debug("Updating language time format to: {} (delegating to global)", timeFormat);
        return updateGlobalTimeFormat(timeFormat);
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
