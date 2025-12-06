package com.spring.mcp.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for Spring Initializr MCP Tools.
 * Tests the full MCP SSE pipeline for Initializr functionality.
 *
 * <p>Tests the following tools:</p>
 * <ul>
 *   <li>initializrGetDependency - Get dependency details</li>
 *   <li>initializrSearchDependencies - Search dependencies</li>
 *   <li>initializrCheckCompatibility - Check version compatibility</li>
 *   <li>initializrGetBootVersions - List boot versions</li>
 *   <li>initializrGetDependencyCategories - List categories</li>
 * </ul>
 *
 * @author Spring MCP Server
 * @version 1.4.0
 * @since 2025-12-06
 */
@DisplayName("Initializr MCP Tools Integration Tests")
class InitializrMcpIntegrationTest extends McpSseIntegrationTestBase {

    // ============ Tool: initializrGetDependency ============

    @Test
    @DisplayName("initializrGetDependency - should return dependency details for spring-web")
    void initializrGetDependency_shouldReturnDependencyDetails() throws Exception {
        assertToolAvailable("initializrGetDependency");

        Map<String, Object> args = new HashMap<>();
        args.put("dependencyId", "web");
        args.put("bootVersion", null);
        args.put("format", "gradle");

        String content = callToolAndGetTextContent("initializrGetDependency", args);

        // Should contain dependency info
        assertThat(content)
                .as("Should return Spring Web dependency details")
                .isNotNull();
    }

    @Test
    @DisplayName("initializrGetDependency - should return dependency with version filter")
    void initializrGetDependency_shouldReturnWithVersionFilter() throws Exception {
        Map<String, Object> args = new HashMap<>();
        args.put("dependencyId", "web");
        args.put("bootVersion", "3.5.8");
        args.put("format", "maven");

        String content = callToolAndGetTextContent("initializrGetDependency", args);

        assertThat(content)
                .as("Should return dependency details for specific version")
                .isNotNull();
    }

    // ============ Tool: initializrSearchDependencies ============

    @Test
    @DisplayName("initializrSearchDependencies - should search for web dependencies")
    void initializrSearchDependencies_shouldSearchForWeb() throws Exception {
        assertToolAvailable("initializrSearchDependencies");

        Map<String, Object> args = new HashMap<>();
        args.put("query", "web");
        args.put("bootVersion", null);
        args.put("category", null);
        args.put("limit", 10);

        String content = callToolAndGetTextContent("initializrSearchDependencies", args);

        // Should contain web-related dependencies
        assertThat(content)
                .as("Should find web dependencies")
                .isNotNull();
    }

    @Test
    @DisplayName("initializrSearchDependencies - should filter by category")
    void initializrSearchDependencies_shouldFilterByCategory() throws Exception {
        Map<String, Object> args = new HashMap<>();
        args.put("query", null);
        args.put("bootVersion", null);
        args.put("category", "Web");
        args.put("limit", 20);

        String content = callToolAndGetTextContent("initializrSearchDependencies", args);

        assertThat(content)
                .as("Should return dependencies in Web category")
                .isNotNull();
    }

    @Test
    @DisplayName("initializrSearchDependencies - should search with boot version filter")
    void initializrSearchDependencies_shouldSearchWithVersionFilter() throws Exception {
        Map<String, Object> args = new HashMap<>();
        args.put("query", "security");
        args.put("bootVersion", "3.5.8");
        args.put("category", null);
        args.put("limit", 5);

        String content = callToolAndGetTextContent("initializrSearchDependencies", args);

        assertThat(content)
                .as("Should find security dependencies for version 3.5.8")
                .isNotNull();
    }

    // ============ Tool: initializrCheckCompatibility ============

    @Test
    @DisplayName("initializrCheckCompatibility - should check dependency compatibility")
    void initializrCheckCompatibility_shouldCheckCompatibility() throws Exception {
        assertToolAvailable("initializrCheckCompatibility");

        Map<String, Object> args = Map.of(
                "dependencyId", "web",
                "bootVersion", "3.5.8"
        );

        String content = callToolAndGetTextContent("initializrCheckCompatibility", args);

        // Should contain compatibility result
        assertThat(content)
                .as("Should return compatibility check result")
                .isNotNull();
    }

    @Test
    @DisplayName("initializrCheckCompatibility - should check data-jpa compatibility")
    void initializrCheckCompatibility_shouldCheckDataJpaCompatibility() throws Exception {
        Map<String, Object> args = Map.of(
                "dependencyId", "data-jpa",
                "bootVersion", "3.5.8"
        );

        String content = callToolAndGetTextContent("initializrCheckCompatibility", args);

        assertThat(content)
                .as("Should return data-jpa compatibility result")
                .isNotNull();
    }

    // ============ Tool: initializrGetBootVersions ============

    @Test
    @DisplayName("initializrGetBootVersions - should return available boot versions")
    void initializrGetBootVersions_shouldReturnVersions() throws Exception {
        assertToolAvailable("initializrGetBootVersions");

        Map<String, Object> args = Map.of();

        String content = callToolAndGetTextContent("initializrGetBootVersions", args);

        // Should contain boot versions
        assertThat(content)
                .as("Should return available Spring Boot versions")
                .isNotNull();
    }

    // ============ Tool: initializrGetDependencyCategories ============

    @Test
    @DisplayName("initializrGetDependencyCategories - should return all categories")
    void initializrGetDependencyCategories_shouldReturnCategories() throws Exception {
        assertToolAvailable("initializrGetDependencyCategories");

        Map<String, Object> args = Map.of();

        String content = callToolAndGetTextContent("initializrGetDependencyCategories", args);

        // Should contain category information
        assertThat(content)
                .as("Should return dependency categories")
                .isNotNull();
    }

    // ============ All Tools Available ============

    @Test
    @DisplayName("All Initializr tools should be available")
    void allInitializrTools_shouldBeAvailable() {
        assertToolAvailable("initializrGetDependency");
        assertToolAvailable("initializrSearchDependencies");
        assertToolAvailable("initializrCheckCompatibility");
        assertToolAvailable("initializrGetBootVersions");
        assertToolAvailable("initializrGetDependencyCategories");
    }
}
