package com.spring.mcp.model.entity;

import com.spring.mcp.model.enums.VersionState;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Entity representing a Spring Boot version.
 * This is the central/primary table for filtering across the entire system.
 */
@Entity
@Table(name = "spring_boot_versions")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@EqualsAndHashCode(of = {"id", "version"})
public class SpringBootVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String version;

    @Column(name = "major_version", nullable = false)
    private Integer majorVersion;

    @Column(name = "minor_version", nullable = false)
    private Integer minorVersion;

    @Column(name = "patch_version")
    private Integer patchVersion;

    @Enumerated(EnumType.STRING)
    @Column(name = "state", nullable = false, length = 20)
    private VersionState state;

    @Column(name = "is_current")
    @Builder.Default
    private Boolean isCurrent = false;

    @Column(name = "released_at")
    private LocalDate releasedAt;

    @Column(name = "oss_support_end")
    private LocalDate ossSupportEnd;

    @Column(name = "enterprise_support_end")
    private LocalDate enterpriseSupportEnd;

    @Column(name = "reference_doc_url", length = 500)
    private String referenceDocUrl;

    @Column(name = "api_doc_url", length = 500)
    private String apiDocUrl;

    /**
     * Indicates if this version is only available under enterprise subscription.
     * Enterprise-only versions have ended OSS support but still have active enterprise support.
     */
    @Column(name = "is_enterprise_only")
    @Builder.Default
    private Boolean isEnterpriseOnly = false;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * Check if this version is end-of-life (enterprise support has ended)
     */
    public boolean isEndOfLife() {
        return enterpriseSupportEnd != null && LocalDate.now().isAfter(enterpriseSupportEnd);
    }


    public String referenceDocUrlFormatted() {
        if (referenceDocUrl == null || referenceDocUrl.isBlank()) {
            return "";
        }

        String url = referenceDocUrl;

        if (Boolean.TRUE.equals(isCurrent)) {
            // https://docs.spring.io/spring-boot/{version}/index.html
            url = url.replace("{version}/", "");
        } else {
            url = url.replace("{version}", getFormattedVersion());
        }
        return url;
    }

    public String apiDocUrlFormatted() {
        if (apiDocUrl == null || apiDocUrl.isBlank()) {
            return "";
        }

        String url = apiDocUrl;

        if (Boolean.TRUE.equals(isCurrent)) {
            // https://docs.spring.io/spring-boot/{version}/index.html
            url = url.replace("{version}/", "");
        } else {
            url = url.replace("{version}", getFormattedVersion());
        }
        return url;
    }

    /**
     * Get formatted version string
     */
    public String getFormattedVersion() {
        final boolean isNew = majorVersion >=3 && minorVersion >= 3;
        String version = "";
        String snapshot = "";
        if (state == VersionState.SNAPSHOT) {
            snapshot = "-SNAPSHOT";
        }

        if(isNew) {
            version = String.format("%d.%d", majorVersion, minorVersion)+snapshot;
        } else {
            version =  majorVersion + "." + minorVersion + "." + patchVersion + snapshot;
        }
        return version;
    }

    /**
     * Check if OSS support is still active
     */
    public boolean isOssSupportActive() {
        return ossSupportEnd == null || LocalDate.now().isBefore(ossSupportEnd);
    }

    /**
     * Check if enterprise support is still active
     */
    public boolean isEnterpriseSupportActive() {
        return enterpriseSupportEnd == null || LocalDate.now().isBefore(enterpriseSupportEnd);
    }

    /**
     * Check if this version should show a red flag (support has ended).
     * Red flag indicates that the version is no longer supported based on the subscription type.
     *
     * @param enterpriseSubscriptionEnabled true if enterprise subscription is enabled, false for OSS only
     * @return true if support has ended (red flag), false if still supported (green flag)
     */
    public boolean getRedFlag(boolean enterpriseSubscriptionEnabled) {
        LocalDate supportEndDate = enterpriseSubscriptionEnabled ? enterpriseSupportEnd : ossSupportEnd;

        if (supportEndDate == null) {
            return false; // No end date means still supported (green)
        }

        LocalDate today = LocalDate.now();
        return today.isAfter(supportEndDate) || today.isEqual(supportEndDate); // Red if today >= support end
    }
}
