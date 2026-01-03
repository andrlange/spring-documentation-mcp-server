package com.spring.mcp.model.dto;

import com.spring.mcp.model.enums.McpToolGroup;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * DTO for MCP Tool Group data with tools.
 * Used for UI rendering of grouped tools.
 *
 * @author Spring MCP Server
 * @version 1.6.2
 * @since 2026-01-03
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class McpToolGroupDto {

    private McpToolGroup group;
    private String displayName;
    private String iconClass;
    private String colorClass;
    private int totalCount;
    private int enabledCount;
    private int disabledCount;
    private int modifiedCount;
    private boolean allEnabled;
    private boolean allDisabled;

    @Builder.Default
    private List<McpToolDto> tools = new ArrayList<>();

    /**
     * Create a group DTO from the enum with empty tools list.
     *
     * @param group the tool group
     * @return the DTO
     */
    public static McpToolGroupDto fromEnum(McpToolGroup group) {
        return McpToolGroupDto.builder()
                .group(group)
                .displayName(group.getDisplayName())
                .iconClass(group.getIconClass())
                .colorClass(group.getColorClass())
                .totalCount(0)
                .enabledCount(0)
                .disabledCount(0)
                .modifiedCount(0)
                .allEnabled(false)
                .allDisabled(true)
                .tools(new ArrayList<>())
                .build();
    }

    /**
     * Add a tool to this group and update counts.
     *
     * @param tool the tool DTO to add
     */
    public void addTool(McpToolDto tool) {
        if (tools == null) {
            tools = new ArrayList<>();
        }
        tools.add(tool);
        updateCounts();
    }

    /**
     * Update counts based on current tools list.
     */
    public void updateCounts() {
        if (tools == null || tools.isEmpty()) {
            totalCount = 0;
            enabledCount = 0;
            disabledCount = 0;
            modifiedCount = 0;
            allEnabled = false;
            allDisabled = true;
            return;
        }

        totalCount = tools.size();
        enabledCount = (int) tools.stream().filter(McpToolDto::isEnabled).count();
        disabledCount = totalCount - enabledCount;
        modifiedCount = (int) tools.stream().filter(McpToolDto::isModified).count();
        allEnabled = enabledCount == totalCount;
        allDisabled = enabledCount == 0;
    }

    /**
     * Get the CSS color class for the enabled/disabled badge.
     *
     * @return CSS class name
     */
    public String getStatusBadgeClass() {
        if (allEnabled) {
            return "bg-success";
        } else if (allDisabled) {
            return "bg-secondary";
        } else {
            return "bg-warning";
        }
    }

    /**
     * Get status text for display.
     *
     * @return status text
     */
    public String getStatusText() {
        if (allEnabled) {
            return "All Enabled";
        } else if (allDisabled) {
            return "All Disabled";
        } else {
            return enabledCount + "/" + totalCount + " Enabled";
        }
    }
}
