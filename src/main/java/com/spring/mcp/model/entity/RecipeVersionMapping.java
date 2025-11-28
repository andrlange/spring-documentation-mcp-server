package com.spring.mcp.model.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * Entity representing the relationship between migration recipes
 * and project versions (source and target versions).
 */
@Entity
@Table(name = "recipe_version_mapping",
       uniqueConstraints = @UniqueConstraint(
           name = "uk_recipe_version",
           columnNames = {"recipe_id", "version_id", "mapping_type"}
       ))
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"recipe", "version"})
@EqualsAndHashCode(of = {"id"})
public class RecipeVersionMapping {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipe_id", nullable = false)
    private MigrationRecipe recipe;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "version_id", nullable = false)
    private ProjectVersion version;

    @Column(name = "mapping_type", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private MappingType mappingType;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Types of version mappings
     */
    public enum MappingType {
        SOURCE,  // The version to migrate FROM
        TARGET   // The version to migrate TO
    }
}
