package com.spring.mcp.service.tools;

import com.spring.mcp.config.InitializrProperties;
import com.spring.mcp.service.initializr.InitializrCacheService;
import com.spring.mcp.service.initializr.InitializrMetadataService;
import com.spring.mcp.service.initializr.InitializrService;
import com.spring.mcp.service.initializr.dto.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Unit tests for InitializrTools MCP tool implementations.
 *
 * @author Spring MCP Server
 * @version 1.4.0
 * @since 2025-12-06
 */
@DisplayName("Initializr MCP Tools Tests")
@ExtendWith(MockitoExtension.class)
class InitializrToolsTest {

    @Mock
    private InitializrMetadataService metadataService;

    @Mock
    private InitializrCacheService cacheService;

    private InitializrTools tools;
    private InitializrService service;
    private InitializrProperties properties;

    @BeforeEach
    void setUp() {
        properties = new InitializrProperties();
        service = new InitializrService(metadataService, cacheService, properties);
        tools = new InitializrTools(service);
    }

    @Nested
    @DisplayName("initializrGetDependency Tests")
    class GetDependencyTests {

        @Test
        @DisplayName("Should get dependency with Gradle format")
        void shouldGetDependencyWithGradleFormat() {
            // Given
            DependencyInfo dep = DependencyInfo.builder()
                .id("web")
                .name("Spring Web")
                .description("Build web applications")
                .groupId("org.springframework.boot")
                .artifactId("spring-boot-starter-web")
                .build();
            when(metadataService.getDependencyById("web")).thenReturn(Optional.of(dep));

            // When
            var result = tools.initializrGetDependency("web", null, "gradle");

            // Then
            assertThat(result.success()).isTrue();
            assertThat(result.dependencyName()).isEqualTo("Spring Web");
            assertThat(result.snippet()).contains("implementation");
            assertThat(result.snippet()).contains("spring-boot-starter-web");
        }

        @Test
        @DisplayName("Should get dependency with Maven format")
        void shouldGetDependencyWithMavenFormat() {
            // Given
            DependencyInfo dep = DependencyInfo.builder()
                .id("data-jpa")
                .name("Spring Data JPA")
                .groupId("org.springframework.boot")
                .artifactId("spring-boot-starter-data-jpa")
                .build();
            when(metadataService.getDependencyById("data-jpa")).thenReturn(Optional.of(dep));

            // When
            var result = tools.initializrGetDependency("data-jpa", "3.5.8", "maven");

            // Then
            assertThat(result.success()).isTrue();
            assertThat(result.snippet()).contains("<dependency>");
            assertThat(result.snippet()).contains("<groupId>");
            assertThat(result.snippet()).contains("spring-boot-starter-data-jpa");
        }

        @Test
        @DisplayName("Should return error for missing dependency ID")
        void shouldReturnErrorForMissingDependencyId() {
            // When
            var result = tools.initializrGetDependency(null, null, "gradle");

            // Then
            assertThat(result.success()).isFalse();
            assertThat(result.message()).contains("required");
        }

        @Test
        @DisplayName("Should return not found for unknown dependency")
        void shouldReturnNotFoundForUnknownDependency() {
            // Given
            when(metadataService.getDependencyById("unknown")).thenReturn(Optional.empty());

            // When
            var result = tools.initializrGetDependency("unknown", null, "gradle");

            // Then
            assertThat(result.success()).isFalse();
            assertThat(result.message()).contains("not found");
        }
    }

    @Nested
    @DisplayName("initializrSearchDependencies Tests")
    class SearchDependenciesTests {

        @Test
        @DisplayName("Should search dependencies by query")
        void shouldSearchDependenciesByQuery() {
            // Given
            List<DependencyInfo> deps = List.of(
                DependencyInfo.builder().id("web").name("Spring Web").description("Web").build(),
                DependencyInfo.builder().id("webflux").name("Spring WebFlux").description("Reactive").build()
            );
            when(metadataService.searchDependencies("web")).thenReturn(deps);

            // When
            var result = tools.initializrSearchDependencies("web", null, null, null);

            // Then
            assertThat(result.count()).isEqualTo(2);
            assertThat(result.dependencies()).hasSize(2);
            assertThat(result.error()).isNull();
        }

        @Test
        @DisplayName("Should return error for empty query")
        void shouldReturnErrorForEmptyQuery() {
            // When
            var result = tools.initializrSearchDependencies("", null, null, null);

            // Then
            assertThat(result.count()).isZero();
            assertThat(result.error()).contains("required");
        }

        @Test
        @DisplayName("Should return empty for no matches")
        void shouldReturnEmptyForNoMatches() {
            // Given
            when(metadataService.searchDependencies("nonexistent")).thenReturn(List.of());

            // When
            var result = tools.initializrSearchDependencies("nonexistent", null, null, null);

            // Then
            assertThat(result.count()).isZero();
            assertThat(result.error()).contains("No dependencies found");
        }
    }

    @Nested
    @DisplayName("initializrCheckCompatibility Tests")
    class CheckCompatibilityTests {

        @Test
        @DisplayName("Should check compatible dependency")
        void shouldCheckCompatibleDependency() {
            // Given
            when(metadataService.checkCompatibility("web", "3.5.8"))
                .thenReturn(CompatibilityInfo.compatible("web", "Spring Web", "3.5.8", "[3.0.0,)"));

            // When
            var result = tools.initializrCheckCompatibility("web", "3.5.8");

            // Then
            assertThat(result.compatible()).isTrue();
            assertThat(result.dependencyName()).isEqualTo("Spring Web");
        }

        @Test
        @DisplayName("Should check incompatible dependency")
        void shouldCheckIncompatibleDependency() {
            // Given
            when(metadataService.checkCompatibility("new-dep", "2.7.0"))
                .thenReturn(CompatibilityInfo.incompatible("new-dep", "New Dependency", "2.7.0", "[3.0.0,)", "3.5.8"));

            // When
            var result = tools.initializrCheckCompatibility("new-dep", "2.7.0");

            // Then
            assertThat(result.compatible()).isFalse();
            assertThat(result.suggestedVersion()).isEqualTo("3.5.8");
        }

        @Test
        @DisplayName("Should return error for missing parameters")
        void shouldReturnErrorForMissingParameters() {
            // When
            var result1 = tools.initializrCheckCompatibility(null, "3.5.8");
            var result2 = tools.initializrCheckCompatibility("web", null);

            // Then
            assertThat(result1.compatible()).isFalse();
            assertThat(result1.reason()).contains("required");
            assertThat(result2.compatible()).isFalse();
            assertThat(result2.reason()).contains("required");
        }
    }

    @Nested
    @DisplayName("initializrGetBootVersions Tests")
    class GetBootVersionsTests {

        @Test
        @DisplayName("Should get boot versions")
        void shouldGetBootVersions() {
            // Given
            List<BootVersion> versions = List.of(
                BootVersion.builder().id("3.5.8").name("3.5.8").defaultVersion(true).build(),
                BootVersion.builder().id("3.4.12").name("3.4.12").build(),
                BootVersion.builder().id("4.0.0-RC1").name("4.0.0 (RC1)").build()
            );
            when(cacheService.getCachedBootVersions()).thenReturn(versions);
            when(metadataService.getDefaultBootVersion())
                .thenReturn(BootVersion.builder().id("3.5.8").name("3.5.8").defaultVersion(true).build());

            // When
            var result = tools.initializrGetBootVersions();

            // Then
            assertThat(result.count()).isEqualTo(3);
            assertThat(result.defaultVersion()).isEqualTo("3.5.8");
            assertThat(result.versions()).hasSize(3);
        }
    }

    @Nested
    @DisplayName("initializrGetDependencyCategories Tests")
    class GetDependencyCategoriesTests {

        @Test
        @DisplayName("Should get dependency categories")
        void shouldGetDependencyCategories() {
            // Given
            List<DependencyCategory> categories = List.of(
                DependencyCategory.builder()
                    .name("Web")
                    .content(List.of(
                        DependencyInfo.builder().id("web").name("Spring Web").build(),
                        DependencyInfo.builder().id("webflux").name("Spring WebFlux").build()
                    ))
                    .build(),
                DependencyCategory.builder()
                    .name("SQL")
                    .content(List.of(
                        DependencyInfo.builder().id("data-jpa").name("Spring Data JPA").build()
                    ))
                    .build()
            );
            when(cacheService.getCachedDependencyCategories()).thenReturn(categories);

            // When - pass null for bootVersion (no filtering)
            var result = tools.initializrGetDependencyCategories(null);

            // Then
            assertThat(result.count()).isEqualTo(2);
            assertThat(result.categories()).containsKeys("Web", "SQL");
            assertThat(result.categories().get("Web")).containsExactly("web", "webflux");
            assertThat(result.filteredByVersion()).isNull();
        }

        @Test
        void shouldFilterCategoriesByBootVersion() {
            // Given - two categories, one with a dependency that has a version range
            // Note: Using artificial version range [1.0.0,2.0.0) to test filtering logic
            // This ensures the test remains stable regardless of real Spring AI releases
            List<DependencyCategory> categories = List.of(
                DependencyCategory.builder()
                    .name("Web")
                    .content(List.of(
                        DependencyInfo.builder().id("web").name("Spring Web").versionRange(null).build()
                    ))
                    .build(),
                DependencyCategory.builder()
                    .name("Legacy")
                    .content(List.of(
                        DependencyInfo.builder().id("legacy-starter").name("Legacy Starter")
                            .versionRange("[1.0.0,2.0.0)").build()  // Only compatible with 1.x
                    ))
                    .build()
            );

            // Mock the cache service to return the categories
            when(cacheService.getCachedDependencyCategories()).thenReturn(categories);

            // When - pass bootVersion 3.0.0 which is outside the [1.0.0,2.0.0) range
            var result = tools.initializrGetDependencyCategories("3.0.0");

            // Then - Legacy category should be filtered out (incompatible with 3.0.0)
            assertThat(result.count()).isEqualTo(1);
            assertThat(result.categories()).containsKeys("Web");
            assertThat(result.categories()).doesNotContainKeys("Legacy");
            assertThat(result.filteredByVersion()).isEqualTo("3.0.0");
        }
    }
}
