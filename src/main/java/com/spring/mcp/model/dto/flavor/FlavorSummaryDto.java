package com.spring.mcp.model.dto.flavor;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.spring.mcp.model.enums.FlavorCategory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Summary DTO for Flavor - used in list views and search results.
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
public class FlavorSummaryDto {
    private Long id;
    private String uniqueName;
    private String displayName;
    private FlavorCategory category;
    private String patternName;
    private String description;
    private List<String> tags;
    private Boolean isActive;
    private LocalDateTime updatedAt;

    /**
     * Get category display name for UI.
     */
    public String getCategoryDisplayName() {
        return category != null ? category.getDisplayName() : null;
    }

    /**
     * Get category icon class for UI.
     */
    public String getCategoryIconClass() {
        return category != null ? category.getIconClass() : null;
    }

    /**
     * Get category color class for UI.
     */
    public String getCategoryColorClass() {
        return category != null ? category.getColorClass() : null;
    }

    /**
     * Get tags as comma-separated string.
     */
    public String getTagsString() {
        return tags != null ? String.join(", ", tags) : "";
    }
}
