package com.spring.mcp.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entity representing a JEP (JDK Enhancement Proposal) specification.
 * Stores the full content fetched from openjdk.org for detail page display.
 *
 * @author Spring MCP Server
 * @version 1.5.2
 * @since 2025-12-17
 */
@Entity
@Table(name = "jep_specifications")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JepSpecification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * JEP number (e.g., "444", "501")
     */
    @Column(name = "jep_number", nullable = false, unique = true, length = 20)
    private String jepNumber;

    /**
     * Title of the JEP
     */
    @Column(name = "title", length = 500)
    private String title;

    /**
     * Brief summary of what the JEP addresses
     */
    @Column(name = "summary", columnDefinition = "TEXT")
    private String summary;

    /**
     * Detailed description of the JEP
     */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /**
     * Motivation section explaining why the JEP is needed
     */
    @Column(name = "motivation", columnDefinition = "TEXT")
    private String motivation;

    /**
     * Goals of the JEP
     */
    @Column(name = "goals", columnDefinition = "TEXT")
    private String goals;

    /**
     * Non-goals - what the JEP explicitly does not aim to do
     */
    @Column(name = "non_goals", columnDefinition = "TEXT")
    private String nonGoals;

    /**
     * Full HTML content for rendering the detail page
     */
    @Column(name = "html_content", columnDefinition = "TEXT")
    private String htmlContent;

    /**
     * JEP status (e.g., "Candidate", "Preview", "Final", "Closed/Delivered")
     */
    @Column(name = "status", length = 50)
    private String status;

    /**
     * Target Java version (e.g., "21", "25")
     */
    @Column(name = "target_version", length = 50)
    private String targetVersion;

    /**
     * Original source URL (e.g., "https://openjdk.org/jeps/444")
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
     * Get the external URL to openjdk.org
     */
    public String getExternalUrl() {
        return "https://openjdk.org/jeps/" + jepNumber;
    }
}
