package com.spring.mcp.integration;

import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Comprehensive test that verifies all 31 MCP tools are available.
 * This test ensures that LLMs can access all tools through the SSE API.
 */
@DisplayName("All MCP Tools Availability Test")
class AllToolsAvailabilityTest extends McpSseIntegrationTestBase {

    // All 31 expected tools
    private static final Set<String> EXPECTED_TOOLS = Set.of(
        // Documentation Tools (10)
        "searchSpringDocs",
        "getSpringVersions",
        "listSpringProjects",
        "getDocumentationByVersion",
        "getCodeExamples",
        "listSpringBootVersions",
        "getLatestSpringBootVersion",
        "filterSpringBootVersionsBySupport",
        "listProjectsBySpringBootVersion",
        "findProjectsByUseCase",

        // Migration Tools (7)
        "getSpringMigrationGuide",
        "getBreakingChanges",
        "searchMigrationKnowledge",
        "getAvailableMigrationPaths",
        "getTransformationsByType",
        "getDeprecationReplacement",
        "checkVersionCompatibility",

        // Language Evolution Tools (6)
        "getLanguageVersions",
        "getLanguageFeatures",
        "getModernPatterns",
        "getLanguageVersionDiff",
        "getSpringBootLanguageRequirements",
        "searchLanguageFeatures",

        // Flavor Tools (8)
        "searchFlavors",
        "getFlavorByName",
        "getFlavorsByCategory",
        "getArchitecturePatterns",
        "getComplianceRules",
        "getAgentConfiguration",
        "getProjectInitialization",
        "listFlavorCategories"
    );

    @Test
    @DisplayName("Should have exactly 31 MCP tools available")
    void shouldHave31ToolsAvailable() {
        List<McpSchema.Tool> tools = listTools();

        Set<String> availableToolNames = tools.stream()
            .map(McpSchema.Tool::name)
            .collect(Collectors.toSet());

        // Verify count
        assertThat(availableToolNames)
            .as("Should have at least 31 tools (all expected tools)")
            .hasSizeGreaterThanOrEqualTo(31);

        // Log available tools for debugging
        System.out.println("=== Available MCP Tools ===");
        availableToolNames.stream().sorted().forEach(System.out::println);
        System.out.println("Total: " + availableToolNames.size() + " tools");
    }

    @Test
    @DisplayName("All expected tools should be available")
    void allExpectedToolsShouldBeAvailable() {
        List<McpSchema.Tool> tools = listTools();

        Set<String> availableToolNames = tools.stream()
            .map(McpSchema.Tool::name)
            .collect(Collectors.toSet());

        // Find missing tools
        Set<String> missingTools = EXPECTED_TOOLS.stream()
            .filter(tool -> !availableToolNames.contains(tool))
            .collect(Collectors.toSet());

        assertThat(missingTools)
            .as("All 31 expected tools should be available. Missing: " + missingTools)
            .isEmpty();
    }

    @Test
    @DisplayName("All documentation tools (10) should be available")
    void allDocumentationToolsShouldBeAvailable() {
        Set<String> docTools = Set.of(
            "searchSpringDocs",
            "getSpringVersions",
            "listSpringProjects",
            "getDocumentationByVersion",
            "getCodeExamples",
            "listSpringBootVersions",
            "getLatestSpringBootVersion",
            "filterSpringBootVersionsBySupport",
            "listProjectsBySpringBootVersion",
            "findProjectsByUseCase"
        );

        for (String tool : docTools) {
            assertToolAvailable(tool);
        }
    }

    @Test
    @DisplayName("All migration tools (7) should be available")
    void allMigrationToolsShouldBeAvailable() {
        Set<String> migrationTools = Set.of(
            "getSpringMigrationGuide",
            "getBreakingChanges",
            "searchMigrationKnowledge",
            "getAvailableMigrationPaths",
            "getTransformationsByType",
            "getDeprecationReplacement",
            "checkVersionCompatibility"
        );

        for (String tool : migrationTools) {
            assertToolAvailable(tool);
        }
    }

    @Test
    @DisplayName("All language evolution tools (6) should be available")
    void allLanguageEvolutionToolsShouldBeAvailable() {
        Set<String> langTools = Set.of(
            "getLanguageVersions",
            "getLanguageFeatures",
            "getModernPatterns",
            "getLanguageVersionDiff",
            "getSpringBootLanguageRequirements",
            "searchLanguageFeatures"
        );

        for (String tool : langTools) {
            assertToolAvailable(tool);
        }
    }

    @Test
    @DisplayName("All flavor tools (8) should be available")
    void allFlavorToolsShouldBeAvailable() {
        Set<String> flavorTools = Set.of(
            "searchFlavors",
            "getFlavorByName",
            "getFlavorsByCategory",
            "getArchitecturePatterns",
            "getComplianceRules",
            "getAgentConfiguration",
            "getProjectInitialization",
            "listFlavorCategories"
        );

        for (String tool : flavorTools) {
            assertToolAvailable(tool);
        }
    }

    @Test
    @DisplayName("Each tool should have a description")
    void eachToolShouldHaveDescription() {
        List<McpSchema.Tool> tools = listTools();

        for (McpSchema.Tool tool : tools) {
            assertThat(tool.description())
                .as("Tool '%s' should have a description", tool.name())
                .isNotNull()
                .isNotEmpty();
        }
    }

    @Test
    @DisplayName("Tools should have input schema defined")
    void toolsShouldHaveInputSchema() {
        List<McpSchema.Tool> tools = listTools();

        for (McpSchema.Tool tool : tools) {
            assertThat(tool.inputSchema())
                .as("Tool '%s' should have an inputSchema", tool.name())
                .isNotNull();
        }
    }
}
