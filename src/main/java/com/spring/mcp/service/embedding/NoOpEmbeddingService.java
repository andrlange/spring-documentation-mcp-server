package com.spring.mcp.service.embedding;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

/**
 * No-op implementation of EmbeddingService for when embeddings are disabled.
 * All methods return empty results without performing any operations.
 *
 * @author Spring MCP Server
 * @version 1.6.0
 * @since 2026-01-01
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "mcp.features.embeddings.enabled", havingValue = "false", matchIfMissing = true)
public class NoOpEmbeddingService implements EmbeddingService {

    private static final int DEFAULT_DIMENSIONS = 768;

    public NoOpEmbeddingService() {
        log.info("Embeddings feature is DISABLED. Using NoOpEmbeddingService.");
    }

    @Override
    public float[] embed(String text) {
        log.trace("NoOp embed called - embeddings are disabled");
        return new float[DEFAULT_DIMENSIONS];
    }

    @Override
    public List<float[]> embed(List<String> texts) {
        log.trace("NoOp embed(List) called - embeddings are disabled");
        return Collections.emptyList();
    }

    @Override
    public float[] embedWithChunking(String text) {
        log.trace("NoOp embedWithChunking called - embeddings are disabled");
        return new float[DEFAULT_DIMENSIONS];
    }

    @Override
    public boolean isAvailable() {
        return false;
    }

    @Override
    public String getProviderName() {
        return "none";
    }

    @Override
    public String getModelName() {
        return "disabled";
    }

    @Override
    public int getDimensions() {
        return DEFAULT_DIMENSIONS;
    }

    @Override
    public double cosineSimilarity(float[] embedding1, float[] embedding2) {
        return 0.0;
    }
}
