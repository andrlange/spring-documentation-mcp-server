package com.spring.mcp.config;

import org.springframework.context.annotation.Configuration;

/**
 * MCP Server Configuration
 *
 * With Spring AI 1.1.0, MCP tools are automatically discovered via the
 * McpServerAnnotationScannerAutoConfiguration. Beans with @McpTool annotated methods
 * are automatically registered as MCP tools.
 *
 * Tool classes:
 * - SpringDocumentationTools: 10 documentation tools (always available)
 * - MigrationTools: 7 OpenRewrite migration tools (optional, when enabled)
 * - LanguageEvolutionTools: 6 language evolution tools (optional, when enabled)
 * - FlavorTools: 8 flavor/guidelines tools (optional, when enabled)
 *
 * @see <a href="https://docs.spring.io/spring-ai/reference/api/mcp/mcp-annotations-server.html">Spring AI MCP Annotations</a>
 */
@Configuration
public class McpConfig {

    // No manual tool registration needed with Spring AI 1.1.0
    // @McpTool annotated beans are auto-discovered by McpServerAnnotationScannerAutoConfiguration

}
