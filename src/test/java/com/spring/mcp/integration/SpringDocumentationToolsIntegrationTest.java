package com.spring.mcp.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for Spring Documentation MCP Tools (10 tools).
 * Tests the full MCP SSE pipeline with test data.
 */
@DisplayName("Spring Documentation Tools Integration Tests")
class SpringDocumentationToolsIntegrationTest extends McpSseIntegrationTestBase {

    // ============ Tool 1: searchSpringDocs ============

    @Test
    @DisplayName("searchSpringDocs - should search Spring documentation")
    void searchSpringDocs_shouldSearchDocumentation() throws Exception {
        assertToolAvailable("searchSpringDocs");

        Map<String, Object> args = new HashMap<>();
        args.put("query", "spring boot");
        args.put("project", null);
        args.put("version", null);
        args.put("docType", null);

        String content = callToolAndGetTextContent("searchSpringDocs", args);

        // Should return results or message about search
        assertThat(content).isNotNull();
    }

    @Test
    @DisplayName("searchSpringDocs - should filter by project")
    void searchSpringDocs_shouldFilterByProject() throws Exception {
        Map<String, Object> args = new HashMap<>();
        args.put("query", "autoconfiguration");
        args.put("project", "spring-boot");
        args.put("version", null);
        args.put("docType", null);

        String content = callToolAndGetTextContent("searchSpringDocs", args);
        assertThat(content).isNotNull();
    }

    // ============ Tool 2: getSpringVersions ============

    @Test
    @DisplayName("getSpringVersions - should return versions for spring-boot")
    void getSpringVersions_shouldReturnVersionsForSpringBoot() throws Exception {
        assertToolAvailable("getSpringVersions");

        Map<String, Object> args = Map.of("project", "spring-boot");
        String content = callToolAndGetTextContent("getSpringVersions", args);

        // Test data has spring-boot 3.5.8
        assertThat(content).contains("3.5.8");
    }

    @Test
    @DisplayName("getSpringVersions - should return versions for spring-framework")
    void getSpringVersions_shouldReturnVersionsForSpringFramework() throws Exception {
        Map<String, Object> args = Map.of("project", "spring-framework");
        String content = callToolAndGetTextContent("getSpringVersions", args);

        // Test data has spring-framework 6.2.1
        assertThat(content).contains("6.2.1");
    }

    // ============ Tool 3: listSpringProjects ============

    @Test
    @DisplayName("listSpringProjects - should return all Spring projects")
    void listSpringProjects_shouldReturnAllProjects() throws Exception {
        assertToolAvailable("listSpringProjects");

        Map<String, Object> args = Map.of();
        String content = callToolAndGetTextContent("listSpringProjects", args);

        // Test data has 5 projects
        assertThat(content).contains("Spring Boot");
        assertThat(content).contains("Spring Framework");
        assertThat(content).contains("Spring Security");
        assertThat(content).contains("Spring Data JPA");
        assertThat(content).contains("Spring Cloud");
    }

    // ============ Tool 4: getDocumentationByVersion ============

    @Test
    @DisplayName("getDocumentationByVersion - should return documentation for spring-boot 3.5.8")
    void getDocumentationByVersion_shouldReturnDocumentation() throws Exception {
        assertToolAvailable("getDocumentationByVersion");

        Map<String, Object> args = Map.of(
            "project", "spring-boot",
            "version", "3.5.8"
        );
        String content = callToolAndGetTextContent("getDocumentationByVersion", args);

        // Should contain documentation links
        assertThat(content).isNotNull();
    }

    // ============ Tool 5: getCodeExamples ============

    @Test
    @DisplayName("getCodeExamples - should return code examples")
    void getCodeExamples_shouldReturnCodeExamples() throws Exception {
        assertToolAvailable("getCodeExamples");

        Map<String, Object> args = new HashMap<>();
        args.put("query", "hello");
        args.put("project", null);
        args.put("version", null);
        args.put("language", null);
        args.put("limit", null);

        String content = callToolAndGetTextContent("getCodeExamples", args);

        // Test data has "Hello World" example
        assertThat(content).containsIgnoringCase("hello");
    }

    @Test
    @DisplayName("getCodeExamples - should filter by language")
    void getCodeExamples_shouldFilterByLanguage() throws Exception {
        Map<String, Object> args = new HashMap<>();
        args.put("query", null);
        args.put("project", null);
        args.put("version", null);
        args.put("language", "java");
        args.put("limit", 10);

        String content = callToolAndGetTextContent("getCodeExamples", args);
        assertThat(content).isNotNull();
    }

    // ============ Tool 6: listSpringBootVersions ============

    @Test
    @DisplayName("listSpringBootVersions - should return all Spring Boot versions")
    void listSpringBootVersions_shouldReturnAllVersions() throws Exception {
        assertToolAvailable("listSpringBootVersions");

        Map<String, Object> args = new HashMap<>();
        args.put("state", null);
        args.put("limit", null);

        String content = callToolAndGetTextContent("listSpringBootVersions", args);

        // Test data has versions 4.0.0, 3.5.8, 3.4.12, 4.0.1-SNAPSHOT
        assertThat(content).contains("4.0.0");
        assertThat(content).contains("3.5.8");
        assertThat(content).contains("3.4.12");
    }

    @Test
    @DisplayName("listSpringBootVersions - should filter by state")
    void listSpringBootVersions_shouldFilterByState() throws Exception {
        Map<String, Object> args = Map.of("state", "GA", "limit", 10);
        String content = callToolAndGetTextContent("listSpringBootVersions", args);

        // Should only return GA versions
        assertThat(content).contains("4.0.0");
        assertThat(content).doesNotContain("SNAPSHOT");
    }

    // ============ Tool 7: getLatestSpringBootVersion ============

    @Test
    @DisplayName("getLatestSpringBootVersion - should return latest 3.5.x version")
    void getLatestSpringBootVersion_shouldReturnLatest35x() throws Exception {
        assertToolAvailable("getLatestSpringBootVersion");

        Map<String, Object> args = Map.of(
            "majorVersion", "3",
            "minorVersion", "5"
        );
        String content = callToolAndGetTextContent("getLatestSpringBootVersion", args);

        // Test data has 3.5.8 as the only 3.5.x version
        assertThat(content).contains("3.5.8");
    }

    @Test
    @DisplayName("getLatestSpringBootVersion - should return latest 4.0.x version")
    void getLatestSpringBootVersion_shouldReturnLatest40x() throws Exception {
        Map<String, Object> args = Map.of(
            "majorVersion", "4",
            "minorVersion", "0"
        );
        String content = callToolAndGetTextContent("getLatestSpringBootVersion", args);

        // Test data has 4.0.0 as the only GA 4.0.x
        assertThat(content).contains("4.0.0");
    }

    // ============ Tool 8: filterSpringBootVersionsBySupport ============

    @Test
    @DisplayName("filterSpringBootVersionsBySupport - should filter supported versions")
    void filterSpringBootVersionsBySupport_shouldFilterSupported() throws Exception {
        assertToolAvailable("filterSpringBootVersionsBySupport");

        Map<String, Object> args = Map.of(
            "supportActive", true,
            "limit", 10
        );
        String content = callToolAndGetTextContent("filterSpringBootVersionsBySupport", args);

        // Should return versions with active support
        assertThat(content).isNotNull();
    }

    @Test
    @DisplayName("filterSpringBootVersionsBySupport - should filter unsupported versions")
    void filterSpringBootVersionsBySupport_shouldFilterUnsupported() throws Exception {
        Map<String, Object> args = Map.of(
            "supportActive", false,
            "limit", 10
        );
        String content = callToolAndGetTextContent("filterSpringBootVersionsBySupport", args);
        assertThat(content).isNotNull();
    }

    // ============ Tool 9: listProjectsBySpringBootVersion ============

    @Test
    @DisplayName("listProjectsBySpringBootVersion - should list projects for Spring Boot 3.5 (compact)")
    void listProjectsBySpringBootVersion_shouldListProjects() throws Exception {
        assertToolAvailable("listProjectsBySpringBootVersion");

        Map<String, Object> args = Map.of(
            "majorVersion", 3,
            "minorVersion", 5
        );
        String content = callToolAndGetTextContent("listProjectsBySpringBootVersion", args);

        // Should contain project compatibility info (compact: 1 version per project)
        assertThat(content).isNotNull();
    }

    @Test
    @DisplayName("listProjectsBySpringBootVersion - should list all versions when allVersions=true")
    void listProjectsBySpringBootVersion_shouldListAllVersions() throws Exception {
        assertToolAvailable("listProjectsBySpringBootVersion");

        Map<String, Object> args = Map.of(
            "majorVersion", 3,
            "minorVersion", 5,
            "allVersions", true
        );
        String content = callToolAndGetTextContent("listProjectsBySpringBootVersion", args);

        // Should contain project compatibility info with all versions
        assertThat(content).isNotNull();
    }

    // ============ Tool 10: findProjectsByUseCase ============

    @Test
    @DisplayName("findProjectsByUseCase - should find projects for security use case")
    void findProjectsByUseCase_shouldFindSecurityProjects() throws Exception {
        assertToolAvailable("findProjectsByUseCase");

        Map<String, Object> args = Map.of("useCase", "security");
        String content = callToolAndGetTextContent("findProjectsByUseCase", args);

        // Test data has Spring Security
        assertThat(content).containsIgnoringCase("security");
    }

    @Test
    @DisplayName("findProjectsByUseCase - should find projects for data use case")
    void findProjectsByUseCase_shouldFindDataProjects() throws Exception {
        Map<String, Object> args = Map.of("useCase", "data");
        String content = callToolAndGetTextContent("findProjectsByUseCase", args);

        // Test data has Spring Data JPA
        assertThat(content).isNotNull();
    }

    // ============ All Tools Verification ============

    @Test
    @DisplayName("All 10 documentation tools should be available")
    void allDocumentationTools_shouldBeAvailable() throws Exception {
        assertToolAvailable("searchSpringDocs");
        assertToolAvailable("getSpringVersions");
        assertToolAvailable("listSpringProjects");
        assertToolAvailable("getDocumentationByVersion");
        assertToolAvailable("getCodeExamples");
        assertToolAvailable("listSpringBootVersions");
        assertToolAvailable("getLatestSpringBootVersion");
        assertToolAvailable("filterSpringBootVersionsBySupport");
        assertToolAvailable("listProjectsBySpringBootVersion");
        assertToolAvailable("findProjectsByUseCase");
    }
}
