package com.spring.mcp.service.language;

import com.spring.mcp.model.entity.*;
import com.spring.mcp.model.enums.FeatureStatus;
import com.spring.mcp.model.enums.LanguageType;
import com.spring.mcp.repository.*;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Core service for language evolution functionality.
 * Provides business logic for querying Java and Kotlin language features,
 * version information, and Spring Boot compatibility.
 *
 * @author Spring MCP Server
 * @version 1.2.0
 * @since 2025-11-29
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LanguageEvolutionService {

    private final LanguageVersionRepository versionRepository;
    private final LanguageFeatureRepository featureRepository;
    private final LanguageCodePatternRepository codePatternRepository;
    private final SpringBootLanguageRequirementRepository requirementRepository;

    // ==================== Version Methods ====================

    /**
     * Get all versions for a language
     *
     * @param language the language type (JAVA or KOTLIN)
     * @return list of versions ordered by version number descending
     */
    @Transactional(readOnly = true)
    public List<LanguageVersion> getLanguageVersions(LanguageType language) {
        log.debug("Getting versions for language: {}", language);
        return versionRepository.findByLanguageOrderByMajorVersionDescMinorVersionDesc(language);
    }

    /**
     * Get a specific version by language and version string
     *
     * @param language the language type
     * @param version the version string (e.g., "21" for Java 21)
     * @return Optional containing the version if found
     */
    @Transactional(readOnly = true)
    public Optional<LanguageVersion> getVersion(LanguageType language, String version) {
        log.debug("Getting version {} for language: {}", version, language);
        return versionRepository.findByLanguageAndVersion(language, version);
    }

    /**
     * Get the current (latest) version for a language
     *
     * @param language the language type
     * @return Optional containing the current version
     */
    @Transactional(readOnly = true)
    public Optional<LanguageVersion> getCurrentVersion(LanguageType language) {
        return versionRepository.findByLanguageAndIsCurrentTrue(language);
    }

    /**
     * Get all LTS versions for a language (Java only)
     *
     * @param language the language type
     * @return list of LTS versions
     */
    @Transactional(readOnly = true)
    public List<LanguageVersion> getLtsVersions(LanguageType language) {
        return versionRepository.findByLanguageAndIsLtsTrueOrderByMajorVersionDesc(language);
    }

    /**
     * Get versions with their features eagerly loaded
     *
     * @param language the language type
     * @return list of versions with features
     */
    @Transactional(readOnly = true)
    public List<LanguageVersion> getVersionsWithFeatures(LanguageType language) {
        return versionRepository.findByLanguageWithFeatures(language);
    }

    // ==================== Feature Methods ====================

    /**
     * Get all features for a specific language version
     *
     * @param languageVersionId the language version ID
     * @return list of features
     */
    @Transactional(readOnly = true)
    public List<LanguageFeature> getFeaturesForVersion(Long languageVersionId) {
        return featureRepository.findByLanguageVersionIdOrderByStatusAscFeatureNameAsc(languageVersionId);
    }

    /**
     * Get features for a language and version string
     *
     * @param language the language type
     * @param version the version string
     * @return list of features
     */
    @Transactional(readOnly = true)
    public List<LanguageFeature> getFeatures(LanguageType language, String version) {
        return featureRepository.findByLanguageAndVersion(language, version);
    }

    /**
     * Get all features for a language across all versions
     *
     * @param language the language type
     * @return list of all features
     */
    @Transactional(readOnly = true)
    public List<LanguageFeature> getAllFeatures(LanguageType language) {
        return featureRepository.findByLanguage(language);
    }

    /**
     * Get features by status
     *
     * @param status the feature status
     * @return list of features with that status
     */
    @Transactional(readOnly = true)
    public List<LanguageFeature> getFeaturesByStatus(FeatureStatus status) {
        return featureRepository.findByStatus(status);
    }

    /**
     * Search features by name or description
     *
     * @param searchTerm the search term
     * @return list of matching features
     */
    @Transactional(readOnly = true)
    public List<LanguageFeature> searchFeatures(String searchTerm) {
        return featureRepository.searchByFeatureNameOrDescription(searchTerm);
    }

    /**
     * Advanced feature search with multiple filters
     *
     * @param language optional language filter
     * @param version optional version filter
     * @param status optional status filter
     * @param category optional category filter
     * @param searchTerm optional search term
     * @return filtered list of features
     */
    @Transactional(readOnly = true)
    public List<LanguageFeature> searchFeatures(
            LanguageType language,
            String version,
            FeatureStatus status,
            String category,
            String searchTerm) {
        // Convert enums to strings for native query
        String languageStr = language != null ? language.name() : null;
        String statusStr = status != null ? status.name() : null;
        return featureRepository.searchFeatures(languageStr, version, statusStr, category, searchTerm);
    }

    /**
     * Get all distinct feature categories
     *
     * @return list of category names
     */
    @Transactional(readOnly = true)
    public List<String> getCategories() {
        return featureRepository.findDistinctCategories();
    }

    /**
     * Get deprecated or removed features between two versions
     *
     * @param language the language type
     * @param fromMajor from major version
     * @param fromMinor from minor version
     * @param toMajor to major version
     * @param toMinor to minor version
     * @return list of deprecated/removed features in that range
     */
    @Transactional(readOnly = true)
    public List<LanguageFeature> getDeprecationsAndRemovals(
            LanguageType language,
            int fromMajor, int fromMinor,
            int toMajor, int toMinor) {
        return featureRepository.findDeprecationsAndRemovalsBetweenVersions(
                language, fromMajor, fromMinor, toMajor, toMinor);
    }

    // ==================== Code Pattern Methods ====================

    /**
     * Get code patterns for a feature
     *
     * @param featureId the feature ID
     * @return list of code patterns
     */
    @Transactional(readOnly = true)
    public List<LanguageCodePattern> getCodePatterns(Long featureId) {
        return codePatternRepository.findByFeatureId(featureId);
    }

    /**
     * Get code patterns by language
     *
     * @param language the code language (java, kotlin)
     * @return list of code patterns
     */
    @Transactional(readOnly = true)
    public List<LanguageCodePattern> getCodePatternsByLanguage(String language) {
        return codePatternRepository.findByLanguage(language);
    }

    /**
     * Search code patterns by explanation
     *
     * @param searchTerm the search term
     * @return list of matching patterns
     */
    @Transactional(readOnly = true)
    public List<LanguageCodePattern> searchCodePatterns(String searchTerm) {
        return codePatternRepository.searchByExplanation(searchTerm);
    }

    // ==================== Spring Boot Compatibility Methods ====================

    /**
     * Get Spring Boot language requirements for a version
     *
     * @param springBootVersion the Spring Boot version string
     * @return list of requirements for that version
     */
    @Transactional(readOnly = true)
    public List<SpringBootLanguageRequirement> getSpringBootRequirements(String springBootVersion) {
        return requirementRepository.findBySpringBootVersion(springBootVersion);
    }

    /**
     * Get Java requirement for a Spring Boot version
     *
     * @param springBootVersion the Spring Boot version string
     * @return Optional containing the Java requirement
     */
    @Transactional(readOnly = true)
    public Optional<SpringBootLanguageRequirement> getJavaRequirement(String springBootVersion) {
        return requirementRepository.findJavaRequirementBySpringBootVersion(springBootVersion);
    }

    /**
     * Get Kotlin requirement for a Spring Boot version
     *
     * @param springBootVersion the Spring Boot version string
     * @return Optional containing the Kotlin requirement
     */
    @Transactional(readOnly = true)
    public Optional<SpringBootLanguageRequirement> getKotlinRequirement(String springBootVersion) {
        return requirementRepository.findKotlinRequirementBySpringBootVersion(springBootVersion);
    }

    // ==================== Migration Helper Methods ====================

    /**
     * Get features introduced between two versions (for migration guidance)
     *
     * @param language the language type
     * @param fromMajor from major version (exclusive)
     * @param fromMinor from minor version (exclusive)
     * @param toMajor to major version (inclusive)
     * @param toMinor to minor version (inclusive)
     * @return list of new features in that range
     */
    @Transactional(readOnly = true)
    public List<LanguageFeature> getNewFeaturesBetweenVersions(
            LanguageType language,
            int fromMajor, int fromMinor,
            int toMajor, int toMinor) {

        // Get all versions in the range
        List<LanguageVersion> versionsInRange = versionRepository.findByLanguageAndVersionGreaterThanEqual(
                language, fromMajor, fromMinor);

        // Filter to versions within the range and get their features
        return versionsInRange.stream()
                .filter(v -> {
                    // After from version
                    if (v.getMajorVersion() < fromMajor ||
                        (v.getMajorVersion() == fromMajor && v.getMinorVersion() <= fromMinor)) {
                        return false;
                    }
                    // Before or equal to to version
                    return v.getMajorVersion() < toMajor ||
                           (v.getMajorVersion() == toMajor && v.getMinorVersion() <= toMinor);
                })
                .flatMap(v -> featureRepository.findByLanguageVersionIdAndStatus(v.getId(), FeatureStatus.NEW).stream())
                .collect(Collectors.toList());
    }

    /**
     * Generate a version diff summary
     *
     * @param language the language type
     * @param fromVersion from version string
     * @param toVersion to version string
     * @return VersionDiff containing changes between versions
     */
    @Transactional(readOnly = true)
    public VersionDiff getVersionDiff(LanguageType language, String fromVersion, String toVersion) {
        log.debug("Generating version diff for {} from {} to {}", language, fromVersion, toVersion);

        Optional<LanguageVersion> from = versionRepository.findByLanguageAndVersion(language, fromVersion);
        Optional<LanguageVersion> to = versionRepository.findByLanguageAndVersion(language, toVersion);

        if (from.isEmpty() || to.isEmpty()) {
            log.warn("Version not found: from={} (found={}), to={} (found={})",
                    fromVersion, from.isPresent(), toVersion, to.isPresent());
            return VersionDiff.empty(language, fromVersion, toVersion);
        }

        LanguageVersion fromVer = from.get();
        LanguageVersion toVer = to.get();

        List<LanguageFeature> newFeatures = getNewFeaturesBetweenVersions(
                language,
                fromVer.getMajorVersion(), fromVer.getMinorVersion(),
                toVer.getMajorVersion(), toVer.getMinorVersion());

        List<LanguageFeature> deprecations = getDeprecationsAndRemovals(
                language,
                fromVer.getMajorVersion(), fromVer.getMinorVersion(),
                toVer.getMajorVersion(), toVer.getMinorVersion());

        return VersionDiff.builder()
                .language(language)
                .fromVersion(fromVersion)
                .toVersion(toVersion)
                .newFeatures(newFeatures.stream()
                        .filter(f -> f.getStatus() == FeatureStatus.NEW)
                        .collect(Collectors.toList()))
                .deprecatedFeatures(deprecations.stream()
                        .filter(f -> f.getStatus() == FeatureStatus.DEPRECATED)
                        .collect(Collectors.toList()))
                .removedFeatures(deprecations.stream()
                        .filter(f -> f.getStatus() == FeatureStatus.REMOVED)
                        .collect(Collectors.toList()))
                .build();
    }

    // ==================== Statistics Methods ====================

    /**
     * Get feature counts by status
     *
     * @return map of status to count
     */
    @Transactional(readOnly = true)
    public Map<FeatureStatus, Long> getFeatureCountsByStatus() {
        Map<FeatureStatus, Long> counts = new HashMap<>();
        for (FeatureStatus status : FeatureStatus.values()) {
            counts.put(status, featureRepository.countByStatus(status));
        }
        return counts;
    }

    /**
     * Get version counts by language
     *
     * @return map of language to count
     */
    @Transactional(readOnly = true)
    public Map<LanguageType, Long> getVersionCountsByLanguage() {
        Map<LanguageType, Long> counts = new HashMap<>();
        for (LanguageType language : LanguageType.values()) {
            counts.put(language, versionRepository.countByLanguage(language));
        }
        return counts;
    }

    // ==================== DTOs ====================

    /**
     * DTO representing differences between two language versions
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VersionDiff {
        private LanguageType language;
        private String fromVersion;
        private String toVersion;
        private List<LanguageFeature> newFeatures;
        private List<LanguageFeature> deprecatedFeatures;
        private List<LanguageFeature> removedFeatures;

        public static VersionDiff empty(LanguageType language, String from, String to) {
            return VersionDiff.builder()
                    .language(language)
                    .fromVersion(from)
                    .toVersion(to)
                    .newFeatures(List.of())
                    .deprecatedFeatures(List.of())
                    .removedFeatures(List.of())
                    .build();
        }

        public int getTotalChanges() {
            return (newFeatures != null ? newFeatures.size() : 0) +
                   (deprecatedFeatures != null ? deprecatedFeatures.size() : 0) +
                   (removedFeatures != null ? removedFeatures.size() : 0);
        }
    }
}
