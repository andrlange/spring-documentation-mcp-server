package com.spring.mcp.service.embedding;

import com.spring.mcp.config.EmbeddingProperties;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for EmbeddingServiceImpl.
 * Tests embedding generation, chunking, and cosine similarity.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("EmbeddingServiceImpl Tests")
class EmbeddingServiceImplTest {

    @Mock
    private EmbeddingProvider embeddingProvider;

    @Mock
    private ChunkingService chunkingService;

    @Mock
    private EmbeddingProperties properties;

    private EmbeddingServiceImpl embeddingService;

    @BeforeEach
    void setUp() {
        when(properties.getDimensions()).thenReturn(768);
        when(properties.getBatchSize()).thenReturn(32);

        embeddingService = new EmbeddingServiceImpl(embeddingProvider, chunkingService, properties);
    }

    @Nested
    @DisplayName("embed(String) Tests")
    class SingleEmbedTests {

        @Test
        @DisplayName("Should embed single text successfully")
        void shouldEmbedSingleText() {
            // Given
            String text = "Spring Boot is a framework for building Java applications";
            float[] expectedEmbedding = new float[768];
            for (int i = 0; i < 768; i++) {
                expectedEmbedding[i] = 0.1f * (i % 10);
            }
            when(embeddingProvider.embed(text)).thenReturn(expectedEmbedding);

            // When
            float[] result = embeddingService.embed(text);

            // Then
            assertThat(result).isNotNull();
            assertThat(result).hasSize(768);
            verify(embeddingProvider).embed(text);
        }

        @Test
        @DisplayName("Should return zero-filled array for null text")
        void shouldReturnZeroFilledArrayForNullText() {
            // When
            float[] result = embeddingService.embed((String) null);

            // Then
            assertThat(result).isNotNull();
            assertThat(result).hasSize(768);
            // All values should be 0
            for (float v : result) {
                assertThat(v).isZero();
            }
            verify(embeddingProvider, never()).embed(anyString());
        }

        @Test
        @DisplayName("Should return zero-filled array for blank text")
        void shouldReturnZeroFilledArrayForBlankText() {
            // When
            float[] result = embeddingService.embed("   ");

            // Then
            assertThat(result).isNotNull();
            assertThat(result).hasSize(768);
            verify(embeddingProvider, never()).embed(anyString());
        }
    }

    @Nested
    @DisplayName("embed(List<String>) Tests")
    class BatchEmbedTests {

        @Test
        @DisplayName("Should embed multiple texts successfully")
        void shouldEmbedMultipleTexts() {
            // Given
            List<String> texts = List.of("Text 1", "Text 2", "Text 3");
            List<float[]> expectedEmbeddings = List.of(
                    new float[768],
                    new float[768],
                    new float[768]
            );
            when(embeddingProvider.embed(texts)).thenReturn(expectedEmbeddings);

            // When
            List<float[]> result = embeddingService.embed(texts);

            // Then
            assertThat(result).hasSize(3);
            verify(embeddingProvider).embed(texts);
        }

        @Test
        @DisplayName("Should return empty list for null input")
        void shouldReturnEmptyForNullList() {
            // When
            List<float[]> result = embeddingService.embed((List<String>) null);

            // Then
            assertThat(result).isEmpty();
            verify(embeddingProvider, never()).embed(anyList());
        }

        @Test
        @DisplayName("Should return empty list for empty input")
        void shouldReturnEmptyForEmptyList() {
            // When
            List<float[]> result = embeddingService.embed(List.of());

            // Then
            assertThat(result).isEmpty();
            verify(embeddingProvider, never()).embed(anyList());
        }
    }

    @Nested
    @DisplayName("embedWithChunking Tests")
    class ChunkingTests {

        @Test
        @DisplayName("Should chunk large text and average embeddings")
        void shouldChunkLargeTextAndAverageEmbeddings() {
            // Given
            String longText = "A".repeat(5000); // Simulate long text
            List<String> chunks = List.of("Chunk 1", "Chunk 2");

            float[] embedding1 = new float[768];
            float[] embedding2 = new float[768];
            for (int i = 0; i < 768; i++) {
                embedding1[i] = 1.0f;
                embedding2[i] = 3.0f;
            }

            when(chunkingService.needsChunking(longText)).thenReturn(true);
            when(chunkingService.chunkText(longText)).thenReturn(chunks);
            when(embeddingProvider.embed(chunks)).thenReturn(List.of(embedding1, embedding2));

            // When
            float[] result = embeddingService.embedWithChunking(longText);

            // Then
            assertThat(result).isNotNull();
            assertThat(result).hasSize(768);
            verify(chunkingService).needsChunking(longText);
            verify(chunkingService).chunkText(longText);
            verify(embeddingProvider).embed(chunks);
        }

        @Test
        @DisplayName("Should return zero-filled array for null text")
        void shouldReturnZeroFilledArrayForNullTextWithChunking() {
            // When
            float[] result = embeddingService.embedWithChunking(null);

            // Then
            assertThat(result).isNotNull();
            assertThat(result).hasSize(768);
            verify(chunkingService, never()).chunkText(anyString());
        }

        @Test
        @DisplayName("Should delegate to embed() when chunking not needed")
        void shouldDelegateToEmbedWhenChunkingNotNeeded() {
            // Given
            String shortText = "Short text";
            float[] expectedEmbedding = new float[768];
            expectedEmbedding[0] = 0.5f;

            when(chunkingService.needsChunking(shortText)).thenReturn(false);
            when(embeddingProvider.embed(shortText)).thenReturn(expectedEmbedding);

            // When
            float[] result = embeddingService.embedWithChunking(shortText);

            // Then
            assertThat(result).isNotNull();
            assertThat(result).hasSize(768);
            assertThat(result[0]).isEqualTo(0.5f);
            verify(chunkingService).needsChunking(shortText);
            verify(chunkingService, never()).chunkText(anyString());
            verify(embeddingProvider).embed(shortText);
        }
    }

    @Nested
    @DisplayName("cosineSimilarity Tests")
    class CosineSimilarityTests {

        @Test
        @DisplayName("Should calculate cosine similarity correctly")
        void shouldCalculateCosineSimilarity() {
            // Given - identical vectors should have similarity of 1.0
            float[] embedding1 = {1.0f, 0.0f, 0.0f};
            float[] embedding2 = {1.0f, 0.0f, 0.0f};

            // When
            double similarity = embeddingService.cosineSimilarity(embedding1, embedding2);

            // Then
            assertThat(similarity).isCloseTo(1.0, within(0.0001));
        }

        @Test
        @DisplayName("Should return 0 for orthogonal vectors")
        void shouldReturnZeroForOrthogonalVectors() {
            // Given
            float[] embedding1 = {1.0f, 0.0f, 0.0f};
            float[] embedding2 = {0.0f, 1.0f, 0.0f};

            // When
            double similarity = embeddingService.cosineSimilarity(embedding1, embedding2);

            // Then
            assertThat(similarity).isCloseTo(0.0, within(0.0001));
        }

        @Test
        @DisplayName("Should return -1 for opposite vectors")
        void shouldReturnNegativeOneForOppositeVectors() {
            // Given
            float[] embedding1 = {1.0f, 0.0f, 0.0f};
            float[] embedding2 = {-1.0f, 0.0f, 0.0f};

            // When
            double similarity = embeddingService.cosineSimilarity(embedding1, embedding2);

            // Then
            assertThat(similarity).isCloseTo(-1.0, within(0.0001));
        }

        @Test
        @DisplayName("Should return 0 for null embeddings")
        void shouldReturnZeroForNullEmbeddings() {
            // When & Then
            assertThat(embeddingService.cosineSimilarity(null, new float[3])).isEqualTo(0.0);
            assertThat(embeddingService.cosineSimilarity(new float[3], null)).isEqualTo(0.0);
        }

        @Test
        @DisplayName("Should throw exception for different length embeddings")
        void shouldThrowExceptionForDifferentLengthEmbeddings() {
            // Given
            float[] embedding1 = {1.0f, 0.0f};
            float[] embedding2 = {1.0f, 0.0f, 0.0f};

            // When & Then
            assertThatThrownBy(() -> embeddingService.cosineSimilarity(embedding1, embedding2))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("same dimensions");
        }
    }

    @Nested
    @DisplayName("Provider Status Tests")
    class ProviderStatusTests {

        @Test
        @DisplayName("Should return availability status from provider")
        void shouldReturnAvailabilityFromProvider() {
            // Given
            when(embeddingProvider.isAvailable()).thenReturn(true);

            // When & Then
            assertThat(embeddingService.isAvailable()).isTrue();
        }

        @Test
        @DisplayName("Should return provider name")
        void shouldReturnProviderName() {
            // Given
            when(embeddingProvider.getProviderName()).thenReturn("ollama");

            // When & Then
            assertThat(embeddingService.getProviderName()).isEqualTo("ollama");
        }

        @Test
        @DisplayName("Should return model name")
        void shouldReturnModelName() {
            // Given
            when(embeddingProvider.getModelName()).thenReturn("nomic-embed-text");

            // When & Then
            assertThat(embeddingService.getModelName()).isEqualTo("nomic-embed-text");
        }

        @Test
        @DisplayName("Should return dimensions")
        void shouldReturnDimensions() {
            // Given
            when(embeddingProvider.getDimensions()).thenReturn(768);

            // When & Then
            assertThat(embeddingService.getDimensions()).isEqualTo(768);
        }
    }
}
