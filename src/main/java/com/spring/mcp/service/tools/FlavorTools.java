package com.spring.mcp.service.tools;

import com.spring.mcp.config.EmbeddingProperties;
import com.spring.mcp.model.dto.flavor.CategoryStatsDto;
import com.spring.mcp.model.dto.flavor.FlavorDto;
import com.spring.mcp.model.dto.flavor.FlavorSummaryDto;
import com.spring.mcp.model.enums.FlavorCategory;
import com.spring.mcp.security.SecurityContextHelper;
import com.spring.mcp.service.FlavorGroupService;
import com.spring.mcp.service.FlavorService;
import com.spring.mcp.service.embedding.HybridSearchService;
import lombok.extern.slf4j.Slf4j;
import org.springaicommunity.mcp.annotation.McpTool;
import org.springaicommunity.mcp.annotation.McpToolParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

/**
 * MCP Tools for Flavors - company guidelines, architecture patterns,
 * compliance rules, agent configurations, and project initialization templates.
 *
 * Uses @McpTool annotation for Spring AI MCP Server auto-discovery.
 * This service is only enabled when the flavors feature is enabled.
 * <p>
 * <strong>Security (v1.4.3+):</strong>
 * All flavor tools filter results based on API key group membership:
 * <ul>
 *   <li>Unassigned flavors (not in any group) are visible to everyone</li>
 *   <li>Flavors in PUBLIC groups (no members) are visible to everyone</li>
 *   <li>Flavors in PRIVATE groups (has members) are visible only to API keys that are members</li>
 *   <li>Flavors in INACTIVE groups are completely hidden</li>
 * </ul>
 * <p>
 * For group-specific operations, use the FlavorGroupTools:
 * <ul>
 *   <li>{@code listFlavorGroups} - List accessible flavor groups</li>
 *   <li>{@code getFlavorsGroup} - Get all flavors in a specific group</li>
 *   <li>{@code getFlavorGroupStatistics} - Get group statistics</li>
 * </ul>
 *
 * @author Spring MCP Server
 * @version 1.4.3
 * @since 2025-11-30
 */
@Service
@Slf4j
@ConditionalOnProperty(name = "mcp.features.flavors.enabled", havingValue = "true", matchIfMissing = true)
public class FlavorTools {

    private final FlavorService flavorService;
    private final FlavorGroupService flavorGroupService;
    private final SecurityContextHelper securityContextHelper;
    private final EmbeddingProperties embeddingProperties;

    // Optional: Only injected when embeddings feature is enabled
    @Autowired(required = false)
    private HybridSearchService hybridSearchService;

    public FlavorTools(FlavorService flavorService,
                       FlavorGroupService flavorGroupService,
                       SecurityContextHelper securityContextHelper,
                       EmbeddingProperties embeddingProperties) {
        this.flavorService = flavorService;
        this.flavorGroupService = flavorGroupService;
        this.securityContextHelper = securityContextHelper;
        this.embeddingProperties = embeddingProperties;
    }

    @McpTool(description = """
        Search company flavors (guidelines, architecture patterns, compliance rules, agent configurations).
        Returns summaries matching the search criteria.
        When embeddings are enabled, uses hybrid search (keyword + semantic) for better results.
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
        Long apiKeyId = securityContextHelper.getCurrentApiKeyId();
        log.info("Tool: searchFlavors - query={}, category={}, tags={}, limit={}, apiKeyId={}",
                query, category, tags, limit, apiKeyId);

        FlavorCategory cat = category != null ? FlavorCategory.fromString(category) : null;
        int maxResults = limit != null ? Math.min(limit, 50) : 10;

        // Get accessible flavor IDs for this API key
        Set<Long> accessibleIds = flavorGroupService.getAccessibleFlavorIdsForApiKey(apiKeyId);

        // Use hybrid search when embeddings are enabled and no filters are applied
        boolean useHybridSearch = embeddingProperties.isEnabled()
                && hybridSearchService != null
                && query != null && !query.isBlank()
                && cat == null
                && (tags == null || tags.isEmpty());

        List<FlavorSummaryDto> results;
        if (useHybridSearch) {
            log.debug("Using hybrid search (keyword + semantic) for flavors: {}", query);
            List<HybridSearchService.SearchResult> hybridResults =
                    hybridSearchService.searchFlavors(query, maxResults);

            // Convert hybrid results to FlavorSummaryDtos
            List<Long> ids = hybridResults.stream()
                    .map(HybridSearchService.SearchResult::id)
                    .toList();

            results = flavorService.getByIds(ids);
        } else {
            // Fall back to traditional search
            results = flavorService.search(query, cat, tags, maxResults);
        }

        // Filter to only accessible flavors
        return results.stream()
                .filter(f -> accessibleIds.contains(f.getId()))
                .toList();
    }

    @McpTool(description = """
        Get complete flavor content by its unique name. Returns full markdown content and metadata.
        """)
    public FlavorDto getFlavorByName(
            @McpToolParam(description = "Unique name of the flavor (e.g., 'hexagonal-spring-boot')")
            String uniqueName
    ) {
        Long apiKeyId = securityContextHelper.getCurrentApiKeyId();
        log.info("Tool: getFlavorByName - uniqueName={}, apiKeyId={}", uniqueName, apiKeyId);

        FlavorDto flavor = flavorService.findByUniqueName(uniqueName)
            .orElseThrow(() -> new IllegalArgumentException("Flavor not found: " + uniqueName));

        // Security check: verify API key can access this flavor
        if (!flavorGroupService.canApiKeyAccessFlavor(flavor.getId(), apiKeyId)) {
            throw new IllegalArgumentException("Flavor not found: " + uniqueName);
        }

        return flavor;
    }

    @McpTool(description = """
        List all active flavors in a specific category.
        """)
    public List<FlavorDto> getFlavorsByCategory(
            @McpToolParam(description = "Category: ARCHITECTURE, COMPLIANCE, AGENTS, INITIALIZATION, or GENERAL")
            String category
    ) {
        Long apiKeyId = securityContextHelper.getCurrentApiKeyId();
        log.info("Tool: getFlavorsByCategory - category={}, apiKeyId={}", category, apiKeyId);

        FlavorCategory cat = FlavorCategory.fromString(category);
        if (cat == null) {
            throw new IllegalArgumentException("Invalid category: " + category);
        }

        // Get accessible flavor IDs for this API key
        Set<Long> accessibleIds = flavorGroupService.getAccessibleFlavorIdsForApiKey(apiKeyId);

        // Filter to only accessible flavors
        return flavorService.findByCategory(cat).stream()
                .filter(f -> accessibleIds.contains(f.getId()))
                .toList();
    }

    @McpTool(description = """
        Get architecture flavors relevant to specific technologies.
        Use this when you need architectural guidance for a particular tech stack.
        """)
    public List<FlavorDto> getArchitecturePatterns(
            @McpToolParam(description = "Technology slugs (e.g., ['spring-boot', 'kafka', 'jpa'])")
            List<String> slugs
    ) {
        Long apiKeyId = securityContextHelper.getCurrentApiKeyId();
        log.info("Tool: getArchitecturePatterns - slugs={}, apiKeyId={}", slugs, apiKeyId);

        if (slugs == null || slugs.isEmpty()) {
            throw new IllegalArgumentException("At least one technology slug is required");
        }

        // Get accessible flavor IDs for this API key
        Set<Long> accessibleIds = flavorGroupService.getAccessibleFlavorIdsForApiKey(apiKeyId);

        // Filter to only accessible flavors
        return flavorService.findArchitectureByTechnologies(slugs).stream()
                .filter(f -> accessibleIds.contains(f.getId()))
                .toList();
    }

    @McpTool(description = """
        Get compliance flavors by rule names or framework identifiers.
        Use this when you need to ensure code meets regulatory requirements.
        """)
    public List<FlavorDto> getComplianceRules(
            @McpToolParam(description = "Rule names or framework identifiers (e.g., ['GDPR', 'SOC2', 'PCI-DSS'])")
            List<String> rules
    ) {
        Long apiKeyId = securityContextHelper.getCurrentApiKeyId();
        log.info("Tool: getComplianceRules - rules={}, apiKeyId={}", rules, apiKeyId);

        if (rules == null || rules.isEmpty()) {
            throw new IllegalArgumentException("At least one rule name is required");
        }

        // Get accessible flavor IDs for this API key
        Set<Long> accessibleIds = flavorGroupService.getAccessibleFlavorIdsForApiKey(apiKeyId);

        // Filter to only accessible flavors
        return flavorService.findComplianceByRules(rules).stream()
                .filter(f -> accessibleIds.contains(f.getId()))
                .toList();
    }

    @McpTool(description = """
        Get agent/subagent configuration for a specific use case.
        Use this to configure AI assistants for specific development workflows.
        """)
    public FlavorDto getAgentConfiguration(
            @McpToolParam(description = "Use case identifier (e.g., 'backend-development', 'ui-development', 'testing')")
            String useCase
    ) {
        Long apiKeyId = securityContextHelper.getCurrentApiKeyId();
        log.info("Tool: getAgentConfiguration - useCase={}, apiKeyId={}", useCase, apiKeyId);

        FlavorDto flavor = flavorService.findAgentConfigurationByUseCase(useCase)
            .orElseThrow(() -> new IllegalArgumentException("No agent configuration found for use case: " + useCase));

        // Security check: verify API key can access this flavor
        if (!flavorGroupService.canApiKeyAccessFlavor(flavor.getId(), apiKeyId)) {
            throw new IllegalArgumentException("No agent configuration found for use case: " + useCase);
        }

        return flavor;
    }

    @McpTool(description = """
        Get project initialization template for a specific use case.
        Use this when setting up new projects to ensure consistent scaffolding.
        """)
    public FlavorDto getProjectInitialization(
            @McpToolParam(description = "Use case identifier (e.g., 'microservice', 'api-gateway', 'monolith')")
            String useCase
    ) {
        Long apiKeyId = securityContextHelper.getCurrentApiKeyId();
        log.info("Tool: getProjectInitialization - useCase={}, apiKeyId={}", useCase, apiKeyId);

        FlavorDto flavor = flavorService.findInitializationByUseCase(useCase)
            .orElseThrow(() -> new IllegalArgumentException("No initialization template found for use case: " + useCase));

        // Security check: verify API key can access this flavor
        if (!flavorGroupService.canApiKeyAccessFlavor(flavor.getId(), apiKeyId)) {
            throw new IllegalArgumentException("No initialization template found for use case: " + useCase);
        }

        return flavor;
    }

    @McpTool(description = """
        List all available flavor categories with counts of active flavors in each.
        """)
    public CategoryStatsDto listFlavorCategories() {
        log.info("Tool: listFlavorCategories");

        return flavorService.getStatistics();
    }
}
