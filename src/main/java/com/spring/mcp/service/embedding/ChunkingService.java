package com.spring.mcp.service.embedding;

import com.spring.mcp.config.EmbeddingProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Service for chunking large texts into smaller pieces for embedding.
 * Implements intelligent text splitting with overlap to preserve context.
 *
 * @author Spring MCP Server
 * @version 1.6.0
 * @since 2026-01-01
 */
@Slf4j
@Service
public class ChunkingService {

    private final int chunkSize;
    private final int chunkOverlap;

    // Approximate tokens per character (for English text, ~4 chars per token)
    private static final double CHARS_PER_TOKEN = 4.0;

    // Pattern for splitting on sentence boundaries
    private static final Pattern SENTENCE_PATTERN = Pattern.compile("(?<=[.!?])\\s+");

    // Pattern for splitting on paragraph boundaries
    private static final Pattern PARAGRAPH_PATTERN = Pattern.compile("\\n\\n+");

    public ChunkingService(EmbeddingProperties properties) {
        this.chunkSize = properties.getChunkSize();
        this.chunkOverlap = properties.getChunkOverlap();
        log.debug("ChunkingService initialized with chunkSize={}, chunkOverlap={}", chunkSize, chunkOverlap);
    }

    /**
     * Split text into chunks suitable for embedding.
     * Uses intelligent splitting that respects sentence and paragraph boundaries.
     *
     * @param text the text to chunk
     * @return list of text chunks
     */
    public List<String> chunkText(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }

        int estimatedTokens = estimateTokens(text);
        if (estimatedTokens <= chunkSize) {
            return List.of(text.trim());
        }

        log.debug("Chunking text: {} chars, ~{} tokens", text.length(), estimatedTokens);

        // Try paragraph-based chunking first
        List<String> chunks = chunkByParagraphs(text);
        if (chunks.isEmpty()) {
            // Fall back to sentence-based chunking
            chunks = chunkBySentences(text);
        }
        if (chunks.isEmpty()) {
            // Fall back to character-based chunking
            chunks = chunkByCharacters(text);
        }

        log.debug("Created {} chunks from text", chunks.size());
        return chunks;
    }

    /**
     * Chunk text by paragraph boundaries.
     */
    private List<String> chunkByParagraphs(String text) {
        String[] paragraphs = PARAGRAPH_PATTERN.split(text);
        return buildChunks(paragraphs);
    }

    /**
     * Chunk text by sentence boundaries.
     */
    private List<String> chunkBySentences(String text) {
        String[] sentences = SENTENCE_PATTERN.split(text);
        return buildChunks(sentences);
    }

    /**
     * Chunk text by character count (fallback method).
     */
    private List<String> chunkByCharacters(String text) {
        List<String> chunks = new ArrayList<>();
        int maxChars = (int) (chunkSize * CHARS_PER_TOKEN);
        int overlapChars = (int) (chunkOverlap * CHARS_PER_TOKEN);

        // Ensure we make progress
        int minAdvance = Math.max(1, maxChars - overlapChars);

        int start = 0;
        while (start < text.length()) {
            int end = Math.min(start + maxChars, text.length());

            // Try to find a word boundary (only if not at end of text)
            if (end < text.length()) {
                int lastSpace = text.lastIndexOf(' ', end);
                if (lastSpace > start) {
                    end = lastSpace;
                }
                // If no space found, just use maxChars to ensure progress
            }

            String chunk = text.substring(start, end).trim();
            if (!chunk.isEmpty()) {
                chunks.add(chunk);
            }

            // If we've reached the end, stop
            if (end >= text.length()) {
                break;
            }

            // Move start forward, ensuring we always make progress
            int nextStart = end - overlapChars;
            if (nextStart <= start) {
                nextStart = start + minAdvance;
            }
            start = nextStart;
        }

        return chunks;
    }

    /**
     * Build chunks from an array of text segments (paragraphs or sentences).
     */
    private List<String> buildChunks(String[] segments) {
        List<String> chunks = new ArrayList<>();
        StringBuilder currentChunk = new StringBuilder();
        int currentTokens = 0;
        List<String> overlapBuffer = new ArrayList<>();

        for (String segment : segments) {
            String trimmedSegment = segment.trim();
            if (trimmedSegment.isEmpty()) {
                continue;
            }

            int segmentTokens = estimateTokens(trimmedSegment);

            // If this single segment exceeds chunk size, we need to split it further
            if (segmentTokens > chunkSize) {
                // Save current chunk if not empty
                if (currentChunk.length() > 0) {
                    chunks.add(currentChunk.toString().trim());
                    currentChunk = new StringBuilder();
                    currentTokens = 0;
                }
                // Split the large segment by characters
                chunks.addAll(chunkByCharacters(trimmedSegment));
                overlapBuffer.clear();
                continue;
            }

            // If adding this segment would exceed chunk size, start a new chunk
            if (currentTokens + segmentTokens > chunkSize && currentChunk.length() > 0) {
                chunks.add(currentChunk.toString().trim());

                // Start new chunk with overlap from previous segments
                currentChunk = new StringBuilder();
                int overlapTokens = 0;
                for (int i = overlapBuffer.size() - 1; i >= 0 && overlapTokens < chunkOverlap; i--) {
                    String overlapSegment = overlapBuffer.get(i);
                    int tokens = estimateTokens(overlapSegment);
                    if (overlapTokens + tokens <= chunkOverlap) {
                        currentChunk.insert(0, overlapSegment + " ");
                        overlapTokens += tokens;
                    }
                }
                currentTokens = overlapTokens;
                overlapBuffer.clear();
            }

            // Add segment to current chunk
            if (currentChunk.length() > 0) {
                currentChunk.append(" ");
            }
            currentChunk.append(trimmedSegment);
            currentTokens += segmentTokens;
            overlapBuffer.add(trimmedSegment);

            // Keep overlap buffer size manageable
            while (overlapBuffer.size() > 10) {
                overlapBuffer.remove(0);
            }
        }

        // Don't forget the last chunk
        if (currentChunk.length() > 0) {
            chunks.add(currentChunk.toString().trim());
        }

        return chunks;
    }

    /**
     * Estimate the number of tokens in a text.
     * Uses a simple heuristic: ~4 characters per token for English text.
     *
     * @param text the text to estimate
     * @return estimated token count
     */
    public int estimateTokens(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        return (int) Math.ceil(text.length() / CHARS_PER_TOKEN);
    }

    /**
     * Check if text needs chunking based on estimated token count.
     *
     * @param text the text to check
     * @return true if text exceeds chunk size and needs chunking
     */
    public boolean needsChunking(String text) {
        return estimateTokens(text) > chunkSize;
    }

    /**
     * Get the configured chunk size.
     *
     * @return chunk size in tokens
     */
    public int getChunkSize() {
        return chunkSize;
    }

    /**
     * Get the configured chunk overlap.
     *
     * @return chunk overlap in tokens
     */
    public int getChunkOverlap() {
        return chunkOverlap;
    }
}
