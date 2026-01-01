package com.spring.mcp.model.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * Entity representing a code example for a Spring project version
 */
@Entity
@Table(name = "code_examples")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"version", "codeSnippet"})
@EqualsAndHashCode(of = {"id"})
public class CodeExample {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "version_id", nullable = false)
    private ProjectVersion version;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "code_snippet", nullable = false, columnDefinition = "TEXT")
    private String codeSnippet;

    @Column(length = 50)
    @Builder.Default
    private String language = "java";

    @Column(length = 255)
    private String category;

    @Column(columnDefinition = "text[]")
    private String[] tags;

    @Column(name = "source_url", length = 500)
    private String sourceUrl;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // === Embedding fields (Release 1.6.0) ===

    /**
     * Name of the embedding model used
     */
    @Column(name = "embedding_model", length = 100)
    private String embeddingModel;

    /**
     * Timestamp when the embedding was generated
     */
    @Column(name = "embedded_at")
    private LocalDateTime embeddedAt;

    /**
     * Check if this code example has an embedding.
     */
    public boolean hasEmbedding() {
        return embeddingModel != null && embeddedAt != null;
    }
}
