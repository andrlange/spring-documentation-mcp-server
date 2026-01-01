package com.spring.mcp.service.embedding;

import java.util.List;

/**
 * Service interface for embedding operations.
 * Provides methods for generating and managing text embeddings.
 *
 * @author Spring MCP Server
 * @version 1.6.0
 * @since 2026-01-01
 */
public interface EmbeddingService {

    /**
     * Generate embedding for a single text.
     *
     * @param text the text to embed
     * @return embedding vector
     */
    float[] embed(String text);

    /**
     * Generate embeddings for multiple texts.
     *
     * @param texts list of texts to embed
     * @return list of embedding vectors
     */
    List<float[]> embed(List<String> texts);

    /**
     * Generate embedding for a text that may be too long for the model.
     * Large texts are chunked, and the embeddings are aggregated.
     *
     * @param text the text to embed
     * @return embedding vector (average of chunk embeddings if chunked)
     */
    float[] embedWithChunking(String text);

    /**
     * Check if the embedding service is available.
     *
     * @return true if the service is available and ready
     */
    boolean isAvailable();

    /**
     * Get the name of the current embedding provider.
     *
     * @return provider name
     */
    String getProviderName();

    /**
     * Get the embedding model name.
     *
     * @return model name
     */
    String getModelName();

    /**
     * Get the dimensionality of embeddings.
     *
     * @return number of dimensions
     */
    int getDimensions();

    /**
     * Calculate cosine similarity between two embedding vectors.
     *
     * @param embedding1 first embedding
     * @param embedding2 second embedding
     * @return cosine similarity score (0.0 to 1.0)
     */
    double cosineSimilarity(float[] embedding1, float[] embedding2);
}
