package com.spring.mcp.model.entity;

import com.spring.mcp.model.enums.LanguageType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entity mapping Spring Boot versions to required Java/Kotlin versions.
 * Used to provide language version guidance for Spring Boot development.
 *
 * @author Spring MCP Server
 * @version 1.2.0
 * @since 2025-11-29
 */
@Entity
@Table(name = "spring_boot_language_requirements")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SpringBootLanguageRequirement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The Spring Boot version this requirement applies to
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "spring_boot_version_id", nullable = false)
    @ToString.Exclude
    private SpringBootVersion springBootVersion;

    /**
     * Programming language type: JAVA or KOTLIN
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "language", nullable = false, length = 20)
    private LanguageType language;

    /**
     * Minimum required language version
     */
    @Column(name = "min_version", nullable = false, length = 50)
    private String minVersion;

    /**
     * Recommended language version for best compatibility
     */
    @Column(name = "recommended_version", length = 50)
    private String recommendedVersion;

    /**
     * Maximum supported language version (if applicable)
     */
    @Column(name = "max_version", length = 50)
    private String maxVersion;

    /**
     * Additional notes about compatibility
     */
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    /**
     * Get display string for version range (e.g., "17+" or "17-21")
     */
    public String getVersionRange() {
        StringBuilder sb = new StringBuilder(minVersion);
        if (maxVersion != null && !maxVersion.isBlank()) {
            sb.append("-").append(maxVersion);
        } else {
            sb.append("+");
        }
        return sb.toString();
    }

    /**
     * Get formatted requirement string
     */
    public String getRequirementSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append(language.getDisplayName()).append(" ").append(minVersion).append("+");
        if (recommendedVersion != null && !recommendedVersion.isBlank()) {
            sb.append(" (").append(recommendedVersion).append(" recommended)");
        }
        return sb.toString();
    }
}
