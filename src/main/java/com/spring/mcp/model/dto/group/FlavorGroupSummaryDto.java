package com.spring.mcp.model.dto.group;

import com.spring.mcp.model.entity.FlavorGroup;

import java.time.LocalDateTime;

/**
 * Summary DTO for a FlavorGroup.
 * Used in list responses and search results.
 *
 * @author Spring MCP Server
 * @version 1.3.3
 * @since 2025-12-04
 */
public record FlavorGroupSummaryDto(
        Long id,
        String uniqueName,
        String displayName,
        String description,
        boolean isPublic,
        int userMemberCount,
        int apiKeyMemberCount,
        int flavorCount,
        boolean isActive,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    /**
     * Creates a summary DTO from a FlavorGroup entity.
     *
     * @param group the group entity
     * @return the summary DTO
     */
    public static FlavorGroupSummaryDto from(FlavorGroup group) {
        return new FlavorGroupSummaryDto(
                group.getId(),
                group.getUniqueName(),
                group.getDisplayName(),
                group.getDescription(),
                group.isPublic(),
                group.getUserMembers() != null ? group.getUserMembers().size() : 0,
                group.getApiKeyMembers() != null ? group.getApiKeyMembers().size() : 0,
                group.getGroupFlavors() != null ? group.getGroupFlavors().size() : 0,
                group.getIsActive(),
                group.getCreatedAt(),
                group.getUpdatedAt()
        );
    }

    /**
     * Creates a summary DTO from a FlavorGroup entity with counts.
     *
     * @param group the group entity
     * @param flavorCount the flavor count (computed separately)
     * @return the summary DTO
     */
    public static FlavorGroupSummaryDto from(FlavorGroup group, int flavorCount) {
        return new FlavorGroupSummaryDto(
                group.getId(),
                group.getUniqueName(),
                group.getDisplayName(),
                group.getDescription(),
                group.isPublic(),
                group.getUserMembers() != null ? group.getUserMembers().size() : 0,
                group.getApiKeyMembers() != null ? group.getApiKeyMembers().size() : 0,
                flavorCount,
                group.getIsActive(),
                group.getCreatedAt(),
                group.getUpdatedAt()
        );
    }
}
