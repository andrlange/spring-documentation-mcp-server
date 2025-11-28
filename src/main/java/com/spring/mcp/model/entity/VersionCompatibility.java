package com.spring.mcp.model.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * Entity representing version compatibility information between
 * Spring Boot versions and their dependencies.
 */
@Entity
@Table(name = "version_compatibility",
       uniqueConstraints = @UniqueConstraint(
           name = "uk_compatibility",
           columnNames = {"spring_boot_version", "dependency_group", "dependency_artifact"}
       ))
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@EqualsAndHashCode(of = {"id"})
public class VersionCompatibility {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "spring_boot_version", nullable = false, length = 50)
    private String springBootVersion;

    @Column(name = "dependency_group", nullable = false, length = 100)
    private String dependencyGroup;

    @Column(name = "dependency_artifact", nullable = false, length = 100)
    private String dependencyArtifact;

    @Column(name = "compatible_version", nullable = false, length = 50)
    private String compatibleVersion;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Builder.Default
    private Boolean verified = false;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
