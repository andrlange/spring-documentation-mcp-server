package com.spring.mcp.service.initializr;

import com.spring.mcp.config.InitializrProperties;
import com.spring.mcp.service.initializr.dto.*;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.*;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for InitializrMetadataService.
 * Uses MockWebServer to simulate Spring Initializr API responses.
 *
 * @author Spring MCP Server
 * @version 1.4.0
 * @since 2025-12-06
 */
@DisplayName("Initializr Metadata Service Tests")
class InitializrMetadataServiceTest {

    private MockWebServer mockWebServer;
    private InitializrMetadataService service;
    private InitializrProperties properties;

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        properties = new InitializrProperties();
        properties.setBaseUrl(mockWebServer.url("/").toString().replaceAll("/$", ""));

        WebClient webClient = WebClient.builder()
            .baseUrl(properties.getBaseUrl())
            .build();

        service = new InitializrMetadataService(webClient, properties);
    }

    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @Nested
    @DisplayName("Metadata Fetching Tests")
    class MetadataFetchingTests {

        @Test
        @DisplayName("Should fetch metadata successfully")
        void shouldFetchMetadataSuccessfully() {
            // Given
            mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody(getSampleMetadataJson()));

            // When
            InitializrMetadata metadata = service.fetchMetadata();

            // Then
            assertThat(metadata).isNotNull();
            assertThat(metadata.getDependencies()).isNotNull();
            assertThat(metadata.getBootVersion()).isNotNull();
        }

        @Test
        @DisplayName("Should handle API error gracefully")
        void shouldHandleApiErrorGracefully() {
            // Given
            mockWebServer.enqueue(new MockResponse()
                .setResponseCode(500)
                .setBody("Internal Server Error"));

            // When/Then
            assertThatThrownBy(() -> service.fetchMetadata())
                .isInstanceOf(InitializrMetadataService.InitializrServiceException.class)
                .hasMessageContaining("Failed to fetch metadata");
        }
    }

    @Nested
    @DisplayName("Boot Version Tests")
    class BootVersionTests {

        @Test
        @DisplayName("Should get boot versions")
        void shouldGetBootVersions() {
            // Given
            mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody(getSampleMetadataJson()));

            // When
            List<BootVersion> versions = service.getBootVersions();

            // Then
            assertThat(versions).isNotEmpty();
            assertThat(versions.stream().anyMatch(BootVersion::isDefaultVersion)).isTrue();
        }

        @Test
        @DisplayName("Should get default boot version")
        void shouldGetDefaultBootVersion() {
            // Given
            mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody(getSampleMetadataJson()));

            // When
            BootVersion defaultVersion = service.getDefaultBootVersion();

            // Then
            assertThat(defaultVersion).isNotNull();
            assertThat(defaultVersion.getId()).isEqualTo("3.5.8");
        }
    }

    @Nested
    @DisplayName("Dependency Search Tests")
    class DependencySearchTests {

        @Test
        @DisplayName("Should get dependency by ID")
        void shouldGetDependencyById() {
            // Given
            mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody(getSampleMetadataJson()));

            // When
            Optional<DependencyInfo> dep = service.getDependencyById("web");

            // Then
            assertThat(dep).isPresent();
            assertThat(dep.get().getId()).isEqualTo("web");
            assertThat(dep.get().getName()).isEqualTo("Spring Web");
        }

        @Test
        @DisplayName("Should return empty for non-existent dependency")
        void shouldReturnEmptyForNonExistentDependency() {
            // Given
            mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody(getSampleMetadataJson()));

            // When
            Optional<DependencyInfo> dep = service.getDependencyById("non-existent");

            // Then
            assertThat(dep).isEmpty();
        }

        @Test
        @DisplayName("Should search dependencies by name")
        void shouldSearchDependenciesByName() {
            // Given
            mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody(getSampleMetadataJson()));

            // When
            List<DependencyInfo> results = service.searchDependencies("Spring");

            // Then
            assertThat(results).isNotEmpty();
            assertThat(results.stream().allMatch(d ->
                d.getName() != null && d.getName().toLowerCase().contains("spring")
            )).isTrue();
        }

        @Test
        @DisplayName("Should search dependencies by description")
        void shouldSearchDependenciesByDescription() {
            // Given
            mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody(getSampleMetadataJson()));

            // When
            List<DependencyInfo> results = service.searchDependencies("RESTful");

            // Then
            assertThat(results).isNotEmpty();
        }

        @Test
        @DisplayName("Should return empty list for empty query")
        void shouldReturnEmptyListForEmptyQuery() {
            // Given - no mock needed for empty query

            // When
            List<DependencyInfo> results = service.searchDependencies("");

            // Then
            assertThat(results).isEmpty();
        }
    }

    @Nested
    @DisplayName("Compatibility Check Tests")
    class CompatibilityCheckTests {

        @Test
        @DisplayName("Should check compatible dependency")
        void shouldCheckCompatibleDependency() {
            // Given
            mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody(getSampleMetadataJson()));

            // When
            CompatibilityInfo info = service.checkCompatibility("web", "3.5.8");

            // Then
            assertThat(info.isCompatible()).isTrue();
            assertThat(info.getDependencyId()).isEqualTo("web");
            assertThat(info.getBootVersion()).isEqualTo("3.5.8");
        }

        @Test
        @DisplayName("Should return not found for unknown dependency")
        void shouldReturnNotFoundForUnknownDependency() {
            // Given
            mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody(getSampleMetadataJson()));

            // When
            CompatibilityInfo info = service.checkCompatibility("unknown-dep", "3.5.8");

            // Then
            assertThat(info.isCompatible()).isFalse();
            assertThat(info.getReason()).contains("not found");
        }
    }

    @Nested
    @DisplayName("Category Tests")
    class CategoryTests {

        @Test
        @DisplayName("Should get dependency categories")
        void shouldGetDependencyCategories() {
            // Given
            mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody(getSampleMetadataJson()));

            // When
            List<DependencyCategory> categories = service.getDependencyCategories();

            // Then
            assertThat(categories).isNotEmpty();
            assertThat(categories.stream().anyMatch(c -> "Web".equals(c.getName()))).isTrue();
        }

        @Test
        @DisplayName("Should get dependencies by category")
        void shouldGetDependenciesByCategory() {
            // Given
            mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody(getSampleMetadataJson()));

            // When
            var byCategory = service.getDependenciesByCategory();

            // Then
            assertThat(byCategory).isNotEmpty();
            assertThat(byCategory).containsKey("Web");
            assertThat(byCategory.get("Web")).isNotEmpty();
        }
    }

    @Nested
    @DisplayName("Options Tests")
    class OptionsTests {

        @Test
        @DisplayName("Should get Java versions")
        void shouldGetJavaVersions() {
            // Given
            mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody(getSampleMetadataJson()));

            // When
            var javaVersions = service.getJavaVersions();

            // Then
            assertThat(javaVersions).isNotEmpty();
            assertThat(javaVersions.stream().anyMatch(v -> "21".equals(v.getId()))).isTrue();
        }

        @Test
        @DisplayName("Should get languages")
        void shouldGetLanguages() {
            // Given
            mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody(getSampleMetadataJson()));

            // When
            var languages = service.getLanguages();

            // Then
            assertThat(languages).isNotEmpty();
            assertThat(languages.stream().anyMatch(l -> "java".equals(l.getId()))).isTrue();
        }

        @Test
        @DisplayName("Should get project types")
        void shouldGetProjectTypes() {
            // Given
            mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody(getSampleMetadataJson()));

            // When
            var types = service.getProjectTypes();

            // Then
            assertThat(types).isNotEmpty();
            assertThat(types.stream().anyMatch(t -> "gradle-project".equals(t.getId()))).isTrue();
        }
    }

    // Helper method to generate sample metadata JSON
    private String getSampleMetadataJson() {
        return """
            {
              "_links": {
                "self": { "href": "https://start.spring.io" }
              },
              "dependencies": {
                "type": "hierarchical-multi-select",
                "values": [
                  {
                    "name": "Web",
                    "content": [
                      {
                        "id": "web",
                        "name": "Spring Web",
                        "description": "Build web, including RESTful, applications using Spring MVC.",
                        "versionRange": "[3.0.0,)"
                      },
                      {
                        "id": "webflux",
                        "name": "Spring Reactive Web",
                        "description": "Build reactive web applications with Spring WebFlux."
                      }
                    ]
                  },
                  {
                    "name": "SQL",
                    "content": [
                      {
                        "id": "data-jpa",
                        "name": "Spring Data JPA",
                        "description": "Persist data in SQL stores with Java Persistence API."
                      },
                      {
                        "id": "postgresql",
                        "name": "PostgreSQL Driver",
                        "description": "A JDBC and R2DBC driver that allows Java programs to connect to a PostgreSQL database."
                      }
                    ]
                  }
                ]
              },
              "type": {
                "type": "action",
                "default": "gradle-project",
                "values": [
                  {
                    "id": "gradle-project",
                    "name": "Gradle - Groovy",
                    "description": "Generate a Gradle based project archive."
                  },
                  {
                    "id": "gradle-project-kotlin",
                    "name": "Gradle - Kotlin",
                    "description": "Generate a Gradle based project archive using Kotlin DSL."
                  },
                  {
                    "id": "maven-project",
                    "name": "Maven",
                    "description": "Generate a Maven based project archive."
                  }
                ]
              },
              "bootVersion": {
                "type": "single-select",
                "default": "3.5.8",
                "values": [
                  { "id": "4.0.0-SNAPSHOT", "name": "4.0.0 (SNAPSHOT)" },
                  { "id": "4.0.0-RC1", "name": "4.0.0 (RC1)" },
                  { "id": "3.5.8", "name": "3.5.8", "default": true },
                  { "id": "3.4.12", "name": "3.4.12" },
                  { "id": "3.3.8", "name": "3.3.8" }
                ]
              },
              "javaVersion": {
                "type": "single-select",
                "default": "21",
                "values": [
                  { "id": "25", "name": "25" },
                  { "id": "21", "name": "21" },
                  { "id": "17", "name": "17" }
                ]
              },
              "language": {
                "type": "single-select",
                "default": "java",
                "values": [
                  { "id": "java", "name": "Java" },
                  { "id": "kotlin", "name": "Kotlin" },
                  { "id": "groovy", "name": "Groovy" }
                ]
              },
              "packaging": {
                "type": "single-select",
                "default": "jar",
                "values": [
                  { "id": "jar", "name": "Jar" },
                  { "id": "war", "name": "War" }
                ]
              }
            }
            """;
    }
}
