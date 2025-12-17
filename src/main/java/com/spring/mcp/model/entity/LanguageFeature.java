package com.spring.mcp.model.entity;

import com.spring.mcp.model.enums.FeatureStatus;
import com.spring.mcp.model.enums.ImpactLevel;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entity representing a language feature, deprecation, or removal.
 * Tracks feature status, category, and associated code patterns.
 *
 * @author Spring MCP Server
 * @version 1.2.0
 * @since 2025-11-29
 */
@Entity
@Table(name = "language_features")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LanguageFeature {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The language version this feature belongs to
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "language_version_id", nullable = false)
    @ToString.Exclude
    private LanguageVersion languageVersion;

    /**
     * Name of the feature
     */
    @Column(name = "feature_name", nullable = false, length = 255)
    private String featureName;

    /**
     * Status of the feature (NEW, DEPRECATED, REMOVED, PREVIEW, INCUBATING)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private FeatureStatus status;

    /**
     * Category (e.g., Syntax, API, Concurrency, Performance)
     */
    @Column(name = "category", length = 100)
    private String category;

    /**
     * JEP (JDK Enhancement Proposal) number for Java features
     */
    @Column(name = "jep_number", length = 20)
    private String jepNumber;

    /**
     * KEP (Kotlin Enhancement Proposal) number for Kotlin features
     */
    @Column(name = "kep_number", length = 20)
    private String kepNumber;

    /**
     * Description of the feature
     */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /**
     * Impact level on existing code
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "impact_level", length = 20)
    private ImpactLevel impactLevel;

    /**
     * Notes about migration/upgrade considerations
     */
    @Column(name = "migration_notes", columnDefinition = "TEXT")
    private String migrationNotes;

    /**
     * Simple code example demonstrating the feature
     */
    @Column(name = "code_example", columnDefinition = "TEXT")
    private String codeExample;

    /**
     * Source type for the code example: OFFICIAL (from specification) or SYNTHESIZED (manually curated)
     */
    @Column(name = "example_source_type", length = 20)
    @Builder.Default
    private String exampleSourceType = "OFFICIAL";

    /**
     * URL to official documentation
     */
    @Column(name = "documentation_url", length = 500)
    private String documentationUrl;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Code patterns demonstrating old vs new approaches
     */
    @OneToMany(mappedBy = "feature", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<LanguageCodePattern> codePatterns = new ArrayList<>();

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
     * Get the JEP or KEP reference if available
     */
    public String getEnhancementProposal() {
        if (jepNumber != null && !jepNumber.isBlank()) {
            return "JEP " + jepNumber;
        }
        if (kepNumber != null && !kepNumber.isBlank()) {
            return "KEP " + kepNumber;
        }
        return null;
    }

    /**
     * Get URL to JEP/KEP if available
     */
    public String getEnhancementProposalUrl() {
        if (jepNumber != null && !jepNumber.isBlank()) {
            return "https://openjdk.org/jeps/" + jepNumber;
        }
        // Kotlin doesn't have a public KEP system yet
        return null;
    }

    /**
     * Check if this feature has code patterns
     */
    public boolean hasCodePatterns() {
        return codePatterns != null && !codePatterns.isEmpty();
    }

    /**
     * Check if this feature has a code example
     */
    public boolean hasCodeExample() {
        return codeExample != null && !codeExample.isBlank();
    }

    /**
     * Check if the code example is synthesized (manually curated)
     */
    public boolean isSynthesizedExample() {
        return "SYNTHESIZED".equals(exampleSourceType);
    }

    /**
     * Check if the code example is official (from specification)
     */
    public boolean isOfficialExample() {
        return "OFFICIAL".equals(exampleSourceType) || exampleSourceType == null;
    }

    /**
     * Constants for example source types
     */
    public static final String EXAMPLE_SOURCE_OFFICIAL = "OFFICIAL";
    public static final String EXAMPLE_SOURCE_SYNTHESIZED = "SYNTHESIZED";
}
