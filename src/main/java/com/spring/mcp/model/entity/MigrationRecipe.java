package com.spring.mcp.model.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entity representing a migration recipe from OpenRewrite or other sources.
 * A recipe contains multiple transformations for upgrading between versions.
 */
@Entity
@Table(name = "migration_recipes")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"transformations", "projectMappings", "versionMappings"})
@EqualsAndHashCode(of = {"id", "name"})
public class MigrationRecipe {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(name = "display_name", nullable = false)
    private String displayName;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "from_project", nullable = false, length = 100)
    private String fromProject;

    @Column(name = "from_version_min", nullable = false, length = 50)
    private String fromVersionMin;

    @Column(name = "from_version_max", length = 50)
    private String fromVersionMax;

    @Column(name = "to_version", nullable = false, length = 50)
    private String toVersion;

    @Column(name = "source_url", length = 500)
    private String sourceUrl;

    @Column(name = "source_type", length = 50)
    @Builder.Default
    private String sourceType = "OPENREWRITE";

    @Column(length = 100)
    private String license;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "recipe", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<MigrationTransformation> transformations = new ArrayList<>();

    @OneToMany(mappedBy = "recipe", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<RecipeProjectMapping> projectMappings = new ArrayList<>();

    @OneToMany(mappedBy = "recipe", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<RecipeVersionMapping> versionMappings = new ArrayList<>();

    /**
     * Add a transformation to this recipe
     */
    public void addTransformation(MigrationTransformation transformation) {
        transformations.add(transformation);
        transformation.setRecipe(this);
    }

    /**
     * Remove a transformation from this recipe
     */
    public void removeTransformation(MigrationTransformation transformation) {
        transformations.remove(transformation);
        transformation.setRecipe(null);
    }

    /**
     * Get count of transformations
     */
    public int getTransformationCount() {
        return transformations != null ? transformations.size() : 0;
    }

    /**
     * Get count of breaking changes
     */
    public long getBreakingCount() {
        return transformations != null
                ? transformations.stream().filter(t -> Boolean.TRUE.equals(t.getBreakingChange())).count()
                : 0;
    }
}
