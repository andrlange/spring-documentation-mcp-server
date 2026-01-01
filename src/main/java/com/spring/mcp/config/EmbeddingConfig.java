package com.spring.mcp.config;

import com.spring.mcp.service.embedding.ChunkingService;
import com.spring.mcp.service.embedding.EmbeddingProvider;
import com.spring.mcp.service.embedding.OllamaEmbeddingProvider;
import com.spring.mcp.service.embedding.OpenAIEmbeddingProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Configuration for the Embeddings feature.
 * Provides auto-configuration for embedding providers based on settings.
 *
 * @author Spring MCP Server
 * @version 1.6.0
 * @since 2026-01-01
 */
@Slf4j
@Configuration
@EnableAsync
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
     * Executor for async embedding job processing.
     */
    @Bean(name = "embeddingTaskExecutor")
    public Executor embeddingTaskExecutor(EmbeddingProperties properties) {
        EmbeddingProperties.JobConfig jobConfig = properties.getJob();

        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(jobConfig.getThreadPoolSize());
        executor.setMaxPoolSize(jobConfig.getThreadPoolSize() * 2);
        executor.setQueueCapacity(jobConfig.getQueueCapacity());
        executor.setThreadNamePrefix("embedding-");
        executor.setRejectedExecutionHandler((r, e) ->
                log.warn("Embedding task rejected - queue full. Consider increasing queue capacity."));
        executor.initialize();

        log.info("Initialized embedding task executor with {} threads, queue capacity {}",
                jobConfig.getThreadPoolSize(), jobConfig.getQueueCapacity());

        return executor;
    }
}
