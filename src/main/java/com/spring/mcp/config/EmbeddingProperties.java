package com.spring.mcp.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for the Embeddings feature.
 * Provides vector embeddings for semantic search using pgvector.
 *
 * @author Spring MCP Server
 * @version 1.6.0
 * @since 2026-01-01
 */
@Configuration
@ConfigurationProperties(prefix = "mcp.features.embeddings")
@Getter
@Setter
public class EmbeddingProperties {

    /**
     * Enable/disable the embeddings feature (default: false)
     * When disabled, the application uses traditional TSVECTOR full-text search.
     */
    private boolean enabled = false;

    /**
     * Embedding provider: 'ollama' or 'openai'
     */
    private String provider = "ollama";

    /**
     * Vector dimensions: 768 for Ollama nomic-embed-text, 1536 for OpenAI
     */
    private int dimensions = 768;

    /**
     * Maximum tokens per chunk for large documents
     */
    private int chunkSize = 512;

    /**
     * Overlap between chunks (tokens)
     */
    private int chunkOverlap = 50;

    /**
     * Number of embeddings to process per batch
     */
    private int batchSize = 50;

    /**
     * Hybrid search configuration
     */
    private HybridConfig hybrid = new HybridConfig();

    /**
     * Retry configuration for failed embeddings
     */
    private RetryConfig retry = new RetryConfig();

    /**
     * Health check configuration
     */
    private HealthCheckConfig healthCheck = new HealthCheckConfig();

    /**
     * Job processing configuration
     */
    private JobConfig job = new JobConfig();

    /**
     * Ollama provider configuration
     */
    private OllamaConfig ollama = new OllamaConfig();

    /**
     * OpenAI provider configuration
     */
    private OpenAIConfig openai = new OpenAIConfig();

    @Getter
    @Setter
    public static class HybridConfig {
        /**
         * Enable hybrid search (combines keyword + semantic search)
         */
        private boolean enabled = true;

        /**
         * Alpha parameter for hybrid search weighting.
         * 0.0 = pure vector search, 1.0 = pure keyword search
         * Default 0.3 = semantic-weighted (70% vector, 30% keyword)
         */
        private double alpha = 0.3;

        /**
         * Minimum cosine similarity threshold for vector results
         */
        private double minSimilarity = 0.5;
    }

    @Getter
    @Setter
    public static class RetryConfig {
        /**
         * Maximum retry attempts for failed embeddings
         */
        private int maxRetries = 3;

        /**
         * Initial delay between retries (ms)
         */
        private long initialDelayMs = 5000;

        /**
         * Maximum delay between retries (ms)
         */
        private long maxDelayMs = 300000;

        /**
         * Exponential backoff multiplier
         */
        private double multiplier = 2.0;
    }

    @Getter
    @Setter
    public static class HealthCheckConfig {
        /**
         * Enable periodic health checks for embedding provider
         */
        private boolean enabled = true;

        /**
         * Interval between health checks (ms)
         */
        private long intervalMs = 60000;

        /**
         * Timeout for health check requests (ms)
         */
        private long timeoutMs = 10000;
    }

    @Getter
    @Setter
    public static class JobConfig {
        /**
         * Batch size for job processing
         */
        private int batchSize = 50;

        /**
         * Thread pool size for async job processing
         */
        private int threadPoolSize = 2;

        /**
         * Maximum queue capacity for pending jobs
         */
        private int queueCapacity = 1000;

        /**
         * Poll interval for checking pending jobs (ms)
         */
        private long pollInterval = 30000;
    }

    @Getter
    @Setter
    public static class OllamaConfig {
        /**
         * Base URL for Ollama API
         */
        private String baseUrl = "http://localhost:11434";

        /**
         * Embedding model name
         */
        private String model = "nomic-embed-text";

        /**
         * Request timeout (ms)
         */
        private long timeoutMs = 30000;
    }

    @Getter
    @Setter
    public static class OpenAIConfig {
        /**
         * OpenAI API key
         */
        private String apiKey = "";

        /**
         * Embedding model name
         */
        private String model = "text-embedding-3-small";

        /**
         * Request timeout (ms)
         */
        private long timeoutMs = 30000;
    }
}
