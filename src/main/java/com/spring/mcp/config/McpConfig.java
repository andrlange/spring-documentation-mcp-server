package com.spring.mcp.config;

import com.spring.mcp.service.tools.LanguageEvolutionTools;
import com.spring.mcp.service.tools.MigrationTools;
import com.spring.mcp.service.tools.SpringDocumentationTools;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * MCP Server Configuration
 * Registers MCP tools for Spring AI MCP Server auto-discovery
 *
 * This configuration follows the official Spring AI MCP Server pattern:
 * - Uses MethodToolCallbackProvider for @Tool annotated methods
 * - Registers tool objects for auto-discovery
 * - Enables automatic tool registration with the MCP server
 *
 * @see <a href="https://docs.spring.io/spring-ai/reference/api/mcp/mcp-server-boot-starter-docs.html">Spring AI MCP Server Documentation</a>
 */
@Configuration
public class McpConfig {

    /**
     * Register all MCP tools for the server.
     * <p>
     * The MethodToolCallbackProvider scans the provided tool objects for @Tool annotated methods
     * and automatically registers them with the MCP server. All methods annotated with @Tool
     * will be exposed as MCP tools.
     * <p>
     * Tools registered:
     * - SpringDocumentationTools: 10 documentation tools (always available)
     * - MigrationTools: 7 OpenRewrite migration tools (optional, when enabled)
     * - LanguageEvolutionTools: 6 language evolution tools (optional, when enabled)
     *
     * @param springDocumentationTools the Spring Documentation tools service
     * @param migrationTools optional Migration tools service (when OpenRewrite feature is enabled)
     * @param languageEvolutionTools optional Language Evolution tools (when feature is enabled)
     * @return ToolCallbackProvider configured with all available tools
     */
    @Bean
    public ToolCallbackProvider toolCallbackProvider(
            SpringDocumentationTools springDocumentationTools,
            Optional<MigrationTools> migrationTools,
            Optional<LanguageEvolutionTools> languageEvolutionTools) {

        List<Object> toolObjects = new ArrayList<>();
        toolObjects.add(springDocumentationTools);

        // Add migration tools if OpenRewrite feature is enabled
        migrationTools.ifPresent(toolObjects::add);

        // Add language evolution tools if feature is enabled
        languageEvolutionTools.ifPresent(toolObjects::add);

        return MethodToolCallbackProvider.builder()
            .toolObjects(toolObjects.toArray())
            .build();
    }
}
