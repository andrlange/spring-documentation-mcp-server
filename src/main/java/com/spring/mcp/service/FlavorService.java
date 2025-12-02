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
    /**
     * Import a flavor from markdown content.
     * Parses YAML front matter header if present to extract metadata.
     * Returns the imported FlavorDto along with any warning messages.
     *
     * @param content the markdown content (may include YAML front matter)
     * @param username the user performing the import
     * @return ImportResult containing the FlavorDto and any warning messages
     */
    ImportResult importFromMarkdown(String content, String username);

    /**
     * Export a flavor as markdown.
     *
     * @param id the flavor ID
     * @param includeMetadata if true, prepends YAML front matter header with metadata
     * @return the markdown content
     */
    String exportToMarkdown(Long id, boolean includeMetadata);

    /**
     * Result of import operation, containing the imported flavor and any warnings.
     */
    record ImportResult(FlavorDto flavor, String warningMessage) {}

    // Validation
    boolean isUniqueNameAvailable(String uniqueName, Long excludeId);
}
