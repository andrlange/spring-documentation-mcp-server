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
 * Entity representing a Spring ecosystem project
 * (e.g., Spring Boot, Spring Framework, Spring Data)
 */
@Entity
@Table(name = "spring_projects")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"versions"})
@EqualsAndHashCode(of = {"id", "slug"})
public class SpringProject {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(nullable = false, unique = true)
    private String slug;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "homepage_url", length = 500)
    private String homepageUrl;

    @Column(name = "github_url", length = 500)
    private String githubUrl;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Embedding fields for semantic search
    @Column(name = "embedding_model", length = 100)
    private String embeddingModel;

    @Column(name = "embedded_at")
    private LocalDateTime embeddedAt;

    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ProjectVersion> versions = new ArrayList<>();


    public List<ProjectVersion> getVisibleVersions() {
        return versions.stream().filter(ProjectVersion::getVisible).toList();
    }
    /**
     * Add a version to this project
     */
    public void addVersion(ProjectVersion version) {
        versions.add(version);
        version.setProject(this);
    }

    /**
     * Remove a version from this project
     */
    public void removeVersion(ProjectVersion version) {
        versions.remove(version);
        version.setProject(null);
    }
}
