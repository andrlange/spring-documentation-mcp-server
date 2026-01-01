package com.spring.mcp.service.embedding;

import java.util.List;

/**
 * Interface for embedding providers (Ollama, OpenAI, etc.).
 * Provides a unified abstraction for generating text embeddings.
 *
 * @author Spring MCP Server
 * @version 1.6.0
 * @since 2026-01-01
 */
public interface EmbeddingProvider {

    /**
     * Generate embeddings for a list of texts.
     *
     * @param texts list of texts to embed
     * @return list of embedding vectors (one per input text)
     */
    List<float[]> embed(List<String> texts);

    /**
     * Generate embedding for a single text.
     *
     * @param text text to embed
     * @return embedding vector
     */
    default float[] embed(String text) {
        List<float[]> results = embed(List.of(text));
        return results.isEmpty() ? new float[0] : results.get(0);
    }

    /**
     * Get the dimensionality of embedding vectors produced by this provider.
     *
     * @return number of dimensions in the embedding vector
     */
    int getDimensions();

    /**
     * Get the name of this embedding provider.
     *
     * @return provider name (e.g., "ollama", "openai")
     */
    String getProviderName();

    /**
     * Get the model name used for embeddings.
     *
     * @return model name (e.g., "nomic-embed-text", "text-embedding-3-small")
     */
    String getModelName();

    /**
     * Check if the provider is available and ready to process embeddings.
     *
     * @return true if provider is available
     */
    boolean isAvailable();

    /**
     * Get the maximum number of tokens the model can process.
     *
     * @return maximum token limit
     */
    default int getMaxTokens() {
        return 8192; // Default max tokens for most embedding models
    }
}
