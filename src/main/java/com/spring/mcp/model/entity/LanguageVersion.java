package com.spring.mcp.model.entity;

import com.spring.mcp.model.enums.LanguageType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entity representing a programming language version (Java or Kotlin).
 * Tracks version information, release dates, and support periods.
 *
 * @author Spring MCP Server
 * @version 1.2.0
 * @since 2025-11-29
 */
@Entity
@Table(name = "language_versions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LanguageVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Programming language type: JAVA or KOTLIN
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "language", nullable = false, length = 20)
    private LanguageType language;

    /**
     * Version string (e.g., "21", "17", "2.0", "1.9")
     */
    @Column(name = "version", nullable = false, length = 50)
    private String version;

    /**
     * Major version number
     */
    @Column(name = "major_version", nullable = false)
    private Integer majorVersion;

    /**
     * Minor version number
     */
    @Column(name = "minor_version", nullable = false)
    private Integer minorVersion;

    /**
     * Patch version number (optional)
     */
    @Column(name = "patch_version")
    private Integer patchVersion;

    /**
     * Version codename (e.g., "Spider" for Java 8)
     */
    @Column(name = "codename", length = 100)
    private String codename;

    /**
     * Release date
     */
    @Column(name = "release_date")
    private LocalDate releaseDate;

    /**
     * Whether this is a Long Term Support (LTS) version (Java only)
     */
    @Column(name = "is_lts")
    @Builder.Default
    private Boolean isLts = false;

    /**
     * Whether this is the current/latest version
     */
    @Column(name = "is_current")
    @Builder.Default
    private Boolean isCurrent = false;

    /**
     * OSS (Open Source Software) support end date
     */
    @Column(name = "oss_support_end")
    private LocalDate ossSupportEnd;

    /**
     * Extended/Enterprise support end date
     */
    @Column(name = "extended_support_end")
    private LocalDate extendedSupportEnd;

    /**
     * Minimum Spring Boot version that supports this language version
     */
    @Column(name = "min_spring_boot_version", length = 50)
    private String minSpringBootVersion;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Features associated with this version
     */
    @OneToMany(mappedBy = "languageVersion", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<LanguageFeature> features = new ArrayList<>();

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
     * Get display name (e.g., "Java 21", "Kotlin 2.0")
     */
    public String getDisplayName() {
        return language.getDisplayName() + " " + version;
    }

    /**
     * Check if version is still supported (OSS)
     */
    public boolean isSupported() {
        if (ossSupportEnd == null) {
            return true; // No end date means still supported
        }
        return LocalDate.now().isBefore(ossSupportEnd);
    }

    /**
     * Check if extended support is active
     */
    public boolean hasExtendedSupport() {
        if (extendedSupportEnd == null) {
            return false;
        }
        return LocalDate.now().isBefore(extendedSupportEnd);
    }
}
