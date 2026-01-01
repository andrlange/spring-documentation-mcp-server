package com.spring.mcp.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entity for storing embedding metadata and chunked embeddings.
 * Large documents are split into chunks, each with its own embedding stored here.
 *
 * @author Spring MCP Server
 * @version 1.6.0
 * @since 2026-01-01
 */
@Entity
@Table(name = "embedding_metadata", uniqueConstraints = {
    @UniqueConstraint(name = "unique_entity_chunk", columnNames = {"entity_type", "entity_id", "chunk_index"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"chunkText", "embedding"})
@EqualsAndHashCode(of = {"id"})
public class EmbeddingMetadata {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Type of entity this embedding belongs to.
     * Values: DOCUMENTATION, TRANSFORMATION, FLAVOR, CODE_EXAMPLE, JAVADOC_CLASS
     */
    @Column(name = "entity_type", nullable = false, length = 100)
    private String entityType;

    /**
     * ID of the entity this embedding belongs to.
     */
    @Column(name = "entity_id", nullable = false)
    private Long entityId;

    /**
     * Index of this chunk (0 for the first/only chunk).
     */
    @Column(name = "chunk_index")
    @Builder.Default
    private Integer chunkIndex = 0;

    /**
     * The text content of this chunk.
     */
    @Column(name = "chunk_text", columnDefinition = "TEXT")
    private String chunkText;

    /**
     * The embedding vector for this chunk.
     * Note: Using native SQL for pgvector operations as JPA doesn't natively support vector type.
     * This field is populated via native queries.
     */
    @Transient
    private float[] embedding;

    /**
     * Name of the embedding model used.
     */
    @Column(name = "embedding_model", nullable = false, length = 100)
    private String embeddingModel;

    /**
     * Estimated token count for this chunk.
     */
    @Column(name = "token_count")
    private Integer tokenCount;

    /**
     * Timestamp when this embedding was created.
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    /**
     * Entity types that can have embeddings.
     */
    public enum EntityType {
        DOCUMENTATION,
        TRANSFORMATION,
        FLAVOR,
        CODE_EXAMPLE,
        JAVADOC_CLASS
    }
}
