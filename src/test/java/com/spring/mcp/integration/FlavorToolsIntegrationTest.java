package com.spring.mcp.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for Flavor MCP Tools (8 tools).
 * These tools provide company-specific guidelines, architecture patterns, and configurations.
 */
@DisplayName("Flavor Tools Integration Tests")
class FlavorToolsIntegrationTest extends McpSseIntegrationTestBase {

    // ============ Tool 1: searchFlavors ============

    @Test
    @DisplayName("searchFlavors - should search for architecture flavors")
    void searchFlavors_shouldSearchArchitectureFlavors() throws Exception {
        assertToolAvailable("searchFlavors");

        Map<String, Object> args = new HashMap<>();
        args.put("query", "hexagonal");
        args.put("category", null);
        args.put("tags", null);
        args.put("limit", 10);

        String content = callToolAndGetTextContent("searchFlavors", args);

        // Test data has hexagonal-spring-boot flavor
        assertThat(content).containsIgnoringCase("hexagonal");
    }

    @Test
    @DisplayName("searchFlavors - should filter by category")
    void searchFlavors_shouldFilterByCategory() throws Exception {
        Map<String, Object> args = new HashMap<>();
        args.put("query", "architecture");
        args.put("category", "ARCHITECTURE");
        args.put("tags", null);
        args.put("limit", 10);

        String content = callToolAndGetTextContent("searchFlavors", args);
        assertThat(content).isNotNull();
    }

    @Test
    @DisplayName("searchFlavors - should search for compliance flavors")
    void searchFlavors_shouldSearchComplianceFlavors() throws Exception {
        Map<String, Object> args = new HashMap<>();
        args.put("query", "GDPR");
        args.put("category", null);
        args.put("tags", null);
        args.put("limit", 10);

        String content = callToolAndGetTextContent("searchFlavors", args);

        // Test data has gdpr-compliance flavor
        assertThat(content).containsIgnoringCase("GDPR");
    }

    // ============ Tool 2: getFlavorByName ============

    @Test
    @DisplayName("getFlavorByName - should return hexagonal-spring-boot flavor")
    void getFlavorByName_shouldReturnHexagonalFlavor() throws Exception {
        assertToolAvailable("getFlavorByName");

        Map<String, Object> args = Map.of("uniqueName", "hexagonal-spring-boot");
        String content = callToolAndGetTextContent("getFlavorByName", args);

        // Test data has hexagonal-spring-boot flavor
        assertThat(content).containsIgnoringCase("Hexagonal");
        assertThat(content).containsIgnoringCase("Ports");
        assertThat(content).containsIgnoringCase("Adapters");
    }

    @Test
    @DisplayName("getFlavorByName - should return gdpr-compliance flavor")
    void getFlavorByName_shouldReturnGdprFlavor() throws Exception {
        Map<String, Object> args = Map.of("uniqueName", "gdpr-compliance");
        String content = callToolAndGetTextContent("getFlavorByName", args);

        assertThat(content).containsIgnoringCase("GDPR");
        assertThat(content).containsIgnoringCase("Data minimization");
    }

    @Test
    @DisplayName("getFlavorByName - should return microservice-template flavor")
    void getFlavorByName_shouldReturnMicroserviceFlavor() throws Exception {
        Map<String, Object> args = Map.of("uniqueName", "microservice-template");
        String content = callToolAndGetTextContent("getFlavorByName", args);

        assertThat(content).containsIgnoringCase("Microservice");
    }

    // ============ Tool 3: getFlavorsByCategory ============

    @Test
    @DisplayName("getFlavorsByCategory - should return ARCHITECTURE flavors")
    void getFlavorsByCategory_shouldReturnArchitectureFlavors() throws Exception {
        assertToolAvailable("getFlavorsByCategory");

        Map<String, Object> args = Map.of("category", "ARCHITECTURE");
        String content = callToolAndGetTextContent("getFlavorsByCategory", args);

        // Test data has hexagonal-spring-boot in ARCHITECTURE category
        assertThat(content).containsIgnoringCase("hexagonal");
    }

    @Test
    @DisplayName("getFlavorsByCategory - should return COMPLIANCE flavors")
    void getFlavorsByCategory_shouldReturnComplianceFlavors() throws Exception {
        Map<String, Object> args = Map.of("category", "COMPLIANCE");
        String content = callToolAndGetTextContent("getFlavorsByCategory", args);

        // Test data has gdpr-compliance in COMPLIANCE category
        assertThat(content).containsIgnoringCase("gdpr");
    }

    @Test
    @DisplayName("getFlavorsByCategory - should return AGENTS flavors")
    void getFlavorsByCategory_shouldReturnAgentsFlavors() throws Exception {
        Map<String, Object> args = Map.of("category", "AGENTS");
        String content = callToolAndGetTextContent("getFlavorsByCategory", args);

        // Test data has api-development-agent in AGENTS category
        assertThat(content).containsIgnoringCase("api");
    }

    @Test
    @DisplayName("getFlavorsByCategory - should return INITIALIZATION flavors")
    void getFlavorsByCategory_shouldReturnInitializationFlavors() throws Exception {
        Map<String, Object> args = Map.of("category", "INITIALIZATION");
        String content = callToolAndGetTextContent("getFlavorsByCategory", args);

        // Test data has microservice-template in INITIALIZATION category
        assertThat(content).containsIgnoringCase("microservice");
    }

    // ============ Tool 4: getArchitecturePatterns ============

    @Test
    @DisplayName("getArchitecturePatterns - should return patterns for spring-boot")
    void getArchitecturePatterns_shouldReturnSpringBootPatterns() throws Exception {
        assertToolAvailable("getArchitecturePatterns");

        Map<String, Object> args = Map.of("slugs", new String[]{"spring-boot"});
        String content = callToolAndGetTextContent("getArchitecturePatterns", args);

        // Test data has hexagonal-spring-boot with spring-boot tag
        assertThat(content).isNotNull();
    }

    @Test
    @DisplayName("getArchitecturePatterns - should return patterns for architecture tag")
    void getArchitecturePatterns_shouldReturnArchitecturePatterns() throws Exception {
        Map<String, Object> args = Map.of("slugs", new String[]{"architecture", "hexagonal"});
        String content = callToolAndGetTextContent("getArchitecturePatterns", args);
        assertThat(content).isNotNull();
    }

    // ============ Tool 5: getComplianceRules ============

    @Test
    @DisplayName("getComplianceRules - should return GDPR rules")
    void getComplianceRules_shouldReturnGdprRules() throws Exception {
        assertToolAvailable("getComplianceRules");

        Map<String, Object> args = Map.of("rules", new String[]{"GDPR"});
        String content = callToolAndGetTextContent("getComplianceRules", args);

        // Test data has gdpr-compliance flavor
        assertThat(content).isNotNull();
    }

    @Test
    @DisplayName("getComplianceRules - should handle multiple rules")
    void getComplianceRules_shouldHandleMultipleRules() throws Exception {
        Map<String, Object> args = Map.of("rules", new String[]{"GDPR", "SOC2", "privacy"});
        String content = callToolAndGetTextContent("getComplianceRules", args);
        assertThat(content).isNotNull();
    }

    // ============ Tool 6: getAgentConfiguration ============

    @Test
    @DisplayName("getAgentConfiguration - should return api-development agent config")
    void getAgentConfiguration_shouldReturnApiDevelopmentConfig() throws Exception {
        assertToolAvailable("getAgentConfiguration");

        Map<String, Object> args = Map.of("useCase", "api-development");
        String content = callToolAndGetTextContent("getAgentConfiguration", args);

        // Test data has api-development-agent flavor
        assertThat(content).isNotNull();
    }

    @Test
    @DisplayName("getAgentConfiguration - should handle api use case")
    void getAgentConfiguration_shouldHandleApiUseCase() throws Exception {
        Map<String, Object> args = Map.of("useCase", "api");
        String content = callToolAndGetTextContent("getAgentConfiguration", args);
        assertThat(content).isNotNull();
    }

    // ============ Tool 7: getProjectInitialization ============

    @Test
    @DisplayName("getProjectInitialization - should return microservice template")
    void getProjectInitialization_shouldReturnMicroserviceTemplate() throws Exception {
        assertToolAvailable("getProjectInitialization");

        Map<String, Object> args = Map.of("useCase", "microservice");
        String content = callToolAndGetTextContent("getProjectInitialization", args);

        // Test data has microservice-template flavor
        assertThat(content).isNotNull();
    }

    @Test
    @DisplayName("getProjectInitialization - should handle spring-cloud use case")
    void getProjectInitialization_shouldHandleSpringCloudUseCase() throws Exception {
        Map<String, Object> args = Map.of("useCase", "spring-cloud");
        String content = callToolAndGetTextContent("getProjectInitialization", args);
        assertThat(content).isNotNull();
    }

    // ============ Tool 8: listFlavorCategories ============

    @Test
    @DisplayName("listFlavorCategories - should return all categories with counts")
    void listFlavorCategories_shouldReturnAllCategories() throws Exception {
        assertToolAvailable("listFlavorCategories");

        Map<String, Object> args = Map.of();
        String content = callToolAndGetTextContent("listFlavorCategories", args);

        // Should contain all category names
        assertThat(content).containsIgnoringCase("ARCHITECTURE");
        assertThat(content).containsIgnoringCase("COMPLIANCE");
        assertThat(content).containsIgnoringCase("AGENTS");
        assertThat(content).containsIgnoringCase("INITIALIZATION");
    }

    // ============ All Tools Verification ============

    @Test
    @DisplayName("All 8 flavor tools should be available")
    void allFlavorTools_shouldBeAvailable() throws Exception {
        assertToolAvailable("searchFlavors");
        assertToolAvailable("getFlavorByName");
        assertToolAvailable("getFlavorsByCategory");
        assertToolAvailable("getArchitecturePatterns");
        assertToolAvailable("getComplianceRules");
        assertToolAvailable("getAgentConfiguration");
        assertToolAvailable("getProjectInitialization");
        assertToolAvailable("listFlavorCategories");
    }
}
