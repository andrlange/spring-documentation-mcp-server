package com.spring.mcp.service;

import com.spring.mcp.model.dto.McpToolDto;
import com.spring.mcp.model.dto.McpToolGroupDto;
import com.spring.mcp.model.dto.McpToolStatisticsDto;
import com.spring.mcp.model.entity.McpTool;
import com.spring.mcp.model.enums.McpToolGroup;
import com.spring.mcp.repository.McpToolRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for managing MCP tool configurations.
 * Provides CRUD operations and group management for MCP tool masquerading.
 *
 * @author Spring MCP Server
 * @version 1.6.2
 * @since 2026-01-03
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class McpToolService {

    private final McpToolRepository mcpToolRepository;
    private final McpToolMasqueradingService masqueradingService;

    /**
     * Get all tools organized by group.
     *
     * @return list of group DTOs with their tools
     */
    @Transactional(readOnly = true)
    public List<McpToolGroupDto> getAllGroupsWithTools() {
        List<McpTool> allTools = mcpToolRepository.findAllOrdered();

        // Group tools by their group
        Map<McpToolGroup, List<McpToolDto>> toolsByGroup = allTools.stream()
                .map(McpToolDto::fromEntity)
                .collect(Collectors.groupingBy(McpToolDto::getToolGroup, LinkedHashMap::new, Collectors.toList()));

        // Create group DTOs for each enum value (preserving order)
        List<McpToolGroupDto> result = new ArrayList<>();
        for (McpToolGroup group : McpToolGroup.values()) {
            McpToolGroupDto groupDto = McpToolGroupDto.fromEnum(group);
            List<McpToolDto> groupTools = toolsByGroup.getOrDefault(group, Collections.emptyList());
            groupTools.forEach(groupDto::addTool);
            result.add(groupDto);
        }

        return result;
    }

    /**
     * Get statistics about tool configurations.
     *
     * @return statistics DTO
     */
    @Transactional(readOnly = true)
    public McpToolStatisticsDto getStatistics() {
        long total = mcpToolRepository.count();
        long enabled = mcpToolRepository.countByEnabledTrue();
        long modified = mcpToolRepository.countModifiedDescriptions();

        return McpToolStatisticsDto.builder()
                .totalTools(total)
                .enabledTools(enabled)
                .disabledTools(total - enabled)
                .modifiedTools(modified)
                .groupCount(McpToolGroup.values().length)
                .build();
    }

    /**
     * Get a single tool by name.
     *
     * @param toolName the tool name
     * @return the tool DTO
     * @throws IllegalArgumentException if tool not found
     */
    @Transactional(readOnly = true)
    public McpToolDto getTool(String toolName) {
        McpTool tool = mcpToolRepository.findByToolName(toolName)
                .orElseThrow(() -> new IllegalArgumentException("Tool not found: " + toolName));
        return McpToolDto.fromEntity(tool);
    }

    /**
     * Toggle a single tool's enabled status.
     *
     * @param toolName the tool name
     * @param enabled  the new enabled status
     * @return the updated tool DTO
     */
    @Transactional
    public McpToolDto toggleTool(String toolName, boolean enabled) {
        McpTool tool = mcpToolRepository.findByToolName(toolName)
                .orElseThrow(() -> new IllegalArgumentException("Tool not found: " + toolName));

        tool.setEnabled(enabled);
        tool.setUpdatedBy(getCurrentUsername());
        McpTool saved = mcpToolRepository.save(tool);

        log.info("Tool {} {} by {}", toolName, enabled ? "enabled" : "disabled", getCurrentUsername());

        // Notify MCP server of change
        masqueradingService.applyToolVisibility(saved);

        return McpToolDto.fromEntity(saved);
    }

    /**
     * Toggle all tools in a group.
     *
     * @param group   the tool group
     * @param enabled the new enabled status
     * @return count of updated tools
     */
    @Transactional
    public int toggleGroup(McpToolGroup group, boolean enabled) {
        String username = getCurrentUsername();
        int updated = mcpToolRepository.updateGroupEnabled(group, enabled, username);

        log.info("Group {} {} ({} tools) by {}", group, enabled ? "enabled" : "disabled", updated, username);

        // Re-apply masquerading for all tools in group
        List<McpTool> groupTools = mcpToolRepository.findByToolGroup(group);
        groupTools.forEach(masqueradingService::applyToolVisibility);

        return updated;
    }

    /**
     * Update a tool's description.
     *
     * @param toolName       the tool name
     * @param newDescription the new description
     * @return the updated tool DTO
     */
    @Transactional
    public McpToolDto updateDescription(String toolName, String newDescription) {
        McpTool tool = mcpToolRepository.findByToolName(toolName)
                .orElseThrow(() -> new IllegalArgumentException("Tool not found: " + toolName));

        if (newDescription == null || newDescription.isBlank()) {
            throw new IllegalArgumentException("Description cannot be empty");
        }

        tool.setDescription(newDescription.trim());
        tool.setUpdatedBy(getCurrentUsername());
        McpTool saved = mcpToolRepository.save(tool);

        log.info("Tool {} description updated by {}", toolName, getCurrentUsername());

        // Update description in MCP server if tool is enabled
        if (saved.getEnabled()) {
            masqueradingService.updateToolDescription(saved);
        }

        return McpToolDto.fromEntity(saved);
    }

    /**
     * Reset a tool's description to the original value.
     *
     * @param toolName the tool name
     * @return the updated tool DTO with wasReset flag indicating if reset occurred
     */
    @Transactional
    public McpToolDto resetToOriginal(String toolName) {
        McpTool tool = mcpToolRepository.findByToolName(toolName)
                .orElseThrow(() -> new IllegalArgumentException("Tool not found: " + toolName));

        boolean wasModified = tool.isDescriptionModified();

        if (wasModified) {
            tool.resetDescription();
            tool.setUpdatedBy(getCurrentUsername());
            tool = mcpToolRepository.save(tool);

            log.info("Tool {} description reset to original by {}", toolName, getCurrentUsername());

            // Update description in MCP server if tool is enabled
            if (tool.getEnabled()) {
                masqueradingService.updateToolDescription(tool);
            }
        }

        return McpToolDto.fromEntity(tool);
    }

    /**
     * Check if a tool's description is modified from original.
     *
     * @param toolName the tool name
     * @return true if modified
     */
    @Transactional(readOnly = true)
    public boolean isDescriptionModified(String toolName) {
        McpTool tool = mcpToolRepository.findByToolName(toolName)
                .orElseThrow(() -> new IllegalArgumentException("Tool not found: " + toolName));
        return tool.isDescriptionModified();
    }

    /**
     * Get all tools with modified descriptions.
     *
     * @return list of modified tool DTOs
     */
    @Transactional(readOnly = true)
    public List<McpToolDto> getModifiedTools() {
        return mcpToolRepository.findAllWithModifiedDescriptions().stream()
                .map(McpToolDto::fromEntity)
                .toList();
    }

    /**
     * Get the current authenticated username.
     *
     * @return username or "system" if not authenticated
     */
    private String getCurrentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()) {
            return auth.getName();
        }
        return "system";
    }
}
