package com.spring.mcp.service.embedding;

import com.spring.mcp.config.EmbeddingProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of EmbeddingService that delegates to an EmbeddingProvider.
 * Handles chunking of large texts and aggregation of chunk embeddings.
 *
 * @author Spring MCP Server
 * @version 1.6.0
 * @since 2026-01-01
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "mcp.features.embeddings.enabled", havingValue = "true")
public class EmbeddingServiceImpl implements EmbeddingService {

    private final EmbeddingProvider embeddingProvider;
    private final ChunkingService chunkingService;
    private final EmbeddingProperties properties;

    @Override
    public float[] embed(String text) {
        if (text == null || text.isBlank()) {
            log.warn("Attempted to embed null or blank text");
            return new float[properties.getDimensions()];
        }

        try {
            return embeddingProvider.embed(text);
        } catch (Exception e) {
            log.error("Failed to embed text: {}", e.getMessage(), e);
            throw new EmbeddingException("Failed to generate embedding", e);
        }
    }

    @Override
    public List<float[]> embed(List<String> texts) {
        if (texts == null || texts.isEmpty()) {
            return List.of();
        }

        try {
            // Filter out null/blank texts
            List<String> validTexts = texts.stream()
                    .filter(t -> t != null && !t.isBlank())
                    .toList();

            if (validTexts.isEmpty()) {
                return List.of();
            }

            // Process in batches
            List<float[]> results = new ArrayList<>();
            int batchSize = properties.getBatchSize();

            for (int i = 0; i < validTexts.size(); i += batchSize) {
                int end = Math.min(i + batchSize, validTexts.size());
                List<String> batch = validTexts.subList(i, end);

                log.debug("Processing embedding batch {}-{} of {}", i, end, validTexts.size());
                List<float[]> batchResults = embeddingProvider.embed(batch);
                results.addAll(batchResults);
            }

            return results;
        } catch (Exception e) {
            log.error("Failed to embed texts: {}", e.getMessage(), e);
            throw new EmbeddingException("Failed to generate embeddings", e);
        }
    }

    @Override
    public float[] embedWithChunking(String text) {
        if (text == null || text.isBlank()) {
            return new float[properties.getDimensions()];
        }

        // Check if chunking is needed
        if (!chunkingService.needsChunking(text)) {
            return embed(text);
        }

        try {
            // Split text into chunks
            List<String> chunks = chunkingService.chunkText(text);
            log.debug("Text chunked into {} pieces for embedding", chunks.size());

            if (chunks.isEmpty()) {
                return new float[properties.getDimensions()];
            }

            if (chunks.size() == 1) {
                return embed(chunks.get(0));
            }

            // Embed all chunks
            List<float[]> chunkEmbeddings = embed(chunks);

            // Average the chunk embeddings
            return averageEmbeddings(chunkEmbeddings);
        } catch (Exception e) {
            log.error("Failed to embed text with chunking: {}", e.getMessage(), e);
            throw new EmbeddingException("Failed to generate embedding with chunking", e);
        }
    }

    @Override
    public boolean isAvailable() {
        try {
            return embeddingProvider.isAvailable();
        } catch (Exception e) {
            log.warn("Error checking embedding provider availability: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public String getProviderName() {
        return embeddingProvider.getProviderName();
    }

    @Override
    public String getModelName() {
        return embeddingProvider.getModelName();
    }

    @Override
    public int getDimensions() {
        return embeddingProvider.getDimensions();
    }

    @Override
    public double cosineSimilarity(float[] embedding1, float[] embedding2) {
        if (embedding1 == null || embedding2 == null) {
            return 0.0;
        }

        if (embedding1.length != embedding2.length) {
            throw new IllegalArgumentException(
                    "Embeddings must have same dimensions: " + embedding1.length + " vs " + embedding2.length);
        }

        double dotProduct = 0.0;
        double norm1 = 0.0;
        double norm2 = 0.0;

        for (int i = 0; i < embedding1.length; i++) {
            dotProduct += embedding1[i] * embedding2[i];
            norm1 += embedding1[i] * embedding1[i];
            norm2 += embedding2[i] * embedding2[i];
        }

        double denominator = Math.sqrt(norm1) * Math.sqrt(norm2);
        if (denominator == 0) {
            return 0.0;
        }

        return dotProduct / denominator;
    }

    /**
     * Average multiple embeddings into a single embedding vector.
     * Used for combining chunk embeddings.
     */
    private float[] averageEmbeddings(List<float[]> embeddings) {
        if (embeddings.isEmpty()) {
            return new float[properties.getDimensions()];
        }

        int dimensions = embeddings.get(0).length;
        float[] result = new float[dimensions];

        for (float[] embedding : embeddings) {
            for (int i = 0; i < dimensions; i++) {
                result[i] += embedding[i];
            }
        }

        int count = embeddings.size();
        for (int i = 0; i < dimensions; i++) {
            result[i] /= count;
        }

        // Normalize the result
        return normalize(result);
    }

    /**
     * Normalize an embedding vector to unit length.
     */
    private float[] normalize(float[] embedding) {
        float norm = 0;
        for (float v : embedding) {
            norm += v * v;
        }
        norm = (float) Math.sqrt(norm);

        if (norm == 0) {
            return embedding;
        }

        float[] normalized = new float[embedding.length];
        for (int i = 0; i < embedding.length; i++) {
            normalized[i] = embedding[i] / norm;
        }
        return normalized;
    }

    /**
     * Exception for embedding-related errors.
     */
    public static class EmbeddingException extends RuntimeException {
        public EmbeddingException(String message) {
            super(message);
        }

        public EmbeddingException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
