package com.spring.mcp.model.entity;

import com.spring.mcp.model.enums.FlavorCategory;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Entity representing a Flavor - company guidelines, architecture patterns,
 * compliance rules, agent configurations, and project initialization templates.
 *
 * @author Spring MCP Server
 * @version 1.2.0
 * @since 2025-11-30
 */
@Entity
@Table(name = "flavors")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"content"})
@EqualsAndHashCode(of = {"id"})
public class Flavor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "unique_name", nullable = false, unique = true)
    private String uniqueName;

    @Column(name = "display_name", nullable = false)
    private String displayName;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false)
    private FlavorCategory category;

    @Column(name = "pattern_name")
    private String patternName;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "content_hash")
    private String contentHash;

    @Column(name = "tags", columnDefinition = "TEXT[]")
    @JdbcTypeCode(SqlTypes.ARRAY)
    @Builder.Default
    private List<String> tags = new ArrayList<>();

    @Column(name = "metadata", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    @Builder.Default
    private Map<String, Object> metadata = new HashMap<>();

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "updated_by")
    private String updatedBy;

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
     * Helper to get tags as a String array for native queries.
     */
    public String[] getTagsArray() {
        return tags != null ? tags.toArray(new String[0]) : new String[0];
    }
}
