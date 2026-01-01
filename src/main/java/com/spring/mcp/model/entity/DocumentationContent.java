package com.spring.mcp.model.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Entity representing cached documentation content
 */
@Entity
@Table(name = "documentation_content", uniqueConstraints = {
    @UniqueConstraint(name = "unique_link_content", columnNames = {"link_id"})
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"link", "content"})
@EqualsAndHashCode(of = {"id"})
public class DocumentationContent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "link_id", nullable = false)
    private DocumentationLink link;

    @Column(name = "content_type", length = 50)
    private String contentType;

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "jsonb")
    private Map<String, Object> metadata;

    // Note: indexed_content (TSVECTOR) is handled by PostgreSQL trigger
    // and not mapped as a field since it's a computed column

    // === Embedding fields (Release 1.6.0) ===
    // Note: The actual vector is stored via native SQL queries as JPA doesn't support pgvector
    // These fields track embedding metadata

    /**
     * Name of the embedding model used (e.g., "nomic-embed-text", "text-embedding-3-small")
     */
    @Column(name = "embedding_model", length = 100)
    private String embeddingModel;

    /**
     * Timestamp when the embedding was generated
     */
    @Column(name = "embedded_at")
    private LocalDateTime embeddedAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * Check if this content has an embedding.
     */
    public boolean hasEmbedding() {
        return embeddingModel != null && embeddedAt != null;
    }
}
