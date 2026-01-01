package com.spring.mcp.service.embedding;

import org.junit.jupiter.api.*;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for NoOpEmbeddingService.
 * Tests the no-operation implementation when embeddings are disabled.
 */
@DisplayName("NoOpEmbeddingService Tests")
class NoOpEmbeddingServiceTest {

    private static final int DEFAULT_DIMENSIONS = 768;

    private NoOpEmbeddingService noOpService;

    @BeforeEach
    void setUp() {
        noOpService = new NoOpEmbeddingService();
    }

    @Test
    @DisplayName("embed(String) should return array with default dimensions")
    void embedStringShouldReturnArrayWithDefaultDimensions() {
        // When
        float[] result = noOpService.embed("any text");

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(DEFAULT_DIMENSIONS);
        // All values should be 0
        for (float value : result) {
            assertThat(value).isZero();
        }
    }

    @Test
    @DisplayName("embed(List) should return empty list")
    void embedListShouldReturnEmptyList() {
        // When
        List<float[]> result = noOpService.embed(List.of("text1", "text2"));

        // Then
        assertThat(result).isNotNull();
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("embedWithChunking should return array with default dimensions")
    void embedWithChunkingShouldReturnArrayWithDefaultDimensions() {
        // When
        float[] result = noOpService.embedWithChunking("any long text");

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(DEFAULT_DIMENSIONS);
    }

    @Test
    @DisplayName("isAvailable should return false")
    void isAvailableShouldReturnFalse() {
        // When & Then
        assertThat(noOpService.isAvailable()).isFalse();
    }

    @Test
    @DisplayName("getProviderName should return 'none'")
    void getProviderNameShouldReturnNone() {
        // When & Then
        assertThat(noOpService.getProviderName()).isEqualTo("none");
    }

    @Test
    @DisplayName("getModelName should return 'disabled'")
    void getModelNameShouldReturnDisabled() {
        // When & Then
        assertThat(noOpService.getModelName()).isEqualTo("disabled");
    }

    @Test
    @DisplayName("getDimensions should return default dimensions")
    void getDimensionsShouldReturnDefaultDimensions() {
        // When & Then
        assertThat(noOpService.getDimensions()).isEqualTo(DEFAULT_DIMENSIONS);
    }

    @Test
    @DisplayName("cosineSimilarity should return 0")
    void cosineSimilarityShouldReturnZero() {
        // Given
        float[] embedding1 = {1.0f, 0.0f};
        float[] embedding2 = {1.0f, 0.0f};

        // When
        double similarity = noOpService.cosineSimilarity(embedding1, embedding2);

        // Then
        assertThat(similarity).isZero();
    }
}
