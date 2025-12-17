package com.spring.mcp.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entity representing a KEP (Kotlin Enhancement Proposal) or KEEP specification.
 * Stores content fetched from GitHub KEEP repo or JetBrains YouTrack.
 *
 * Sources:
 * - KEEP: https://github.com/Kotlin/KEEP/blob/master/proposals/{name}.md
 * - YouTrack: https://youtrack.jetbrains.com/issue/{KT-number}
 *
 * @author Spring MCP Server
 * @version 1.5.2
 * @since 2025-12-17
 */
@Entity
@Table(name = "kep_specifications")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KepSpecification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * KEP identifier (e.g., "KT-11550" or "context-parameters")
     */
    @Column(name = "kep_number", nullable = false, unique = true, length = 50)
    private String kepNumber;

    /**
     * Title of the KEP/proposal
     */
    @Column(name = "title", length = 500)
    private String title;

    /**
     * Brief summary of what the proposal addresses
     */
    @Column(name = "summary", columnDefinition = "TEXT")
    private String summary;

    /**
     * Detailed description of the proposal
     */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /**
     * Motivation section explaining why the proposal is needed
     */
    @Column(name = "motivation", columnDefinition = "TEXT")
    private String motivation;

    /**
     * Original markdown content (for KEEP sources)
     */
    @Column(name = "markdown_content", columnDefinition = "TEXT")
    private String markdownContent;

    /**
     * Rendered HTML content for display
     */
    @Column(name = "html_content", columnDefinition = "TEXT")
    private String htmlContent;

    /**
     * Proposal status (e.g., "Open", "In Progress", "Implemented", "Closed")
     */
    @Column(name = "status", length = 50)
    private String status;

    /**
     * Source type: KEEP (GitHub KEEP repo) or YOUTRACK (JetBrains issue tracker)
     */
    @Column(name = "source_type", nullable = false, length = 20)
    private String sourceType;

    /**
     * Original source URL
     */
    @Column(name = "source_url", length = 500)
    private String sourceUrl;

    /**
     * When the content was fetched from the source
     */
    @Column(name = "fetched_at")
    private LocalDateTime fetchedAt;

    /**
     * When this record was created
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * When this record was last updated
     */
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (sourceType == null) {
            sourceType = "YOUTRACK";
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * Check if the specification content has been fetched
     */
    public boolean isFetched() {
        return fetchedAt != null;
    }

    /**
     * Check if this is from KEEP repository
     */
    public boolean isFromKeep() {
        return "KEEP".equals(sourceType);
    }

    /**
     * Check if this is from YouTrack
     */
    public boolean isFromYouTrack() {
        return "YOUTRACK".equals(sourceType);
    }

    /**
     * Get the external URL based on source type
     */
    public String getExternalUrl() {
        if (sourceUrl != null && !sourceUrl.isBlank()) {
            return sourceUrl;
        }
        if (kepNumber.startsWith("KT-")) {
            return "https://youtrack.jetbrains.com/issue/" + kepNumber;
        }
        return "https://github.com/Kotlin/KEEP/blob/master/proposals/" + kepNumber + ".md";
    }

    /**
     * Source type constants
     */
    public static final String SOURCE_KEEP = "KEEP";
    public static final String SOURCE_YOUTRACK = "YOUTRACK";
}
