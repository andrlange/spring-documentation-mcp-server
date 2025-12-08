package com.spring.mcp.service.javadoc;

import com.spring.mcp.repository.JavadocPackageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service for managing Javadoc version resolution.
 * Handles "latest" version resolution and semantic version comparison.
 */
@Service
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "mcp.features.javadocs", name = "enabled", havingValue = "true", matchIfMissing = true)
public class JavadocVersionService {

    private static final Pattern VERSION_PATTERN = Pattern.compile(
            "^(\\d+)\\.(\\d+)(?:\\.(\\d+))?(?:[.-]?(SNAPSHOT|RC\\d*|M\\d*|RELEASE|GA))?$",
            Pattern.CASE_INSENSITIVE
    );

    private final JavadocPackageRepository packageRepository;

    /**
     * Resolve version - if null or "latest", returns the latest available version.
     *
     * @param libraryName Library name
     * @param version     Requested version (null or "latest" for latest)
     * @return Resolved version or null if no versions available
     */
    public Optional<String> resolveVersion(String libraryName, String version) {
        if (version != null && !version.equalsIgnoreCase("latest") && !version.isBlank()) {
            // Specific version requested, check if it exists
            if (packageRepository.existsByLibraryNameAndVersion(libraryName, version)) {
                return Optional.of(version);
            }
            log.debug("Requested version {} not found for {}", version, libraryName);
            return Optional.empty();
        }

        // Get latest version
        return getLatestVersion(libraryName);
    }

    /**
     * Get the latest version for a library.
     * Prefers stable releases over snapshots/RC.
     *
     * @param libraryName Library name
     * @return Latest version or empty if none available
     */
    public Optional<String> getLatestVersion(String libraryName) {
        List<String> versions = packageRepository.findVersionsByLibraryName(libraryName);
        if (versions.isEmpty()) {
            return Optional.empty();
        }

        // Sort versions semantically (highest first)
        versions.sort(Comparator.comparing(this::parseVersion).reversed());

        // Prefer stable versions
        Optional<String> stable = versions.stream()
                .filter(this::isStableVersion)
                .findFirst();

        if (stable.isPresent()) {
            return stable;
        }

        // Fall back to any version
        return versions.stream().findFirst();
    }

    /**
     * Get all available versions for a library, sorted descending.
     *
     * @param libraryName Library name
     * @return List of versions, sorted newest first
     */
    public List<String> getAvailableVersions(String libraryName) {
        List<String> versions = packageRepository.findVersionsByLibraryName(libraryName);
        versions.sort(Comparator.comparing(this::parseVersion).reversed());
        return versions;
    }

    /**
     * Check if a version is stable (not SNAPSHOT, RC, or milestone).
     */
    public boolean isStableVersion(String version) {
        if (version == null) return false;
        String upper = version.toUpperCase();
        return !upper.contains("SNAPSHOT") &&
               !upper.contains("-RC") &&
               !upper.contains(".RC") &&
               !upper.contains("-M") &&
               !upper.contains(".M") &&
               !upper.contains("ALPHA") &&
               !upper.contains("BETA");
    }

    /**
     * Compare two versions.
     *
     * @return negative if v1 < v2, positive if v1 > v2, 0 if equal
     */
    public int compareVersions(String v1, String v2) {
        return parseVersion(v1).compareTo(parseVersion(v2));
    }

    /**
     * Parse version string into a comparable object.
     */
    private SemanticVersion parseVersion(String version) {
        if (version == null) {
            return new SemanticVersion(0, 0, 0, VersionQualifier.UNKNOWN, 0);
        }

        Matcher matcher = VERSION_PATTERN.matcher(version);
        if (matcher.matches()) {
            int major = Integer.parseInt(matcher.group(1));
            int minor = Integer.parseInt(matcher.group(2));
            int patch = matcher.group(3) != null ? Integer.parseInt(matcher.group(3)) : 0;

            String qualifierStr = matcher.group(4);
            VersionQualifier qualifier = VersionQualifier.RELEASE;
            int qualifierNum = 0;

            if (qualifierStr != null) {
                String upper = qualifierStr.toUpperCase();
                if (upper.equals("SNAPSHOT")) {
                    qualifier = VersionQualifier.SNAPSHOT;
                } else if (upper.startsWith("RC")) {
                    qualifier = VersionQualifier.RC;
                    qualifierNum = parseQualifierNumber(upper.substring(2));
                } else if (upper.startsWith("M")) {
                    qualifier = VersionQualifier.MILESTONE;
                    qualifierNum = parseQualifierNumber(upper.substring(1));
                }
            }

            return new SemanticVersion(major, minor, patch, qualifier, qualifierNum);
        }

        // Can't parse, return low priority version
        return new SemanticVersion(0, 0, 0, VersionQualifier.UNKNOWN, version.hashCode());
    }

    private int parseQualifierNumber(String num) {
        try {
            return num.isEmpty() ? 1 : Integer.parseInt(num);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * Version qualifier types, ordered by stability.
     */
    private enum VersionQualifier {
        SNAPSHOT(0), MILESTONE(1), RC(2), RELEASE(3), UNKNOWN(-1);

        final int priority;
        VersionQualifier(int priority) { this.priority = priority; }
    }

    /**
     * Semantic version for comparison.
     */
    private record SemanticVersion(
            int major,
            int minor,
            int patch,
            VersionQualifier qualifier,
            int qualifierNum
    ) implements Comparable<SemanticVersion> {

        @Override
        public int compareTo(SemanticVersion other) {
            // Compare major.minor.patch
            int result = Integer.compare(this.major, other.major);
            if (result != 0) return result;

            result = Integer.compare(this.minor, other.minor);
            if (result != 0) return result;

            result = Integer.compare(this.patch, other.patch);
            if (result != 0) return result;

            // Compare qualifiers (RELEASE > RC > M > SNAPSHOT)
            result = Integer.compare(this.qualifier.priority, other.qualifier.priority);
            if (result != 0) return result;

            // Compare qualifier numbers (RC2 > RC1)
            return Integer.compare(this.qualifierNum, other.qualifierNum);
        }
    }
}
