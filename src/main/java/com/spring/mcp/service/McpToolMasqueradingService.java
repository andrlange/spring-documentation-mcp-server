package com.spring.mcp.service;

import com.spring.mcp.model.entity.McpTool;
import com.spring.mcp.repository.McpToolRepository;
import io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.spec.McpSchema.Tool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for managing MCP tool visibility and descriptions dynamically.
 *
 * <p>This service enables runtime tool masquerading by:
 * <ul>
 *   <li>Caching full SyncToolSpecification objects (with handlers) at startup</li>
 *   <li>Using cached specifications to add/remove tools at runtime</li>
 *   <li>Synchronizing visibility state between database and MCP server</li>
 * </ul>
 *
 * <p>Implementation notes:
 * <ul>
 *   <li>The MCP SDK's addTool() requires a SyncToolSpecification (with handler)</li>
 *   <li>We cache these specs at startup from the customToolSpecs bean</li>
 *   <li>removeTool() and addTool() operations work correctly with cached specs</li>
 * </ul>
 *
 * @author Spring MCP Server
 * @version 1.6.2
 * @since 2026-01-03
 */
@Service
@Slf4j
public class McpToolMasqueradingService {

    private final McpToolRepository mcpToolRepository;

    // Optional injection - may not be available in test contexts
    @Autowired(required = false)
    private McpSyncServer mcpSyncServer;

    // Inject the tool specifications from McpToolsConfig
    @Autowired(required = false)
    @Qualifier("customToolSpecs")
    private List<SyncToolSpecification> toolSpecifications;

    // Cache of full tool specifications by name (includes handlers)
    private final Map<String, SyncToolSpecification> toolSpecCache = new ConcurrentHashMap<>();

    // Flag to track if we have an active MCP server connection
    private volatile boolean serverAvailable = false;

    // Flag to track if initial masquerading has been applied
    private volatile boolean initialMasqueradingApplied = false;

    // Flag to enable/disable runtime modifications
    private static final boolean RUNTIME_MODIFICATIONS_ENABLED = true;

    public McpToolMasqueradingService(McpToolRepository mcpToolRepository) {
        this.mcpToolRepository = mcpToolRepository;
    }

    /**
     * Initialize the masquerading service after Spring context is fully ready.
     * Uses ContextRefreshedEvent to ensure all beans are initialized.
     */
    @EventListener(ContextRefreshedEvent.class)
    @Order(100) // Run after other initializers
    public void onContextRefreshed() {
        if (mcpSyncServer == null) {
            log.warn("McpSyncServer not available - masquerading will be disabled");
            return;
        }

        serverAvailable = true;
        log.info("MCP Masquerading Service initialized with McpSyncServer");

        // Cache tool specifications
        cacheToolSpecifications();

        // Apply initial masquerading based on database state
        if (!initialMasqueradingApplied && RUNTIME_MODIFICATIONS_ENABLED) {
            applyInitialMasquerading();
            initialMasqueradingApplied = true;
        }
    }

    /**
     * Cache all tool specifications from the customToolSpecs bean.
     * This preserves the full SyncToolSpecification (with handlers) for later use.
     */
    private void cacheToolSpecifications() {
        if (toolSpecifications == null || toolSpecifications.isEmpty()) {
            log.warn("No tool specifications available to cache");
            return;
        }

        for (SyncToolSpecification spec : toolSpecifications) {
            String toolName = spec.tool().name();
            toolSpecCache.put(toolName, spec);
            log.debug("Cached tool specification: {}", toolName);
        }

        log.info("Cached {} tool specifications for masquerading", toolSpecCache.size());
    }

    /**
     * Apply initial masquerading based on database state.
     * Removes tools that are marked as disabled in the database.
     */
    private void applyInitialMasquerading() {
        List<McpTool> disabledTools = mcpToolRepository.findByEnabledFalse();

        if (disabledTools.isEmpty()) {
            log.info("All tools are enabled - no initial masquerading needed");
            return;
        }

        log.info("Applying initial masquerading: {} tools disabled", disabledTools.size());

        for (McpTool tool : disabledTools) {
            try {
                mcpSyncServer.removeTool(tool.getToolName());
                log.debug("Removed disabled tool: {}", tool.getToolName());
            } catch (Exception e) {
                log.warn("Failed to remove tool {}: {}", tool.getToolName(), e.getMessage());
            }
        }

        // Notify clients of the change
        try {
            mcpSyncServer.notifyToolsListChanged();
            log.info("Initial masquerading applied - notified clients");
        } catch (Exception e) {
            log.warn("Failed to notify clients of tool list change: {}", e.getMessage());
        }
    }

    /**
     * Apply tool visibility based on database configuration.
     * Called when a tool's enabled status changes.
     *
     * @param tool the tool entity
     */
    public void applyToolVisibility(McpTool tool) {
        if (!serverAvailable) {
            log.debug("MCP server not available, skipping visibility update for {}", tool.getToolName());
            return;
        }

        String toolName = tool.getToolName();
        String status = tool.getEnabled() ? "enabled" : "disabled";

        if (!RUNTIME_MODIFICATIONS_ENABLED) {
            log.info("Tool visibility preference updated: {} -> {} (database only)", toolName, status);
            log.debug("Runtime modifications disabled - tool {} remains visible to MCP clients", toolName);
            return;
        }

        try {
            if (tool.getEnabled()) {
                // Re-add the tool using cached specification
                enableTool(toolName);
            } else {
                // Remove the tool from MCP server
                disableTool(toolName);
            }

            mcpSyncServer.notifyToolsListChanged();
            log.info("MCP tool visibility updated: {} -> {}", toolName, status);

        } catch (Exception e) {
            log.error("Failed to apply tool visibility for {}: {}", toolName, e.getMessage(), e);
        }
    }

    /**
     * Enable a tool by adding it back to the MCP server.
     *
     * @param toolName the tool name
     */
    private void enableTool(String toolName) {
        SyncToolSpecification spec = toolSpecCache.get(toolName);
        if (spec == null) {
            log.warn("No cached specification for tool {}, cannot enable", toolName);
            return;
        }

        try {
            mcpSyncServer.addTool(spec);
            log.debug("Enabled tool: {}", toolName);
        } catch (Exception e) {
            log.warn("Failed to add tool {} (may already exist): {}", toolName, e.getMessage());
        }
    }

    /**
     * Disable a tool by removing it from the MCP server.
     *
     * @param toolName the tool name
     */
    private void disableTool(String toolName) {
        try {
            mcpSyncServer.removeTool(toolName);
            log.debug("Disabled tool: {}", toolName);
        } catch (Exception e) {
            log.warn("Failed to remove tool {}: {}", toolName, e.getMessage());
        }
    }

    /**
     * Update a tool's description in the MCP server.
     * Only called for enabled tools.
     *
     * <p>NOTE: Description updates require removing and re-adding the tool.
     * The cached specification is modified with the new description.
     *
     * @param tool the tool entity with new description
     */
    public void updateToolDescription(McpTool tool) {
        if (!serverAvailable) {
            log.debug("MCP server not available, skipping description update for {}", tool.getToolName());
            return;
        }

        String toolName = tool.getToolName();
        boolean isModified = !tool.getDescription().equals(tool.getOriginalDescription());

        log.info("Tool description updated: {} (modified={})", toolName, isModified);

        if (!RUNTIME_MODIFICATIONS_ENABLED) {
            log.debug("Runtime modifications disabled - tool {} description not updated in MCP server", toolName);
            return;
        }

        if (!tool.getEnabled()) {
            log.debug("Tool {} is disabled, skipping MCP server description update", toolName);
            return;
        }

        // Get the original cached specification
        SyncToolSpecification originalSpec = toolSpecCache.get(toolName);
        if (originalSpec == null) {
            log.warn("No cached specification for tool {}, cannot update description", toolName);
            return;
        }

        try {
            // Create a new specification with the updated description
            Tool originalTool = originalSpec.tool();
            Tool updatedTool = Tool.builder()
                    .name(toolName)
                    .description(tool.getDescription())
                    .inputSchema(originalTool.inputSchema())
                    .title(originalTool.title())
                    .outputSchema(originalTool.outputSchema())
                    .annotations(originalTool.annotations())
                    .meta(originalTool.meta())
                    .build();

            SyncToolSpecification updatedSpec = new SyncToolSpecification(updatedTool, originalSpec.call());

            // Remove the old tool and add the updated one
            mcpSyncServer.removeTool(toolName);
            mcpSyncServer.addTool(updatedSpec);

            // Update the cache with the new spec
            toolSpecCache.put(toolName, updatedSpec);

            // Notify clients
            mcpSyncServer.notifyToolsListChanged();
            log.info("MCP tool description updated in server: {}", toolName);

        } catch (Exception e) {
            log.error("Failed to update tool description for {}: {}", toolName, e.getMessage(), e);
        }
    }

    /**
     * Apply masquerading for all tools based on database configuration.
     * Called on startup or when full refresh is needed.
     */
    public void applyAllMasquerading() {
        if (!serverAvailable) {
            log.warn("MCP server not available, skipping full masquerading apply");
            return;
        }

        List<McpTool> allTools = mcpToolRepository.findAllOrdered();
        long enabled = allTools.stream().filter(McpTool::getEnabled).count();
        long disabled = allTools.size() - enabled;

        log.info("Tool masquerading status: {} total, {} enabled, {} disabled",
                allTools.size(), enabled, disabled);

        if (!RUNTIME_MODIFICATIONS_ENABLED) {
            log.info("Runtime modifications disabled - all {} tools remain visible to MCP clients", allTools.size());
            return;
        }

        log.info("Applying masquerading for all MCP tools...");

        for (McpTool tool : allTools) {
            try {
                if (tool.getEnabled()) {
                    enableTool(tool.getToolName());
                } else {
                    disableTool(tool.getToolName());
                }
            } catch (Exception e) {
                log.error("Failed to apply masquerading for tool {}: {}", tool.getToolName(), e.getMessage());
            }
        }

        // Notify clients once after all changes
        try {
            mcpSyncServer.notifyToolsListChanged();
        } catch (Exception e) {
            log.warn("Failed to notify clients of tool list change: {}", e.getMessage());
        }

        log.info("MCP masquerading applied: {} enabled, {} disabled", enabled, disabled);
    }

    /**
     * Get the number of cached tool specifications.
     *
     * @return cached spec count
     */
    public int getCachedSpecCount() {
        return toolSpecCache.size();
    }

    /**
     * Check if a tool specification is cached.
     *
     * @param toolName the tool name
     * @return true if cached
     */
    public boolean hasToolSpec(String toolName) {
        return toolSpecCache.containsKey(toolName);
    }

    /**
     * Check if the MCP server is available.
     *
     * @return true if server is available
     */
    public boolean isServerAvailable() {
        return serverAvailable;
    }

    /**
     * Get count of enabled tools.
     *
     * @return enabled tool count
     */
    public long getEnabledToolCount() {
        return mcpToolRepository.countByEnabledTrue();
    }

    /**
     * Get count of disabled tools.
     *
     * @return disabled tool count
     */
    public long getDisabledToolCount() {
        return mcpToolRepository.countByEnabledFalse();
    }
}
