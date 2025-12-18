package com.spring.mcp.controller.web;

import com.spring.mcp.model.dto.ApiKeyUsageDto;
import com.spring.mcp.model.dto.ClientUsageDto;
import com.spring.mcp.model.dto.MonitoringOverviewDto;
import com.spring.mcp.model.dto.ToolDetailDto;
import com.spring.mcp.model.dto.ToolGroupDto;
import com.spring.mcp.model.dto.ToolMetricDto;
import com.spring.mcp.model.enums.BucketType;
import com.spring.mcp.service.monitoring.McpMonitoringCleanupService;
import com.spring.mcp.service.monitoring.McpMonitoringService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Controller for MCP monitoring dashboard.
 * Provides a dark-themed dashboard for viewing metrics, connections, and tool usage.
 * Accessible only to ADMIN users.
 *
 * @author Spring MCP Server
 * @version 1.4.4
 * @since 2025-12-16
 */
@Controller
@RequestMapping("/monitoring")
@Slf4j
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class MonitoringController {

    private final McpMonitoringService monitoringService;
    private final McpMonitoringCleanupService cleanupService;

    /**
     * Display the monitoring dashboard.
     *
     * @param period Time period for metrics (default: FIVE_MIN)
     * @param model Spring MVC model
     * @return view name "monitoring/index"
     */
    @GetMapping
    public String index(
            @RequestParam(value = "period", defaultValue = "FIVE_MIN") String period,
            Model model) {

        log.debug("Showing monitoring dashboard with period: {}", period);

        BucketType bucketType = BucketType.fromString(period);

        // Get overview metrics
        MonitoringOverviewDto overview = monitoringService.getOverviewMetrics(bucketType);
        model.addAttribute("overview", overview);

        // Get tool groups with metrics
        List<ToolGroupDto> toolGroups = monitoringService.getToolGroups(bucketType);
        model.addAttribute("toolGroups", toolGroups);

        // Get active connections count
        long activeConnections = monitoringService.getActiveConnections();
        model.addAttribute("activeConnections", activeConnections);

        // Get monitoring settings
        Map<String, String> settings = monitoringService.getSettings();
        model.addAttribute("monitoringSettings", settings);

        // Get API key usage statistics
        ApiKeyUsageDto.ApiKeyUsageSummary apiKeyUsage = monitoringService.getApiKeyUsageStats();
        model.addAttribute("apiKeyUsage", apiKeyUsage);

        // Get client usage statistics
        ClientUsageDto.ClientUsageSummary clientUsage = monitoringService.getClientUsageStats();
        model.addAttribute("clientUsage", clientUsage);

        // Set page context
        model.addAttribute("activePage", "monitoring");
        model.addAttribute("pageTitle", "MCP Monitoring");
        model.addAttribute("selectedPeriod", bucketType.name());
        model.addAttribute("bucketTypes", BucketType.values());

        // Auto-refresh interval from settings (default: 30 seconds)
        int autoRefreshSeconds = Integer.parseInt(
                settings.getOrDefault("auto_refresh_seconds", "30"));
        model.addAttribute("autoRefreshSeconds", autoRefreshSeconds);

        return "monitoring/index";
    }

    // ==================== API Endpoints for AJAX/HTMX ====================

    /**
     * Get overview metrics as JSON.
     */
    @GetMapping("/api/overview")
    @ResponseBody
    public ResponseEntity<MonitoringOverviewDto> getOverview(
            @RequestParam(value = "period", defaultValue = "FIVE_MIN") String period) {

        BucketType bucketType = BucketType.fromString(period);
        MonitoringOverviewDto overview = monitoringService.getOverviewMetrics(bucketType);
        return ResponseEntity.ok(overview);
    }

    /**
     * Get tool groups with metrics as JSON.
     */
    @GetMapping("/api/tool-groups")
    @ResponseBody
    public ResponseEntity<List<ToolGroupDto>> getToolGroups(
            @RequestParam(value = "period", defaultValue = "FIVE_MIN") String period) {

        BucketType bucketType = BucketType.fromString(period);
        List<ToolGroupDto> toolGroups = monitoringService.getToolGroups(bucketType);
        return ResponseEntity.ok(toolGroups);
    }

    /**
     * Get metrics for all tools as JSON.
     */
    @GetMapping("/api/tools")
    @ResponseBody
    public ResponseEntity<List<ToolMetricDto>> getToolMetrics(
            @RequestParam(value = "period", defaultValue = "FIVE_MIN") String period) {

        BucketType bucketType = BucketType.fromString(period);
        List<ToolMetricDto> metrics = monitoringService.getToolMetrics(bucketType);
        return ResponseEntity.ok(metrics);
    }

    /**
     * Get detailed metrics for a specific tool.
     */
    @GetMapping("/api/tools/{toolName}")
    @ResponseBody
    public ResponseEntity<ToolDetailDto> getToolDetail(
            @PathVariable String toolName,
            @RequestParam(value = "period", defaultValue = "FIVE_MIN") String period) {

        BucketType bucketType = BucketType.fromString(period);
        ToolDetailDto detail = monitoringService.getToolDetail(toolName, bucketType);

        if (detail == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(detail);
    }

    /**
     * Get active connection count.
     */
    @GetMapping("/api/connections/count")
    @ResponseBody
    public ResponseEntity<Map<String, Long>> getConnectionCount() {
        long count = monitoringService.getActiveConnections();
        return ResponseEntity.ok(Map.of("activeConnections", count));
    }

    /**
     * Get API key usage statistics as JSON.
     */
    @GetMapping("/api/apikeys/usage")
    @ResponseBody
    public ResponseEntity<ApiKeyUsageDto.ApiKeyUsageSummary> getApiKeyUsage() {
        ApiKeyUsageDto.ApiKeyUsageSummary usage = monitoringService.getApiKeyUsageStats();
        return ResponseEntity.ok(usage);
    }

    /**
     * Get client usage statistics as JSON.
     */
    @GetMapping("/api/clients/usage")
    @ResponseBody
    public ResponseEntity<ClientUsageDto.ClientUsageSummary> getClientUsage() {
        ClientUsageDto.ClientUsageSummary usage = monitoringService.getClientUsageStats();
        return ResponseEntity.ok(usage);
    }

    // ==================== HTMX Fragment Endpoints ====================

    /**
     * Refresh endpoint for AJAX partial updates.
     * Returns the full page HTML which is then parsed client-side to update specific sections.
     * This preserves expanded/collapsed state of groups.
     *
     * @param period Time period for metrics
     * @param model Spring MVC model
     * @return view name "monitoring/index"
     */
    @GetMapping("/refresh")
    public String refresh(
            @RequestParam(value = "period", defaultValue = "FIVE_MIN") String period,
            Model model) {

        // Reuse the main index logic
        return index(period, model);
    }

    /**
     * Get overview cards fragment (for HTMX refresh).
     */
    @GetMapping("/fragments/overview")
    public String getOverviewFragment(
            @RequestParam(value = "period", defaultValue = "FIVE_MIN") String period,
            Model model) {

        BucketType bucketType = BucketType.fromString(period);
        MonitoringOverviewDto overview = monitoringService.getOverviewMetrics(bucketType);
        model.addAttribute("overview", overview);
        return "monitoring/fragments/overview-cards :: overview-cards";
    }

    /**
     * Get tool groups fragment (for HTMX refresh).
     */
    @GetMapping("/fragments/tool-groups")
    public String getToolGroupsFragment(
            @RequestParam(value = "period", defaultValue = "FIVE_MIN") String period,
            Model model) {

        BucketType bucketType = BucketType.fromString(period);
        List<ToolGroupDto> toolGroups = monitoringService.getToolGroups(bucketType);
        model.addAttribute("toolGroups", toolGroups);
        model.addAttribute("selectedPeriod", bucketType.name());
        return "monitoring/fragments/tool-groups :: tool-groups";
    }

    // ==================== Settings Management ====================

    /**
     * Update monitoring settings.
     */
    @PostMapping("/settings")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> updateSettings(
            @RequestParam String key,
            @RequestParam String value,
            Authentication authentication) {

        log.info("Updating monitoring setting: {}={}", key, value);

        try {
            String username = authentication.getName();
            monitoringService.updateSetting(key, value, username);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Setting updated successfully",
                    "key", key,
                    "value", value
            ));
        } catch (Exception e) {
            log.error("Error updating monitoring setting", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    // ==================== Cleanup Management ====================

    /**
     * Get retention statistics.
     */
    @GetMapping("/api/retention")
    @ResponseBody
    public ResponseEntity<McpMonitoringCleanupService.RetentionStats> getRetentionStats() {
        return ResponseEntity.ok(cleanupService.getRetentionStats());
    }

    /**
     * Update retention period.
     */
    @PostMapping("/settings/retention")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> updateRetention(
            @RequestParam int hours,
            Authentication authentication) {

        log.info("Updating retention period to {} hours", hours);

        try {
            String username = authentication.getName();
            cleanupService.updateRetentionHours(hours, username);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Retention period updated to " + hours + " hours"
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "error", e.getMessage()));
        } catch (Exception e) {
            log.error("Error updating retention period", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    /**
     * Trigger manual cleanup.
     */
    @PostMapping("/cleanup")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> triggerCleanup(
            @RequestParam(required = false) Integer hours) {

        log.info("Manual cleanup triggered");

        try {
            LocalDateTime cutoff = hours != null
                    ? LocalDateTime.now().minusHours(hours)
                    : LocalDateTime.now().minusHours(24);

            McpMonitoringCleanupService.CleanupResult result = cleanupService.cleanupOlderThan(cutoff);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Cleanup completed",
                    "deletedMetrics", result.deletedMetrics(),
                    "deletedEvents", result.deletedEvents(),
                    "cutoff", result.cutoff().toString()
            ));
        } catch (Exception e) {
            log.error("Error during manual cleanup", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("success", false, "error", e.getMessage()));
        }
    }
}
