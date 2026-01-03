package com.spring.mcp.controller.web;

import com.spring.mcp.model.dto.McpToolDto;
import com.spring.mcp.model.dto.McpToolGroupDto;
import com.spring.mcp.model.dto.McpToolStatisticsDto;
import com.spring.mcp.model.enums.McpToolGroup;
import com.spring.mcp.service.McpToolService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Controller for MCP Tools Masquerading page.
 * Allows administrators to control which MCP tools are exposed to LLMs.
 *
 * @author Spring MCP Server
 * @version 1.6.2
 * @since 2026-01-03
 */
@Controller
@RequestMapping("/mcp-tools")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('ADMIN')")
public class McpToolsController {

    private final McpToolService mcpToolService;

    /**
     * Display MCP tools management page.
     */
    @GetMapping
    public String index(Model model) {
        log.debug("MCP Tools page accessed");

        model.addAttribute("activePage", "mcp-tools");
        model.addAttribute("pageTitle", "MCP Masquerading");

        // Get all tool groups with their tools
        List<McpToolGroupDto> toolGroups = mcpToolService.getAllGroupsWithTools();
        model.addAttribute("toolGroups", toolGroups);

        // Get statistics
        McpToolStatisticsDto stats = mcpToolService.getStatistics();
        model.addAttribute("stats", stats);

        return "mcp-tools/index";
    }

    /**
     * Get a single tool's details.
     */
    @GetMapping("/{toolName}")
    @ResponseBody
    public ResponseEntity<McpToolDto> getTool(@PathVariable String toolName) {
        try {
            McpToolDto tool = mcpToolService.getTool(toolName);
            return ResponseEntity.ok(tool);
        } catch (IllegalArgumentException e) {
            log.warn("Tool not found: {}", toolName);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Toggle a single tool's enabled status.
     */
    @PostMapping("/{toolName}/toggle")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> toggleTool(
            @PathVariable String toolName,
            @RequestParam boolean enabled) {

        log.info("Toggling tool {} to {}", toolName, enabled);

        try {
            McpToolDto tool = mcpToolService.toggleTool(toolName, enabled);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "toolName", tool.getToolName(),
                    "enabled", tool.isEnabled()
            ));
        } catch (IllegalArgumentException e) {
            log.warn("Error toggling tool {}: {}", toolName, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        } catch (Exception e) {
            log.error("Error toggling tool {}", toolName, e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "error", "Internal server error"
            ));
        }
    }

    /**
     * Toggle all tools in a group.
     */
    @PostMapping("/groups/{group}/toggle")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> toggleGroup(
            @PathVariable McpToolGroup group,
            @RequestParam boolean enabled) {

        log.info("Toggling group {} to {}", group, enabled);

        try {
            int updated = mcpToolService.toggleGroup(group, enabled);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "group", group.name(),
                    "enabled", enabled,
                    "updatedCount", updated
            ));
        } catch (Exception e) {
            log.error("Error toggling group {}", group, e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }

    /**
     * Update a tool's description.
     */
    @PostMapping("/{toolName}/description")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> updateDescription(
            @PathVariable String toolName,
            @RequestBody Map<String, String> request) {

        String newDescription = request.get("description");
        log.info("Updating description for tool {}", toolName);

        try {
            McpToolDto tool = mcpToolService.updateDescription(toolName, newDescription);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "toolName", tool.getToolName(),
                    "modified", tool.isModified()
            ));
        } catch (IllegalArgumentException e) {
            log.warn("Error updating description for {}: {}", toolName, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        } catch (Exception e) {
            log.error("Error updating description for {}", toolName, e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "error", "Internal server error"
            ));
        }
    }

    /**
     * Reset a tool's description to the original value.
     */
    @PostMapping("/{toolName}/reset")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> resetDescription(@PathVariable String toolName) {
        log.info("Resetting description for tool {}", toolName);

        try {
            McpToolDto tool = mcpToolService.resetToOriginal(toolName);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "toolName", tool.getToolName(),
                    "modified", tool.isModified(),
                    "description", tool.getDescription()
            ));
        } catch (IllegalArgumentException e) {
            log.warn("Error resetting description for {}: {}", toolName, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        } catch (Exception e) {
            log.error("Error resetting description for {}", toolName, e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "error", "Internal server error"
            ));
        }
    }

    /**
     * Check if a tool's description is modified.
     */
    @GetMapping("/{toolName}/modified")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> isModified(@PathVariable String toolName) {
        try {
            boolean modified = mcpToolService.isDescriptionModified(toolName);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "toolName", toolName,
                    "modified", modified
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Get all tools with modified descriptions.
     */
    @GetMapping("/modified")
    @ResponseBody
    public ResponseEntity<List<McpToolDto>> getModifiedTools() {
        List<McpToolDto> modifiedTools = mcpToolService.getModifiedTools();
        return ResponseEntity.ok(modifiedTools);
    }
}
