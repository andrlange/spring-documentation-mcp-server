package com.spring.mcp.service.initializr.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * DTO representing dependency compatibility information.
 *
 * <p>Used to check whether a specific dependency is compatible with
 * a given Spring Boot version, along with explanatory information
 * about the compatibility status.</p>
 *
 * <p>Example response:</p>
 * <pre>{@code
 * {
 *   "compatible": false,
 *   "dependencyId": "spring-cloud-starter-gateway",
 *   "dependencyName": "Spring Cloud Gateway",
 *   "bootVersion": "2.7.0",
 *   "reason": "Requires Spring Boot 3.0.0 or higher",
 *   "versionRange": "[3.0.0,)",
 *   "suggestedBootVersion": "3.5.8",
 *   "alternatives": ["spring-cloud-starter-gateway-mvc"]
 * }
 * }</pre>
 *
 * @author Spring MCP Server
 * @version 1.4.0
 * @since 2025-12-06
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class CompatibilityInfo {

    /**
     * Whether the dependency is compatible with the specified version.
     */
    private boolean compatible;

    /**
     * The dependency ID that was checked.
     */
    private String dependencyId;

    /**
     * The display name of the dependency.
     */
    private String dependencyName;

    /**
     * The Spring Boot version that was checked against.
     */
    private String bootVersion;

    /**
     * Human-readable explanation of the compatibility status.
     * Examples:
     * - "Compatible with all Spring Boot 3.x versions"
     * - "Requires Spring Boot 3.0.0 or higher"
     * - "Only available for Spring Boot versions 3.0.0 to 3.5.x"
     */
    private String reason;

    /**
     * The version range constraint for this dependency.
     * Maven version range syntax (e.g., "[3.0.0,4.0.0)", "[3.2.0,)")
     */
    private String versionRange;

    /**
     * Suggested Spring Boot version if current is incompatible.
     * Only populated when compatible=false.
     */
    private String suggestedBootVersion;

    /**
     * Alternative dependencies that might work instead.
     * Only populated when compatible=false and alternatives exist.
     */
    @Builder.Default
    private List<String> alternatives = new ArrayList<>();

    /**
     * Category of the dependency (e.g., "Web", "Security", "SQL").
     */
    private String category;

    /**
     * Additional notes or recommendations.
     */
    private String notes;

    /**
     * Create a compatible result.
     *
     * @param dependencyId the dependency ID
     * @param dependencyName the dependency display name
     * @param bootVersion the Spring Boot version
     * @param versionRange the version range (or null if unrestricted)
     * @return CompatibilityInfo indicating compatibility
     */
    public static CompatibilityInfo compatible(String dependencyId, String dependencyName,
                                                String bootVersion, String versionRange) {
        String reason;
        if (versionRange == null || versionRange.isBlank()) {
            reason = "Compatible with all Spring Boot versions";
        } else {
            reason = "Compatible - " + VersionRangeUtil.describeRange(versionRange);
        }

        return CompatibilityInfo.builder()
            .compatible(true)
            .dependencyId(dependencyId)
            .dependencyName(dependencyName)
            .bootVersion(bootVersion)
            .versionRange(versionRange)
            .reason(reason)
            .build();
    }

    /**
     * Create an incompatible result.
     *
     * @param dependencyId the dependency ID
     * @param dependencyName the dependency display name
     * @param bootVersion the incompatible Spring Boot version
     * @param versionRange the required version range
     * @param suggestedVersion a compatible version to suggest
     * @return CompatibilityInfo indicating incompatibility
     */
    public static CompatibilityInfo incompatible(String dependencyId, String dependencyName,
                                                  String bootVersion, String versionRange,
                                                  String suggestedVersion) {
        String reason = String.format("Incompatible - requires %s but got %s",
            VersionRangeUtil.describeRange(versionRange), bootVersion);

        return CompatibilityInfo.builder()
            .compatible(false)
            .dependencyId(dependencyId)
            .dependencyName(dependencyName)
            .bootVersion(bootVersion)
            .versionRange(versionRange)
            .reason(reason)
            .suggestedBootVersion(suggestedVersion)
            .build();
    }

    /**
     * Create a not-found result.
     *
     * @param dependencyId the dependency ID that was not found
     * @param bootVersion the Spring Boot version
     * @return CompatibilityInfo indicating dependency not found
     */
    public static CompatibilityInfo notFound(String dependencyId, String bootVersion) {
        return CompatibilityInfo.builder()
            .compatible(false)
            .dependencyId(dependencyId)
            .bootVersion(bootVersion)
            .reason("Dependency not found: " + dependencyId)
            .build();
    }

    /**
     * Add an alternative dependency suggestion.
     *
     * @param alternative the alternative dependency ID
     * @return this object for chaining
     */
    public CompatibilityInfo withAlternative(String alternative) {
        if (this.alternatives == null) {
            this.alternatives = new ArrayList<>();
        }
        this.alternatives.add(alternative);
        return this;
    }

    /**
     * Add a note to the compatibility info.
     *
     * @param note the note to add
     * @return this object for chaining
     */
    public CompatibilityInfo withNote(String note) {
        this.notes = note;
        return this;
    }

    /**
     * Get a summary message suitable for display.
     *
     * @return summary message
     */
    public String getSummary() {
        StringBuilder sb = new StringBuilder();

        if (compatible) {
            sb.append("✓ ").append(dependencyName != null ? dependencyName : dependencyId);
            sb.append(" is compatible with Spring Boot ").append(bootVersion);
        } else {
            sb.append("✗ ").append(dependencyName != null ? dependencyName : dependencyId);
            sb.append(" is NOT compatible with Spring Boot ").append(bootVersion);
            if (reason != null) {
                sb.append("\n  Reason: ").append(reason);
            }
            if (suggestedBootVersion != null) {
                sb.append("\n  Suggested version: ").append(suggestedBootVersion);
            }
            if (alternatives != null && !alternatives.isEmpty()) {
                sb.append("\n  Alternatives: ").append(String.join(", ", alternatives));
            }
        }

        return sb.toString();
    }
}
