package com.spring.mcp.controller.web;

import com.spring.mcp.config.InitializrProperties;
import com.spring.mcp.service.initializr.InitializrCacheService;
import com.spring.mcp.service.initializr.InitializrService;
import com.spring.mcp.service.initializr.dto.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

/**
 * Unit tests for InitializrController.
 *
 * @author Spring MCP Server
 * @version 1.4.0
 * @since 2025-12-06
 */
@DisplayName("Initializr Controller Tests")
@ExtendWith(MockitoExtension.class)
class InitializrControllerTest {

    @Mock
    private InitializrService initializrService;

    @Mock
    private InitializrProperties properties;

    @Mock
    private RestTemplate restTemplate;

    private InitializrController controller;

    @BeforeEach
    void setUp() {
        controller = new InitializrController(initializrService, properties, restTemplate);
    }

    @Nested
    @DisplayName("GET /initializr - Main Page")
    class ShowInitializrPageTests {

        @Test
        @DisplayName("Should render initializr page with all model attributes")
        void shouldRenderInitializrPageWithModelAttributes() {
            // Given
            List<BootVersion> bootVersions = List.of(
                    BootVersion.builder().id("3.5.8").name("3.5.8").defaultVersion(true).build(),
                    BootVersion.builder().id("4.0.0-RC1").name("4.0.0 (RC1)").build()
            );
            BootVersion defaultVersion = bootVersions.get(0);

            List<DependencyCategory> categories = List.of(
                    DependencyCategory.builder()
                            .name("Web")
                            .content(List.of(
                                    DependencyInfo.builder().id("web").name("Spring Web").build(),
                                    DependencyInfo.builder().id("webflux").name("Spring WebFlux").build()
                            ))
                            .build()
            );

            List<InitializrMetadata.JavaVersion> javaVersions = List.of(
                    new InitializrMetadata.JavaVersion("21", "21")
            );

            List<InitializrMetadata.LanguageOption> languages = List.of(
                    new InitializrMetadata.LanguageOption("java", "Java")
            );

            List<InitializrMetadata.TypeOption> projectTypes = List.of(
                    new InitializrMetadata.TypeOption("gradle-project", "Gradle - Groovy", "", "/starter.zip", null)
            );

            List<InitializrMetadata.PackagingOption> packagingTypes = List.of(
                    new InitializrMetadata.PackagingOption("jar", "Jar")
            );

            InitializrProperties.Defaults defaults = new InitializrProperties.Defaults();
            InitializrCacheService.CacheSummary cacheSummary =
                    new InitializrCacheService.CacheSummary(100L, 5L, 2L, 95.0, null, true);

            when(initializrService.getBootVersions()).thenReturn(bootVersions);
            when(initializrService.getDefaultBootVersion()).thenReturn(defaultVersion);
            when(initializrService.getDependencyCategories()).thenReturn(categories);
            when(initializrService.getJavaVersions()).thenReturn(javaVersions);
            when(initializrService.getLanguages()).thenReturn(languages);
            when(initializrService.getProjectTypes()).thenReturn(projectTypes);
            when(initializrService.getPackagingTypes()).thenReturn(packagingTypes);
            when(initializrService.getCacheStatus()).thenReturn(cacheSummary);
            when(properties.getDefaults()).thenReturn(defaults);

            Model model = new ConcurrentModel();

            // When
            String viewName = controller.showInitializrPage(model);

            // Then
            assertThat(viewName).isEqualTo("initializr/index");
            assertThat(model.getAttribute("activePage")).isEqualTo("initializr");
            assertThat(model.getAttribute("pageTitle")).isEqualTo("Spring Initializr");
            assertThat(model.getAttribute("bootVersions")).isEqualTo(bootVersions);
            assertThat(model.getAttribute("defaultBootVersion")).isEqualTo("3.5.8");
            assertThat(model.getAttribute("dependencyCategories")).isEqualTo(categories);
            assertThat(model.getAttribute("javaVersions")).isEqualTo(javaVersions);
            assertThat(model.getAttribute("languages")).isEqualTo(languages);
            assertThat(model.getAttribute("projectTypes")).isEqualTo(projectTypes);
            assertThat(model.getAttribute("packagingTypes")).isEqualTo(packagingTypes);
            assertThat(model.getAttribute("totalDependencies")).isEqualTo(2);
        }

        @Test
        @DisplayName("Should use default boot version from properties when no default found")
        void shouldUsePropertiesDefaultWhenNoDefaultVersion() {
            // Given
            InitializrProperties.Defaults defaults = new InitializrProperties.Defaults();
            defaults.setBootVersion("3.4.0");

            when(initializrService.getBootVersions()).thenReturn(List.of());
            when(initializrService.getDefaultBootVersion()).thenReturn(null);
            when(initializrService.getDependencyCategories()).thenReturn(List.of());
            when(initializrService.getJavaVersions()).thenReturn(List.of());
            when(initializrService.getLanguages()).thenReturn(List.of());
            when(initializrService.getProjectTypes()).thenReturn(List.of());
            when(initializrService.getPackagingTypes()).thenReturn(List.of());
            when(initializrService.getCacheStatus()).thenReturn(
                    new InitializrCacheService.CacheSummary(0L, 0L, 0L, 0.0, null, false));
            when(properties.getDefaults()).thenReturn(defaults);

            Model model = new ConcurrentModel();

            // When
            controller.showInitializrPage(model);

            // Then
            assertThat(model.getAttribute("defaultBootVersion")).isEqualTo("3.4.0");
        }
    }

    @Nested
    @DisplayName("GET /initializr/dependencies/search")
    class SearchDependenciesTests {

        @Test
        @DisplayName("Should return all categories when query is empty")
        void shouldReturnAllCategoriesWhenQueryIsEmpty() {
            // Given
            List<DependencyCategory> categories = List.of(
                    DependencyCategory.builder()
                            .name("Web")
                            .content(List.of(
                                    DependencyInfo.builder().id("web").name("Spring Web").build()
                            ))
                            .build()
            );
            when(initializrService.getDependencyCategories()).thenReturn(categories);

            // When
            ResponseEntity<Map<String, Object>> response = controller.searchDependencies(
                    null, null, null, 20);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).containsKey("success");
            assertThat(response.getBody().get("success")).isEqualTo(true);
            assertThat(response.getBody()).containsKey("categories");
        }

        @Test
        @DisplayName("Should search dependencies by query")
        void shouldSearchDependenciesByQuery() {
            // Given
            List<DependencyInfo> deps = List.of(
                    DependencyInfo.builder().id("web").name("Spring Web").description("Web").build()
            );
            InitializrService.SearchResult searchResult = new InitializrService.SearchResult(
                    "web", 1, deps, Map.of("Web", deps));

            when(initializrService.searchDependencies("web", null, null, 20)).thenReturn(searchResult);

            // When
            ResponseEntity<Map<String, Object>> response = controller.searchDependencies(
                    "web", null, null, 20);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().get("success")).isEqualTo(true);
            assertThat(response.getBody().get("query")).isEqualTo("web");
            assertThat(response.getBody().get("count")).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("GET /initializr/dependencies/{id}")
    class GetDependencyTests {

        @Test
        @DisplayName("Should return dependency details")
        void shouldReturnDependencyDetails() {
            // Given
            DependencyInfo dep = DependencyInfo.builder()
                    .id("web")
                    .name("Spring Web")
                    .groupId("org.springframework.boot")
                    .artifactId("spring-boot-starter-web")
                    .build();

            InitializrService.DependencyResult result = InitializrService.DependencyResult.success(
                    dep, "implementation 'org.springframework.boot:spring-boot-starter-web'", "gradle");

            when(initializrService.getDependency("web", "3.5.8", "gradle")).thenReturn(result);

            // When
            ResponseEntity<Map<String, Object>> response = controller.getDependency("web", "3.5.8");

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().get("success")).isEqualTo(true);
            assertThat(response.getBody().get("compatible")).isEqualTo(true);
        }

        @Test
        @DisplayName("Should return 404 for unknown dependency")
        void shouldReturn404ForUnknownDependency() {
            // Given
            InitializrService.DependencyResult result = InitializrService.DependencyResult.notFound("unknown");

            when(initializrService.getDependency("unknown", null, "gradle")).thenReturn(result);

            // When
            ResponseEntity<Map<String, Object>> response = controller.getDependency("unknown", null);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
            assertThat(response.getBody().get("success")).isEqualTo(false);
        }
    }

    @Nested
    @DisplayName("POST /initializr/cache/refresh")
    class RefreshCacheTests {

        @Test
        @DisplayName("Should refresh cache successfully")
        void shouldRefreshCacheSuccessfully() {
            // Given
            InitializrCacheService.CacheSummary summary =
                    new InitializrCacheService.CacheSummary(100L, 10L, 50L, 90.0, null, true);
            when(initializrService.getCacheStatus()).thenReturn(summary);

            // When
            ResponseEntity<Map<String, Object>> response = controller.refreshCache();

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().get("success")).isEqualTo(true);
            assertThat(response.getBody().get("message")).isEqualTo("Cache refreshed successfully");
        }
    }
}
