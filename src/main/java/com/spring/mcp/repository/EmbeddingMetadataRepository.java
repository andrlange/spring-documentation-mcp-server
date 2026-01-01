package com.spring.mcp.repository;

import com.spring.mcp.model.entity.EmbeddingMetadata;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for EmbeddingMetadata entity.
 * Handles chunked embeddings for large documents.
 *
 * @author Spring MCP Server
 * @version 1.6.0
 * @since 2026-01-01
 */
@Repository
public interface EmbeddingMetadataRepository extends JpaRepository<EmbeddingMetadata, Long> {

    /**
     * Find all chunks for a specific entity.
     */
    List<EmbeddingMetadata> findByEntityTypeAndEntityIdOrderByChunkIndex(String entityType, Long entityId);

    /**
     * Find all chunks for an entity type.
     */
    List<EmbeddingMetadata> findByEntityType(String entityType);

    /**
     * Delete all chunks for a specific entity.
     */
    @Modifying
    void deleteByEntityTypeAndEntityId(String entityType, Long entityId);

    /**
     * Delete all chunks for an entity type.
     */
    @Modifying
    void deleteByEntityType(String entityType);

    /**
     * Count chunks for a specific entity.
     */
    long countByEntityTypeAndEntityId(String entityType, Long entityId);

    /**
     * Count entities with embeddings by type.
     */
    @Query("SELECT COUNT(DISTINCT em.entityId) FROM EmbeddingMetadata em WHERE em.entityType = :entityType")
    long countDistinctEntitiesByType(@Param("entityType") String entityType);

    /**
     * Find entity IDs that have embeddings for a given type.
     */
    @Query("SELECT DISTINCT em.entityId FROM EmbeddingMetadata em WHERE em.entityType = :entityType")
    List<Long> findEntityIdsByType(@Param("entityType") String entityType);

    /**
     * Check if an entity has embeddings.
     */
    boolean existsByEntityTypeAndEntityId(String entityType, Long entityId);

    /**
     * Save embedding with vector using native query.
     * JPA doesn't natively support pgvector types, so we use native SQL.
     */
    @Modifying
    @Query(value = """
        INSERT INTO embedding_metadata (entity_type, entity_id, chunk_index, chunk_text, embedding, embedding_model, token_count, created_at)
        VALUES (:entityType, :entityId, :chunkIndex, :chunkText, :embedding::vector, :embeddingModel, :tokenCount, NOW())
        ON CONFLICT (entity_type, entity_id, chunk_index)
        DO UPDATE SET
            chunk_text = EXCLUDED.chunk_text,
            embedding = EXCLUDED.embedding,
            embedding_model = EXCLUDED.embedding_model,
            token_count = EXCLUDED.token_count
        """, nativeQuery = true)
    void upsertEmbedding(
            @Param("entityType") String entityType,
            @Param("entityId") Long entityId,
            @Param("chunkIndex") Integer chunkIndex,
            @Param("chunkText") String chunkText,
            @Param("embedding") String embedding,
            @Param("embeddingModel") String embeddingModel,
            @Param("tokenCount") Integer tokenCount
    );

    /**
     * Find similar chunks using cosine distance.
     * Returns chunk IDs ordered by similarity (closest first).
     */
    @Query(value = """
        SELECT em.id, em.entity_type, em.entity_id, em.chunk_index,
               1 - (em.embedding <=> :queryEmbedding::vector) as similarity
        FROM embedding_metadata em
        WHERE em.entity_type = :entityType
        AND em.embedding IS NOT NULL
        AND 1 - (em.embedding <=> :queryEmbedding::vector) >= :minSimilarity
        ORDER BY em.embedding <=> :queryEmbedding::vector
        LIMIT :limit
        """, nativeQuery = true)
    List<Object[]> findSimilarChunks(
            @Param("entityType") String entityType,
            @Param("queryEmbedding") String queryEmbedding,
            @Param("minSimilarity") double minSimilarity,
            @Param("limit") int limit
    );
}
