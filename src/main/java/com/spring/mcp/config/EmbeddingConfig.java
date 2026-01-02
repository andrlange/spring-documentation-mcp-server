package com.spring.mcp.config;

import com.spring.mcp.service.embedding.ChunkingService;
import com.spring.mcp.service.embedding.EmbeddingProvider;
import com.spring.mcp.service.embedding.OllamaEmbeddingProvider;
import com.spring.mcp.service.embedding.OpenAIEmbeddingProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Configuration for the Embeddings feature.
 * <p>
 * Provides auto-configuration for embedding providers based on settings.
 * Uses virtual threads for async embedding operations, providing lightweight
 * and scalable concurrent processing.
 * <p>
 * Note: @EnableAsync is configured centrally in {@link AsyncConfig}.
 *
 * @author Spring MCP Server
 * @version 1.6.1
 * @since 2026-01-01
 */
@Slf4j
@Configuration
@ConditionalOnProperty(name = "mcp.features.embeddings.enabled", havingValue = "true")
public class EmbeddingConfig {

    /**
     * Create embedding provider based on configuration.
     * Supports 'ollama' (default) and 'openai' providers.
     */
    @Bean
    public EmbeddingProvider embeddingProvider(EmbeddingProperties properties) {
        String provider = properties.getProvider().toLowerCase();
        log.info("Configuring embedding provider: {}", provider);

        return switch (provider) {
            case "openai" -> {
                log.info("Using OpenAI embedding provider with model: {}", properties.getOpenai().getModel());
                yield new OpenAIEmbeddingProvider(properties);
            }
            case "ollama" -> {
                log.info("Using Ollama embedding provider with model: {} at {}",
                        properties.getOllama().getModel(), properties.getOllama().getBaseUrl());
                yield new OllamaEmbeddingProvider(properties);
            }
            default -> {
                log.warn("Unknown embedding provider '{}', falling back to Ollama", provider);
                yield new OllamaEmbeddingProvider(properties);
            }
        };
    }

    /**
     * Create chunking service for handling large documents.
     */
    @Bean
    public ChunkingService chunkingService(EmbeddingProperties properties) {
        return new ChunkingService(properties);
    }

    /**
     * Executor for async embedding job processing using virtual threads.
     * <p>
     * Virtual threads are lightweight (~1KB vs ~1MB for platform threads)
     * and can scale to millions of concurrent tasks. They are ideal for
     * I/O-bound operations like API calls to embedding providers (Ollama, OpenAI).
     * <p>
     * With virtual threads, traditional thread pool sizing (core size, max size,
     * queue capacity) is no longer necessary - the JVM manages scheduling efficiently.
     *
     * @param properties embedding configuration properties (for logging)
     * @return virtual thread executor for embedding operations
     */
    @Bean(name = "embeddingTaskExecutor")
    public Executor embeddingTaskExecutor(EmbeddingProperties properties) {
        log.info("Configuring embedding task executor with virtual threads");
        log.info("Virtual threads provide automatic scaling - no pool sizing needed");
        log.info("Embedding provider: {}, configured batch size: {}",
                properties.getProvider(), properties.getJob().getBatchSize());

        return Executors.newVirtualThreadPerTaskExecutor();
    }
}
