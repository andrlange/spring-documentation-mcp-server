package com.spring.mcp.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for MCP Tool statistics.
 * Used for the statistics cards in the UI.
 *
 * @author Spring MCP Server
 * @version 1.6.2
 * @since 2026-01-03
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class McpToolStatisticsDto {

    private long totalTools;
    private long enabledTools;
    private long disabledTools;
    private long modifiedTools;
    private int groupCount;

    /**
     * Get the enabled percentage (0-100).
     *
     * @return enabled percentage
     */
    public int getEnabledPercentage() {
        if (totalTools == 0) {
            return 0;
        }
        return (int) Math.round((enabledTools * 100.0) / totalTools);
    }

    /**
     * Get the modified percentage (0-100).
     *
     * @return modified percentage
     */
    public int getModifiedPercentage() {
        if (totalTools == 0) {
            return 0;
        }
        return (int) Math.round((modifiedTools * 100.0) / totalTools);
    }

    /**
     * Check if there are any disabled tools.
     *
     * @return true if some tools are disabled
     */
    public boolean hasDisabledTools() {
        return disabledTools > 0;
    }

    /**
     * Check if there are any modified tools.
     *
     * @return true if some tools have modified descriptions
     */
    public boolean hasModifiedTools() {
        return modifiedTools > 0;
    }
}
