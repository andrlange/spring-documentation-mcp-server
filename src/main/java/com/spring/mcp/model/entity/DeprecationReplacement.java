package com.spring.mcp.model.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * Entity representing deprecation information and their replacements.
 * Tracks deprecated classes/methods and what to use instead.
 */
@Entity
@Table(name = "deprecation_replacements",
       uniqueConstraints = @UniqueConstraint(
           name = "uk_deprecation",
           columnNames = {"deprecated_class", "deprecated_method"}
       ))
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@EqualsAndHashCode(of = {"id"})
public class DeprecationReplacement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "deprecated_class", nullable = false, length = 500)
    private String deprecatedClass;

    @Column(name = "deprecated_method", length = 255)
    private String deprecatedMethod;

    @Column(name = "replacement_class", length = 500)
    private String replacementClass;

    @Column(name = "replacement_method", length = 255)
    private String replacementMethod;

    @Column(name = "deprecated_since", length = 50)
    private String deprecatedSince;

    @Column(name = "removed_in", length = 50)
    private String removedIn;

    @Column(name = "migration_notes", columnDefinition = "TEXT")
    private String migrationNotes;

    @Column(name = "code_before", columnDefinition = "TEXT")
    private String codeBefore;

    @Column(name = "code_after", columnDefinition = "TEXT")
    private String codeAfter;

    @Column(name = "project_slug", nullable = false, length = 100)
    private String projectSlug;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
