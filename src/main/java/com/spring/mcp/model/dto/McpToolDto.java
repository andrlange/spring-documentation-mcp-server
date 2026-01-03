package com.spring.mcp.model.dto;

import com.spring.mcp.model.entity.McpTool;
import com.spring.mcp.model.enums.McpToolGroup;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for MCP Tool data transfer.
 * Used for API responses and UI rendering.
 *
 * @author Spring MCP Server
 * @version 1.6.2
 * @since 2026-01-03
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class McpToolDto {

    private Long id;
    private String toolName;
    private McpToolGroup toolGroup;
    private String groupDisplayName;
    private String groupIconClass;
    private String groupColorClass;
    private boolean enabled;
    private String description;
    private String originalDescription;
    private boolean modified;
    private Integer displayOrder;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String updatedBy;

    /**
     * Create DTO from entity.
     *
     * @param entity the McpTool entity
     * @return the DTO
     */
    public static McpToolDto fromEntity(McpTool entity) {
        if (entity == null) {
            return null;
        }

        McpToolGroup group = entity.getToolGroup();

        return McpToolDto.builder()
                .id(entity.getId())
                .toolName(entity.getToolName())
                .toolGroup(group)
                .groupDisplayName(group != null ? group.getDisplayName() : null)
                .groupIconClass(group != null ? group.getIconClass() : null)
                .groupColorClass(group != null ? group.getColorClass() : null)
                .enabled(entity.getEnabled() != null && entity.getEnabled())
                .description(entity.getDescription())
                .originalDescription(entity.getOriginalDescription())
                .modified(entity.isDescriptionModified())
                .displayOrder(entity.getDisplayOrder())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .updatedBy(entity.getUpdatedBy())
                .build();
    }

    /**
     * Get a truncated description for display in tables.
     *
     * @param maxLength maximum length before truncation
     * @return truncated description with ellipsis if needed
     */
    public String getTruncatedDescription(int maxLength) {
        if (description == null) {
            return "";
        }
        if (description.length() <= maxLength) {
            return description;
        }
        return description.substring(0, maxLength - 3) + "...";
    }

    /**
     * Get default truncated description (80 chars).
     *
     * @return truncated description
     */
    public String getTruncatedDescription() {
        return getTruncatedDescription(80);
    }
}
