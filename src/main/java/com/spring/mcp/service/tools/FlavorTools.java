package com.spring.mcp.service.tools;

import com.spring.mcp.model.dto.flavor.CategoryStatsDto;
import com.spring.mcp.model.dto.flavor.FlavorDto;
import com.spring.mcp.model.dto.flavor.FlavorSummaryDto;
import com.spring.mcp.model.enums.FlavorCategory;
import com.spring.mcp.service.FlavorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springaicommunity.mcp.annotation.McpTool;
import org.springaicommunity.mcp.annotation.McpToolParam;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * MCP Tools for Flavors - company guidelines, architecture patterns,
 * compliance rules, agent configurations, and project initialization templates.
 *
 * Uses @McpTool annotation for Spring AI MCP Server auto-discovery.
 * This service is only enabled when the flavors feature is enabled.
 * <p>
 * <strong>Note on Flavor Groups (v1.3.3+):</strong>
 * Flavors can now be organized into groups with visibility rules:
 * <ul>
 *   <li>Unassigned flavors are visible to everyone</li>
 *   <li>Flavors in PUBLIC groups (no members) are visible to everyone</li>
 *   <li>Flavors in PRIVATE groups (has members) are visible only to group members</li>
 *   <li>Flavors in INACTIVE groups are completely hidden</li>
 * </ul>
 * <p>
 * Currently, these tools return all active unassigned and public group flavors.
 * For group-specific filtering, use the FlavorGroupTools:
 * <ul>
 *   <li>{@code listFlavorGroups} - List accessible flavor groups</li>
 *   <li>{@code getFlavorsGroup} - Get all flavors in a specific group</li>
 *   <li>{@code getFlavorGroupStatistics} - Get group statistics</li>
 * </ul>
 * <p>
 * Future enhancement: When API key context propagation is implemented,
 * these tools will automatically filter based on group membership.
 *
 * @author Spring MCP Server
 * @version 1.3.3
 * @since 2025-11-30
 */
@Service
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(name = "mcp.features.flavors.enabled", havingValue = "true", matchIfMissing = true)
public class FlavorTools {

    private final FlavorService flavorService;

    @McpTool(description = """
        Search company flavors (guidelines, architecture patterns, compliance rules, agent configurations).
        Returns summaries matching the search criteria.
        """)
    public List<FlavorSummaryDto> searchFlavors(
            @McpToolParam(description = "Search query for flavor content (searches name, description, and content)")
            String query,
            @McpToolParam(description = "Filter by category: ARCHITECTURE, COMPLIANCE, AGENTS, INITIALIZATION, GENERAL. Optional.")
            String category,
            @McpToolParam(description = "Filter by tags (e.g., ['spring-boot', 'kafka']). Optional.")
            List<String> tags,
            @McpToolParam(description = "Maximum results to return (default: 10, max: 50)")
            Integer limit
    ) {
        log.info("Tool: searchFlavors - query={}, category={}, tags={}, limit={}", query, category, tags, limit);

        FlavorCategory cat = category != null ? FlavorCategory.fromString(category) : null;
        int maxResults = limit != null ? Math.min(limit, 50) : 10;
        return flavorService.search(query, cat, tags, maxResults);
    }

    @McpTool(description = """
        Get complete flavor content by its unique name. Returns full markdown content and metadata.
        """)
    public FlavorDto getFlavorByName(
            @McpToolParam(description = "Unique name of the flavor (e.g., 'hexagonal-spring-boot')")
            String uniqueName
    ) {
        log.info("Tool: getFlavorByName - uniqueName={}", uniqueName);

        return flavorService.findByUniqueName(uniqueName)
            .orElseThrow(() -> new IllegalArgumentException("Flavor not found: " + uniqueName));
    }

    @McpTool(description = """
        List all active flavors in a specific category.
        """)
    public List<FlavorDto> getFlavorsByCategory(
            @McpToolParam(description = "Category: ARCHITECTURE, COMPLIANCE, AGENTS, INITIALIZATION, or GENERAL")
            String category
    ) {
        log.info("Tool: getFlavorsByCategory - category={}", category);

        FlavorCategory cat = FlavorCategory.fromString(category);
        if (cat == null) {
            throw new IllegalArgumentException("Invalid category: " + category);
        }
        return flavorService.findByCategory(cat);
    }

    @McpTool(description = """
        Get architecture flavors relevant to specific technologies.
        Use this when you need architectural guidance for a particular tech stack.
        """)
    public List<FlavorDto> getArchitecturePatterns(
            @McpToolParam(description = "Technology slugs (e.g., ['spring-boot', 'kafka', 'jpa'])")
            List<String> slugs
    ) {
        log.info("Tool: getArchitecturePatterns - slugs={}", slugs);

        if (slugs == null || slugs.isEmpty()) {
            throw new IllegalArgumentException("At least one technology slug is required");
        }
        return flavorService.findArchitectureByTechnologies(slugs);
    }

    @McpTool(description = """
        Get compliance flavors by rule names or framework identifiers.
        Use this when you need to ensure code meets regulatory requirements.
        """)
    public List<FlavorDto> getComplianceRules(
            @McpToolParam(description = "Rule names or framework identifiers (e.g., ['GDPR', 'SOC2', 'PCI-DSS'])")
            List<String> rules
    ) {
        log.info("Tool: getComplianceRules - rules={}", rules);

        if (rules == null || rules.isEmpty()) {
            throw new IllegalArgumentException("At least one rule name is required");
        }
        return flavorService.findComplianceByRules(rules);
    }

    @McpTool(description = """
        Get agent/subagent configuration for a specific use case.
        Use this to configure AI assistants for specific development workflows.
        """)
    public FlavorDto getAgentConfiguration(
            @McpToolParam(description = "Use case identifier (e.g., 'backend-development', 'ui-development', 'testing')")
            String useCase
    ) {
        log.info("Tool: getAgentConfiguration - useCase={}", useCase);

        return flavorService.findAgentConfigurationByUseCase(useCase)
            .orElseThrow(() -> new IllegalArgumentException("No agent configuration found for use case: " + useCase));
    }

    @McpTool(description = """
        Get project initialization template for a specific use case.
        Use this when setting up new projects to ensure consistent scaffolding.
        """)
    public FlavorDto getProjectInitialization(
            @McpToolParam(description = "Use case identifier (e.g., 'microservice', 'api-gateway', 'monolith')")
            String useCase
    ) {
        log.info("Tool: getProjectInitialization - useCase={}", useCase);

        return flavorService.findInitializationByUseCase(useCase)
            .orElseThrow(() -> new IllegalArgumentException("No initialization template found for use case: " + useCase));
    }

    @McpTool(description = """
        List all available flavor categories with counts of active flavors in each.
        """)
    public CategoryStatsDto listFlavorCategories() {
        log.info("Tool: listFlavorCategories");

        return flavorService.getStatistics();
    }
}
