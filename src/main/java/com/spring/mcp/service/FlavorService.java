package com.spring.mcp.service;

import com.spring.mcp.model.dto.flavor.CategoryStatsDto;
import com.spring.mcp.model.dto.flavor.FlavorDto;
import com.spring.mcp.model.dto.flavor.FlavorSummaryDto;
import com.spring.mcp.model.enums.FlavorCategory;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Service interface for Flavor operations.
 *
 * @author Spring MCP Server
 * @version 1.2.0
 * @since 2025-11-30
 */
public interface FlavorService {

    // CRUD operations
    FlavorDto create(FlavorDto flavorDto, String username);
    FlavorDto update(Long id, FlavorDto flavorDto, String username);
    void delete(Long id);
    void toggleActive(Long id, boolean active, String username);

    // Retrieval
    Optional<FlavorDto> findById(Long id);
    Optional<FlavorDto> findByUniqueName(String uniqueName);
    List<FlavorDto> findAll();
    List<FlavorDto> findAllActive();
    List<FlavorDto> findByCategory(FlavorCategory category);

    // Search
    List<FlavorSummaryDto> search(String query, FlavorCategory category, List<String> tags, int limit);
    List<FlavorDto> searchByTags(List<String> tags);

    // Category-specific queries (for MCP tools)
    List<FlavorDto> findArchitectureByTechnologies(List<String> slugs);
    List<FlavorDto> findComplianceByRules(List<String> rules);
    Optional<FlavorDto> findAgentConfigurationByUseCase(String useCase);
    Optional<FlavorDto> findInitializationByUseCase(String useCase);

    // Statistics
    Map<FlavorCategory, Long> getCategoryCounts();
    CategoryStatsDto getStatistics();

    // Import/Export
    FlavorDto importFromMarkdown(String content, FlavorCategory category, String username);
    String exportToMarkdown(Long id);

    // Validation
    boolean isUniqueNameAvailable(String uniqueName, Long excludeId);
}
