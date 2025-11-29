package com.spring.mcp.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entity representing a code pattern example showing old vs new approaches.
 * Used to demonstrate how to modernize code using new language features.
 *
 * @author Spring MCP Server
 * @version 1.2.0
 * @since 2025-11-29
 */
@Entity
@Table(name = "language_code_patterns")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LanguageCodePattern {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The feature this code pattern demonstrates
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "feature_id", nullable = false)
    @ToString.Exclude
    private LanguageFeature feature;

    /**
     * Legacy/old code pattern (pre-feature)
     */
    @Column(name = "old_pattern", nullable = false, columnDefinition = "TEXT")
    private String oldPattern;

    /**
     * Modern/new code pattern using the feature
     */
    @Column(name = "new_pattern", nullable = false, columnDefinition = "TEXT")
    private String newPattern;

    /**
     * Programming language of the code (java, kotlin)
     */
    @Column(name = "pattern_language", nullable = false, length = 20)
    private String patternLanguage;

    /**
     * Explanation of the improvement
     */
    @Column(name = "explanation", columnDefinition = "TEXT")
    private String explanation;

    /**
     * Minimum language version required for the new pattern
     */
    @Column(name = "min_version", length = 50)
    private String minVersion;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    /**
     * Get code language display name
     */
    public String getLanguageDisplayName() {
        return patternLanguage != null ?
            patternLanguage.substring(0, 1).toUpperCase() + patternLanguage.substring(1).toLowerCase()
            : "Unknown";
    }
}
