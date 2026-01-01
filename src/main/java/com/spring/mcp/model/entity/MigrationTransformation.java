package com.spring.mcp.model.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Entity representing an individual transformation within a migration recipe.
 * Examples: import changes, dependency updates, property migrations, code modifications.
 */
@Entity
@Table(name = "migration_transformations")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"recipe"})
@EqualsAndHashCode(of = {"id"})
public class MigrationTransformation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipe_id", nullable = false)
    private MigrationRecipe recipe;

    @Column(name = "transformation_type", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private TransformationType transformationType;

    @Column(length = 100)
    private String category;

    @Column(length = 100)
    private String subcategory;

    @Column(name = "old_pattern", nullable = false, columnDefinition = "TEXT")
    private String oldPattern;

    @Column(name = "new_pattern", nullable = false, columnDefinition = "TEXT")
    private String newPattern;

    @Column(name = "file_pattern", length = 255)
    private String filePattern;

    @Column(name = "regex_pattern")
    @Builder.Default
    private Boolean regexPattern = false;

    @Column(columnDefinition = "TEXT")
    private String explanation;

    @Column(name = "code_example", columnDefinition = "TEXT")
    private String codeExample;

    @Column(name = "additional_steps", columnDefinition = "TEXT")
    private String additionalSteps;

    @Column(name = "breaking_change")
    @Builder.Default
    private Boolean breakingChange = false;

    @Column(length = 20)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private Severity severity = Severity.INFO;

    @Builder.Default
    private Integer priority = 0;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(columnDefinition = "TEXT[]")
    private List<String> tags;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

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
     * Check if this transformation has an embedding.
     */
    public boolean hasEmbedding() {
        return embeddingModel != null && embeddedAt != null;
    }

    /**
     * Types of transformations
     */
    public enum TransformationType {
        IMPORT,      // Import statement changes
        DEPENDENCY,  // Build dependency changes
        PROPERTY,    // Application property changes
        CODE,        // Code pattern changes
        BUILD,       // Build configuration changes
        TEMPLATE,    // Template/view changes (Thymeleaf, etc.)
        ANNOTATION,  // Annotation changes
        CONFIG       // Configuration class changes
    }

    /**
     * Severity levels for transformations
     */
    public enum Severity {
        INFO,        // Informational, non-breaking
        WARNING,     // May cause issues
        ERROR,       // Will cause compilation errors
        CRITICAL     // Must be addressed for application to work
    }
}
