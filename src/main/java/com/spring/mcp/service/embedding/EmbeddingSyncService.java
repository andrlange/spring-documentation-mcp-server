package com.spring.mcp.service.embedding;

import com.spring.mcp.config.EmbeddingProperties;
import com.spring.mcp.model.entity.*;
import com.spring.mcp.repository.*;
import com.spring.mcp.repository.WikiReleaseNotesRepository;
import com.spring.mcp.repository.WikiMigrationGuideRepository;
import com.spring.mcp.repository.SpringProjectRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Service for synchronizing embeddings for entities.
 * Handles batch embedding generation and incremental updates.
 *
 * @author Spring MCP Server
 * @version 1.6.0
 * @since 2026-01-01
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "mcp.features.embeddings.enabled", havingValue = "true")
public class EmbeddingSyncService {

    private final EmbeddingService embeddingService;
    private final ChunkingService chunkingService;
    private final EmbeddingProperties properties;
    private final JdbcTemplate jdbcTemplate;

    private final DocumentationContentRepository documentationContentRepository;
    private final MigrationTransformationRepository transformationRepository;
    private final FlavorRepository flavorRepository;
    private final CodeExampleRepository codeExampleRepository;
    private final WikiReleaseNotesRepository wikiReleaseNotesRepository;
    private final WikiMigrationGuideRepository wikiMigrationGuideRepository;
    private final SpringProjectRepository springProjectRepository;
    private final EmbeddingJobRepository embeddingJobRepository;
    private final EmbeddingMetadataRepository embeddingMetadataRepository;

    /**
     * Sync embeddings for all documentation content.
     */
    @Async("embeddingTaskExecutor")
    @Transactional
    public CompletableFuture<SyncResult> syncDocumentationEmbeddings() {
        log.info("Starting documentation embedding sync...");
        return syncEntityEmbeddings(
                "DOCUMENTATION",
                this::fetchDocumentationBatch,
                this::getDocumentationTextForEmbedding,
                this::updateDocumentationEmbedding
        );
    }

    /**
     * Sync embeddings for all migration transformations.
     */
    @Async("embeddingTaskExecutor")
    @Transactional
    public CompletableFuture<SyncResult> syncTransformationEmbeddings() {
        log.info("Starting transformation embedding sync...");
        return syncEntityEmbeddings(
                "TRANSFORMATION",
                this::fetchTransformationBatch,
                this::getTransformationTextForEmbedding,
                this::updateTransformationEmbedding
        );
    }

    /**
     * Sync embeddings for all flavors.
     */
    @Async("embeddingTaskExecutor")
    @Transactional
    public CompletableFuture<SyncResult> syncFlavorEmbeddings() {
        log.info("Starting flavor embedding sync...");
        return syncEntityEmbeddings(
                "FLAVOR",
                this::fetchFlavorBatch,
                this::getFlavorTextForEmbedding,
                this::updateFlavorEmbedding
        );
    }

    /**
     * Sync embeddings for all code examples.
     */
    @Async("embeddingTaskExecutor")
    @Transactional
    public CompletableFuture<SyncResult> syncCodeExampleEmbeddings() {
        log.info("Starting code example embedding sync...");
        return syncEntityEmbeddings(
                "CODE_EXAMPLE",
                this::fetchCodeExampleBatch,
                this::getCodeExampleTextForEmbedding,
                this::updateCodeExampleEmbedding
        );
    }

    /**
     * Sync embeddings for all wiki release notes.
     */
    @Async("embeddingTaskExecutor")
    @Transactional
    public CompletableFuture<SyncResult> syncWikiReleaseNotesEmbeddings() {
        log.info("Starting wiki release notes embedding sync...");
        return syncEntityEmbeddings(
                "WIKI_RELEASE_NOTES",
                this::fetchWikiReleaseNotesBatch,
                this::getWikiReleaseNotesTextForEmbedding,
                this::updateWikiReleaseNotesEmbedding
        );
    }

    /**
     * Sync embeddings for all wiki migration guides.
     */
    @Async("embeddingTaskExecutor")
    @Transactional
    public CompletableFuture<SyncResult> syncWikiMigrationGuideEmbeddings() {
        log.info("Starting wiki migration guide embedding sync...");
        return syncEntityEmbeddings(
                "WIKI_MIGRATION_GUIDE",
                this::fetchWikiMigrationGuideBatch,
                this::getWikiMigrationGuideTextForEmbedding,
                this::updateWikiMigrationGuideEmbedding
        );
    }

    /**
     * Sync embeddings for all Spring projects.
     */
    @Async("embeddingTaskExecutor")
    @Transactional
    public CompletableFuture<SyncResult> syncProjectEmbeddings() {
        log.info("Starting Spring project embedding sync...");
        return syncEntityEmbeddings(
                "PROJECT",
                this::fetchProjectBatch,
                this::getProjectTextForEmbedding,
                this::updateProjectEmbedding
        );
    }

    /**
     * Sync embeddings for entities without embeddings only.
     */
    @Async("embeddingTaskExecutor")
    public CompletableFuture<SyncResult> syncMissingEmbeddings() {
        log.info("Starting sync of missing embeddings...");

        SyncResult totalResult = new SyncResult(0, 0, 0);

        // Sync each entity type
        try {
            SyncResult docResult = syncMissingForEntityType("DOCUMENTATION").get();
            totalResult = totalResult.add(docResult);

            SyncResult transResult = syncMissingForEntityType("TRANSFORMATION").get();
            totalResult = totalResult.add(transResult);

            SyncResult flavorResult = syncMissingForEntityType("FLAVOR").get();
            totalResult = totalResult.add(flavorResult);

            SyncResult codeResult = syncMissingForEntityType("CODE_EXAMPLE").get();
            totalResult = totalResult.add(codeResult);

            SyncResult wikiReleaseNotesResult = syncMissingForEntityType("WIKI_RELEASE_NOTES").get();
            totalResult = totalResult.add(wikiReleaseNotesResult);

            SyncResult wikiMigrationGuideResult = syncMissingForEntityType("WIKI_MIGRATION_GUIDE").get();
            totalResult = totalResult.add(wikiMigrationGuideResult);

            SyncResult projectResult = syncMissingForEntityType("PROJECT").get();
            totalResult = totalResult.add(projectResult);

        } catch (Exception e) {
            log.error("Error syncing missing embeddings: {}", e.getMessage(), e);
        }

        log.info("Completed sync of missing embeddings: {}", totalResult);
        return CompletableFuture.completedFuture(totalResult);
    }

    /**
     * Create an embedding job for a specific entity.
     */
    @Transactional
    public void createEmbeddingJob(String entityType, Long entityId) {
        if (!embeddingJobRepository.existsPendingJobForEntity(entityType, entityId)) {
            EmbeddingJob job = EmbeddingJob.builder()
                    .entityType(entityType)
                    .entityId(entityId)
                    .status(EmbeddingJob.JobStatus.PENDING)
                    .priority(5)
                    .provider(embeddingService.getProviderName())
                    .model(embeddingService.getModelName())
                    .build();
            embeddingJobRepository.save(job);
            log.debug("Created embedding job for {} #{}", entityType, entityId);
        }
    }

    /**
     * Generate and store embedding for a single entity.
     */
    @Transactional
    public void generateEmbedding(String entityType, Long entityId, String text) {
        if (text == null || text.isBlank()) {
            log.warn("Cannot generate embedding for {} #{}: text is empty", entityType, entityId);
            return;
        }

        try {
            String modelName = embeddingService.getModelName();

            if (chunkingService.needsChunking(text)) {
                // Handle chunked embedding
                List<String> chunks = chunkingService.chunkText(text);
                for (int i = 0; i < chunks.size(); i++) {
                    String chunk = chunks.get(i);
                    float[] embedding = embeddingService.embed(chunk);
                    storeChunkEmbedding(entityType, entityId, i, chunk, embedding, modelName);
                }
            } else {
                // Single embedding
                float[] embedding = embeddingService.embed(text);
                storeEntityEmbedding(entityType, entityId, embedding, modelName);
            }

            log.debug("Generated embedding for {} #{}", entityType, entityId);
        } catch (Exception e) {
            log.error("Failed to generate embedding for {} #{}: {}", entityType, entityId, e.getMessage());
            throw new RuntimeException("Embedding generation failed", e);
        }
    }

    /**
     * Get embedding sync statistics.
     */
    public EmbeddingStats getEmbeddingStats() {
        long docsWithEmbedding = countEntitiesWithEmbedding("documentation_content", "content_embedding");
        long docsTotal = documentationContentRepository.count();

        long transformsWithEmbedding = countEntitiesWithEmbedding("migration_transformations", "transformation_embedding");
        long transformsTotal = transformationRepository.count();

        long flavorsWithEmbedding = countEntitiesWithEmbedding("flavors", "flavor_embedding");
        long flavorsTotal = flavorRepository.count();

        long examplesWithEmbedding = countEntitiesWithEmbedding("code_examples", "example_embedding");
        long examplesTotal = codeExampleRepository.count();

        long wikiReleaseNotesWithEmbedding = countEntitiesWithEmbedding("wiki_release_notes", "content_embedding");
        long wikiReleaseNotesTotal = wikiReleaseNotesRepository.count();

        long wikiMigrationGuidesWithEmbedding = countEntitiesWithEmbedding("wiki_migration_guides", "content_embedding");
        long wikiMigrationGuidesTotal = wikiMigrationGuideRepository.count();

        long projectsWithEmbedding = countEntitiesWithEmbedding("spring_projects", "project_embedding");
        long projectsTotal = springProjectRepository.count();

        long pendingJobs = embeddingJobRepository.countPendingAndRetryJobs();
        long failedJobs = embeddingJobRepository.countByStatus(EmbeddingJob.JobStatus.FAILED);

        return new EmbeddingStats(
                docsWithEmbedding, docsTotal,
                transformsWithEmbedding, transformsTotal,
                flavorsWithEmbedding, flavorsTotal,
                examplesWithEmbedding, examplesTotal,
                wikiReleaseNotesWithEmbedding, wikiReleaseNotesTotal,
                wikiMigrationGuidesWithEmbedding, wikiMigrationGuidesTotal,
                projectsWithEmbedding, projectsTotal,
                pendingJobs,
                failedJobs,
                embeddingService.isAvailable(),
                embeddingService.getProviderName(),
                embeddingService.getModelName()
        );
    }

    // ========== Private helper methods ==========

    @FunctionalInterface
    private interface BatchFetcher<T> {
        Page<T> fetch(PageRequest pageRequest);
    }

    @FunctionalInterface
    private interface TextExtractor<T> {
        String extract(T entity);
    }

    @FunctionalInterface
    private interface EmbeddingUpdater {
        void update(Long id, float[] embedding, String model);
    }

    private <T> CompletableFuture<SyncResult> syncEntityEmbeddings(
            String entityType,
            BatchFetcher<T> fetcher,
            TextExtractor<T> textExtractor,
            EmbeddingUpdater updater
    ) {
        AtomicInteger processed = new AtomicInteger(0);
        AtomicInteger succeeded = new AtomicInteger(0);
        AtomicInteger failed = new AtomicInteger(0);

        int batchSize = properties.getBatchSize();
        int page = 0;
        Page<T> batch;

        do {
            batch = fetcher.fetch(PageRequest.of(page, batchSize));
            List<String> texts = new ArrayList<>();
            List<Long> ids = new ArrayList<>();

            for (T entity : batch.getContent()) {
                String text = textExtractor.extract(entity);
                if (text != null && !text.isBlank()) {
                    texts.add(text);
                    ids.add(getEntityId(entity));
                }
            }

            if (!texts.isEmpty()) {
                String modelName = embeddingService.getModelName();
                // Process each text individually to handle chunking for large texts
                for (int i = 0; i < texts.size(); i++) {
                    String text = texts.get(i);
                    Long entityId = ids.get(i);
                    try {
                        float[] embedding;
                        // Check if text needs chunking (exceeds chunk size limit)
                        if (chunkingService.needsChunking(text)) {
                            log.debug("Text for {} #{} needs chunking ({} estimated tokens)",
                                    entityType, entityId, chunkingService.estimateTokens(text));
                            embedding = embeddingService.embedWithChunking(text);
                        } else {
                            embedding = embeddingService.embed(text);
                        }

                        updater.update(entityId, embedding, modelName);
                        succeeded.incrementAndGet();
                    } catch (Exception e) {
                        failed.incrementAndGet();
                        log.warn("Failed to embed {} #{}: {}", entityType, entityId, e.getMessage());
                    }
                    processed.incrementAndGet();
                }
            }

            page++;
        } while (batch.hasNext());

        SyncResult result = new SyncResult(processed.get(), succeeded.get(), failed.get());
        log.info("Completed {} embedding sync: {}", entityType, result);
        return CompletableFuture.completedFuture(result);
    }

    private CompletableFuture<SyncResult> syncMissingForEntityType(String entityType) {
        // Query for entities without embeddings
        String sql = switch (entityType) {
            case "DOCUMENTATION" -> "SELECT id FROM documentation_content WHERE content_embedding IS NULL";
            case "TRANSFORMATION" -> "SELECT id FROM migration_transformations WHERE transformation_embedding IS NULL";
            case "FLAVOR" -> "SELECT id FROM flavors WHERE flavor_embedding IS NULL";
            case "CODE_EXAMPLE" -> "SELECT id FROM code_examples WHERE example_embedding IS NULL";
            case "WIKI_RELEASE_NOTES" -> "SELECT id FROM wiki_release_notes WHERE content_embedding IS NULL";
            case "WIKI_MIGRATION_GUIDE" -> "SELECT id FROM wiki_migration_guides WHERE content_embedding IS NULL";
            case "PROJECT" -> "SELECT id FROM spring_projects WHERE project_embedding IS NULL";
            default -> throw new IllegalArgumentException("Unknown entity type: " + entityType);
        };

        List<Long> ids = jdbcTemplate.queryForList(sql, Long.class);
        log.info("Found {} {} entities without embeddings", ids.size(), entityType);

        if (ids.isEmpty()) {
            return CompletableFuture.completedFuture(new SyncResult(0, 0, 0));
        }

        // Process in batches
        int batchSize = properties.getBatchSize();
        AtomicInteger processed = new AtomicInteger(0);
        AtomicInteger succeeded = new AtomicInteger(0);
        AtomicInteger failed = new AtomicInteger(0);
        String modelName = embeddingService.getModelName();

        for (int i = 0; i < ids.size(); i += batchSize) {
            List<Long> batchIds = ids.subList(i, Math.min(i + batchSize, ids.size()));
            List<String> texts = new ArrayList<>();
            List<Long> validIds = new ArrayList<>();

            // Fetch entities and extract text
            for (Long id : batchIds) {
                try {
                    String text = getTextForEntity(entityType, id);
                    if (text != null && !text.isBlank()) {
                        texts.add(text);
                        validIds.add(id);
                    } else {
                        log.debug("Skipping {} #{}: empty text", entityType, id);
                    }
                } catch (Exception e) {
                    log.warn("Failed to get text for {} #{}: {}", entityType, id, e.getMessage());
                    failed.incrementAndGet();
                }
            }

            if (!texts.isEmpty()) {
                // Process each text individually to handle chunking for large texts
                for (int j = 0; j < texts.size(); j++) {
                    String text = texts.get(j);
                    Long entityId = validIds.get(j);
                    try {
                        float[] embedding;
                        // Check if text needs chunking (exceeds chunk size limit)
                        if (chunkingService.needsChunking(text)) {
                            log.debug("Text for {} #{} needs chunking ({} estimated tokens)",
                                    entityType, entityId, chunkingService.estimateTokens(text));
                            embedding = embeddingService.embedWithChunking(text);
                        } else {
                            embedding = embeddingService.embed(text);
                        }

                        updateEmbeddingForEntity(entityType, entityId, embedding, modelName);
                        succeeded.incrementAndGet();

                        // Mark job as completed if it exists
                        markJobCompleted(entityType, entityId);
                    } catch (Exception e) {
                        failed.incrementAndGet();
                        log.warn("Failed to embed {} #{}: {}", entityType, entityId, e.getMessage());
                    }
                    processed.incrementAndGet();
                }

                // Log progress every 100 items
                if (processed.get() % 100 == 0 || processed.get() == ids.size()) {
                    log.info("Progress: {} {}/{} processed ({} succeeded, {} failed)",
                            entityType, processed.get(), ids.size(), succeeded.get(), failed.get());
                }
            }
        }

        SyncResult result = new SyncResult(processed.get(), succeeded.get(), failed.get());
        log.info("Completed {} missing embedding sync: {}", entityType, result);
        return CompletableFuture.completedFuture(result);
    }

    /**
     * Get text content for an entity by type and ID.
     * Used by both sync service and job processor.
     *
     * @param entityType the entity type
     * @param id the entity ID
     * @return the text content for embedding, or null if not found
     */
    public String getTextForEntity(String entityType, Long id) {
        return switch (entityType) {
            case "DOCUMENTATION" -> documentationContentRepository.findById(id)
                    .map(this::getDocumentationTextForEmbedding).orElse(null);
            case "TRANSFORMATION" -> transformationRepository.findById(id)
                    .map(this::getTransformationTextForEmbedding).orElse(null);
            case "FLAVOR" -> flavorRepository.findById(id)
                    .map(this::getFlavorTextForEmbedding).orElse(null);
            case "CODE_EXAMPLE" -> codeExampleRepository.findById(id)
                    .map(this::getCodeExampleTextForEmbedding).orElse(null);
            case "WIKI_RELEASE_NOTES" -> wikiReleaseNotesRepository.findById(id)
                    .map(this::getWikiReleaseNotesTextForEmbedding).orElse(null);
            case "WIKI_MIGRATION_GUIDE" -> wikiMigrationGuideRepository.findById(id)
                    .map(this::getWikiMigrationGuideTextForEmbedding).orElse(null);
            case "PROJECT" -> springProjectRepository.findById(id)
                    .map(this::getProjectTextForEmbedding).orElse(null);
            default -> null;
        };
    }

    /**
     * Update embedding for an entity by type and ID.
     * Used by both sync service and job processor.
     *
     * @param entityType the entity type
     * @param id the entity ID
     * @param embedding the embedding vector
     * @param model the model name used for embedding
     */
    public void updateEmbeddingForEntity(String entityType, Long id, float[] embedding, String model) {
        switch (entityType) {
            case "DOCUMENTATION" -> updateDocumentationEmbedding(id, embedding, model);
            case "TRANSFORMATION" -> updateTransformationEmbedding(id, embedding, model);
            case "FLAVOR" -> updateFlavorEmbedding(id, embedding, model);
            case "CODE_EXAMPLE" -> updateCodeExampleEmbedding(id, embedding, model);
            case "WIKI_RELEASE_NOTES" -> updateWikiReleaseNotesEmbedding(id, embedding, model);
            case "WIKI_MIGRATION_GUIDE" -> updateWikiMigrationGuideEmbedding(id, embedding, model);
            case "PROJECT" -> updateProjectEmbedding(id, embedding, model);
        }
    }

    private void markJobCompleted(String entityType, Long entityId) {
        try {
            embeddingJobRepository.findByEntityTypeAndEntityIdAndStatus(entityType, entityId, EmbeddingJob.JobStatus.PENDING)
                    .ifPresent(job -> {
                        job.setStatus(EmbeddingJob.JobStatus.COMPLETED);
                        job.setCompletedAt(LocalDateTime.now());
                        embeddingJobRepository.save(job);
                    });
        } catch (Exception e) {
            // Ignore - job completion is optional
            log.trace("Could not mark job completed: {}", e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private <T> Long getEntityId(T entity) {
        try {
            return (Long) entity.getClass().getMethod("getId").invoke(entity);
        } catch (Exception e) {
            throw new RuntimeException("Failed to get entity ID", e);
        }
    }

    private Page<DocumentationContent> fetchDocumentationBatch(PageRequest pageRequest) {
        return documentationContentRepository.findAll(pageRequest);
    }

    private String getDocumentationTextForEmbedding(DocumentationContent doc) {
        return doc.getContent();
    }

    private void updateDocumentationEmbedding(Long id, float[] embedding, String model) {
        String vectorString = floatArrayToVectorString(embedding);
        jdbcTemplate.update("""
            UPDATE documentation_content
            SET content_embedding = ?::vector, embedding_model = ?, embedded_at = NOW()
            WHERE id = ?
            """, vectorString, model, id);
    }

    private Page<MigrationTransformation> fetchTransformationBatch(PageRequest pageRequest) {
        return transformationRepository.findAll(pageRequest);
    }

    private String getTransformationTextForEmbedding(MigrationTransformation t) {
        StringBuilder sb = new StringBuilder();
        if (t.getOldPattern() != null) sb.append(t.getOldPattern()).append(" ");
        if (t.getNewPattern() != null) sb.append(t.getNewPattern()).append(" ");
        if (t.getExplanation() != null) sb.append(t.getExplanation());
        return sb.toString().trim();
    }

    private void updateTransformationEmbedding(Long id, float[] embedding, String model) {
        String vectorString = floatArrayToVectorString(embedding);
        jdbcTemplate.update("""
            UPDATE migration_transformations
            SET transformation_embedding = ?::vector, embedding_model = ?, embedded_at = NOW()
            WHERE id = ?
            """, vectorString, model, id);
    }

    private Page<Flavor> fetchFlavorBatch(PageRequest pageRequest) {
        return flavorRepository.findAll(pageRequest);
    }

    private String getFlavorTextForEmbedding(Flavor f) {
        StringBuilder sb = new StringBuilder();
        if (f.getDisplayName() != null) sb.append(f.getDisplayName()).append(" ");
        if (f.getDescription() != null) sb.append(f.getDescription()).append(" ");
        if (f.getContent() != null) sb.append(f.getContent());
        return sb.toString().trim();
    }

    private void updateFlavorEmbedding(Long id, float[] embedding, String model) {
        String vectorString = floatArrayToVectorString(embedding);
        jdbcTemplate.update("""
            UPDATE flavors
            SET flavor_embedding = ?::vector, embedding_model = ?, embedded_at = NOW()
            WHERE id = ?
            """, vectorString, model, id);
    }

    private Page<CodeExample> fetchCodeExampleBatch(PageRequest pageRequest) {
        return codeExampleRepository.findAll(pageRequest);
    }

    private String getCodeExampleTextForEmbedding(CodeExample e) {
        StringBuilder sb = new StringBuilder();
        if (e.getTitle() != null) sb.append(e.getTitle()).append(" ");
        if (e.getDescription() != null) sb.append(e.getDescription()).append(" ");
        if (e.getCodeSnippet() != null) sb.append(e.getCodeSnippet());
        return sb.toString().trim();
    }

    private void updateCodeExampleEmbedding(Long id, float[] embedding, String model) {
        String vectorString = floatArrayToVectorString(embedding);
        jdbcTemplate.update("""
            UPDATE code_examples
            SET example_embedding = ?::vector, embedding_model = ?, embedded_at = NOW()
            WHERE id = ?
            """, vectorString, model, id);
    }

    private Page<WikiReleaseNotes> fetchWikiReleaseNotesBatch(PageRequest pageRequest) {
        return wikiReleaseNotesRepository.findAll(pageRequest);
    }

    private String getWikiReleaseNotesTextForEmbedding(WikiReleaseNotes rn) {
        StringBuilder sb = new StringBuilder();
        if (rn.getTitle() != null) sb.append(rn.getTitle()).append(" ");
        if (rn.getContentMarkdown() != null) sb.append(rn.getContentMarkdown());
        return sb.toString().trim();
    }

    private void updateWikiReleaseNotesEmbedding(Long id, float[] embedding, String model) {
        String vectorString = floatArrayToVectorString(embedding);
        jdbcTemplate.update("""
            UPDATE wiki_release_notes
            SET content_embedding = ?::vector, embedding_model = ?, embedded_at = NOW()
            WHERE id = ?
            """, vectorString, model, id);
    }

    private Page<WikiMigrationGuide> fetchWikiMigrationGuideBatch(PageRequest pageRequest) {
        return wikiMigrationGuideRepository.findAll(pageRequest);
    }

    private String getWikiMigrationGuideTextForEmbedding(WikiMigrationGuide mg) {
        StringBuilder sb = new StringBuilder();
        if (mg.getTitle() != null) sb.append(mg.getTitle()).append(" ");
        if (mg.getContentMarkdown() != null) sb.append(mg.getContentMarkdown());
        return sb.toString().trim();
    }

    private void updateWikiMigrationGuideEmbedding(Long id, float[] embedding, String model) {
        String vectorString = floatArrayToVectorString(embedding);
        jdbcTemplate.update("""
            UPDATE wiki_migration_guides
            SET content_embedding = ?::vector, embedding_model = ?, embedded_at = NOW()
            WHERE id = ?
            """, vectorString, model, id);
    }

    private Page<SpringProject> fetchProjectBatch(PageRequest pageRequest) {
        return springProjectRepository.findAll(pageRequest);
    }

    private String getProjectTextForEmbedding(SpringProject project) {
        StringBuilder sb = new StringBuilder();
        if (project.getName() != null) sb.append(project.getName()).append(" ");
        if (project.getDescription() != null) sb.append(project.getDescription());
        return sb.toString().trim();
    }

    private void updateProjectEmbedding(Long id, float[] embedding, String model) {
        String vectorString = floatArrayToVectorString(embedding);
        jdbcTemplate.update("""
            UPDATE spring_projects
            SET project_embedding = ?::vector, embedding_model = ?, embedded_at = NOW()
            WHERE id = ?
            """, vectorString, model, id);
    }

    private void storeEntityEmbedding(String entityType, Long entityId, float[] embedding, String model) {
        String vectorString = floatArrayToVectorString(embedding);
        String table = switch (entityType) {
            case "DOCUMENTATION" -> "documentation_content";
            case "TRANSFORMATION" -> "migration_transformations";
            case "FLAVOR" -> "flavors";
            case "CODE_EXAMPLE" -> "code_examples";
            case "WIKI_RELEASE_NOTES" -> "wiki_release_notes";
            case "WIKI_MIGRATION_GUIDE" -> "wiki_migration_guides";
            case "PROJECT" -> "spring_projects";
            default -> throw new IllegalArgumentException("Unknown entity type: " + entityType);
        };
        String column = switch (entityType) {
            case "DOCUMENTATION" -> "content_embedding";
            case "TRANSFORMATION" -> "transformation_embedding";
            case "FLAVOR" -> "flavor_embedding";
            case "CODE_EXAMPLE" -> "example_embedding";
            case "WIKI_RELEASE_NOTES", "WIKI_MIGRATION_GUIDE" -> "content_embedding";
            case "PROJECT" -> "project_embedding";
            default -> throw new IllegalArgumentException("Unknown entity type: " + entityType);
        };

        jdbcTemplate.update(
                String.format("UPDATE %s SET %s = ?::vector, embedding_model = ?, embedded_at = NOW() WHERE id = ?",
                        table, column),
                vectorString, model, entityId
        );
    }

    private void storeChunkEmbedding(String entityType, Long entityId, int chunkIndex, String chunkText,
                                      float[] embedding, String model) {
        String vectorString = floatArrayToVectorString(embedding);
        int tokenCount = chunkingService.estimateTokens(chunkText);

        embeddingMetadataRepository.upsertEmbedding(
                entityType, entityId, chunkIndex, chunkText, vectorString, model, tokenCount
        );
    }

    private long countEntitiesWithEmbedding(String table, String embeddingColumn) {
        String sql = String.format("SELECT COUNT(*) FROM %s WHERE %s IS NOT NULL", table, embeddingColumn);
        Long count = jdbcTemplate.queryForObject(sql, Long.class);
        return count != null ? count : 0;
    }

    private String floatArrayToVectorString(float[] embedding) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < embedding.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(embedding[i]);
        }
        sb.append("]");
        return sb.toString();
    }

    /**
     * Result of a sync operation.
     */
    public record SyncResult(int processed, int succeeded, int failed) {
        public SyncResult add(SyncResult other) {
            return new SyncResult(
                    this.processed + other.processed,
                    this.succeeded + other.succeeded,
                    this.failed + other.failed
            );
        }
    }

    /**
     * Embedding statistics.
     */
    public record EmbeddingStats(
            long docsWithEmbedding, long docsTotal,
            long transformsWithEmbedding, long transformsTotal,
            long flavorsWithEmbedding, long flavorsTotal,
            long examplesWithEmbedding, long examplesTotal,
            long wikiReleaseNotesWithEmbedding, long wikiReleaseNotesTotal,
            long wikiMigrationGuidesWithEmbedding, long wikiMigrationGuidesTotal,
            long projectsWithEmbedding, long projectsTotal,
            long pendingJobs,
            long failedJobs,
            boolean providerAvailable,
            String providerName,
            String modelName
    ) {
        public double getDocsCoverage() {
            return docsTotal > 0 ? (double) docsWithEmbedding / docsTotal * 100 : 0;
        }

        public double getTransformsCoverage() {
            return transformsTotal > 0 ? (double) transformsWithEmbedding / transformsTotal * 100 : 0;
        }

        public double getFlavorsCoverage() {
            return flavorsTotal > 0 ? (double) flavorsWithEmbedding / flavorsTotal * 100 : 0;
        }

        public double getExamplesCoverage() {
            return examplesTotal > 0 ? (double) examplesWithEmbedding / examplesTotal * 100 : 0;
        }

        public double getWikiReleaseNotesCoverage() {
            return wikiReleaseNotesTotal > 0 ? (double) wikiReleaseNotesWithEmbedding / wikiReleaseNotesTotal * 100 : 0;
        }

        public double getWikiMigrationGuidesCoverage() {
            return wikiMigrationGuidesTotal > 0 ? (double) wikiMigrationGuidesWithEmbedding / wikiMigrationGuidesTotal * 100 : 0;
        }

        public double getProjectsCoverage() {
            return projectsTotal > 0 ? (double) projectsWithEmbedding / projectsTotal * 100 : 0;
        }

        public long getTotalWithEmbedding() {
            return docsWithEmbedding + transformsWithEmbedding + flavorsWithEmbedding + examplesWithEmbedding
                    + wikiReleaseNotesWithEmbedding + wikiMigrationGuidesWithEmbedding + projectsWithEmbedding;
        }

        public long getTotalEntities() {
            return docsTotal + transformsTotal + flavorsTotal + examplesTotal
                    + wikiReleaseNotesTotal + wikiMigrationGuidesTotal + projectsTotal;
        }

        public double getOverallCoverage() {
            long total = getTotalEntities();
            return total > 0 ? (double) getTotalWithEmbedding() / total * 100 : 0;
        }
    }
}
