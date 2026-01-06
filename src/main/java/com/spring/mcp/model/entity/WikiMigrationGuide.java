package com.spring.mcp.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entity representing Spring Boot Migration Guides from the GitHub Wiki.
 * Migration guides document breaking changes, deprecations, and upgrade instructions
 * when migrating between Spring Boot versions.
 *
 * @author Spring MCP Server
 * @version 1.7.0
 * @since 2026-01-06
 */
@Entity
@Table(name = "wiki_migration_guides")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"content", "contentMarkdown"})
@EqualsAndHashCode(of = {"id"})
public class WikiMigrationGuide {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Reference to the source Spring Boot version entity (optional)
     */
    @Column(name = "source_version_id")
    private Long sourceVersionId;

    /**
     * Reference to the target Spring Boot version entity (optional)
     */
    @Column(name = "target_version_id")
    private Long targetVersionId;

    /**
     * Source version string (e.g., "3.5")
     */
    @Column(name = "source_version_string", nullable = false)
    private String sourceVersionString;

    /**
     * Target version string (e.g., "4.0")
     */
    @Column(name = "target_version_string", nullable = false)
    private String targetVersionString;

    /**
     * Source major version number
     */
    @Column(name = "source_major", nullable = false)
    private Integer sourceMajor;

    /**
     * Source minor version number
     */
    @Column(name = "source_minor", nullable = false)
    private Integer sourceMinor;

    /**
     * Target major version number
     */
    @Column(name = "target_major", nullable = false)
    private Integer targetMajor;

    /**
     * Target minor version number
     */
    @Column(name = "target_minor", nullable = false)
    private Integer targetMinor;

    /**
     * Title of the migration guide (e.g., "Spring Boot 3.5 to 4.0 Migration Guide")
     */
    @Column(name = "title", nullable = false, length = 500)
    private String title;

    /**
     * Original AsciiDoc content from the wiki
     */
    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    /**
     * Converted Markdown content for rendering
     */
    @Column(name = "content_markdown", columnDefinition = "TEXT")
    private String contentMarkdown;

    /**
     * SHA-256 hash of content for change detection
     */
    @Column(name = "content_hash", length = 64)
    private String contentHash;

    /**
     * URL to the wiki page on GitHub
     */
    @Column(name = "source_url", length = 1000)
    private String sourceUrl;

    /**
     * Source filename in the wiki repository
     */
    @Column(name = "source_file", length = 255)
    private String sourceFile;

    /**
     * Last modification time from the wiki
     */
    @Column(name = "wiki_last_modified")
    private LocalDateTime wikiLastModified;

    // === Embedding fields ===

    /**
     * Name of the embedding model used (e.g., "nomic-embed-text")
     */
    @Column(name = "embedding_model", length = 100)
    private String embeddingModel;

    /**
     * Timestamp when the embedding was generated
     */
    @Column(name = "embedded_at")
    private LocalDateTime embeddedAt;

    // === Timestamps ===

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * Check if this migration guide has an embedding.
     */
    public boolean hasEmbedding() {
        return embeddingModel != null && embeddedAt != null;
    }

    /**
     * Get the migration path display string (e.g., "3.5 -> 4.0")
     */
    public String getMigrationPath() {
        return sourceVersionString + " -> " + targetVersionString;
    }

    /**
     * Get the source display version
     */
    public String getSourceDisplayVersion() {
        return sourceMajor + "." + sourceMinor;
    }

    /**
     * Get the target display version
     */
    public String getTargetDisplayVersion() {
        return targetMajor + "." + targetMinor;
    }
}
