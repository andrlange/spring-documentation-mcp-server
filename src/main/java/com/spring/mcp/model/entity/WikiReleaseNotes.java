package com.spring.mcp.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entity representing Spring Boot Release Notes from the GitHub Wiki.
 * Release notes document what's new, changed, or fixed in each Spring Boot version.
 *
 * @author Spring MCP Server
 * @version 1.7.0
 * @since 2026-01-06
 */
@Entity
@Table(name = "wiki_release_notes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"content", "contentMarkdown"})
@EqualsAndHashCode(of = {"id"})
public class WikiReleaseNotes {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Reference to the Spring Boot version entity (optional)
     */
    @Column(name = "spring_boot_version_id")
    private Long springBootVersionId;

    /**
     * Version string (e.g., "3.5", "4.0")
     */
    @Column(name = "version_string", nullable = false, unique = true)
    private String versionString;

    /**
     * Major version number (e.g., 3 for "3.5")
     */
    @Column(name = "major_version", nullable = false)
    private Integer majorVersion;

    /**
     * Minor version number (e.g., 5 for "3.5")
     */
    @Column(name = "minor_version", nullable = false)
    private Integer minorVersion;

    /**
     * Title of the release notes (e.g., "Spring Boot 3.5 Release Notes")
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
     * Check if this release notes entry has an embedding.
     */
    public boolean hasEmbedding() {
        return embeddingModel != null && embeddedAt != null;
    }

    /**
     * Get the display version (e.g., "3.5" or "4.0")
     */
    public String getDisplayVersion() {
        return majorVersion + "." + minorVersion;
    }
}
