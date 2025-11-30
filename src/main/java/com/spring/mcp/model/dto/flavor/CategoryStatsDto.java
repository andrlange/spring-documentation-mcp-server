package com.spring.mcp.model.dto.flavor;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.spring.mcp.model.enums.FlavorCategory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Statistics DTO for Flavor categories.
 *
 * @author Spring MCP Server
 * @version 1.2.0
 * @since 2025-11-30
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CategoryStatsDto {
    private Long totalActive;
    private Long totalInactive;
    private Map<FlavorCategory, Long> categoryCounts;

    public CategoryStatsDto(Long totalActive, Map<FlavorCategory, Long> categoryCounts) {
        this.totalActive = totalActive;
        this.categoryCounts = categoryCounts;
    }

    /**
     * Get count for a specific category.
     */
    public Long getCountForCategory(FlavorCategory category) {
        return categoryCounts != null ? categoryCounts.getOrDefault(category, 0L) : 0L;
    }

    /**
     * Get architecture count.
     */
    public Long getArchitectureCount() {
        return getCountForCategory(FlavorCategory.ARCHITECTURE);
    }

    /**
     * Get compliance count.
     */
    public Long getComplianceCount() {
        return getCountForCategory(FlavorCategory.COMPLIANCE);
    }

    /**
     * Get agents count.
     */
    public Long getAgentsCount() {
        return getCountForCategory(FlavorCategory.AGENTS);
    }

    /**
     * Get initialization count.
     */
    public Long getInitializationCount() {
        return getCountForCategory(FlavorCategory.INITIALIZATION);
    }

    /**
     * Get general count.
     */
    public Long getGeneralCount() {
        return getCountForCategory(FlavorCategory.GENERAL);
    }
}
