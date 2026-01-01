package com.spring.mcp.service.embedding;

import com.spring.mcp.config.EmbeddingProperties;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for HybridSearchService.
 * Tests hybrid search combining keyword and semantic search with RRF.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("HybridSearchService Tests")
class HybridSearchServiceTest {

    @Mock
    private EmbeddingService embeddingService;

    @Mock
    private EmbeddingProperties properties;

    @Mock
    private EmbeddingProperties.HybridConfig hybridConfig;

    @Mock
    private JdbcTemplate jdbcTemplate;

    private HybridSearchService hybridSearchService;

    @BeforeEach
    void setUp() {
        when(properties.getHybrid()).thenReturn(hybridConfig);
        when(hybridConfig.getAlpha()).thenReturn(0.3);
        when(hybridConfig.getMinSimilarity()).thenReturn(0.5);

        hybridSearchService = new HybridSearchService(embeddingService, properties, jdbcTemplate);
    }

    @Nested
    @DisplayName("searchDocumentation Tests")
    class SearchDocumentationTests {

        @Test
        @DisplayName("Should return empty list for null query")
        void shouldReturnEmptyForNullQuery() {
            // When
            List<HybridSearchService.SearchResult> results =
                    hybridSearchService.searchDocumentation(null, 10);

            // Then
            assertThat(results).isEmpty();
        }

        @Test
        @DisplayName("Should return empty list for blank query")
        void shouldReturnEmptyForBlankQuery() {
            // When
            List<HybridSearchService.SearchResult> results =
                    hybridSearchService.searchDocumentation("   ", 10);

            // Then
            assertThat(results).isEmpty();
        }

        @Test
        @DisplayName("Should fall back to keyword search when embeddings unavailable")
        void shouldFallbackToKeywordSearchWhenEmbeddingsUnavailable() {
            // Given
            when(embeddingService.isAvailable()).thenReturn(false);

            // When
            List<HybridSearchService.SearchResult> results =
                    hybridSearchService.searchDocumentation("spring boot", 10);

            // Then
            // Should still complete without errors
            assertThat(results).isNotNull();
            verify(embeddingService, never()).embed(anyString());
        }
    }

    @Nested
    @DisplayName("searchTransformations Tests")
    class SearchTransformationsTests {

        @Test
        @DisplayName("Should return empty list for null query")
        void shouldReturnEmptyForNullQuery() {
            // When
            List<HybridSearchService.SearchResult> results =
                    hybridSearchService.searchTransformations(null, 10);

            // Then
            assertThat(results).isEmpty();
        }

        @Test
        @DisplayName("Should handle exception gracefully")
        void shouldHandleExceptionGracefully() {
            // Given
            when(embeddingService.isAvailable()).thenReturn(true);
            when(embeddingService.embed(anyString())).thenThrow(new RuntimeException("Test error"));

            // When
            List<HybridSearchService.SearchResult> results =
                    hybridSearchService.searchTransformations("test query", 10);

            // Then - should not throw and return results (may be empty due to fallback)
            assertThat(results).isNotNull();
        }
    }

    @Nested
    @DisplayName("searchFlavors Tests")
    class SearchFlavorsTests {

        @Test
        @DisplayName("Should return empty list for null query")
        void shouldReturnEmptyForNullQuery() {
            // When
            List<HybridSearchService.SearchResult> results =
                    hybridSearchService.searchFlavors(null, 10);

            // Then
            assertThat(results).isEmpty();
        }

        @Test
        @DisplayName("Should return empty list for blank query")
        void shouldReturnEmptyForBlankQuery() {
            // When
            List<HybridSearchService.SearchResult> results =
                    hybridSearchService.searchFlavors("", 10);

            // Then
            assertThat(results).isEmpty();
        }
    }

    @Nested
    @DisplayName("searchCodeExamples Tests")
    class SearchCodeExamplesTests {

        @Test
        @DisplayName("Should return empty list for null query")
        void shouldReturnEmptyForNullQuery() {
            // When
            List<HybridSearchService.SearchResult> results =
                    hybridSearchService.searchCodeExamples(null, 10);

            // Then
            assertThat(results).isEmpty();
        }
    }

    @Nested
    @DisplayName("RRF Algorithm Tests")
    class RrfAlgorithmTests {

        @Test
        @DisplayName("SearchResult should have correct structure")
        void searchResultShouldHaveCorrectStructure() {
            // Given
            HybridSearchService.SearchResult result = new HybridSearchService.SearchResult(1L, 0.85);

            // Then
            assertThat(result.id()).isEqualTo(1L);
            assertThat(result.score()).isEqualTo(0.85);
        }

        @Test
        @DisplayName("Multiple SearchResults should be comparable by score")
        void searchResultsShouldBeComparableByScore() {
            // Given
            HybridSearchService.SearchResult result1 = new HybridSearchService.SearchResult(1L, 0.85);
            HybridSearchService.SearchResult result2 = new HybridSearchService.SearchResult(2L, 0.95);
            HybridSearchService.SearchResult result3 = new HybridSearchService.SearchResult(3L, 0.75);

            List<HybridSearchService.SearchResult> results = List.of(result1, result2, result3);

            // When
            List<HybridSearchService.SearchResult> sorted = results.stream()
                    .sorted((a, b) -> Double.compare(b.score(), a.score()))
                    .toList();

            // Then
            assertThat(sorted.get(0).id()).isEqualTo(2L);
            assertThat(sorted.get(1).id()).isEqualTo(1L);
            assertThat(sorted.get(2).id()).isEqualTo(3L);
        }
    }

    @Nested
    @DisplayName("Configuration Tests")
    class ConfigurationTests {

        @Test
        @DisplayName("Should use configured alpha value")
        void shouldUseConfiguredAlpha() {
            // Given - alpha is set up in beforeEach

            // When
            hybridSearchService.searchDocumentation("test", 10);

            // Then - verify hybridConfig was accessed
            verify(hybridConfig, atLeastOnce()).getAlpha();
        }

        @Test
        @DisplayName("Should use configured minSimilarity value")
        void shouldUseConfiguredMinSimilarity() {
            // Given - minSimilarity is set up in beforeEach

            // When
            hybridSearchService.searchDocumentation("test", 10);

            // Then - verify hybridConfig was accessed
            verify(hybridConfig, atLeastOnce()).getMinSimilarity();
        }
    }
}
