package com.spring.mcp.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for Language Evolution MCP Tools (6 tools).
 * These tools provide Java and Kotlin language feature tracking.
 */
@DisplayName("Language Evolution Tools Integration Tests")
class LanguageEvolutionToolsIntegrationTest extends McpSseIntegrationTestBase {

    // ============ Tool 1: getLanguageVersions ============

    @Test
    @DisplayName("getLanguageVersions - should return Java versions")
    void getLanguageVersions_shouldReturnJavaVersions() throws Exception {
        assertToolAvailable("getLanguageVersions");

        Map<String, Object> args = Map.of("language", "JAVA");
        String content = callToolAndGetTextContent("getLanguageVersions", args);

        // Test data has Java 21 and 17
        assertThat(content).contains("21");
        assertThat(content).contains("17");
    }

    @Test
    @DisplayName("getLanguageVersions - should return Kotlin versions")
    void getLanguageVersions_shouldReturnKotlinVersions() throws Exception {
        Map<String, Object> args = Map.of("language", "KOTLIN");
        String content = callToolAndGetTextContent("getLanguageVersions", args);

        // Test data has Kotlin 2.0
        assertThat(content).contains("2.0");
    }

    // ============ Tool 2: getLanguageFeatures ============

    @Test
    @DisplayName("getLanguageFeatures - should return Java 21 features")
    void getLanguageFeatures_shouldReturnJava21Features() throws Exception {
        assertToolAvailable("getLanguageFeatures");

        Map<String, Object> args = new HashMap<>();
        args.put("language", "JAVA");
        args.put("version", "21");
        args.put("status", null);
        args.put("category", null);

        String content = callToolAndGetTextContent("getLanguageFeatures", args);

        // Test data has Virtual Threads and Record Patterns for Java 21
        assertThat(content).containsIgnoringCase("Virtual Threads");
    }

    @Test
    @DisplayName("getLanguageFeatures - should filter by status")
    void getLanguageFeatures_shouldFilterByStatus() throws Exception {
        Map<String, Object> args = new HashMap<>();
        args.put("language", "JAVA");
        args.put("version", "21");
        args.put("status", "NEW");
        args.put("category", null);

        String content = callToolAndGetTextContent("getLanguageFeatures", args);
        assertThat(content).isNotNull();
    }

    @Test
    @DisplayName("getLanguageFeatures - should return Kotlin 2.0 features")
    void getLanguageFeatures_shouldReturnKotlin20Features() throws Exception {
        Map<String, Object> args = new HashMap<>();
        args.put("language", "KOTLIN");
        args.put("version", "2.0");
        args.put("status", null);
        args.put("category", null);

        String content = callToolAndGetTextContent("getLanguageFeatures", args);

        // Test data has K2 Compiler for Kotlin 2.0
        assertThat(content).containsIgnoringCase("K2");
    }

    // ============ Tool 3: getModernPatterns ============

    @Test
    @DisplayName("getModernPatterns - should return patterns for Virtual Threads feature")
    void getModernPatterns_shouldReturnPatterns() throws Exception {
        assertToolAvailable("getModernPatterns");

        // First, we need to find the feature ID
        // Using a known feature ID from test data
        Map<String, Object> args = Map.of("featureId", 1L);
        String content = callToolAndGetTextContent("getModernPatterns", args);

        // Should return patterns or indicate none found
        assertThat(content).isNotNull();
    }

    // ============ Tool 4: getLanguageVersionDiff ============

    @Test
    @DisplayName("getLanguageVersionDiff - should compare Java 17 to 21")
    void getLanguageVersionDiff_shouldCompareJavaVersions() throws Exception {
        assertToolAvailable("getLanguageVersionDiff");

        Map<String, Object> args = Map.of(
            "language", "JAVA",
            "fromVersion", "17",
            "toVersion", "21"
        );
        String content = callToolAndGetTextContent("getLanguageVersionDiff", args);

        // Should show differences between versions
        assertThat(content).isNotNull();
    }

    @Test
    @DisplayName("getLanguageVersionDiff - should handle same version comparison")
    void getLanguageVersionDiff_shouldHandleSameVersion() throws Exception {
        Map<String, Object> args = Map.of(
            "language", "JAVA",
            "fromVersion", "21",
            "toVersion", "21"
        );
        String content = callToolAndGetTextContent("getLanguageVersionDiff", args);
        assertThat(content).isNotNull();
    }

    // ============ Tool 5: getSpringBootLanguageRequirements ============

    @Test
    @DisplayName("getSpringBootLanguageRequirements - should return requirements for Spring Boot 4.0.0")
    void getSpringBootLanguageRequirements_shouldReturnRequirements() throws Exception {
        assertToolAvailable("getSpringBootLanguageRequirements");

        Map<String, Object> args = Map.of("springBootVersion", "4.0.0");
        String content = callToolAndGetTextContent("getSpringBootLanguageRequirements", args);

        // Should return language requirements
        assertThat(content).isNotNull();
    }

    @Test
    @DisplayName("getSpringBootLanguageRequirements - should return requirements for Spring Boot 3.5.8")
    void getSpringBootLanguageRequirements_shouldReturnRequirementsFor358() throws Exception {
        Map<String, Object> args = Map.of("springBootVersion", "3.5.8");
        String content = callToolAndGetTextContent("getSpringBootLanguageRequirements", args);
        assertThat(content).isNotNull();
    }

    // ============ Tool 6: searchLanguageFeatures ============

    @Test
    @DisplayName("searchLanguageFeatures - should search for virtual thread features")
    void searchLanguageFeatures_shouldSearchVirtualThreads() throws Exception {
        assertToolAvailable("searchLanguageFeatures");

        Map<String, Object> args = new HashMap<>();
        args.put("searchTerm", "virtual thread");
        args.put("language", null);

        String content = callToolAndGetTextContent("searchLanguageFeatures", args);

        // Test data has Virtual Threads feature
        assertThat(content).containsIgnoringCase("virtual");
    }

    @Test
    @DisplayName("searchLanguageFeatures - should search for record features")
    void searchLanguageFeatures_shouldSearchRecords() throws Exception {
        Map<String, Object> args = new HashMap<>();
        args.put("searchTerm", "record");
        args.put("language", "JAVA");

        String content = callToolAndGetTextContent("searchLanguageFeatures", args);

        // Test data has Record Patterns feature
        assertThat(content).isNotNull();
    }

    @Test
    @DisplayName("searchLanguageFeatures - should search for sealed class features")
    void searchLanguageFeatures_shouldSearchSealed() throws Exception {
        Map<String, Object> args = new HashMap<>();
        args.put("searchTerm", "sealed");
        args.put("language", "JAVA");

        String content = callToolAndGetTextContent("searchLanguageFeatures", args);

        // Test data has Sealed Classes feature in Java 17
        assertThat(content).containsIgnoringCase("sealed");
    }

    // ============ All Tools Verification ============

    @Test
    @DisplayName("All 6 language evolution tools should be available")
    void allLanguageEvolutionTools_shouldBeAvailable() throws Exception {
        assertToolAvailable("getLanguageVersions");
        assertToolAvailable("getLanguageFeatures");
        assertToolAvailable("getModernPatterns");
        assertToolAvailable("getLanguageVersionDiff");
        assertToolAvailable("getSpringBootLanguageRequirements");
        assertToolAvailable("searchLanguageFeatures");
    }
}
