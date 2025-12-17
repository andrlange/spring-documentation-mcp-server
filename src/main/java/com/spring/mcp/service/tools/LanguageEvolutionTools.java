package com.spring.mcp.service.tools;

import com.spring.mcp.model.dto.mcp.*;
import com.spring.mcp.model.entity.*;
import com.spring.mcp.model.enums.FeatureStatus;
import com.spring.mcp.model.enums.LanguageType;
import com.spring.mcp.repository.*;
import com.spring.mcp.service.language.LanguageEvolutionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springaicommunity.mcp.annotation.McpTool;
import org.springaicommunity.mcp.annotation.McpToolParam;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * MCP Tools for Language Evolution (Java and Kotlin)
 * Provides tools for LLMs to query language version information,
 * features, deprecations, and modern coding patterns.
 *
 * @author Spring MCP Server
 * @version 1.5.2
 * @since 2025-11-29
 */
@Service
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(name = "mcp.features.language-evolution.enabled", havingValue = "true", matchIfMissing = true)
public class LanguageEvolutionTools {

    private final LanguageEvolutionService languageEvolutionService;
    private final LanguageFeatureRepository featureRepository;
    private final LanguageCodePatternRepository codePatternRepository;

    /**
     * Get available versions for a programming language (Java or Kotlin)
     */
    @McpTool(description = """
        Get all available versions for Java or Kotlin language.
        Returns version information including LTS status, release dates, and support end dates.
        Use this to determine which language versions are currently supported or recommended.
        """)
    public LanguageVersionsResponse getLanguageVersions(
            @McpToolParam(description = "Language type: 'JAVA' or 'KOTLIN' (required)") String language) {

        log.info("Tool: getLanguageVersions - language={}", language);

        LanguageType langType = parseLanguageType(language);
        List<LanguageVersion> versions = languageEvolutionService.getLanguageVersions(langType);
        Optional<LanguageVersion> currentVersion = languageEvolutionService.getCurrentVersion(langType);
        List<LanguageVersion> ltsVersions = languageEvolutionService.getLtsVersions(langType);

        List<LanguageVersionsResponse.VersionInfo> versionInfos = versions.stream()
                .map(this::mapVersionToInfo)
                .collect(Collectors.toList());

        return LanguageVersionsResponse.builder()
                .language(langType.getDisplayName())
                .versions(versionInfos)
                .count(versions.size())
                .currentVersion(currentVersion.map(this::mapVersionToInfo).orElse(null))
                .ltsVersions(ltsVersions.stream().map(this::mapVersionToInfo).collect(Collectors.toList()))
                .build();
    }

    /**
     * Get features for a specific language version
     */
    @McpTool(description = """
        Get language features for a specific Java or Kotlin version.
        Returns new features, deprecations, and removals with detailed descriptions.
        Includes JEP/KEP numbers for Java Enhancement Proposals and Kotlin Enhancement Proposals.
        Use this to understand what's new or changed in a specific language version.
        """)
    public LanguageFeaturesResponse getLanguageFeatures(
            @McpToolParam(description = "Language type: 'JAVA' or 'KOTLIN' (required)") String language,
            @McpToolParam(description = "Version string (optional, e.g., '21' for Java 21, '2.0' for Kotlin 2.0). If not provided, returns all features.") String version,
            @McpToolParam(description = "Filter by status: 'NEW', 'DEPRECATED', 'REMOVED', 'PREVIEW', 'INCUBATING' (optional)") String status,
            @McpToolParam(description = "Filter by category (optional, e.g., 'Pattern Matching', 'Concurrency')") String category) {

        log.info("Tool: getLanguageFeatures - language={}, version={}, status={}, category={}", language, version, status, category);

        LanguageType langType = parseLanguageType(language);

        // Handle "null" string passed by MCP clients
        String effectiveStatus = (status != null && !status.isBlank() && !"null".equalsIgnoreCase(status)) ? status : null;
        String effectiveCategory = (category != null && !category.isBlank() && !"null".equalsIgnoreCase(category)) ? category : null;

        FeatureStatus featureStatus = effectiveStatus != null ? FeatureStatus.fromString(effectiveStatus) : null;

        List<LanguageFeature> features;
        if (version != null && !version.isBlank() && !"null".equalsIgnoreCase(version)) {
            features = languageEvolutionService.getFeatures(langType, version);
        } else {
            features = languageEvolutionService.searchFeatures(langType, null, featureStatus, effectiveCategory, null);
        }

        // Apply additional filters if provided
        if (featureStatus != null) {
            features = features.stream()
                    .filter(f -> f.getStatus() == featureStatus)
                    .collect(Collectors.toList());
        }
        if (effectiveCategory != null) {
            features = features.stream()
                    .filter(f -> effectiveCategory.equalsIgnoreCase(f.getCategory()))
                    .collect(Collectors.toList());
        }

        List<LanguageFeaturesResponse.FeatureInfo> featureInfos = features.stream()
                .map(this::mapFeatureToInfo)
                .collect(Collectors.toList());

        // Calculate stats
        LanguageFeaturesResponse.FeatureStats stats = calculateFeatureStats(features);

        return LanguageFeaturesResponse.builder()
                .language(langType.getDisplayName())
                .version(version)
                .features(featureInfos)
                .totalCount(features.size())
                .stats(stats)
                .categories(languageEvolutionService.getCategories())
                .build();
    }

    /**
     * Get modern code patterns for a language feature
     */
    @McpTool(description = """
        Get modern code pattern examples showing old vs new ways to write code.
        Returns before/after code snippets with explanations for migrating to newer language features.
        Use this to help developers modernize their code when upgrading Java or Kotlin versions.
        """)
    public CodePatternResponse getModernPatterns(
            @McpToolParam(description = "Feature ID to get patterns for (required, use getLanguageFeatures to find feature IDs)") Long featureId) {

        log.info("Tool: getModernPatterns - featureId={}", featureId);

        if (featureId == null) {
            throw new IllegalArgumentException("featureId parameter is required");
        }

        LanguageFeature feature = featureRepository.findById(featureId)
                .orElseThrow(() -> new IllegalArgumentException("Feature not found: " + featureId));

        List<LanguageCodePattern> patterns = languageEvolutionService.getCodePatterns(featureId);

        List<CodePatternResponse.PatternInfo> patternInfos = patterns.stream()
                .map(p -> CodePatternResponse.PatternInfo.builder()
                        .id(p.getId())
                        .patternLanguage(p.getPatternLanguage())
                        .oldPattern(p.getOldPattern())
                        .newPattern(p.getNewPattern())
                        .explanation(p.getExplanation())
                        .minVersion(p.getMinVersion())
                        .build())
                .collect(Collectors.toList());

        return CodePatternResponse.builder()
                .featureName(feature.getFeatureName())
                .featureDescription(feature.getDescription())
                .language(feature.getLanguageVersion().getLanguage().getDisplayName())
                .version(feature.getLanguageVersion().getVersion())
                .patterns(patternInfos)
                .totalPatterns(patterns.size())
                .build();
    }

    /**
     * Check deprecated APIs between language versions
     */
    @McpTool(description = """
        Check for deprecated and removed APIs when upgrading from one language version to another.
        Returns list of deprecations and removals that need attention during migration.
        Use this before upgrading Java or Kotlin version to identify breaking changes.
        """)
    public LanguageVersionDiffResponse getLanguageVersionDiff(
            @McpToolParam(description = "Language type: 'JAVA' or 'KOTLIN' (required)") String language,
            @McpToolParam(description = "Source version to migrate from (required, e.g., '11' for Java 11)") String fromVersion,
            @McpToolParam(description = "Target version to migrate to (required, e.g., '21' for Java 21)") String toVersion) {

        log.info("Tool: getLanguageVersionDiff - language={}, from={}, to={}", language, fromVersion, toVersion);

        LanguageType langType = parseLanguageType(language);

        if (fromVersion == null || fromVersion.isBlank()) {
            throw new IllegalArgumentException("fromVersion parameter is required");
        }
        if (toVersion == null || toVersion.isBlank()) {
            throw new IllegalArgumentException("toVersion parameter is required");
        }

        LanguageEvolutionService.VersionDiff diff = languageEvolutionService.getVersionDiff(langType, fromVersion, toVersion);

        List<LanguageVersionDiffResponse.FeatureSummary> newFeatures = diff.getNewFeatures().stream()
                .map(this::mapFeatureToSummary)
                .collect(Collectors.toList());

        List<LanguageVersionDiffResponse.FeatureSummary> deprecatedFeatures = diff.getDeprecatedFeatures().stream()
                .map(this::mapFeatureToSummary)
                .collect(Collectors.toList());

        List<LanguageVersionDiffResponse.FeatureSummary> removedFeatures = diff.getRemovedFeatures().stream()
                .map(this::mapFeatureToSummary)
                .collect(Collectors.toList());

        // Generate migration recommendations
        LanguageVersionDiffResponse.MigrationRecommendations recommendations = generateRecommendations(
                diff, langType, fromVersion, toVersion);

        return LanguageVersionDiffResponse.builder()
                .language(langType.getDisplayName())
                .fromVersion(fromVersion)
                .toVersion(toVersion)
                .totalChanges(diff.getTotalChanges())
                .newFeatures(newFeatures)
                .deprecatedFeatures(deprecatedFeatures)
                .removedFeatures(removedFeatures)
                .recommendations(recommendations)
                .build();
    }

    /**
     * Get Spring Boot language requirements
     */
    @McpTool(description = """
        Get Java and Kotlin version requirements for a specific Spring Boot version.
        Returns minimum, recommended, and maximum supported language versions.
        Use this to ensure correct language version when starting a new Spring Boot project.
        """)
    public SpringBootLanguageRequirementResponse getSpringBootLanguageRequirements(
            @McpToolParam(description = "Spring Boot version (required, e.g., '3.5.7', '4.0.0')") String springBootVersion) {

        log.info("Tool: getSpringBootLanguageRequirements - springBootVersion={}", springBootVersion);

        if (springBootVersion == null || springBootVersion.isBlank()) {
            throw new IllegalArgumentException("springBootVersion parameter is required");
        }

        List<SpringBootLanguageRequirement> requirements = languageEvolutionService.getSpringBootRequirements(springBootVersion);

        if (requirements.isEmpty()) {
            // Generate default requirements based on Spring Boot version
            return generateDefaultRequirements(springBootVersion);
        }

        List<SpringBootLanguageRequirementResponse.LanguageRequirement> reqInfos = requirements.stream()
                .map(r -> SpringBootLanguageRequirementResponse.LanguageRequirement.builder()
                        .language(r.getLanguage().getDisplayName())
                        .minimumVersion(r.getMinVersion())
                        .recommendedVersion(r.getRecommendedVersion())
                        .maximumVersion(r.getMaxVersion())
                        .versionRange(r.getVersionRange())
                        .notes(r.getNotes())
                        .build())
                .collect(Collectors.toList());

        String recommendation = generateRecommendation(requirements);

        return SpringBootLanguageRequirementResponse.builder()
                .springBootVersion(springBootVersion)
                .requirements(reqInfos)
                .recommendation(recommendation)
                .build();
    }

    /**
     * Get code example for a specific language feature
     */
    @McpTool(description = """
        Get a code example for a specific language feature.
        Search by JEP number (e.g., '444'), KEP number (e.g., 'KT-11550'),
        or feature name (e.g., 'Virtual Threads', 'Records').
        Returns practical code example with feature description and source type.
        """)
    public LanguageFeatureExampleResponse getLanguageFeatureExample(
            @McpToolParam(description = "Language type: 'JAVA', 'KOTLIN', or null for auto-detect (optional)") String language,
            @McpToolParam(description = "JEP/KEP number or feature name (required, e.g., '444', 'KT-11550', 'Virtual Threads')") String featureIdentifier) {

        log.info("Tool: getLanguageFeatureExample - language={}, featureIdentifier={}", language, featureIdentifier);

        if (featureIdentifier == null || featureIdentifier.isBlank()) {
            return LanguageFeatureExampleResponse.error("featureIdentifier parameter is required");
        }

        String identifier = featureIdentifier.trim();
        LanguageFeature feature = null;

        // Try to find by JEP number (just digits or "JEP XXX" format)
        String jepNumber = null;
        if (identifier.matches("\\d+") || identifier.toUpperCase().startsWith("JEP ")) {
            jepNumber = identifier.replaceAll("(?i)^JEP\\s*", "");
            List<LanguageFeature> jepFeatures = featureRepository.findByJepNumber(jepNumber);
            if (!jepFeatures.isEmpty()) {
                feature = jepFeatures.get(0);
            }
        }

        // Try to find by KEP number (KT-XXXXX format or just the name)
        if (feature == null && (identifier.toUpperCase().startsWith("KT-") || identifier.toUpperCase().startsWith("KEP "))) {
            String kepNumber = identifier.replaceAll("(?i)^KEP\\s*", "");
            List<LanguageFeature> kepFeatures = featureRepository.findByKepNumber(kepNumber);
            if (!kepFeatures.isEmpty()) {
                feature = kepFeatures.get(0);
            }
        }

        // Try to find by feature name
        if (feature == null) {
            LanguageType langType = (language != null && !language.isBlank()) ? parseLanguageTypeOrNull(language) : null;
            List<LanguageFeature> matchingFeatures;

            if (langType != null) {
                matchingFeatures = featureRepository.findByLanguage(langType);
            } else {
                matchingFeatures = featureRepository.findAll();
            }

            // Case-insensitive name match
            for (LanguageFeature f : matchingFeatures) {
                if (f.getFeatureName().equalsIgnoreCase(identifier)) {
                    feature = f;
                    break;
                }
            }

            // Partial match if exact match not found
            if (feature == null) {
                String lowerIdentifier = identifier.toLowerCase();
                for (LanguageFeature f : matchingFeatures) {
                    if (f.getFeatureName().toLowerCase().contains(lowerIdentifier)) {
                        feature = f;
                        break;
                    }
                }
            }
        }

        if (feature == null) {
            return LanguageFeatureExampleResponse.error("Feature not found: " + featureIdentifier);
        }

        // Build internal detail page URL
        String detailPageUrl = null;
        if (feature.getJepNumber() != null && !feature.getJepNumber().isBlank()) {
            detailPageUrl = "/languages/jep/" + feature.getJepNumber();
        } else if (feature.getKepNumber() != null && !feature.getKepNumber().isBlank()) {
            detailPageUrl = "/languages/kep/" + feature.getKepNumber();
        }

        long patternCount = codePatternRepository.countByFeatureId(feature.getId());

        return LanguageFeatureExampleResponse.builder()
                .language(feature.getLanguageVersion().getLanguage().getDisplayName())
                .version(feature.getLanguageVersion().getVersion())
                .featureName(feature.getFeatureName())
                .description(feature.getDescription())
                .codeExample(feature.getCodeExample())
                .exampleSourceType(feature.getExampleSourceType())
                .jepNumber(feature.getJepNumber())
                .kepNumber(feature.getKepNumber())
                .specificationUrl(feature.getEnhancementProposalUrl())
                .detailPageUrl(detailPageUrl)
                .category(feature.getCategory())
                .status(feature.getStatus() != null ? feature.getStatus().name() : null)
                .impactLevel(feature.getImpactLevel() != null ? feature.getImpactLevel().name() : null)
                .hasPatterns(patternCount > 0)
                .build();
    }

    /**
     * Search language features by keyword
     */
    @McpTool(description = """
        Search for language features by keyword across all Java and Kotlin versions.
        Returns features matching the search term in name or description.
        Use this to find specific features or capabilities across language versions.
        """)
    public LanguageFeaturesResponse searchLanguageFeatures(
            @McpToolParam(description = "Search term (required, e.g., 'virtual thread', 'record', 'sealed')") String searchTerm,
            @McpToolParam(description = "Language type: 'JAVA', 'KOTLIN', or null for both (optional)") String language) {

        log.info("Tool: searchLanguageFeatures - searchTerm={}, language={}", searchTerm, language);

        if (searchTerm == null || searchTerm.isBlank()) {
            throw new IllegalArgumentException("searchTerm parameter is required");
        }

        LanguageType langType = language != null && !language.isBlank() ? parseLanguageType(language) : null;

        List<LanguageFeature> features = languageEvolutionService.searchFeatures(langType, null, null, null, searchTerm);

        List<LanguageFeaturesResponse.FeatureInfo> featureInfos = features.stream()
                .map(this::mapFeatureToInfo)
                .collect(Collectors.toList());

        LanguageFeaturesResponse.FeatureStats stats = calculateFeatureStats(features);

        return LanguageFeaturesResponse.builder()
                .language(langType != null ? langType.getDisplayName() : "All")
                .features(featureInfos)
                .totalCount(features.size())
                .stats(stats)
                .categories(languageEvolutionService.getCategories())
                .build();
    }

    // ==================== Helper Methods ====================

    private LanguageType parseLanguageType(String language) {
        if (language == null || language.isBlank()) {
            throw new IllegalArgumentException("language parameter is required");
        }
        LanguageType langType = LanguageType.fromString(language);
        if (langType == null) {
            throw new IllegalArgumentException("Invalid language: " + language + ". Use 'JAVA' or 'KOTLIN'.");
        }
        return langType;
    }

    private LanguageType parseLanguageTypeOrNull(String language) {
        if (language == null || language.isBlank()) {
            return null;
        }
        return LanguageType.fromString(language);
    }

    private LanguageVersionsResponse.VersionInfo mapVersionToInfo(LanguageVersion v) {
        return LanguageVersionsResponse.VersionInfo.builder()
                .version(v.getVersion())
                .displayName(v.getDisplayName())
                .majorVersion(v.getMajorVersion())
                .minorVersion(v.getMinorVersion())
                .isLts(v.getIsLts())
                .isCurrent(v.getIsCurrent())
                .releaseDate(v.getReleaseDate() != null ? v.getReleaseDate().toString() : null)
                .endOfLife(v.getOssSupportEnd() != null ? v.getOssSupportEnd().toString() : null)
                .extendedSupportEnd(v.getExtendedSupportEnd() != null ? v.getExtendedSupportEnd().toString() : null)
                .featureCount(v.getFeatures() != null ? v.getFeatures().size() : 0)
                .build();
    }

    private LanguageFeaturesResponse.FeatureInfo mapFeatureToInfo(LanguageFeature f) {
        long patternCount = codePatternRepository.countByFeatureId(f.getId());
        return LanguageFeaturesResponse.FeatureInfo.builder()
                .id(f.getId())
                .featureName(f.getFeatureName())
                .description(f.getDescription())
                .status(f.getStatus().name())
                .statusBadgeClass(f.getStatus().getBadgeClass())
                .statusIconClass(f.getStatus().getIconClass())
                .category(f.getCategory())
                .jepNumber(f.getJepNumber())
                .kepNumber(f.getKepNumber())
                .proposalUrl(f.getEnhancementProposalUrl())
                .impactLevel(f.getImpactLevel() != null ? f.getImpactLevel().name() : null)
                .introducedVersion(f.getLanguageVersion() != null ? f.getLanguageVersion().getVersion() : null)
                .hasCodePatterns(patternCount > 0)
                .codePatternCount((int) patternCount)
                .codeExample(f.getCodeExample())
                .build();
    }

    private LanguageVersionDiffResponse.FeatureSummary mapFeatureToSummary(LanguageFeature f) {
        return LanguageVersionDiffResponse.FeatureSummary.builder()
                .featureName(f.getFeatureName())
                .description(f.getDescription())
                .category(f.getCategory())
                .jepNumber(f.getJepNumber())
                .kepNumber(f.getKepNumber())
                .impactLevel(f.getImpactLevel() != null ? f.getImpactLevel().name() : null)
                .introducedIn(f.getLanguageVersion() != null ? f.getLanguageVersion().getVersion() : null)
                .build();
    }

    private LanguageFeaturesResponse.FeatureStats calculateFeatureStats(List<LanguageFeature> features) {
        return LanguageFeaturesResponse.FeatureStats.builder()
                .newCount((int) features.stream().filter(f -> f.getStatus() == FeatureStatus.NEW).count())
                .deprecatedCount((int) features.stream().filter(f -> f.getStatus() == FeatureStatus.DEPRECATED).count())
                .removedCount((int) features.stream().filter(f -> f.getStatus() == FeatureStatus.REMOVED).count())
                .previewCount((int) features.stream().filter(f -> f.getStatus() == FeatureStatus.PREVIEW).count())
                .incubatingCount((int) features.stream().filter(f -> f.getStatus() == FeatureStatus.INCUBATING).count())
                .build();
    }

    private LanguageVersionDiffResponse.MigrationRecommendations generateRecommendations(
            LanguageEvolutionService.VersionDiff diff,
            LanguageType language,
            String fromVersion,
            String toVersion) {

        List<String> mustDo = new ArrayList<>();
        List<String> shouldDo = new ArrayList<>();
        List<String> niceToHave = new ArrayList<>();

        // Add must-do items for removed features
        for (LanguageFeature f : diff.getRemovedFeatures()) {
            mustDo.add("Update code using removed feature: " + f.getFeatureName());
        }

        // Add should-do items for deprecated features
        for (LanguageFeature f : diff.getDeprecatedFeatures()) {
            shouldDo.add("Consider migrating away from deprecated: " + f.getFeatureName());
        }

        // Add nice-to-have items for new features
        for (LanguageFeature f : diff.getNewFeatures()) {
            if (f.getImpactLevel() != null && f.getImpactLevel().getPriority() >= 2) {
                niceToHave.add("Consider adopting: " + f.getFeatureName());
            }
        }

        // Determine overall complexity
        String complexity;
        if (!diff.getRemovedFeatures().isEmpty()) {
            complexity = "HIGH - Breaking changes require code modifications";
        } else if (!diff.getDeprecatedFeatures().isEmpty()) {
            complexity = "MEDIUM - Deprecated APIs should be updated";
        } else {
            complexity = "LOW - Mostly new features, minimal migration needed";
        }

        return LanguageVersionDiffResponse.MigrationRecommendations.builder()
                .mustDoItems(mustDo)
                .shouldDoItems(shouldDo)
                .niceToHaveItems(niceToHave)
                .overallComplexity(complexity)
                .build();
    }

    private SpringBootLanguageRequirementResponse generateDefaultRequirements(String springBootVersion) {
        List<SpringBootLanguageRequirementResponse.LanguageRequirement> requirements = new ArrayList<>();

        // Parse Spring Boot version
        String[] parts = springBootVersion.split("\\.");
        if (parts.length >= 2) {
            int major = Integer.parseInt(parts[0]);
            int minor = Integer.parseInt(parts[1]);

            String javaMin, javaRec;
            String kotlinMin, kotlinRec;

            if (major >= 4) {
                javaMin = "25";
                javaRec = "25";
                kotlinMin = "2.1";
                kotlinRec = "2.1";
            } else if (major >= 3 && minor >= 4) {
                javaMin = "17";
                javaRec = "21";
                kotlinMin = "1.9";
                kotlinRec = "2.0";
            } else if (major >= 3) {
                javaMin = "17";
                javaRec = "17";
                kotlinMin = "1.8";
                kotlinRec = "1.9";
            } else {
                javaMin = "8";
                javaRec = "11";
                kotlinMin = "1.6";
                kotlinRec = "1.8";
            }

            requirements.add(SpringBootLanguageRequirementResponse.LanguageRequirement.builder()
                    .language("Java")
                    .minimumVersion(javaMin)
                    .recommendedVersion(javaRec)
                    .versionRange(javaMin + "+")
                    .build());

            requirements.add(SpringBootLanguageRequirementResponse.LanguageRequirement.builder()
                    .language("Kotlin")
                    .minimumVersion(kotlinMin)
                    .recommendedVersion(kotlinRec)
                    .versionRange(kotlinMin + "+")
                    .build());
        }

        return SpringBootLanguageRequirementResponse.builder()
                .springBootVersion(springBootVersion)
                .requirements(requirements)
                .recommendation("Use Java " + requirements.get(0).getRecommendedVersion() +
                        " for best compatibility with Spring Boot " + springBootVersion)
                .build();
    }

    private String generateRecommendation(List<SpringBootLanguageRequirement> requirements) {
        Optional<SpringBootLanguageRequirement> javaReq = requirements.stream()
                .filter(r -> r.getLanguage() == LanguageType.JAVA)
                .findFirst();

        if (javaReq.isPresent()) {
            String rec = javaReq.get().getRecommendedVersion();
            return "Java " + rec + " is recommended for optimal performance and compatibility.";
        }
        return "Use the minimum required version for best compatibility.";
    }
}
