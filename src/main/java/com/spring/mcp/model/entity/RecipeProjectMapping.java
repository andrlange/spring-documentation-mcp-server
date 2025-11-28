package com.spring.mcp.model.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * Entity representing the many-to-many relationship between
 * migration recipes and Spring projects.
 */
@Entity
@Table(name = "recipe_project_mapping",
       uniqueConstraints = @UniqueConstraint(
           name = "uk_recipe_project",
           columnNames = {"recipe_id", "project_id"}
       ))
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"recipe", "project"})
@EqualsAndHashCode(of = {"id"})
public class RecipeProjectMapping {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipe_id", nullable = false)
    private MigrationRecipe recipe;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private SpringProject project;

    @Column(name = "relevance_score")
    @Builder.Default
    private Integer relevanceScore = 100;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
