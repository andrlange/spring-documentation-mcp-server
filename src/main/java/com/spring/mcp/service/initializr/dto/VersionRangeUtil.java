package com.spring.mcp.service.initializr.dto;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class for parsing and evaluating Maven version range expressions.
 *
 * <p>Maven version range syntax:</p>
 * <ul>
 *   <li>{@code [3.0.0,4.0.0)} - 3.0.0 <= version < 4.0.0 (inclusive start, exclusive end)</li>
 *   <li>{@code [3.0.0,4.0.0]} - 3.0.0 <= version <= 4.0.0 (both inclusive)</li>
 *   <li>{@code (3.0.0,4.0.0)} - 3.0.0 < version < 4.0.0 (both exclusive)</li>
 *   <li>{@code [3.2.0,)} - version >= 3.2.0 (no upper bound)</li>
 *   <li>{@code (,3.5.0]} - version <= 3.5.0 (no lower bound)</li>
 *   <li>{@code 3.5.0} - exact version match</li>
 * </ul>
 *
 * @author Spring MCP Server
 * @version 1.4.0
 * @since 2025-12-06
 */
public final class VersionRangeUtil {

    private static final Pattern RANGE_PATTERN = Pattern.compile(
        "^([\\[\\(])([^,]*),([^\\]\\)]*)([\\]\\)])$"
    );

    private static final Pattern VERSION_PATTERN = Pattern.compile(
        "^(\\d+)(?:\\.(\\d+))?(?:\\.(\\d+))?(?:[.-](.+))?$"
    );

    private VersionRangeUtil() {
        // Utility class, no instantiation
    }

    /**
     * Check if a version is within a Maven version range.
     *
     * @param version the version to check (e.g., "3.5.8")
     * @param range the Maven version range (e.g., "[3.0.0,4.0.0)")
     * @return true if version is within range, false otherwise
     */
    public static boolean isInRange(String version, String range) {
        if (version == null || version.isBlank() || range == null || range.isBlank()) {
            return true; // No constraint means all versions are valid
        }

        // Normalize version (remove qualifiers like -SNAPSHOT for comparison)
        String normalizedVersion = normalizeVersion(version);

        // Check if it's a simple version (not a range)
        if (!range.startsWith("[") && !range.startsWith("(")) {
            return compareVersions(normalizedVersion, normalizeVersion(range)) == 0;
        }

        // Parse the range
        Matcher matcher = RANGE_PATTERN.matcher(range);
        if (!matcher.matches()) {
            // Invalid range format, assume compatible
            return true;
        }

        boolean lowerInclusive = "[".equals(matcher.group(1));
        String lowerBound = matcher.group(2).trim();
        String upperBound = matcher.group(3).trim();
        boolean upperInclusive = "]".equals(matcher.group(4));

        // Check lower bound
        if (!lowerBound.isEmpty()) {
            int cmp = compareVersions(normalizedVersion, normalizeVersion(lowerBound));
            if (lowerInclusive && cmp < 0) {
                return false;
            }
            if (!lowerInclusive && cmp <= 0) {
                return false;
            }
        }

        // Check upper bound
        if (!upperBound.isEmpty()) {
            int cmp = compareVersions(normalizedVersion, normalizeVersion(upperBound));
            if (upperInclusive && cmp > 0) {
                return false;
            }
            if (!upperInclusive && cmp >= 0) {
                return false;
            }
        }

        return true;
    }

    /**
     * Compare two version strings.
     *
     * @param v1 first version
     * @param v2 second version
     * @return negative if v1 < v2, zero if equal, positive if v1 > v2
     */
    public static int compareVersions(String v1, String v2) {
        int[] parts1 = parseVersionParts(v1);
        int[] parts2 = parseVersionParts(v2);

        for (int i = 0; i < Math.max(parts1.length, parts2.length); i++) {
            int p1 = i < parts1.length ? parts1[i] : 0;
            int p2 = i < parts2.length ? parts2[i] : 0;

            if (p1 != p2) {
                return Integer.compare(p1, p2);
            }
        }

        return 0;
    }

    /**
     * Normalize a version string by removing qualifiers.
     *
     * @param version the version string (e.g., "3.5.8-SNAPSHOT")
     * @return normalized version (e.g., "3.5.8")
     */
    public static String normalizeVersion(String version) {
        if (version == null) {
            return "";
        }
        // Remove common suffixes
        return version
            .replace("-SNAPSHOT", "")
            .replace(".RELEASE", "")
            .replace("-RELEASE", "")
            .replace(".GA", "")
            .replace("-GA", "")
            .replace("-M", ".")
            .replace("-RC", ".")
            .replaceAll("-[A-Za-z]+\\d*$", "")
            .trim();
    }

    /**
     * Parse version string into numeric parts.
     *
     * @param version the version string
     * @return array of version parts [major, minor, patch, ...]
     */
    private static int[] parseVersionParts(String version) {
        if (version == null || version.isBlank()) {
            return new int[]{0};
        }

        String[] parts = version.split("[.-]");
        int[] result = new int[parts.length];

        for (int i = 0; i < parts.length; i++) {
            try {
                result[i] = Integer.parseInt(parts[i]);
            } catch (NumberFormatException e) {
                // Non-numeric part (like "SNAPSHOT"), stop parsing
                int[] truncated = new int[i];
                System.arraycopy(result, 0, truncated, 0, i);
                return truncated;
            }
        }

        return result;
    }

    /**
     * Parse a version range and extract the lower bound.
     *
     * @param range the version range
     * @return the lower bound version, or null if no lower bound
     */
    public static String getLowerBound(String range) {
        if (range == null || range.isBlank()) {
            return null;
        }

        Matcher matcher = RANGE_PATTERN.matcher(range);
        if (matcher.matches()) {
            String lower = matcher.group(2).trim();
            return lower.isEmpty() ? null : lower;
        }

        return range; // Simple version is both lower and upper bound
    }

    /**
     * Parse a version range and extract the upper bound.
     *
     * @param range the version range
     * @return the upper bound version, or null if no upper bound
     */
    public static String getUpperBound(String range) {
        if (range == null || range.isBlank()) {
            return null;
        }

        Matcher matcher = RANGE_PATTERN.matcher(range);
        if (matcher.matches()) {
            String upper = matcher.group(3).trim();
            return upper.isEmpty() ? null : upper;
        }

        return range; // Simple version is both lower and upper bound
    }

    /**
     * Format a human-readable description of the version range.
     *
     * @param range the version range
     * @return human-readable description
     */
    public static String describeRange(String range) {
        if (range == null || range.isBlank()) {
            return "all versions";
        }

        Matcher matcher = RANGE_PATTERN.matcher(range);
        if (!matcher.matches()) {
            return "version " + range;
        }

        boolean lowerInclusive = "[".equals(matcher.group(1));
        String lowerBound = matcher.group(2).trim();
        String upperBound = matcher.group(3).trim();
        boolean upperInclusive = "]".equals(matcher.group(4));

        StringBuilder sb = new StringBuilder();

        if (!lowerBound.isEmpty()) {
            sb.append("version ");
            sb.append(lowerInclusive ? ">= " : "> ");
            sb.append(lowerBound);
        }

        if (!upperBound.isEmpty()) {
            if (!lowerBound.isEmpty()) {
                sb.append(" and ");
            } else {
                sb.append("version ");
            }
            sb.append(upperInclusive ? "<= " : "< ");
            sb.append(upperBound);
        }

        return sb.toString();
    }
}
