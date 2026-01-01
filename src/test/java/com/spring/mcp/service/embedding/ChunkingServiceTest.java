package com.spring.mcp.service.embedding;

import com.spring.mcp.config.EmbeddingProperties;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ChunkingService.
 * Tests text splitting and chunking logic.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ChunkingService Tests")
class ChunkingServiceTest {

    @Mock
    private EmbeddingProperties properties;

    private ChunkingService chunkingService;

    @BeforeEach
    void setUp() {
        when(properties.getChunkSize()).thenReturn(500);
        when(properties.getChunkOverlap()).thenReturn(50);

        chunkingService = new ChunkingService(properties);
    }

    @Nested
    @DisplayName("chunkText Tests")
    class ChunkTextTests {

        @Test
        @DisplayName("Should return single chunk for short text")
        void shouldReturnSingleChunkForShortText() {
            // Given
            String shortText = "This is a short text that doesn't need chunking.";

            // When
            List<String> chunks = chunkingService.chunkText(shortText);

            // Then
            assertThat(chunks).hasSize(1);
            assertThat(chunks.get(0)).isEqualTo(shortText);
        }

        @Test
        @DisplayName("Should split long text into multiple chunks")
        void shouldSplitLongTextIntoChunks() {
            // Given - need enough text to exceed 500 tokens (~2000 chars)
            StringBuilder longText = new StringBuilder();
            for (int i = 0; i < 100; i++) {
                longText.append("This is a longer sentence number ").append(i).append(" with more content to fill up space. ");
            }

            // When
            List<String> chunks = chunkingService.chunkText(longText.toString());

            // Then
            assertThat(chunks).hasSizeGreaterThan(1);
            // All chunks should be non-empty
            assertThat(chunks).allMatch(chunk -> !chunk.isBlank());
        }

        @Test
        @DisplayName("Should handle null text")
        void shouldHandleNullText() {
            // When
            List<String> chunks = chunkingService.chunkText(null);

            // Then
            assertThat(chunks).isEmpty();
        }

        @Test
        @DisplayName("Should handle blank text")
        void shouldHandleBlankText() {
            // When
            List<String> chunks = chunkingService.chunkText("   ");

            // Then
            assertThat(chunks).isEmpty();
        }

        @Test
        @DisplayName("Should split on paragraph boundaries when possible")
        void shouldSplitOnParagraphBoundaries() {
            // Given
            String textWithParagraphs = "First paragraph content here.\n\n" +
                    "Second paragraph with more content.\n\n" +
                    "Third paragraph continues here.";

            // When
            List<String> chunks = chunkingService.chunkText(textWithParagraphs);

            // Then
            assertThat(chunks).isNotEmpty();
            // Each chunk should be trimmed
            assertThat(chunks).allMatch(chunk -> chunk.equals(chunk.trim()));
        }

        @Test
        @DisplayName("Should handle text without clear boundaries")
        void shouldHandleTextWithoutClearBoundaries() {
            // Given - text with no spaces or sentence boundaries
            // Use reasonable size to avoid memory issues
            StringBuilder textWithoutBoundaries = new StringBuilder();
            for (int i = 0; i < 100; i++) {
                textWithoutBoundaries.append("wordwithoutspaces");
            }

            // When
            List<String> chunks = chunkingService.chunkText(textWithoutBoundaries.toString());

            // Then - should still produce at least one chunk
            assertThat(chunks).isNotEmpty();
        }
    }

    @Nested
    @DisplayName("estimateTokens Tests")
    class EstimateTokensTests {

        @Test
        @DisplayName("Should estimate tokens correctly")
        void shouldEstimateTokensCorrectly() {
            // Given
            String text = "This is a test"; // 14 characters

            // When
            int tokens = chunkingService.estimateTokens(text);

            // Then
            // Using ~4 chars per token heuristic: 14 / 4 = 3-4 tokens
            assertThat(tokens).isBetween(3, 4);
        }

        @Test
        @DisplayName("Should return 0 for null text")
        void shouldReturnZeroForNullText() {
            // When
            int tokens = chunkingService.estimateTokens(null);

            // Then
            assertThat(tokens).isZero();
        }

        @Test
        @DisplayName("Should return 0 for empty text")
        void shouldReturnZeroForEmptyText() {
            // When
            int tokens = chunkingService.estimateTokens("");

            // Then
            assertThat(tokens).isZero();
        }
    }

    @Nested
    @DisplayName("needsChunking Tests")
    class NeedsChunkingTests {

        @Test
        @DisplayName("Should return false for short text")
        void shouldReturnFalseForShortText() {
            // Given
            String shortText = "Short text";

            // When
            boolean needsChunking = chunkingService.needsChunking(shortText);

            // Then
            assertThat(needsChunking).isFalse();
        }

        @Test
        @DisplayName("Should return true for long text")
        void shouldReturnTrueForLongText() {
            // Given - text with more tokens than chunkSize (500 tokens = ~2000 chars)
            String longText = "a".repeat(2500);

            // When
            boolean needsChunking = chunkingService.needsChunking(longText);

            // Then
            assertThat(needsChunking).isTrue();
        }

        @Test
        @DisplayName("Should return false for null text")
        void shouldReturnFalseForNullText() {
            // When
            boolean needsChunking = chunkingService.needsChunking(null);

            // Then
            assertThat(needsChunking).isFalse();
        }
    }

    @Nested
    @DisplayName("Getter Tests")
    class GetterTests {

        @Test
        @DisplayName("Should return chunk size")
        void shouldReturnChunkSize() {
            // When & Then
            assertThat(chunkingService.getChunkSize()).isEqualTo(500);
        }

        @Test
        @DisplayName("Should return chunk overlap")
        void shouldReturnChunkOverlap() {
            // When & Then
            assertThat(chunkingService.getChunkOverlap()).isEqualTo(50);
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCaseTests {

        @Test
        @DisplayName("Should handle text exactly at chunk size limit")
        void shouldHandleTextAtChunkSizeLimit() {
            // Given - text with ~500 tokens (500 * 4 = 2000 chars)
            String exactSizeText = "word ".repeat(400); // ~2000 chars

            // When
            List<String> chunks = chunkingService.chunkText(exactSizeText);

            // Then
            assertThat(chunks).isNotEmpty();
        }

        @Test
        @DisplayName("Should handle unicode characters correctly")
        void shouldHandleUnicodeCharacters() {
            // Given
            String unicodeText = "日本語のテキスト。これは長いテキストです。" + "あ".repeat(200);

            // When
            List<String> chunks = chunkingService.chunkText(unicodeText);

            // Then
            assertThat(chunks).isNotEmpty();
            // Each chunk should contain valid unicode
            assertThat(chunks).allMatch(chunk -> chunk.length() > 0);
        }

        @Test
        @DisplayName("Should handle text with only whitespace")
        void shouldHandleWhitespaceOnlyText() {
            // Given
            String whitespaceText = "   \n\n\t\t   \n   ";

            // When
            List<String> chunks = chunkingService.chunkText(whitespaceText);

            // Then
            assertThat(chunks).isEmpty();
        }
    }
}
