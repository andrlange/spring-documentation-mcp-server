package com.spring.mcp.service.initializr.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO representing a Spring Boot version from the Initializr API.
 *
 * <p>Contains information about available Spring Boot versions including
 * whether a version is the default choice for new projects.</p>
 *
 * <p>Version types include:</p>
 * <ul>
 *   <li>GA (General Availability) - Stable releases (e.g., 3.5.9)</li>
 *   <li>RC (Release Candidate) - Pre-release versions (e.g., 4.0.0-RC1)</li>
 *   <li>M (Milestone) - Development snapshots (e.g., 4.0.0-M3)</li>
 *   <li>SNAPSHOT - Nightly builds (e.g., 4.0.0-SNAPSHOT)</li>
 * </ul>
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
public class BootVersion {

    /**
     * Version identifier (e.g., "3.5.9", "4.0.0-RC1", "3.6.0-SNAPSHOT").
     */
    private String id;

    /**
     * Display name for the version (e.g., "3.5.9 (SNAPSHOT)", "4.0.0 (RC1)").
     */
    private String name;

    /**
     * Whether this is the default version for new projects.
     */
    @JsonProperty("default")
    private boolean defaultVersion;

    /**
     * Check if this is a stable (GA) release.
     *
     * @return true if this is a GA release, false for RC/M/SNAPSHOT
     */
    public boolean isStable() {
        return id != null && !id.contains("-");
    }

    /**
     * Check if this is a snapshot version.
     *
     * @return true if this is a SNAPSHOT version
     */
    public boolean isSnapshot() {
        return id != null && id.contains("SNAPSHOT");
    }

    /**
     * Check if this is a release candidate.
     *
     * @return true if this is an RC version
     */
    public boolean isReleaseCandidate() {
        return id != null && id.contains("-RC");
    }

    /**
     * Check if this is a milestone version.
     *
     * @return true if this is a milestone version
     */
    public boolean isMilestone() {
        return id != null && id.contains("-M");
    }

    /**
     * Get the version type as a string.
     *
     * @return version type: "GA", "RC", "Milestone", or "Snapshot"
     */
    public String getVersionType() {
        if (isSnapshot()) return "Snapshot";
        if (isReleaseCandidate()) return "RC";
        if (isMilestone()) return "Milestone";
        return "GA";
    }

    /**
     * Extract the major version number.
     *
     * @return major version (e.g., 3 for "3.5.9")
     */
    public int getMajorVersion() {
        if (id == null) return 0;
        try {
            return Integer.parseInt(id.split("\\.")[0]);
        } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
            return 0;
        }
    }

    /**
     * Extract the minor version number.
     *
     * @return minor version (e.g., 5 for "3.5.9")
     */
    public int getMinorVersion() {
        if (id == null) return 0;
        try {
            return Integer.parseInt(id.split("\\.")[1]);
        } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
            return 0;
        }
    }

    /**
     * Extract the patch version number.
     *
     * @return patch version (e.g., 8 for "3.5.9")
     */
    public int getPatchVersion() {
        if (id == null) return 0;
        try {
            String[] parts = id.split("\\.");
            if (parts.length < 3) return 0;
            // Handle versions like "3.5.8-SNAPSHOT"
            String patchStr = parts[2].split("-")[0];
            return Integer.parseInt(patchStr);
        } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
            return 0;
        }
    }

    /**
     * Get the base version without qualifiers.
     *
     * @return base version (e.g., "3.5.8" for "3.5.8-SNAPSHOT")
     */
    public String getBaseVersion() {
        if (id == null) return "";
        int dashIndex = id.indexOf('-');
        return dashIndex > 0 ? id.substring(0, dashIndex) : id;
    }
}
