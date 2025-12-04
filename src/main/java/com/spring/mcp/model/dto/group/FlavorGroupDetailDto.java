package com.spring.mcp.model.dto.group;

import com.spring.mcp.model.dto.flavor.FlavorSummaryDto;
import com.spring.mcp.model.entity.Flavor;
import com.spring.mcp.model.entity.FlavorGroup;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Detailed DTO for a FlavorGroup.
 * Includes full flavor list for the group.
 *
 * @author Spring MCP Server
 * @version 1.3.3
 * @since 2025-12-04
 */
public record FlavorGroupDetailDto(
        Long id,
        String uniqueName,
        String displayName,
        String description,
        boolean isPublic,
        List<FlavorSummaryDto> flavors,
        int userMemberCount,
        int apiKeyMemberCount,
        boolean isActive,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    /**
     * Creates a detailed DTO from a FlavorGroup entity and its flavors.
     *
     * @param group the group entity
     * @param flavors the list of flavors in this group
     * @return the detailed DTO
     */
    public static FlavorGroupDetailDto from(FlavorGroup group, List<Flavor> flavors) {
        List<FlavorSummaryDto> flavorDtos = flavors.stream()
                .map(FlavorSummaryDto::from)
                .toList();

        return new FlavorGroupDetailDto(
                group.getId(),
                group.getUniqueName(),
                group.getDisplayName(),
                group.getDescription(),
                group.isPublic(),
                flavorDtos,
                group.getUserMembers() != null ? group.getUserMembers().size() : 0,
                group.getApiKeyMembers() != null ? group.getApiKeyMembers().size() : 0,
                group.getIsActive(),
                group.getCreatedAt(),
                group.getUpdatedAt()
        );
    }
}
