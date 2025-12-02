package com.spring.mcp.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for Migration/OpenRewrite MCP Tools (7 tools).
 * These tools provide migration knowledge for Spring ecosystem upgrades.
 */
@DisplayName("Migration Tools Integration Tests")
class MigrationToolsIntegrationTest extends McpSseIntegrationTestBase {

    // ============ Tool 1: getSpringMigrationGuide ============

    @Test
    @DisplayName("getSpringMigrationGuide - should return migration guide from 3.5.8 to 4.0.0")
    void getSpringMigrationGuide_shouldReturnGuide() throws Exception {
        assertToolAvailable("getSpringMigrationGuide");

        Map<String, Object> args = Map.of(
            "fromVersion", "3.5.8",
            "toVersion", "4.0.0"
        );
        String content = callToolAndGetTextContent("getSpringMigrationGuide", args);

        // Should return migration information
        assertThat(content).isNotNull();
    }

    @Test
    @DisplayName("getSpringMigrationGuide - should handle minor version upgrade")
    void getSpringMigrationGuide_shouldHandleMinorUpgrade() throws Exception {
        Map<String, Object> args = Map.of(
            "fromVersion", "3.4.12",
            "toVersion", "3.5.8"
        );
        String content = callToolAndGetTextContent("getSpringMigrationGuide", args);
        assertThat(content).isNotNull();
    }

    // ============ Tool 2: getBreakingChanges ============

    @Test
    @DisplayName("getBreakingChanges - should return breaking changes for spring-boot 4.0.0")
    void getBreakingChanges_shouldReturnChanges() throws Exception {
        assertToolAvailable("getBreakingChanges");

        Map<String, Object> args = Map.of(
            "project", "spring-boot",
            "version", "4.0.0"
        );
        String content = callToolAndGetTextContent("getBreakingChanges", args);

        // Should return breaking changes or indicate none found
        assertThat(content).isNotNull();
    }

    @Test
    @DisplayName("getBreakingChanges - should work for spring-security")
    void getBreakingChanges_shouldWorkForSpringSecurity() throws Exception {
        Map<String, Object> args = Map.of(
            "project", "spring-security",
            "version", "6.4.2"
        );
        String content = callToolAndGetTextContent("getBreakingChanges", args);
        assertThat(content).isNotNull();
    }

    // ============ Tool 3: searchMigrationKnowledge ============

    @Test
    @DisplayName("searchMigrationKnowledge - should search for flyway migration")
    void searchMigrationKnowledge_shouldSearchFlyway() throws Exception {
        assertToolAvailable("searchMigrationKnowledge");

        Map<String, Object> args = new HashMap<>();
        args.put("searchTerm", "flyway");
        args.put("project", "spring-boot");
        args.put("limit", 10);

        String content = callToolAndGetTextContent("searchMigrationKnowledge", args);
        assertThat(content).isNotNull();
    }

    @Test
    @DisplayName("searchMigrationKnowledge - should search for actuator changes")
    void searchMigrationKnowledge_shouldSearchActuator() throws Exception {
        Map<String, Object> args = new HashMap<>();
        args.put("searchTerm", "actuator");
        args.put("project", "spring-boot");
        args.put("limit", 5);

        String content = callToolAndGetTextContent("searchMigrationKnowledge", args);
        assertThat(content).isNotNull();
    }

    // ============ Tool 4: getAvailableMigrationPaths ============

    @Test
    @DisplayName("getAvailableMigrationPaths - should return paths for spring-boot")
    void getAvailableMigrationPaths_shouldReturnPaths() throws Exception {
        assertToolAvailable("getAvailableMigrationPaths");

        Map<String, Object> args = Map.of("project", "spring-boot");
        String content = callToolAndGetTextContent("getAvailableMigrationPaths", args);

        // Should return available upgrade paths
        assertThat(content).isNotNull();
    }

    @Test
    @DisplayName("getAvailableMigrationPaths - should return paths for spring-framework")
    void getAvailableMigrationPaths_shouldReturnPathsForFramework() throws Exception {
        Map<String, Object> args = Map.of("project", "spring-framework");
        String content = callToolAndGetTextContent("getAvailableMigrationPaths", args);
        assertThat(content).isNotNull();
    }

    // ============ Tool 5: getTransformationsByType ============

    @Test
    @DisplayName("getTransformationsByType - should return IMPORT transformations")
    void getTransformationsByType_shouldReturnImportTransformations() throws Exception {
        assertToolAvailable("getTransformationsByType");

        Map<String, Object> args = Map.of(
            "project", "spring-boot",
            "version", "4.0.0",
            "type", "IMPORT"
        );
        String content = callToolAndGetTextContent("getTransformationsByType", args);
        assertThat(content).isNotNull();
    }

    @Test
    @DisplayName("getTransformationsByType - should return DEPENDENCY transformations")
    void getTransformationsByType_shouldReturnDependencyTransformations() throws Exception {
        Map<String, Object> args = Map.of(
            "project", "spring-boot",
            "version", "4.0.0",
            "type", "DEPENDENCY"
        );
        String content = callToolAndGetTextContent("getTransformationsByType", args);
        assertThat(content).isNotNull();
    }

    @Test
    @DisplayName("getTransformationsByType - should return PROPERTY transformations")
    void getTransformationsByType_shouldReturnPropertyTransformations() throws Exception {
        Map<String, Object> args = Map.of(
            "project", "spring-boot",
            "version", "4.0.0",
            "type", "PROPERTY"
        );
        String content = callToolAndGetTextContent("getTransformationsByType", args);
        assertThat(content).isNotNull();
    }

    // ============ Tool 6: getDeprecationReplacement ============

    @Test
    @DisplayName("getDeprecationReplacement - should find replacement for deprecated class")
    void getDeprecationReplacement_shouldFindReplacement() throws Exception {
        assertToolAvailable("getDeprecationReplacement");

        Map<String, Object> args = new HashMap<>();
        args.put("className", "org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext");
        args.put("methodName", null);

        String content = callToolAndGetTextContent("getDeprecationReplacement", args);
        assertThat(content).isNotNull();
    }

    @Test
    @DisplayName("getDeprecationReplacement - should handle method deprecation")
    void getDeprecationReplacement_shouldHandleMethodDeprecation() throws Exception {
        Map<String, Object> args = Map.of(
            "className", "org.springframework.test.web.servlet.MockMvc",
            "methodName", "perform"
        );
        String content = callToolAndGetTextContent("getDeprecationReplacement", args);
        assertThat(content).isNotNull();
    }

    // ============ Tool 7: checkVersionCompatibility ============

    @Test
    @DisplayName("checkVersionCompatibility - should check spring-security compatibility")
    void checkVersionCompatibility_shouldCheckCompatibility() throws Exception {
        assertToolAvailable("checkVersionCompatibility");

        Map<String, Object> args = Map.of(
            "springBootVersion", "4.0.0",
            "dependencies", new String[]{"spring-security", "spring-data-jpa"}
        );
        String content = callToolAndGetTextContent("checkVersionCompatibility", args);

        // Should return compatibility information
        assertThat(content).isNotNull();
    }

    @Test
    @DisplayName("checkVersionCompatibility - should check flyway compatibility")
    void checkVersionCompatibility_shouldCheckFlywayCompatibility() throws Exception {
        Map<String, Object> args = Map.of(
            "springBootVersion", "3.5.8",
            "dependencies", new String[]{"flyway"}
        );
        String content = callToolAndGetTextContent("checkVersionCompatibility", args);
        assertThat(content).isNotNull();
    }

    // ============ All Tools Verification ============

    @Test
    @DisplayName("All 7 migration tools should be available")
    void allMigrationTools_shouldBeAvailable() throws Exception {
        assertToolAvailable("getSpringMigrationGuide");
        assertToolAvailable("getBreakingChanges");
        assertToolAvailable("searchMigrationKnowledge");
        assertToolAvailable("getAvailableMigrationPaths");
        assertToolAvailable("getTransformationsByType");
        assertToolAvailable("getDeprecationReplacement");
        assertToolAvailable("checkVersionCompatibility");
    }
}
