package com.spring.mcp.service.language;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.spring.mcp.model.entity.*;
import com.spring.mcp.model.enums.FeatureStatus;
import com.spring.mcp.model.enums.ImpactLevel;
import com.spring.mcp.model.enums.LanguageType;
import com.spring.mcp.repository.*;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service for synchronizing language evolution data from external sources.
 * Fetches Java and Kotlin version information, features, and compatibility data.
 *
 * Sources:
 * - Oracle JDK Release Notes: https://www.oracle.com/java/technologies/javase/jdk-relnotes-index.html
 * - OpenJDK JEPs: https://openjdk.org/jeps/
 * - Kotlin Releases: https://kotlinlang.org/docs/releases.html
 * - Kotlin KEPs: https://github.com/Kotlin/KEEP
 *
 * @author Spring MCP Server
 * @version 1.2.0
 * @since 2025-11-29
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LanguageSyncService {

    private final LanguageVersionRepository versionRepository;
    private final LanguageFeatureRepository featureRepository;
    private final LanguageCodePatternRepository codePatternRepository;
    private final SpringBootLanguageRequirementRepository requirementRepository;
    private final SpringBootVersionRepository springBootVersionRepository;

    @Value("${mcp.features.language-evolution.sync.timeout:30000}")
    private int fetchTimeout;

    @Value("${mcp.features.language-evolution.sync.enabled:true}")
    private boolean syncEnabled;

    // URLs for fetching data
    private static final String OPENJDK_JEPS_URL = "https://openjdk.org/jeps/";
    private static final String KOTLIN_RELEASES_URL = "https://kotlinlang.org/docs/releases.html";
    private static final String ORACLE_JDK_URL = "https://www.oracle.com/java/technologies/javase/jdk-relnotes-index.html";

    // Code examples data file
    private static final String CODE_EXAMPLES_FILE = "data/language-examples.json";

    private final ObjectMapper objectMapper = new ObjectMapper();

    // Pattern to extract JEP numbers
    private static final Pattern JEP_PATTERN = Pattern.compile("JEP\\s*(\\d+)");
    private static final Pattern KOTLIN_VERSION_PATTERN = Pattern.compile("(\\d+\\.\\d+(?:\\.\\d+)?)");

    /**
     * Synchronize all language evolution data
     *
     * @return SyncResult with statistics
     */
    @Transactional
    public SyncResult syncAll() {
        if (!syncEnabled) {
            log.info("Language sync is disabled, skipping");
            return SyncResult.builder()
                    .success(true)
                    .message("Sync disabled")
                    .build();
        }

        log.info("=".repeat(60));
        log.info("Starting language evolution sync...");
        log.info("=".repeat(60));

        SyncResult result = new SyncResult();
        result.setStartTime(LocalDateTime.now());

        try {
            // Phase 1: Sync Java versions and features
            log.info("Phase 1/4: Syncing Java data...");
            JavaSyncResult javaResult = syncJavaData();
            result.addVersionsUpdated(javaResult.getVersionsUpdated());
            result.addFeaturesUpdated(javaResult.getFeaturesUpdated());
            result.addErrorsEncountered(javaResult.getErrors());

            // Phase 2: Sync Kotlin versions and features
            log.info("Phase 2/4: Syncing Kotlin data...");
            KotlinSyncResult kotlinResult = syncKotlinData();
            result.addVersionsUpdated(kotlinResult.getVersionsUpdated());
            result.addFeaturesUpdated(kotlinResult.getFeaturesUpdated());
            result.addErrorsEncountered(kotlinResult.getErrors());

            // Phase 3: Update Spring Boot compatibility mappings
            log.info("Phase 3/4: Updating Spring Boot compatibility...");
            int compatUpdated = updateSpringBootCompatibility();
            result.addCompatibilityUpdated(compatUpdated);

            // Phase 4: Load code examples from curated data file
            log.info("Phase 4/4: Loading code examples...");
            int examplesUpdated = loadCodeExamples();
            result.addCodeExamplesUpdated(examplesUpdated);
            log.info("Loaded {} code examples", examplesUpdated);

            result.setSuccess(true);
            result.setMessage(String.format(
                    "Sync completed: %d versions, %d features, %d compatibility mappings, %d code examples updated",
                    result.getVersionsUpdated(), result.getFeaturesUpdated(), result.getCompatibilityUpdated(), result.getCodeExamplesUpdated()));

        } catch (Exception e) {
            log.error("Error during language sync", e);
            result.setSuccess(false);
            result.setErrorMessage(e.getMessage());
            result.addErrorsEncountered(1);
        } finally {
            result.setEndTime(LocalDateTime.now());
            log.info("=".repeat(60));
            log.info("Language sync completed: {}", result.getMessage());
            log.info("=".repeat(60));
        }

        return result;
    }

    /**
     * Sync Java-specific data from OpenJDK and Oracle
     */
    private JavaSyncResult syncJavaData() {
        JavaSyncResult result = new JavaSyncResult();

        try {
            // Fetch JEP information
            log.debug("Fetching JEP data from OpenJDK...");
            Map<String, JepInfo> jeps = fetchJepData();
            log.info("Fetched {} JEP entries", jeps.size());

            // Update existing features with JEP info
            List<LanguageFeature> javaFeatures = featureRepository.findByLanguage(LanguageType.JAVA);
            for (LanguageFeature feature : javaFeatures) {
                if (feature.getJepNumber() != null) {
                    JepInfo jep = jeps.get(feature.getJepNumber());
                    if (jep != null && updateFeatureFromJep(feature, jep)) {
                        featureRepository.save(feature);
                        result.incrementFeaturesUpdated();
                    }
                }
            }

            // Check for new Java versions (by checking highest version in DB vs what we can detect)
            result.setVersionsUpdated(checkForNewJavaVersions());

        } catch (IOException e) {
            log.warn("Failed to fetch Java data: {}", e.getMessage());
            result.incrementErrors();
        }

        return result;
    }

    /**
     * Sync Kotlin-specific data
     */
    private KotlinSyncResult syncKotlinData() {
        KotlinSyncResult result = new KotlinSyncResult();

        try {
            log.debug("Fetching Kotlin release data...");
            Map<String, KotlinReleaseInfo> releases = fetchKotlinReleases();
            log.info("Fetched {} Kotlin release entries", releases.size());

            // Update existing versions with release info
            List<LanguageVersion> kotlinVersions = versionRepository.findByLanguageOrderByMajorVersionDescMinorVersionDesc(LanguageType.KOTLIN);
            for (LanguageVersion version : kotlinVersions) {
                KotlinReleaseInfo release = releases.get(version.getVersion());
                if (release != null && updateVersionFromKotlinRelease(version, release)) {
                    versionRepository.save(version);
                    result.incrementVersionsUpdated();
                }
            }

        } catch (IOException e) {
            log.warn("Failed to fetch Kotlin data: {}", e.getMessage());
            result.incrementErrors();
        }

        return result;
    }

    /**
     * Update Spring Boot compatibility mappings
     */
    private int updateSpringBootCompatibility() {
        int updated = 0;

        // Get all Spring Boot versions
        List<SpringBootVersion> bootVersions = springBootVersionRepository.findAll();

        for (SpringBootVersion bootVersion : bootVersions) {
            // Check if we already have requirements for this version
            if (requirementRepository.existsBySpringBootVersionId(bootVersion.getId())) {
                continue;
            }

            // Determine required Java version based on Spring Boot version
            String requiredJava = determineRequiredJavaVersion(bootVersion.getVersion());
            String requiredKotlin = determineRequiredKotlinVersion(bootVersion.getVersion());

            if (requiredJava != null) {
                SpringBootLanguageRequirement javaReq = SpringBootLanguageRequirement.builder()
                        .springBootVersion(bootVersion)
                        .language(LanguageType.JAVA)
                        .minVersion(requiredJava)
                        .recommendedVersion(requiredJava)
                        .build();
                requirementRepository.save(javaReq);
                updated++;
            }

            if (requiredKotlin != null) {
                SpringBootLanguageRequirement kotlinReq = SpringBootLanguageRequirement.builder()
                        .springBootVersion(bootVersion)
                        .language(LanguageType.KOTLIN)
                        .minVersion(requiredKotlin)
                        .recommendedVersion(requiredKotlin)
                        .build();
                requirementRepository.save(kotlinReq);
                updated++;
            }
        }

        return updated;
    }

    /**
     * Fetch JEP data from OpenJDK
     */
    private Map<String, JepInfo> fetchJepData() throws IOException {
        Map<String, JepInfo> jeps = new HashMap<>();

        try {
            Document doc = Jsoup.connect(OPENJDK_JEPS_URL)
                    .timeout(fetchTimeout)
                    .get();

            // Parse JEP table
            Elements rows = doc.select("table tr");
            for (Element row : rows) {
                Elements cols = row.select("td");
                if (cols.size() >= 3) {
                    String jepNum = cols.get(0).text().trim();
                    String title = cols.get(1).text().trim();
                    String status = cols.get(2).text().trim();

                    if (!jepNum.isEmpty() && jepNum.matches("\\d+")) {
                        JepInfo jep = new JepInfo();
                        jep.setNumber(jepNum);
                        jep.setTitle(title);
                        jep.setStatus(status);
                        jeps.put(jepNum, jep);
                    }
                }
            }
        } catch (IOException e) {
            log.warn("Could not fetch JEP data: {}", e.getMessage());
            // Return empty map - don't fail the entire sync
        }

        return jeps;
    }

    /**
     * Fetch Kotlin release information
     */
    private Map<String, KotlinReleaseInfo> fetchKotlinReleases() throws IOException {
        Map<String, KotlinReleaseInfo> releases = new HashMap<>();

        try {
            Document doc = Jsoup.connect(KOTLIN_RELEASES_URL)
                    .timeout(fetchTimeout)
                    .get();

            // Parse release sections
            Elements releaseSections = doc.select(".release-section, .kotlin-release");
            for (Element section : releaseSections) {
                String versionText = section.select("h2, h3, .version").text();
                Matcher matcher = KOTLIN_VERSION_PATTERN.matcher(versionText);
                if (matcher.find()) {
                    String version = matcher.group(1);
                    KotlinReleaseInfo info = new KotlinReleaseInfo();
                    info.setVersion(version);
                    info.setDescription(section.select("p").first() != null ?
                            section.select("p").first().text() : "");
                    releases.put(version, info);
                }
            }
        } catch (IOException e) {
            log.warn("Could not fetch Kotlin releases: {}", e.getMessage());
        }

        return releases;
    }

    /**
     * Update a feature with JEP information
     */
    private boolean updateFeatureFromJep(LanguageFeature feature, JepInfo jep) {
        boolean updated = false;

        // Update description if empty
        if ((feature.getDescription() == null || feature.getDescription().isEmpty()) &&
            jep.getTitle() != null) {
            feature.setDescription(jep.getTitle());
            updated = true;
        }

        // Update status based on JEP status
        if (jep.getStatus() != null) {
            String jepStatus = jep.getStatus().toLowerCase();
            if (jepStatus.contains("preview") && feature.getStatus() != FeatureStatus.PREVIEW) {
                feature.setStatus(FeatureStatus.PREVIEW);
                updated = true;
            } else if (jepStatus.contains("incubator") && feature.getStatus() != FeatureStatus.INCUBATING) {
                feature.setStatus(FeatureStatus.INCUBATING);
                updated = true;
            }
        }

        return updated;
    }

    /**
     * Update a version with Kotlin release info
     */
    private boolean updateVersionFromKotlinRelease(LanguageVersion version, KotlinReleaseInfo release) {
        boolean updated = false;

        if (release.getReleaseDate() != null && version.getReleaseDate() == null) {
            version.setReleaseDate(release.getReleaseDate());
            updated = true;
        }

        return updated;
    }

    /**
     * Check for new Java versions that might have been released
     */
    private int checkForNewJavaVersions() {
        // Get the latest Java version in our database
        Optional<LanguageVersion> latestOpt = versionRepository
                .findByLanguageAndIsCurrentTrue(LanguageType.JAVA);

        if (latestOpt.isEmpty()) {
            return 0;
        }

        // This is a placeholder - in a real implementation, we would
        // check Oracle/OpenJDK for newer versions and add them
        return 0;
    }

    /**
     * Determine required Java version for a Spring Boot version
     */
    private String determineRequiredJavaVersion(String springBootVersion) {
        // Parse Spring Boot version
        String[] parts = springBootVersion.split("\\.");
        if (parts.length < 2) return null;

        int major = Integer.parseInt(parts[0]);
        int minor = Integer.parseInt(parts[1]);

        // Spring Boot 4.0+ requires Java 25
        if (major >= 4) return "25";

        // Spring Boot 3.4+ requires Java 17
        if (major >= 3 && minor >= 4) return "17";

        // Spring Boot 3.0-3.3 requires Java 17
        if (major >= 3) return "17";

        // Spring Boot 2.7+ requires Java 8
        if (major >= 2 && minor >= 7) return "8";

        // Spring Boot 2.x requires Java 8
        if (major >= 2) return "8";

        return "8";
    }

    /**
     * Determine required Kotlin version for a Spring Boot version
     */
    private String determineRequiredKotlinVersion(String springBootVersion) {
        String[] parts = springBootVersion.split("\\.");
        if (parts.length < 2) return null;

        int major = Integer.parseInt(parts[0]);
        int minor = Integer.parseInt(parts[1]);

        // Spring Boot 4.0+ recommends Kotlin 2.1
        if (major >= 4) return "2.1";

        // Spring Boot 3.4+ recommends Kotlin 2.0
        if (major >= 3 && minor >= 4) return "2.0";

        // Spring Boot 3.0-3.3 recommends Kotlin 1.9
        if (major >= 3) return "1.9";

        // Spring Boot 2.x works with Kotlin 1.6+
        return "1.6";
    }

    /**
     * Load code examples from the curated JSON data file and update features.
     * Matches features by JEP number (Java) or feature key (Kotlin).
     *
     * @return number of features updated with code examples
     */
    private int loadCodeExamples() {
        int updated = 0;

        try {
            ClassPathResource resource = new ClassPathResource(CODE_EXAMPLES_FILE);
            if (!resource.exists()) {
                log.warn("Code examples file not found: {}", CODE_EXAMPLES_FILE);
                return 0;
            }

            try (InputStream is = resource.getInputStream()) {
                JsonNode root = objectMapper.readTree(is);

                // Process Java examples (keyed by JEP number)
                JsonNode javaExamples = root.get("java");
                if (javaExamples != null) {
                    updated += loadJavaExamples(javaExamples);
                }

                // Process Kotlin examples (keyed by feature key)
                JsonNode kotlinExamples = root.get("kotlin");
                if (kotlinExamples != null) {
                    updated += loadKotlinExamples(kotlinExamples);
                }
            }

        } catch (IOException e) {
            log.error("Failed to load code examples: {}", e.getMessage());
        }

        return updated;
    }

    /**
     * Load Java code examples by matching JEP numbers or normalized feature names.
     * Supports both numeric JEP keys (e.g., "286") and normalized name keys (e.g., "optional-class").
     */
    private int loadJavaExamples(JsonNode javaExamples) {
        int updated = 0;

        // Get all Java features for name-based matching
        List<LanguageFeature> javaFeatures = featureRepository.findByLanguage(LanguageType.JAVA);

        Iterator<String> keys = javaExamples.fieldNames();
        while (keys.hasNext()) {
            String key = keys.next();
            JsonNode exampleNode = javaExamples.get(key);

            String codeExample = exampleNode.has("example") ? exampleNode.get("example").asText() : null;
            String title = exampleNode.has("title") ? exampleNode.get("title").asText() : null;

            if (codeExample == null || codeExample.isBlank()) {
                continue;
            }

            boolean isJepNumber = key.matches("\\d+");
            List<LanguageFeature> matchedFeatures = new java.util.ArrayList<>();

            if (isJepNumber) {
                // Match by JEP number
                matchedFeatures = featureRepository.findByJepNumber(key);
            } else {
                // Match by normalized feature name (for non-JEP features)
                for (LanguageFeature feature : javaFeatures) {
                    String normalizedName = normalizeFeatureName(feature.getFeatureName());
                    if (normalizedName.equals(key) ||
                        (title != null && feature.getFeatureName().equalsIgnoreCase(title))) {
                        matchedFeatures.add(feature);
                        break; // Only match one feature per key
                    }
                }
            }

            for (LanguageFeature feature : matchedFeatures) {
                // Only update if example is different or not set
                if (feature.getCodeExample() == null || !feature.getCodeExample().equals(codeExample)) {
                    feature.setCodeExample(codeExample);
                    featureRepository.save(feature);
                    updated++;
                    log.debug("Updated code example for {}: {}", isJepNumber ? "JEP " + key : key, feature.getFeatureName());
                }
            }
        }

        log.info("Loaded {} Java code examples", updated);
        return updated;
    }

    /**
     * Load Kotlin code examples by matching feature key (lowercase, hyphenated name).
     */
    private int loadKotlinExamples(JsonNode kotlinExamples) {
        int updated = 0;

        // Get all Kotlin features
        List<LanguageFeature> kotlinFeatures = featureRepository.findByLanguage(LanguageType.KOTLIN);

        Iterator<String> featureKeys = kotlinExamples.fieldNames();
        while (featureKeys.hasNext()) {
            String featureKey = featureKeys.next();
            JsonNode exampleNode = kotlinExamples.get(featureKey);

            String codeExample = exampleNode.has("example") ? exampleNode.get("example").asText() : null;
            String title = exampleNode.has("title") ? exampleNode.get("title").asText() : null;

            if (codeExample == null || codeExample.isBlank()) {
                continue;
            }

            // Match by converting feature name to key format
            for (LanguageFeature feature : kotlinFeatures) {
                String normalizedName = normalizeFeatureName(feature.getFeatureName());
                if (normalizedName.equals(featureKey) ||
                    (title != null && feature.getFeatureName().equalsIgnoreCase(title))) {

                    if (feature.getCodeExample() == null || !feature.getCodeExample().equals(codeExample)) {
                        feature.setCodeExample(codeExample);
                        featureRepository.save(feature);
                        updated++;
                        log.debug("Updated code example for Kotlin feature: {}", feature.getFeatureName());
                    }
                    break;
                }
            }
        }

        log.info("Loaded {} Kotlin code examples", updated);
        return updated;
    }

    /**
     * Normalize feature name to key format: lowercase with hyphens.
     * Example: "Value Classes" -> "value-classes"
     */
    private String normalizeFeatureName(String name) {
        if (name == null) return "";
        return name.toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-|-$", "");
    }

    // ==================== Inner Classes ====================

    @Data
    private static class JepInfo {
        private String number;
        private String title;
        private String status;
        private String targetVersion;
    }

    @Data
    private static class KotlinReleaseInfo {
        private String version;
        private String description;
        private LocalDate releaseDate;
    }

    @Data
    private static class JavaSyncResult {
        private int versionsUpdated;
        private int featuresUpdated;
        private int errors;

        void incrementVersionsUpdated() { versionsUpdated++; }
        void incrementFeaturesUpdated() { featuresUpdated++; }
        void incrementErrors() { errors++; }
    }

    @Data
    private static class KotlinSyncResult {
        private int versionsUpdated;
        private int featuresUpdated;
        private int errors;

        void incrementVersionsUpdated() { versionsUpdated++; }
        void incrementFeaturesUpdated() { featuresUpdated++; }
        void incrementErrors() { errors++; }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SyncResult {
        private boolean success;
        private String message;
        private String errorMessage;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private int versionsUpdated;
        private int featuresUpdated;
        private int compatibilityUpdated;
        private int codeExamplesUpdated;
        private int errorsEncountered;

        public void addVersionsUpdated(int count) { versionsUpdated += count; }
        public void addFeaturesUpdated(int count) { featuresUpdated += count; }
        public void addCompatibilityUpdated(int count) { compatibilityUpdated += count; }
        public void addCodeExamplesUpdated(int count) { codeExamplesUpdated += count; }
        public void addErrorsEncountered(int count) { errorsEncountered += count; }
    }
}
