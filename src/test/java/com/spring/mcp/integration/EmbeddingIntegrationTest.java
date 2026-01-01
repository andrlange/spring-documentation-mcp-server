package com.spring.mcp.integration;

import com.spring.mcp.config.EmbeddingProperties;
import com.spring.mcp.service.embedding.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration tests for the Embedding feature.
 * Tests the complete embedding pipeline with PostgreSQL (pgvector).
 */
@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
@DisplayName("Embedding Integration Tests")
class EmbeddingIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
            DockerImageName.parse("pgvector/pgvector:pg17")
                    .asCompatibleSubstituteFor("postgres")
    ).withDatabaseName("spring_mcp_test")
     .withUsername("test")
     .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        // Disable embeddings by default for tests (no Ollama in CI)
        registry.add("mcp.features.embeddings.enabled", () -> "false");
    }

    @Autowired
    private EmbeddingService embeddingService;

    @Autowired
    private EmbeddingProperties embeddingProperties;

    @Autowired(required = false)
    private HybridSearchService hybridSearchService;

    @Autowired(required = false)
    private ChunkingService chunkingService;

    @Nested
    @DisplayName("When Embeddings Disabled")
    class EmbeddingsDisabledTests {

        @Test
        @DisplayName("Should use NoOpEmbeddingService when disabled")
        void shouldUseNoOpServiceWhenDisabled() {
            // When embeddings are disabled, we should get NoOpEmbeddingService
            assertThat(embeddingService).isNotNull();
            assertThat(embeddingService.isAvailable()).isFalse();
            assertThat(embeddingService.getProviderName()).isEqualTo("none");
        }

        @Test
        @DisplayName("NoOp service should return zero-filled embeddings with default dimensions")
        void noOpServiceShouldReturnZeroFilledEmbeddings() {
            // When
            float[] embedding = embeddingService.embed("test text");

            // Then - NoOp returns array with default dimensions (768)
            assertThat(embedding).isNotNull();
            assertThat(embedding).hasSize(768);
        }

        @Test
        @DisplayName("NoOp service should handle batch requests")
        void noOpServiceShouldHandleBatchRequests() {
            // When
            var embeddings = embeddingService.embed(java.util.List.of("text1", "text2"));

            // Then - batch embed returns empty list for NoOp
            assertThat(embeddings).isEmpty();
        }
    }

    @Nested
    @DisplayName("Embedding Properties Tests")
    class EmbeddingPropertiesTests {

        @Test
        @DisplayName("Should load embedding configuration")
        void shouldLoadEmbeddingConfiguration() {
            // Then
            assertThat(embeddingProperties).isNotNull();
        }

        @Test
        @DisplayName("Should have default dimension value")
        void shouldHaveDefaultDimensionValue() {
            // Default is 768 for nomic-embed-text
            assertThat(embeddingProperties.getDimensions()).isPositive();
        }

        @Test
        @DisplayName("Should have hybrid search configuration")
        void shouldHaveHybridSearchConfiguration() {
            // Then
            assertThat(embeddingProperties.getHybrid()).isNotNull();
            assertThat(embeddingProperties.getHybrid().getAlpha()).isBetween(0.0, 1.0);
        }

        @Test
        @DisplayName("Should have chunking configuration")
        void shouldHaveChunkingConfiguration() {
            // Then
            assertThat(embeddingProperties.getChunkSize()).isPositive();
            assertThat(embeddingProperties.getChunkOverlap()).isPositive();
        }
    }

    @Nested
    @DisplayName("ChunkingService Tests")
    class ChunkingServiceTests {

        @Test
        @DisplayName("ChunkingService should be available")
        void chunkingServiceShouldBeAvailable() {
            // ChunkingService should always be available
            assertThat(chunkingService).isNotNull();
        }

        @Test
        @DisplayName("Should chunk text correctly")
        void shouldChunkTextCorrectly() {
            // Skip if chunkingService is null
            Assumptions.assumeTrue(chunkingService != null);

            // Given
            String longText = "This is a sentence. ".repeat(100);

            // When
            var chunks = chunkingService.chunkText(longText);

            // Then
            assertThat(chunks).isNotEmpty();
        }
    }

    @Nested
    @DisplayName("Cosine Similarity Tests")
    class CosineSimilarityTests {

        @Test
        @DisplayName("NoOp service should return 0.0 for cosine similarity")
        void noOpServiceShouldReturnZeroForCosineSimilarity() {
            // Given
            float[] embedding1 = {1.0f, 0.0f, 0.0f};
            float[] embedding2 = {1.0f, 0.0f, 0.0f};

            // When
            double similarity = embeddingService.cosineSimilarity(embedding1, embedding2);

            // Then - NoOp implementation always returns 0.0
            assertThat(similarity).isEqualTo(0.0);
        }
    }
}
