package com.spring.mcp.service.embedding;

import com.spring.mcp.config.EmbeddingProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.ollama.OllamaEmbeddingModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaEmbeddingOptions;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Embedding provider implementation using Ollama.
 * Uses the Spring AI OllamaEmbeddingModel for local embedding generation.
 *
 * Supports the following models:
 * - nomic-embed-text (768 dimensions) - recommended for general use
 * - mxbai-embed-large (1024 dimensions) - for higher quality embeddings
 * - all-minilm (384 dimensions) - lightweight, fast
 *
 * @author Spring MCP Server
 * @version 1.6.0
 * @since 2026-01-01
 */
@Slf4j
public class OllamaEmbeddingProvider implements EmbeddingProvider {

    private final OllamaEmbeddingModel embeddingModel;
    private final String modelName;
    private final int dimensions;
    private final AtomicBoolean available = new AtomicBoolean(false);

    public OllamaEmbeddingProvider(EmbeddingProperties properties) {
        EmbeddingProperties.OllamaConfig config = properties.getOllama();
        this.modelName = config.getModel();
        this.dimensions = properties.getDimensions();

        // Use configured timeout (default 30s, but allow longer for large embeddings)
        long timeoutMs = Math.max(config.getTimeoutMs(), 120000); // At least 2 minutes

        log.info("Initializing OllamaEmbeddingProvider with baseUrl={}, model={}, dimensions={}, timeout={}ms",
                config.getBaseUrl(), modelName, dimensions, timeoutMs);

        try {
            // Create request factory with timeout
            SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
            requestFactory.setConnectTimeout(Duration.ofMillis(10000)); // 10s connect
            requestFactory.setReadTimeout(Duration.ofMillis(timeoutMs)); // Read timeout

            // Create RestClient.Builder with timeout
            RestClient.Builder restClientBuilder = RestClient.builder()
                    .requestFactory(requestFactory);

            // Create Ollama API client with custom RestClient builder
            OllamaApi ollamaApi = OllamaApi.builder()
                    .baseUrl(config.getBaseUrl())
                    .restClientBuilder(restClientBuilder)
                    .build();

            // Create embedding model with options
            OllamaEmbeddingOptions options = OllamaEmbeddingOptions.builder()
                    .model(modelName)
                    .build();

            this.embeddingModel = OllamaEmbeddingModel.builder()
                    .ollamaApi(ollamaApi)
                    .defaultOptions(options)
                    .build();

            // Test connectivity
            checkAvailability();
        } catch (Exception e) {
            log.error("Failed to initialize OllamaEmbeddingProvider: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to initialize Ollama embedding provider", e);
        }
    }

    @Override
    public List<float[]> embed(List<String> texts) {
        if (texts == null || texts.isEmpty()) {
            return List.of();
        }

        try {
            log.debug("Embedding {} texts with Ollama model {}", texts.size(), modelName);

            // Use Spring AI's embed method which returns List<float[]>
            List<float[]> embeddings = embeddingModel.embed(texts);

            log.debug("Successfully generated {} embeddings", embeddings.size());
            available.set(true);
            return embeddings;
        } catch (Exception e) {
            log.error("Failed to generate embeddings with Ollama: {}", e.getMessage(), e);
            available.set(false);
            throw new EmbeddingServiceImpl.EmbeddingException("Ollama embedding failed: " + e.getMessage(), e);
        }
    }

    @Override
    public int getDimensions() {
        return dimensions;
    }

    @Override
    public String getProviderName() {
        return "ollama";
    }

    @Override
    public String getModelName() {
        return modelName;
    }

    @Override
    public boolean isAvailable() {
        return available.get();
    }

    @Override
    public int getMaxTokens() {
        // Most Ollama embedding models support up to 8192 tokens
        return 8192;
    }

    /**
     * Check if Ollama is available by attempting a simple embedding.
     */
    private void checkAvailability() {
        try {
            log.debug("Checking Ollama availability...");
            float[] testEmbedding = embed("test");
            if (testEmbedding != null && testEmbedding.length > 0) {
                available.set(true);
                log.info("Ollama embedding provider is available. Vector dimensions: {}", testEmbedding.length);
            } else {
                available.set(false);
                log.warn("Ollama returned empty embedding");
            }
        } catch (Exception e) {
            available.set(false);
            log.warn("Ollama is not available: {}", e.getMessage());
        }
    }
}
