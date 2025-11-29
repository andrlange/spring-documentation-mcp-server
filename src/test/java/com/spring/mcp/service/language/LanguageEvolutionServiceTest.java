package com.spring.mcp.service.language;

import com.spring.mcp.model.entity.LanguageFeature;
import com.spring.mcp.model.entity.LanguageVersion;
import com.spring.mcp.model.entity.SpringBootLanguageRequirement;
import com.spring.mcp.model.entity.SpringBootVersion;
import com.spring.mcp.model.enums.FeatureStatus;
import com.spring.mcp.model.enums.LanguageType;
import com.spring.mcp.repository.LanguageCodePatternRepository;
import com.spring.mcp.repository.LanguageFeatureRepository;
import com.spring.mcp.repository.LanguageVersionRepository;
import com.spring.mcp.repository.SpringBootLanguageRequirementRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Unit tests for LanguageEvolutionService.
 * Tests language version queries, feature filtering, and version comparison logic.
 *
 * @author Spring MCP Server
 * @version 1.2.0
 * @since 2025-11-29
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("LanguageEvolutionService Tests")
class LanguageEvolutionServiceTest {

    @Mock
    private LanguageVersionRepository versionRepository;

    @Mock
    private LanguageFeatureRepository featureRepository;

    @Mock
    private LanguageCodePatternRepository codePatternRepository;

    @Mock
    private SpringBootLanguageRequirementRepository requirementRepository;

    @InjectMocks
    private LanguageEvolutionService service;

    private LanguageVersion java21;
    private LanguageVersion java17;
    private LanguageVersion kotlin19;
    private LanguageFeature recordsFeature;
    private LanguageFeature sealedClassesFeature;

    @BeforeEach
    void setUp() {
        // Setup Java 21 version
        java21 = new LanguageVersion();
        java21.setId(1L);
        java21.setLanguage(LanguageType.JAVA);
        java21.setVersion("21");
        java21.setMajorVersion(21);
        java21.setMinorVersion(0);
        java21.setIsLts(true);
        java21.setReleaseDate(LocalDate.of(2023, 9, 19));

        // Setup Java 17 version
        java17 = new LanguageVersion();
        java17.setId(2L);
        java17.setLanguage(LanguageType.JAVA);
        java17.setVersion("17");
        java17.setMajorVersion(17);
        java17.setMinorVersion(0);
        java17.setIsLts(true);
        java17.setReleaseDate(LocalDate.of(2021, 9, 14));

        // Setup Kotlin 1.9 version
        kotlin19 = new LanguageVersion();
        kotlin19.setId(3L);
        kotlin19.setLanguage(LanguageType.KOTLIN);
        kotlin19.setVersion("1.9");
        kotlin19.setMajorVersion(1);
        kotlin19.setMinorVersion(9);
        kotlin19.setIsLts(false);
        kotlin19.setReleaseDate(LocalDate.of(2023, 7, 6));

        // Setup Records feature
        recordsFeature = new LanguageFeature();
        recordsFeature.setId(1L);
        recordsFeature.setLanguageVersion(java21);
        recordsFeature.setFeatureName("Records");
        recordsFeature.setDescription("Record classes for immutable data carriers");
        recordsFeature.setStatus(FeatureStatus.NEW);
        recordsFeature.setCategory("Language");
        recordsFeature.setJepNumber("395");

        // Setup Sealed Classes feature
        sealedClassesFeature = new LanguageFeature();
        sealedClassesFeature.setId(2L);
        sealedClassesFeature.setLanguageVersion(java17);
        sealedClassesFeature.setFeatureName("Sealed Classes");
        sealedClassesFeature.setDescription("Restrict which classes can extend or implement");
        sealedClassesFeature.setStatus(FeatureStatus.NEW);
        sealedClassesFeature.setCategory("Language");
        sealedClassesFeature.setJepNumber("409");
    }

    @Nested
    @DisplayName("getLanguageVersions Tests")
    class GetLanguageVersionsTests {

        @Test
        @DisplayName("Should return all Java versions ordered by major version descending")
        void shouldReturnJavaVersionsOrdered() {
            when(versionRepository.findByLanguageOrderByMajorVersionDescMinorVersionDesc(LanguageType.JAVA))
                    .thenReturn(Arrays.asList(java21, java17));

            List<LanguageVersion> versions = service.getLanguageVersions(LanguageType.JAVA);

            assertThat(versions).hasSize(2);
            assertThat(versions.get(0).getMajorVersion()).isEqualTo(21);
            assertThat(versions.get(1).getMajorVersion()).isEqualTo(17);
        }

        @Test
        @DisplayName("Should return empty list when no versions exist")
        void shouldReturnEmptyListWhenNoVersions() {
            when(versionRepository.findByLanguageOrderByMajorVersionDescMinorVersionDesc(LanguageType.KOTLIN))
                    .thenReturn(Collections.emptyList());

            List<LanguageVersion> versions = service.getLanguageVersions(LanguageType.KOTLIN);

            assertThat(versions).isEmpty();
        }

        @Test
        @DisplayName("Should return Kotlin versions")
        void shouldReturnKotlinVersions() {
            when(versionRepository.findByLanguageOrderByMajorVersionDescMinorVersionDesc(LanguageType.KOTLIN))
                    .thenReturn(List.of(kotlin19));

            List<LanguageVersion> versions = service.getLanguageVersions(LanguageType.KOTLIN);

            assertThat(versions).hasSize(1);
            assertThat(versions.get(0).getLanguage()).isEqualTo(LanguageType.KOTLIN);
        }
    }

    @Nested
    @DisplayName("getFeatures Tests")
    class GetFeaturesTests {

        @Test
        @DisplayName("Should return features for specific version")
        void shouldReturnFeaturesForSpecificVersion() {
            when(featureRepository.findByLanguageAndVersion(LanguageType.JAVA, "21"))
                    .thenReturn(List.of(recordsFeature));

            List<LanguageFeature> features = service.getFeatures(LanguageType.JAVA, "21");

            assertThat(features).hasSize(1);
            assertThat(features.get(0).getFeatureName()).isEqualTo("Records");
        }

        @Test
        @DisplayName("Should return features for version ID")
        void shouldReturnFeaturesForVersionId() {
            when(featureRepository.findByLanguageVersionIdOrderByStatusAscFeatureNameAsc(1L))
                    .thenReturn(List.of(recordsFeature));

            List<LanguageFeature> features = service.getFeaturesForVersion(1L);

            assertThat(features).hasSize(1);
            assertThat(features.get(0).getFeatureName()).isEqualTo("Records");
        }

        @Test
        @DisplayName("Should return all features for a language")
        void shouldReturnAllFeaturesForLanguage() {
            when(featureRepository.findByLanguage(LanguageType.JAVA))
                    .thenReturn(Arrays.asList(recordsFeature, sealedClassesFeature));

            List<LanguageFeature> features = service.getAllFeatures(LanguageType.JAVA);

            assertThat(features).hasSize(2);
        }
    }

    @Nested
    @DisplayName("searchFeatures Tests")
    class SearchFeaturesTests {

        @Test
        @DisplayName("Should search features by keyword")
        void shouldSearchFeaturesByKeyword() {
            when(featureRepository.searchByFeatureNameOrDescription("record"))
                    .thenReturn(List.of(recordsFeature));

            List<LanguageFeature> features = service.searchFeatures("record");

            assertThat(features).hasSize(1);
            assertThat(features.get(0).getFeatureName()).containsIgnoringCase("record");
        }

        @Test
        @DisplayName("Should return empty list when no matches found")
        void shouldReturnEmptyListWhenNoMatches() {
            when(featureRepository.searchByFeatureNameOrDescription("nonexistent"))
                    .thenReturn(Collections.emptyList());

            List<LanguageFeature> features = service.searchFeatures("nonexistent");

            assertThat(features).isEmpty();
        }

        @Test
        @DisplayName("Should search with multiple filters")
        void shouldSearchWithMultipleFilters() {
            // Repository uses String parameters for native query
            when(featureRepository.searchFeatures("JAVA", "21", "NEW", "Language", "record"))
                    .thenReturn(List.of(recordsFeature));

            List<LanguageFeature> features = service.searchFeatures(
                    LanguageType.JAVA, "21", FeatureStatus.NEW, "Language", "record");

            assertThat(features).hasSize(1);
        }
    }

    @Nested
    @DisplayName("getVersionDiff Tests")
    class GetVersionDiffTests {

        @Test
        @DisplayName("Should calculate version diff correctly")
        void shouldCalculateVersionDiff() {
            // Setup mock data for Java 21 features
            LanguageFeature java21Feature = new LanguageFeature();
            java21Feature.setLanguageVersion(java21);
            java21Feature.setFeatureName("Virtual Threads");
            java21Feature.setStatus(FeatureStatus.NEW);

            when(versionRepository.findByLanguageAndVersion(LanguageType.JAVA, "17"))
                    .thenReturn(Optional.of(java17));
            when(versionRepository.findByLanguageAndVersion(LanguageType.JAVA, "21"))
                    .thenReturn(Optional.of(java21));
            when(versionRepository.findByLanguageAndVersionGreaterThanEqual(eq(LanguageType.JAVA), eq(17), eq(0)))
                    .thenReturn(Arrays.asList(java17, java21));
            when(featureRepository.findByLanguageVersionIdAndStatus(1L, FeatureStatus.NEW))
                    .thenReturn(List.of(java21Feature));
            when(featureRepository.findDeprecationsAndRemovalsBetweenVersions(
                    eq(LanguageType.JAVA), eq(17), eq(0), eq(21), eq(0)))
                    .thenReturn(Collections.emptyList());

            LanguageEvolutionService.VersionDiff diff = service.getVersionDiff(LanguageType.JAVA, "17", "21");

            assertThat(diff.getNewFeatures()).hasSize(1);
            assertThat(diff.getDeprecatedFeatures()).isEmpty();
            assertThat(diff.getRemovedFeatures()).isEmpty();
            assertThat(diff.getTotalChanges()).isEqualTo(1);
        }

        @Test
        @DisplayName("Should return empty diff when version not found")
        void shouldReturnEmptyDiffWhenVersionNotFound() {
            when(versionRepository.findByLanguageAndVersion(LanguageType.JAVA, "99"))
                    .thenReturn(Optional.empty());
            when(versionRepository.findByLanguageAndVersion(LanguageType.JAVA, "21"))
                    .thenReturn(Optional.of(java21));

            LanguageEvolutionService.VersionDiff diff = service.getVersionDiff(LanguageType.JAVA, "99", "21");

            assertThat(diff.getTotalChanges()).isZero();
        }
    }

    @Nested
    @DisplayName("getSpringBootRequirements Tests")
    class GetSpringBootRequirementsTests {

        @Test
        @DisplayName("Should return requirements for Spring Boot version")
        void shouldReturnSpringBootRequirements() {
            SpringBootVersion sbVersion = new SpringBootVersion();
            sbVersion.setVersion("3.5.8");

            SpringBootLanguageRequirement requirement = new SpringBootLanguageRequirement();
            requirement.setSpringBootVersion(sbVersion);
            requirement.setLanguage(LanguageType.JAVA);
            requirement.setMinVersion("17");
            requirement.setRecommendedVersion("21");

            when(requirementRepository.findBySpringBootVersion("3.5.8"))
                    .thenReturn(List.of(requirement));

            List<SpringBootLanguageRequirement> result = service.getSpringBootRequirements("3.5.8");

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getMinVersion()).isEqualTo("17");
            assertThat(result.get(0).getRecommendedVersion()).isEqualTo("21");
        }

        @Test
        @DisplayName("Should return empty when Spring Boot version not found")
        void shouldReturnEmptyWhenVersionNotFound() {
            when(requirementRepository.findBySpringBootVersion("99.0.0"))
                    .thenReturn(Collections.emptyList());

            List<SpringBootLanguageRequirement> result = service.getSpringBootRequirements("99.0.0");

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("getCategories Tests")
    class GetCategoriesTests {

        @Test
        @DisplayName("Should return distinct categories")
        void shouldReturnDistinctCategories() {
            when(featureRepository.findDistinctCategories())
                    .thenReturn(Arrays.asList("Language", "API", "Performance", "Security"));

            List<String> categories = service.getCategories();

            assertThat(categories).hasSize(4);
            assertThat(categories).contains("Language", "API", "Performance", "Security");
        }
    }

    @Nested
    @DisplayName("getLtsVersions Tests")
    class GetLtsVersionsTests {

        @Test
        @DisplayName("Should return LTS versions")
        void shouldReturnLtsVersions() {
            when(versionRepository.findByLanguageAndIsLtsTrueOrderByMajorVersionDesc(LanguageType.JAVA))
                    .thenReturn(Arrays.asList(java21, java17));

            List<LanguageVersion> ltsVersions = service.getLtsVersions(LanguageType.JAVA);

            assertThat(ltsVersions).hasSize(2);
            assertThat(ltsVersions).allMatch(v -> v.getIsLts());
        }
    }

    @Nested
    @DisplayName("getCurrentVersion Tests")
    class GetCurrentVersionTests {

        @Test
        @DisplayName("Should return current version")
        void shouldReturnCurrentVersion() {
            java21.setIsCurrent(true);
            when(versionRepository.findByLanguageAndIsCurrentTrue(LanguageType.JAVA))
                    .thenReturn(Optional.of(java21));

            Optional<LanguageVersion> currentVersion = service.getCurrentVersion(LanguageType.JAVA);

            assertThat(currentVersion).isPresent();
            assertThat(currentVersion.get().getVersion()).isEqualTo("21");
        }

        @Test
        @DisplayName("Should return empty when no current version")
        void shouldReturnEmptyWhenNoCurrentVersion() {
            when(versionRepository.findByLanguageAndIsCurrentTrue(LanguageType.JAVA))
                    .thenReturn(Optional.empty());

            Optional<LanguageVersion> currentVersion = service.getCurrentVersion(LanguageType.JAVA);

            assertThat(currentVersion).isEmpty();
        }
    }
}
