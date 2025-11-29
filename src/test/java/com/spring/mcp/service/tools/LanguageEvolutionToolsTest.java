package com.spring.mcp.service.tools;

import com.spring.mcp.model.dto.mcp.*;
import com.spring.mcp.model.entity.*;
import com.spring.mcp.model.enums.FeatureStatus;
import com.spring.mcp.model.enums.ImpactLevel;
import com.spring.mcp.model.enums.LanguageType;
import com.spring.mcp.repository.LanguageCodePatternRepository;
import com.spring.mcp.repository.LanguageFeatureRepository;
import com.spring.mcp.service.language.LanguageEvolutionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for LanguageEvolutionTools MCP tools.
 * Tests all 6 MCP tools for language evolution functionality.
 *
 * @author Spring MCP Server
 * @version 1.2.0
 * @since 2025-11-29
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("LanguageEvolutionTools Tests")
class LanguageEvolutionToolsTest {

    @Mock
    private LanguageEvolutionService languageEvolutionService;

    @Mock
    private LanguageFeatureRepository featureRepository;

    @Mock
    private LanguageCodePatternRepository codePatternRepository;

    @InjectMocks
    private LanguageEvolutionTools tools;

    private LanguageVersion java21;
    private LanguageVersion java17;
    private LanguageVersion kotlin20;
    private LanguageFeature virtualThreadsFeature;
    private LanguageFeature recordsFeature;
    private LanguageCodePattern codePattern;

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
        java21.setIsCurrent(true);
        java21.setReleaseDate(LocalDate.of(2023, 9, 19));
        java21.setOssSupportEnd(LocalDate.of(2028, 9, 1));

        // Setup Java 17 version
        java17 = new LanguageVersion();
        java17.setId(2L);
        java17.setLanguage(LanguageType.JAVA);
        java17.setVersion("17");
        java17.setMajorVersion(17);
        java17.setMinorVersion(0);
        java17.setIsLts(true);
        java17.setIsCurrent(false);
        java17.setReleaseDate(LocalDate.of(2021, 9, 14));

        // Setup Kotlin 2.0 version
        kotlin20 = new LanguageVersion();
        kotlin20.setId(3L);
        kotlin20.setLanguage(LanguageType.KOTLIN);
        kotlin20.setVersion("2.0");
        kotlin20.setMajorVersion(2);
        kotlin20.setMinorVersion(0);
        kotlin20.setIsLts(false);
        kotlin20.setIsCurrent(true);
        kotlin20.setReleaseDate(LocalDate.of(2024, 5, 21));

        // Setup Virtual Threads feature
        virtualThreadsFeature = new LanguageFeature();
        virtualThreadsFeature.setId(1L);
        virtualThreadsFeature.setLanguageVersion(java21);
        virtualThreadsFeature.setFeatureName("Virtual Threads");
        virtualThreadsFeature.setDescription("Lightweight threads for high-throughput concurrent applications");
        virtualThreadsFeature.setStatus(FeatureStatus.NEW);
        virtualThreadsFeature.setCategory("Concurrency");
        virtualThreadsFeature.setJepNumber("444");
        virtualThreadsFeature.setImpactLevel(ImpactLevel.HIGH);

        // Setup Records feature
        recordsFeature = new LanguageFeature();
        recordsFeature.setId(2L);
        recordsFeature.setLanguageVersion(java17);
        recordsFeature.setFeatureName("Records");
        recordsFeature.setDescription("Compact syntax for declaring immutable data classes");
        recordsFeature.setStatus(FeatureStatus.NEW);
        recordsFeature.setCategory("Language");
        recordsFeature.setJepNumber("395");
        recordsFeature.setImpactLevel(ImpactLevel.HIGH);

        // Setup Code Pattern
        codePattern = new LanguageCodePattern();
        codePattern.setId(1L);
        codePattern.setFeature(virtualThreadsFeature);
        codePattern.setPatternLanguage("java");
        codePattern.setOldPattern("new Thread(() -> task.run()).start();");
        codePattern.setNewPattern("Thread.startVirtualThread(() -> task.run());");
        codePattern.setExplanation("Virtual threads are more efficient for I/O-bound tasks");
        codePattern.setMinVersion("21");
    }

    @Nested
    @DisplayName("getLanguageVersions Tests")
    class GetLanguageVersionsTests {

        @Test
        @DisplayName("Should return Java versions successfully")
        void shouldReturnJavaVersions() {
            when(languageEvolutionService.getLanguageVersions(LanguageType.JAVA))
                    .thenReturn(Arrays.asList(java21, java17));
            when(languageEvolutionService.getCurrentVersion(LanguageType.JAVA))
                    .thenReturn(Optional.of(java21));
            when(languageEvolutionService.getLtsVersions(LanguageType.JAVA))
                    .thenReturn(Arrays.asList(java21, java17));

            LanguageVersionsResponse response = tools.getLanguageVersions("JAVA");

            assertThat(response.getLanguage()).isEqualTo("Java");
            assertThat(response.getCount()).isEqualTo(2);
            assertThat(response.getVersions()).hasSize(2);
            assertThat(response.getCurrentVersion()).isNotNull();
            assertThat(response.getCurrentVersion().getVersion()).isEqualTo("21");
            assertThat(response.getLtsVersions()).hasSize(2);
        }

        @Test
        @DisplayName("Should return Kotlin versions successfully")
        void shouldReturnKotlinVersions() {
            when(languageEvolutionService.getLanguageVersions(LanguageType.KOTLIN))
                    .thenReturn(List.of(kotlin20));
            when(languageEvolutionService.getCurrentVersion(LanguageType.KOTLIN))
                    .thenReturn(Optional.of(kotlin20));
            when(languageEvolutionService.getLtsVersions(LanguageType.KOTLIN))
                    .thenReturn(Collections.emptyList());

            LanguageVersionsResponse response = tools.getLanguageVersions("KOTLIN");

            assertThat(response.getLanguage()).isEqualTo("Kotlin");
            assertThat(response.getCount()).isEqualTo(1);
            assertThat(response.getVersions()).hasSize(1);
            assertThat(response.getLtsVersions()).isEmpty();
        }

        @Test
        @DisplayName("Should throw exception for invalid language")
        void shouldThrowExceptionForInvalidLanguage() {
            assertThatThrownBy(() -> tools.getLanguageVersions("INVALID"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Invalid language");
        }

        @Test
        @DisplayName("Should throw exception for null language")
        void shouldThrowExceptionForNullLanguage() {
            assertThatThrownBy(() -> tools.getLanguageVersions(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("language parameter is required");
        }

        @Test
        @DisplayName("Should handle case-insensitive language input")
        void shouldHandleCaseInsensitiveLanguage() {
            when(languageEvolutionService.getLanguageVersions(LanguageType.JAVA))
                    .thenReturn(List.of(java21));
            when(languageEvolutionService.getCurrentVersion(LanguageType.JAVA))
                    .thenReturn(Optional.of(java21));
            when(languageEvolutionService.getLtsVersions(LanguageType.JAVA))
                    .thenReturn(List.of(java21));

            LanguageVersionsResponse response = tools.getLanguageVersions("java");

            assertThat(response.getLanguage()).isEqualTo("Java");
        }
    }

    @Nested
    @DisplayName("getLanguageFeatures Tests")
    class GetLanguageFeaturesTests {

        @Test
        @DisplayName("Should return features for specific version")
        void shouldReturnFeaturesForVersion() {
            when(languageEvolutionService.getFeatures(LanguageType.JAVA, "21"))
                    .thenReturn(List.of(virtualThreadsFeature));
            when(codePatternRepository.countByFeatureId(1L)).thenReturn(1L);
            when(languageEvolutionService.getCategories())
                    .thenReturn(Arrays.asList("Concurrency", "Language"));

            LanguageFeaturesResponse response = tools.getLanguageFeatures("JAVA", "21", null, null);

            assertThat(response.getLanguage()).isEqualTo("Java");
            assertThat(response.getVersion()).isEqualTo("21");
            assertThat(response.getTotalCount()).isEqualTo(1);
            assertThat(response.getFeatures()).hasSize(1);
            assertThat(response.getFeatures().get(0).getFeatureName()).isEqualTo("Virtual Threads");
        }

        @Test
        @DisplayName("Should filter features by status")
        void shouldFilterFeaturesByStatus() {
            when(languageEvolutionService.getFeatures(LanguageType.JAVA, "21"))
                    .thenReturn(List.of(virtualThreadsFeature));
            when(codePatternRepository.countByFeatureId(1L)).thenReturn(0L);
            when(languageEvolutionService.getCategories())
                    .thenReturn(List.of("Concurrency"));

            LanguageFeaturesResponse response = tools.getLanguageFeatures("JAVA", "21", "NEW", null);

            assertThat(response.getTotalCount()).isEqualTo(1);
            assertThat(response.getStats().getNewCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("Should filter features by category")
        void shouldFilterFeaturesByCategory() {
            when(languageEvolutionService.getFeatures(LanguageType.JAVA, "21"))
                    .thenReturn(List.of(virtualThreadsFeature));
            when(codePatternRepository.countByFeatureId(1L)).thenReturn(1L);
            when(languageEvolutionService.getCategories())
                    .thenReturn(List.of("Concurrency"));

            LanguageFeaturesResponse response = tools.getLanguageFeatures("JAVA", "21", null, "Concurrency");

            assertThat(response.getTotalCount()).isEqualTo(1);
            assertThat(response.getFeatures().get(0).getCategory()).isEqualTo("Concurrency");
        }

        @Test
        @DisplayName("Should return features with code pattern info")
        void shouldReturnFeaturesWithCodePatternInfo() {
            when(languageEvolutionService.getFeatures(LanguageType.JAVA, "21"))
                    .thenReturn(List.of(virtualThreadsFeature));
            when(codePatternRepository.countByFeatureId(1L)).thenReturn(3L);
            when(languageEvolutionService.getCategories())
                    .thenReturn(List.of("Concurrency"));

            LanguageFeaturesResponse response = tools.getLanguageFeatures("JAVA", "21", null, null);

            assertThat(response.getFeatures().get(0).getHasCodePatterns()).isTrue();
            assertThat(response.getFeatures().get(0).getCodePatternCount()).isEqualTo(3);
        }
    }

    @Nested
    @DisplayName("getModernPatterns Tests")
    class GetModernPatternsTests {

        @Test
        @DisplayName("Should return code patterns for feature")
        void shouldReturnCodePatternsForFeature() {
            when(featureRepository.findById(1L)).thenReturn(Optional.of(virtualThreadsFeature));
            when(languageEvolutionService.getCodePatterns(1L)).thenReturn(List.of(codePattern));

            CodePatternResponse response = tools.getModernPatterns(1L);

            assertThat(response.getFeatureName()).isEqualTo("Virtual Threads");
            assertThat(response.getLanguage()).isEqualTo("Java");
            assertThat(response.getVersion()).isEqualTo("21");
            assertThat(response.getTotalPatterns()).isEqualTo(1);
            assertThat(response.getPatterns()).hasSize(1);
            assertThat(response.getPatterns().get(0).getOldPattern()).contains("new Thread");
            assertThat(response.getPatterns().get(0).getNewPattern()).contains("startVirtualThread");
        }

        @Test
        @DisplayName("Should throw exception when feature not found")
        void shouldThrowExceptionWhenFeatureNotFound() {
            when(featureRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> tools.getModernPatterns(999L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Feature not found: 999");
        }

        @Test
        @DisplayName("Should throw exception for null featureId")
        void shouldThrowExceptionForNullFeatureId() {
            assertThatThrownBy(() -> tools.getModernPatterns(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("featureId parameter is required");
        }
    }

    @Nested
    @DisplayName("getLanguageVersionDiff Tests")
    class GetLanguageVersionDiffTests {

        @Test
        @DisplayName("Should return version diff with new features")
        void shouldReturnVersionDiffWithNewFeatures() {
            LanguageEvolutionService.VersionDiff diff = LanguageEvolutionService.VersionDiff.builder()
                    .language(LanguageType.JAVA)
                    .fromVersion("17")
                    .toVersion("21")
                    .newFeatures(List.of(virtualThreadsFeature))
                    .deprecatedFeatures(Collections.emptyList())
                    .removedFeatures(Collections.emptyList())
                    .build();

            when(languageEvolutionService.getVersionDiff(LanguageType.JAVA, "17", "21"))
                    .thenReturn(diff);

            LanguageVersionDiffResponse response = tools.getLanguageVersionDiff("JAVA", "17", "21");

            assertThat(response.getLanguage()).isEqualTo("Java");
            assertThat(response.getFromVersion()).isEqualTo("17");
            assertThat(response.getToVersion()).isEqualTo("21");
            assertThat(response.getTotalChanges()).isEqualTo(1);
            assertThat(response.getNewFeatures()).hasSize(1);
            assertThat(response.getDeprecatedFeatures()).isEmpty();
            assertThat(response.getRemovedFeatures()).isEmpty();
        }

        @Test
        @DisplayName("Should include migration recommendations")
        void shouldIncludeMigrationRecommendations() {
            LanguageEvolutionService.VersionDiff diff = LanguageEvolutionService.VersionDiff.builder()
                    .language(LanguageType.JAVA)
                    .fromVersion("17")
                    .toVersion("21")
                    .newFeatures(List.of(virtualThreadsFeature))
                    .deprecatedFeatures(Collections.emptyList())
                    .removedFeatures(Collections.emptyList())
                    .build();

            when(languageEvolutionService.getVersionDiff(LanguageType.JAVA, "17", "21"))
                    .thenReturn(diff);

            LanguageVersionDiffResponse response = tools.getLanguageVersionDiff("JAVA", "17", "21");

            assertThat(response.getRecommendations()).isNotNull();
            assertThat(response.getRecommendations().getOverallComplexity()).contains("LOW");
            assertThat(response.getRecommendations().getNiceToHaveItems())
                    .anyMatch(item -> item.contains("Virtual Threads"));
        }

        @Test
        @DisplayName("Should mark high complexity for removed features")
        void shouldMarkHighComplexityForRemovedFeatures() {
            LanguageFeature removedFeature = new LanguageFeature();
            removedFeature.setFeatureName("Removed API");
            removedFeature.setStatus(FeatureStatus.REMOVED);
            removedFeature.setLanguageVersion(java17);

            LanguageEvolutionService.VersionDiff diff = LanguageEvolutionService.VersionDiff.builder()
                    .language(LanguageType.JAVA)
                    .fromVersion("11")
                    .toVersion("17")
                    .newFeatures(Collections.emptyList())
                    .deprecatedFeatures(Collections.emptyList())
                    .removedFeatures(List.of(removedFeature))
                    .build();

            when(languageEvolutionService.getVersionDiff(LanguageType.JAVA, "11", "17"))
                    .thenReturn(diff);

            LanguageVersionDiffResponse response = tools.getLanguageVersionDiff("JAVA", "11", "17");

            assertThat(response.getRecommendations().getOverallComplexity()).contains("HIGH");
            assertThat(response.getRecommendations().getMustDoItems())
                    .anyMatch(item -> item.contains("Removed API"));
        }

        @Test
        @DisplayName("Should throw exception for missing fromVersion")
        void shouldThrowExceptionForMissingFromVersion() {
            assertThatThrownBy(() -> tools.getLanguageVersionDiff("JAVA", null, "21"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("fromVersion parameter is required");
        }

        @Test
        @DisplayName("Should throw exception for missing toVersion")
        void shouldThrowExceptionForMissingToVersion() {
            assertThatThrownBy(() -> tools.getLanguageVersionDiff("JAVA", "17", null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("toVersion parameter is required");
        }
    }

    @Nested
    @DisplayName("getSpringBootLanguageRequirements Tests")
    class GetSpringBootLanguageRequirementsTests {

        @Test
        @DisplayName("Should return requirements from database")
        void shouldReturnRequirementsFromDatabase() {
            SpringBootVersion sbVersion = new SpringBootVersion();
            sbVersion.setVersion("3.5.8");

            SpringBootLanguageRequirement javaReq = new SpringBootLanguageRequirement();
            javaReq.setSpringBootVersion(sbVersion);
            javaReq.setLanguage(LanguageType.JAVA);
            javaReq.setMinVersion("17");
            javaReq.setRecommendedVersion("21");
            javaReq.setMaxVersion("25");

            when(languageEvolutionService.getSpringBootRequirements("3.5.8"))
                    .thenReturn(List.of(javaReq));

            SpringBootLanguageRequirementResponse response = tools.getSpringBootLanguageRequirements("3.5.8");

            assertThat(response.getSpringBootVersion()).isEqualTo("3.5.8");
            assertThat(response.getRequirements()).hasSize(1);
            assertThat(response.getRequirements().get(0).getLanguage()).isEqualTo("Java");
            assertThat(response.getRequirements().get(0).getMinimumVersion()).isEqualTo("17");
            assertThat(response.getRequirements().get(0).getRecommendedVersion()).isEqualTo("21");
        }

        @Test
        @DisplayName("Should generate default requirements when not in database")
        void shouldGenerateDefaultRequirements() {
            when(languageEvolutionService.getSpringBootRequirements("3.5.8"))
                    .thenReturn(Collections.emptyList());

            SpringBootLanguageRequirementResponse response = tools.getSpringBootLanguageRequirements("3.5.8");

            assertThat(response.getSpringBootVersion()).isEqualTo("3.5.8");
            assertThat(response.getRequirements()).hasSize(2); // Java and Kotlin
            assertThat(response.getRequirements().get(0).getMinimumVersion()).isEqualTo("17");
            assertThat(response.getRequirements().get(0).getRecommendedVersion()).isEqualTo("21");
        }

        @Test
        @DisplayName("Should generate correct requirements for Spring Boot 4.x")
        void shouldGenerateCorrectRequirementsForSpringBoot4() {
            when(languageEvolutionService.getSpringBootRequirements("4.0.0"))
                    .thenReturn(Collections.emptyList());

            SpringBootLanguageRequirementResponse response = tools.getSpringBootLanguageRequirements("4.0.0");

            assertThat(response.getRequirements().get(0).getMinimumVersion()).isEqualTo("25");
            assertThat(response.getRequirements().get(0).getRecommendedVersion()).isEqualTo("25");
        }

        @Test
        @DisplayName("Should throw exception for null Spring Boot version")
        void shouldThrowExceptionForNullSpringBootVersion() {
            assertThatThrownBy(() -> tools.getSpringBootLanguageRequirements(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("springBootVersion parameter is required");
        }
    }

    @Nested
    @DisplayName("searchLanguageFeatures Tests")
    class SearchLanguageFeaturesTests {

        @Test
        @DisplayName("Should search features by keyword")
        void shouldSearchFeaturesByKeyword() {
            when(languageEvolutionService.searchFeatures(null, null, null, null, "virtual"))
                    .thenReturn(List.of(virtualThreadsFeature));
            when(codePatternRepository.countByFeatureId(1L)).thenReturn(1L);
            when(languageEvolutionService.getCategories())
                    .thenReturn(List.of("Concurrency"));

            LanguageFeaturesResponse response = tools.searchLanguageFeatures("virtual", null);

            assertThat(response.getLanguage()).isEqualTo("All");
            assertThat(response.getTotalCount()).isEqualTo(1);
            assertThat(response.getFeatures().get(0).getFeatureName()).isEqualTo("Virtual Threads");
        }

        @Test
        @DisplayName("Should search features filtered by language")
        void shouldSearchFeaturesFilteredByLanguage() {
            when(languageEvolutionService.searchFeatures(LanguageType.JAVA, null, null, null, "thread"))
                    .thenReturn(List.of(virtualThreadsFeature));
            when(codePatternRepository.countByFeatureId(1L)).thenReturn(0L);
            when(languageEvolutionService.getCategories())
                    .thenReturn(List.of("Concurrency"));

            LanguageFeaturesResponse response = tools.searchLanguageFeatures("thread", "JAVA");

            assertThat(response.getLanguage()).isEqualTo("Java");
            assertThat(response.getTotalCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("Should return empty results for no matches")
        void shouldReturnEmptyResultsForNoMatches() {
            when(languageEvolutionService.searchFeatures(any(), isNull(), isNull(), isNull(), eq("nonexistent")))
                    .thenReturn(Collections.emptyList());
            when(languageEvolutionService.getCategories())
                    .thenReturn(Collections.emptyList());

            LanguageFeaturesResponse response = tools.searchLanguageFeatures("nonexistent", null);

            assertThat(response.getTotalCount()).isZero();
            assertThat(response.getFeatures()).isEmpty();
        }

        @Test
        @DisplayName("Should throw exception for null search term")
        void shouldThrowExceptionForNullSearchTerm() {
            assertThatThrownBy(() -> tools.searchLanguageFeatures(null, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("searchTerm parameter is required");
        }

        @Test
        @DisplayName("Should throw exception for blank search term")
        void shouldThrowExceptionForBlankSearchTerm() {
            assertThatThrownBy(() -> tools.searchLanguageFeatures("  ", null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("searchTerm parameter is required");
        }
    }
}
