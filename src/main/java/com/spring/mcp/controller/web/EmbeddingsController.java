package com.spring.mcp.controller.web;

import com.spring.mcp.config.EmbeddingProperties;
import com.spring.mcp.service.embedding.EmbeddingJobProcessor;
import com.spring.mcp.service.embedding.EmbeddingService;
import com.spring.mcp.service.embedding.EmbeddingSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controller for Embeddings dashboard.
 * Provides statistics and status information about the embedding feature.
 * Only visible when embeddings feature is enabled.
 * Accessible only to ADMIN users.
 *
 * @author Spring MCP Server
 * @version 1.6.0
 * @since 2026-01-01
 */
@Controller
@RequestMapping("/embeddings")
@Slf4j
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@ConditionalOnProperty(name = "mcp.features.embeddings.enabled", havingValue = "true")
public class EmbeddingsController {

    private final EmbeddingService embeddingService;
    private final EmbeddingSyncService embeddingSyncService;
    private final EmbeddingJobProcessor embeddingJobProcessor;
    private final EmbeddingProperties properties;

    /**
     * Display the embeddings dashboard.
     *
     * @param model Spring MVC model
     * @return view name "embeddings/index"
     */
    @GetMapping
    public String index(Model model) {
        log.debug("Showing embeddings dashboard");

        // Get embedding statistics
        EmbeddingSyncService.EmbeddingStats stats = embeddingSyncService.getEmbeddingStats();
        model.addAttribute("stats", stats);

        // Provider information
        model.addAttribute("providerName", embeddingService.getProviderName());
        model.addAttribute("modelName", embeddingService.getModelName());
        model.addAttribute("dimensions", embeddingService.getDimensions());
        model.addAttribute("providerAvailable", embeddingService.isAvailable());

        // Configuration
        model.addAttribute("hybridEnabled", properties.getHybrid().isEnabled());
        model.addAttribute("alpha", properties.getHybrid().getAlpha());
        model.addAttribute("minSimilarity", properties.getHybrid().getMinSimilarity());
        model.addAttribute("chunkSize", properties.getChunkSize());
        model.addAttribute("chunkOverlap", properties.getChunkOverlap());
        model.addAttribute("batchSize", properties.getBatchSize());

        // Ollama/OpenAI specific config
        if ("ollama".equals(properties.getProvider())) {
            model.addAttribute("ollamaBaseUrl", properties.getOllama().getBaseUrl());
            model.addAttribute("ollamaModel", properties.getOllama().getModel());
        } else if ("openai".equals(properties.getProvider())) {
            model.addAttribute("openaiModel", properties.getOpenai().getModel());
            model.addAttribute("openaiConfigured", !properties.getOpenai().getApiKey().isBlank());
        }

        model.addAttribute("activePage", "embeddings");
        model.addAttribute("pageTitle", "Embeddings Dashboard");

        return "embeddings/index";
    }

    /**
     * Get current embedding statistics as JSON (for AJAX refresh).
     *
     * @return embedding statistics
     */
    @GetMapping("/stats")
    @ResponseBody
    public ResponseEntity<EmbeddingSyncService.EmbeddingStats> getStats() {
        return ResponseEntity.ok(embeddingSyncService.getEmbeddingStats());
    }

    /**
     * Check provider health status.
     *
     * @return provider status map
     */
    @GetMapping("/health")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getHealth() {
        boolean available = embeddingService.isAvailable();
        return ResponseEntity.ok(Map.of(
                "available", available,
                "provider", embeddingService.getProviderName(),
                "model", embeddingService.getModelName(),
                "dimensions", embeddingService.getDimensions()
        ));
    }

    /**
     * Trigger embedding sync for all missing embeddings.
     *
     * @return success response
     */
    @PostMapping("/sync")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> triggerSync() {
        if (embeddingJobProcessor.isProcessing()) {
            log.debug("Sync already in progress, ignoring request");
            return ResponseEntity.ok(Map.of(
                    "status", "already_running",
                    "message", "Embedding sync is already running",
                    "processing", true
            ));
        }

        log.info("Triggering embedding sync from dashboard");
        embeddingSyncService.syncMissingEmbeddings();
        return ResponseEntity.ok(Map.of(
                "status", "started",
                "message", "Embedding sync started in background",
                "processing", true
        ));
    }

    /**
     * Get processing status.
     *
     * @return processing status with pending count
     */
    @GetMapping("/status")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getStatus() {
        EmbeddingSyncService.EmbeddingStats stats = embeddingSyncService.getEmbeddingStats();
        return ResponseEntity.ok(Map.of(
                "processing", embeddingJobProcessor.isProcessing(),
                "pendingJobs", stats.pendingJobs(),
                "providerAvailable", stats.providerAvailable()
        ));
    }
}
