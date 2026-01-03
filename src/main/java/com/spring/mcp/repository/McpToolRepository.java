package com.spring.mcp.repository;

import com.spring.mcp.model.entity.McpTool;
import com.spring.mcp.model.enums.McpToolGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for McpTool entity operations.
 * Provides data access for MCP tool masquerading functionality.
 *
 * @author Spring MCP Server
 * @version 1.6.2
 * @since 2026-01-03
 */
@Repository
public interface McpToolRepository extends JpaRepository<McpTool, Long> {

    /**
     * Find a tool by its unique name.
     *
     * @param toolName the tool name
     * @return optional containing the tool if found
     */
    Optional<McpTool> findByToolName(String toolName);

    /**
     * Find all tools in a specific group, ordered by display order.
     *
     * @param toolGroup the tool group
     * @return list of tools in the group
     */
    List<McpTool> findByToolGroupOrderByDisplayOrderAsc(McpToolGroup toolGroup);

    /**
     * Find all enabled tools, ordered by group and display order.
     *
     * @return list of enabled tools
     */
    List<McpTool> findByEnabledTrueOrderByToolGroupAscDisplayOrderAsc();

    /**
     * Find all tools ordered by group and display order.
     *
     * @return list of all tools
     */
    @Query("SELECT t FROM McpTool t ORDER BY t.toolGroup ASC, t.displayOrder ASC")
    List<McpTool> findAllOrdered();

    /**
     * Get group statistics: group, total count, enabled count.
     *
     * @return list of Object arrays with [McpToolGroup, Long totalCount, Long enabledCount]
     */
    @Query("SELECT t.toolGroup, COUNT(t), SUM(CASE WHEN t.enabled = true THEN 1 ELSE 0 END) " +
           "FROM McpTool t GROUP BY t.toolGroup ORDER BY t.toolGroup")
    List<Object[]> getGroupStatistics();

    /**
     * Update enabled status for all tools in a group.
     *
     * @param toolGroup the tool group
     * @param enabled   the new enabled status
     * @return number of rows updated
     */
    @Modifying
    @Query("UPDATE McpTool t SET t.enabled = :enabled, t.updatedBy = :updatedBy WHERE t.toolGroup = :toolGroup")
    int updateGroupEnabled(@Param("toolGroup") McpToolGroup toolGroup,
                           @Param("enabled") boolean enabled,
                           @Param("updatedBy") String updatedBy);

    /**
     * Check if a tool exists by name.
     *
     * @param toolName the tool name
     * @return true if tool exists
     */
    boolean existsByToolName(String toolName);

    /**
     * Count all enabled tools.
     *
     * @return count of enabled tools
     */
    long countByEnabledTrue();

    /**
     * Count all disabled tools.
     *
     * @return count of disabled tools
     */
    long countByEnabledFalse();

    /**
     * Find all disabled tools.
     *
     * @return list of disabled tools
     */
    List<McpTool> findByEnabledFalse();

    /**
     * Count enabled tools in a specific group.
     *
     * @param toolGroup the tool group
     * @return count of enabled tools in the group
     */
    long countByToolGroupAndEnabledTrue(McpToolGroup toolGroup);

    /**
     * Count tools in a specific group.
     *
     * @param toolGroup the tool group
     * @return count of tools in the group
     */
    long countByToolGroup(McpToolGroup toolGroup);

    /**
     * Find all tools with modified descriptions.
     *
     * @return list of tools where description differs from original
     */
    @Query("SELECT t FROM McpTool t WHERE t.description <> t.originalDescription ORDER BY t.toolGroup, t.displayOrder")
    List<McpTool> findAllWithModifiedDescriptions();

    /**
     * Count tools with modified descriptions.
     *
     * @return count of modified tools
     */
    @Query("SELECT COUNT(t) FROM McpTool t WHERE t.description <> t.originalDescription")
    long countModifiedDescriptions();

    /**
     * Find tools by group with modified description indicator.
     *
     * @param toolGroup the tool group
     * @return list of tools
     */
    List<McpTool> findByToolGroup(McpToolGroup toolGroup);
}
