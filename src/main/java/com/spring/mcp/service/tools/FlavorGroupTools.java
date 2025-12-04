package com.spring.mcp.service.tools;

import com.spring.mcp.model.dto.group.FlavorGroupDetailDto;
import com.spring.mcp.model.dto.group.FlavorGroupSummaryDto;
import com.spring.mcp.model.entity.Flavor;
import com.spring.mcp.model.entity.FlavorGroup;
import com.spring.mcp.security.SecurityContextHelper;
import com.spring.mcp.service.FlavorGroupService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springaicommunity.mcp.annotation.McpTool;
import org.springaicommunity.mcp.annotation.McpToolParam;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * MCP Tools for Flavor Groups - team-based authorization and organization.
 * <p>
 * These tools provide access to flavor groups with visibility filtering:
 * <ul>
 *   <li>Public groups (no members) are visible to everyone</li>
 *   <li>Private groups (has members) are visible only to members</li>
 *   <li>Inactive groups are completely hidden</li>
 * </ul>
 * <p>
 * API key context is extracted from the security context to determine access.
 * Private groups are only visible to API keys that are members of the group.
 *
 * @author Spring MCP Server
 * @version 1.3.3
 * @since 2025-12-04
 */
@Service
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(name = "mcp.features.flavors.enabled", havingValue = "true", matchIfMissing = true)
public class FlavorGroupTools {

    private final FlavorGroupService flavorGroupService;
    private final SecurityContextHelper securityContextHelper;

    /**
     * Lists all accessible flavor groups.
     * Returns public groups and private groups where the caller is a member.
     * Inactive groups are completely hidden.
     *
     * @param includePublic Include public groups in results (default: true)
     * @param includePrivate Include private groups where caller is member (default: true)
     * @return List of accessible flavor groups
     */
    @McpTool(description = """
        List all accessible flavor groups. Returns public groups and private groups where caller is member.
        Inactive groups are completely hidden from results.

        Groups organize flavors by team or topic:
        - PUBLIC groups (no members): visible to everyone, used for organization-wide standards
        - PRIVATE groups (has members): visible only to member API keys/users, used for team-specific guidelines
        """)
    public List<FlavorGroupSummaryDto> listFlavorGroups(
            @McpToolParam(description = "Include public groups (default: true)")
            Boolean includePublic,
            @McpToolParam(description = "Include private groups where caller is member (default: true)")
            Boolean includePrivate
    ) {
        // Extract API key ID from security context
        Long apiKeyId = securityContextHelper.getCurrentApiKeyId();
        log.info("Tool: listFlavorGroups - includePublic={}, includePrivate={}, apiKeyId={}",
                includePublic, includePrivate, apiKeyId);

        boolean showPublic = includePublic == null || includePublic;
        boolean showPrivate = includePrivate == null || includePrivate;

        List<FlavorGroup> allAccessible = flavorGroupService.findAccessibleGroupsForApiKey(apiKeyId);

        return allAccessible.stream()
                .filter(g -> {
                    boolean isPublic = g.isPublic();
                    return (showPublic && isPublic) || (showPrivate && !isPublic);
                })
                .map(FlavorGroupSummaryDto::from)
                .toList();
    }

    /**
     * Gets detailed information about a specific flavor group.
     * Returns the group metadata and all flavors in the group.
     * Only accessible if the group is public or the caller is a member.
     *
     * @param groupName Unique name of the group
     * @return Detailed group information including flavors
     */
    @McpTool(description = """
        Get all flavors in a specific group. Returns group metadata and all member flavors.
        Only returns active groups - inactive groups are completely hidden.

        The group must be either:
        - PUBLIC (no members): accessible to everyone
        - PRIVATE: caller must be a member (API key must belong to the group)

        Use listFlavorGroups first to see available groups, then use this tool to get details.
        """)
    public FlavorGroupDetailDto getFlavorsGroup(
            @McpToolParam(description = "Unique name of the group (e.g., 'engineering-standards', 'payment-platform')")
            String groupName
    ) {
        // Extract API key ID from security context
        Long apiKeyId = securityContextHelper.getCurrentApiKeyId();
        log.info("Tool: getFlavorsGroup - groupName={}, apiKeyId={}", groupName, apiKeyId);

        if (groupName == null || groupName.isBlank()) {
            throw new IllegalArgumentException("Group name is required");
        }

        try {
            List<Flavor> flavors = flavorGroupService.getFlavorsInGroup(groupName, apiKeyId);
            FlavorGroup group = flavorGroupService.findActiveByUniqueName(groupName)
                    .orElseThrow(() -> new EntityNotFoundException("Group not found: " + groupName));

            return FlavorGroupDetailDto.from(group, flavors);

        } catch (SecurityException e) {
            // Re-throw as IllegalArgumentException for better MCP error handling
            throw new IllegalArgumentException("Access denied to group: " + groupName +
                    ". The group is private and requires API key membership.");
        }
    }

    /**
     * Gets group statistics including counts.
     *
     * @return Group statistics
     */
    @McpTool(description = """
        Get statistics about flavor groups.
        Returns total, active, inactive, public, and private group counts.
        """)
    public FlavorGroupService.GroupStatistics getFlavorGroupStatistics() {
        log.info("Tool: getFlavorGroupStatistics");
        return flavorGroupService.getGroupStatistics();
    }
}
