package com.spring.mcp.service.embedding;

import com.spring.mcp.config.EmbeddingProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Embedding provider implementation using OpenAI API.
 * Uses WebClient for direct API calls (no Spring AI OpenAI dependency required).
 *
 * Supports the following models:
 * - text-embedding-3-small (1536 dimensions) - recommended for most use cases
 * - text-embedding-3-large (3072 dimensions) - highest quality
 * - text-embedding-ada-002 (1536 dimensions) - legacy
 *
 * @author Spring MCP Server
 * @version 1.6.0
 * @since 2026-01-01
 */
@Slf4j
public class OpenAIEmbeddingProvider implements EmbeddingProvider {

    private static final String OPENAI_API_URL = "https://api.openai.com/v1/embeddings";

    private final WebClient webClient;
    private final String modelName;
    private final int dimensions;
    private final long timeoutMs;
    private final AtomicBoolean available = new AtomicBoolean(false);

    public OpenAIEmbeddingProvider(EmbeddingProperties properties) {
        EmbeddingProperties.OpenAIConfig config = properties.getOpenai();

        if (config.getApiKey() == null || config.getApiKey().isBlank()) {
            throw new IllegalArgumentException("OpenAI API key is required");
        }

        this.modelName = config.getModel();
        this.dimensions = properties.getDimensions();
        this.timeoutMs = config.getTimeoutMs();

        log.info("Initializing OpenAIEmbeddingProvider with model={}, dimensions={}", modelName, dimensions);

        this.webClient = WebClient.builder()
                .baseUrl(OPENAI_API_URL)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + config.getApiKey())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();

        // Test connectivity
        checkAvailability();
    }

    @Override
    public List<float[]> embed(List<String> texts) {
        if (texts == null || texts.isEmpty()) {
            return List.of();
        }

        try {
            log.debug("Embedding {} texts with OpenAI model {}", texts.size(), modelName);

            // Build request body
            Map<String, Object> requestBody = Map.of(
                    "input", texts,
                    "model", modelName
            );

            // Make API call
            @SuppressWarnings("unchecked")
            Map<String, Object> response = webClient.post()
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(Duration.ofMillis(timeoutMs))
                    .block();

            if (response == null) {
                throw new EmbeddingServiceImpl.EmbeddingException("Empty response from OpenAI");
            }

            // Parse response
            List<float[]> embeddings = parseEmbeddingsResponse(response);
            log.debug("Successfully generated {} embeddings from OpenAI", embeddings.size());

            available.set(true);
            return embeddings;

        } catch (Exception e) {
            log.error("Failed to generate embeddings with OpenAI: {}", e.getMessage(), e);
            available.set(false);
            throw new EmbeddingServiceImpl.EmbeddingException("OpenAI embedding failed: " + e.getMessage(), e);
        }
    }

    @Override
    public int getDimensions() {
        return dimensions;
    }

    @Override
    public String getProviderName() {
        return "openai";
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
        // text-embedding-3-small and text-embedding-3-large support 8191 tokens
        return 8191;
    }

    /**
     * Parse the embeddings from OpenAI API response.
     */
    @SuppressWarnings("unchecked")
    private List<float[]> parseEmbeddingsResponse(Map<String, Object> response) {
        List<Map<String, Object>> data = (List<Map<String, Object>>) response.get("data");
        if (data == null || data.isEmpty()) {
            throw new EmbeddingServiceImpl.EmbeddingException("No embeddings in OpenAI response");
        }

        List<float[]> embeddings = new ArrayList<>();
        for (Map<String, Object> item : data) {
            List<Number> embeddingList = (List<Number>) item.get("embedding");
            if (embeddingList == null) {
                continue;
            }

            float[] embedding = new float[embeddingList.size()];
            for (int i = 0; i < embeddingList.size(); i++) {
                embedding[i] = embeddingList.get(i).floatValue();
            }
            embeddings.add(embedding);
        }

        return embeddings;
    }

    /**
     * Check if OpenAI API is available.
     */
    private void checkAvailability() {
        try {
            log.debug("Checking OpenAI availability...");
            float[] testEmbedding = embed("test");
            if (testEmbedding != null && testEmbedding.length > 0) {
                available.set(true);
                log.info("OpenAI embedding provider is available. Vector dimensions: {}", testEmbedding.length);
            } else {
                available.set(false);
                log.warn("OpenAI returned empty embedding");
            }
        } catch (Exception e) {
            available.set(false);
            log.warn("OpenAI is not available: {}", e.getMessage());
        }
    }
}
