package com.spring.mcp.service.sync;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.spring.mcp.model.entity.SpringBootVersion;
import com.spring.mcp.model.enums.VersionState;
import com.spring.mcp.repository.SpringBootVersionRepository;
import com.spring.mcp.service.SettingsService;
import com.spring.mcp.util.VersionParser;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for synchronizing Spring Boot versions into the spring_boot_versions table.
 * This table serves as the PRIMARY filter for the entire Spring MCP Server system.
 *
 * Data source: https://spring.io/projects/spring-boot (page-data.json)
 *
 * @author Spring MCP Server
 * @version 1.0
 * @since 2025-01-09
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SpringBootVersionSyncService {

    private final SpringBootVersionRepository springBootVersionRepository;
    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper objectMapper;
    private final SettingsService settingsService;

    private static final String SPRING_BOOT_PAGE_DATA_URL =
        "https://spring.io/page-data/projects/spring-boot/page-data.json";
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(30);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");

    /**
     * Synchronize Spring Boot versions from spring.io into the spring_boot_versions table.
     *
     * @return SyncResult with statistics
     */
    @Transactional
    public SyncResult syncSpringBootVersions() {
        log.info("Starting Spring Boot versions sync for spring_boot_versions table");
        SyncResult result = new SyncResult();

        try {
            // Fetch page data JSON
            String jsonData = fetchPageData();

            // Parse version data
            List<VersionData> versionDataList = parseVersionData(jsonData);
            log.info("Parsed {} Spring Boot versions from spring.io", versionDataList.size());
            result.setVersionsParsed(versionDataList.size());

            // Process each version
            for (VersionData versionData : versionDataList) {
                try {
                    processVersion(versionData, result);
                } catch (Exception e) {
                    log.error("Error processing Spring Boot version: {}", versionData.getVersion(), e);
                    result.incrementErrors();
                }
            }

            result.setSuccess(true);
            log.info("Spring Boot versions sync completed. Created: {}, Updated: {}, Errors: {}",
                result.getVersionsCreated(), result.getVersionsUpdated(), result.getErrorsEncountered());

        } catch (Exception e) {
            log.error("Error during Spring Boot versions sync", e);
            result.setSuccess(false);
            result.setErrorMessage(e.getMessage());
        }

        return result;
    }

    /**
     * Fetch Spring Boot page data JSON
     */
    private String fetchPageData() {
        log.debug("Fetching Spring Boot page data from: {}", SPRING_BOOT_PAGE_DATA_URL);

        WebClient webClient = webClientBuilder.build();

        return webClient.get()
            .uri(SPRING_BOOT_PAGE_DATA_URL)
            .retrieve()
            .bodyToMono(String.class)
            .timeout(REQUEST_TIMEOUT)
            .doOnError(error -> log.error("Error fetching Spring Boot page data: {}", error.getMessage()))
            .block();
    }

    /**
     * Parse Spring Boot version data from JSON.
     * Includes enterprise-supported versions when enterprise subscription is enabled.
     */
    private List<VersionData> parseVersionData(String jsonData) {
        List<VersionData> versionDataList = new ArrayList<>();

        if (jsonData == null || jsonData.isEmpty()) {
            log.warn("Empty JSON data received");
            return versionDataList;
        }

        boolean enterpriseEnabled = settingsService.isEnterpriseSubscriptionEnabled();
        LocalDate today = LocalDate.now();

        try {
            JsonNode root = objectMapper.readTree(jsonData);
            JsonNode fields = root.path("result").path("data").path("page").path("fields");

            // Parse documentation array for version info (actively documented versions)
            JsonNode documentation = fields.path("documentation");
            // Use a multi-map to store ALL versions per major.minor (not just the last one)
            Map<String, List<VersionData>> versionsByMajorMinor = new HashMap<>();

            if (documentation.isArray()) {
                for (JsonNode versionNode : documentation) {
                    String version = versionNode.path("version").asText(null);
                    if (version != null && !version.isEmpty()) {
                        VersionData versionData = VersionData.builder()
                            .version(version)
                            .referenceDocUrl(versionNode.path("ref").asText(null))
                            .apiDocUrl(versionNode.path("api").asText(null))
                            .isCurrent(versionNode.path("current").asBoolean(false))
                            .gatsbyStatus(versionNode.path("status").asText(null))
                            .isEnterpriseOnly(false)
                            .build();

                        versionDataList.add(versionData);
                        // Store by major.minor for matching - collect ALL versions per major.minor
                        VersionParser.ParsedVersion parsed = VersionParser.parse(version);
                        String key = parsed.getMajorVersion() + "." + parsed.getMinorVersion();
                        versionsByMajorMinor.computeIfAbsent(key, k -> new ArrayList<>()).add(versionData);
                        log.trace("Parsed version: {} (current: {})", version, versionData.isCurrent());
                    }
                }
            }

            // Parse support generations for support dates AND enterprise-only versions
            JsonNode support = fields.path("support").path("generations");

            if (support.isArray()) {
                for (JsonNode generation : support) {
                    String gen = generation.path("generation").asText(null);
                    if (gen == null) continue;

                    LocalDate initialRelease = parseDate(generation.path("initialRelease").asText(null));
                    LocalDate ossSupportEnd = parseDate(generation.path("ossSupportEnd").asText(null));
                    LocalDate enterpriseSupportEnd = parseDate(generation.path("enterpriseSupportEnd").asText(null));

                    // Extract major.minor from generation (e.g., "2.7.x" -> "2.7")
                    VersionParser.ParsedVersion genParsed = VersionParser.parse(gen);
                    String key = genParsed.getMajorVersion() + "." + genParsed.getMinorVersion();

                    // Get ALL versions for this generation (e.g., both 3.5.8 and 3.5.9-SNAPSHOT)
                    List<VersionData> versionsForGen = versionsByMajorMinor.get(key);

                    if (versionsForGen != null && !versionsForGen.isEmpty()) {
                        // Update support dates for ALL versions in this generation
                        for (VersionData versionData : versionsForGen) {
                            versionData.setInitialRelease(initialRelease);
                            versionData.setOssSupportEnd(ossSupportEnd);
                            versionData.setEnterpriseSupportEnd(enterpriseSupportEnd);
                            log.trace("Matched generation {} to version {}", gen, versionData.getVersion());
                        }
                        log.debug("Applied support dates from {} to {} versions", gen, versionsForGen.size());
                    } else if (enterpriseEnabled && enterpriseSupportEnd != null && enterpriseSupportEnd.isAfter(today)) {
                        // No documented version, but enterprise support is still active
                        // Create an enterprise-only version entry using the generation pattern
                        log.info("Adding enterprise-supported version {} (enterprise support until {})",
                            gen, enterpriseSupportEnd);

                        VersionData enterpriseVersion = VersionData.builder()
                            .version(gen) // Use generation pattern like "2.7.x"
                            .isCurrent(false)
                            .gatsbyStatus("ENTERPRISE")
                            .initialRelease(initialRelease)
                            .ossSupportEnd(ossSupportEnd)
                            .enterpriseSupportEnd(enterpriseSupportEnd)
                            .isEnterpriseOnly(true)
                            .build();

                        versionDataList.add(enterpriseVersion);
                        versionsByMajorMinor.computeIfAbsent(key, k -> new ArrayList<>()).add(enterpriseVersion);
                    } else if (enterpriseSupportEnd != null) {
                        log.debug("Skipping generation {} - enterprise support ended {} (enterprise mode: {})",
                            gen, enterpriseSupportEnd, enterpriseEnabled ? "enabled" : "disabled");
                    }
                }
            }

        } catch (Exception e) {
            log.error("Error parsing Spring Boot version data", e);
        }

        log.info("Parsed {} Spring Boot versions (enterprise mode: {})",
            versionDataList.size(), enterpriseEnabled ? "enabled" : "disabled");
        return versionDataList;
    }

    /**
     * Parse date string in YYYY-MM format to LocalDate
     */
    private LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.isEmpty() || dateStr.equals("-")) {
            return null;
        }

        try {
            YearMonth yearMonth = YearMonth.parse(dateStr, DATE_FORMATTER);
            return yearMonth.atDay(1);
        } catch (Exception e) {
            log.debug("Failed to parse date: {}", dateStr);
            return null;
        }
    }

    /**
     * Process a single version - create or update in database
     */
    private void processVersion(VersionData versionData, SyncResult result) {
        String version = versionData.getVersion();

        // Check if version already exists
        SpringBootVersion existingVersion = springBootVersionRepository
            .findByVersion(version)
            .orElse(null);

        if (existingVersion != null) {
            // Update existing version
            updateVersion(existingVersion, versionData);
            springBootVersionRepository.save(existingVersion);
            result.incrementUpdated();
            log.debug("Updated Spring Boot version: {}", version);
        } else {
            // Create new version
            SpringBootVersion newVersion = createVersion(versionData);
            springBootVersionRepository.save(newVersion);
            result.incrementCreated();
            log.info("Created Spring Boot version: {} (state: {}, current: {})",
                version, newVersion.getState(), newVersion.getIsCurrent());
        }
    }

    /**
     * Create a new SpringBootVersion entity from VersionData
     */
    private SpringBootVersion createVersion(VersionData versionData) {
        VersionParser.ParsedVersion parsed = VersionParser.parse(versionData.getVersion());
        VersionState state = determineVersionState(versionData.getGatsbyStatus(), versionData.getVersion());

        return SpringBootVersion.builder()
            .version(versionData.getVersion())
            .majorVersion(parsed.getMajorVersion())
            .minorVersion(parsed.getMinorVersion())
            .patchVersion(parsed.getPatchVersion())
            .state(state)
            .isCurrent(versionData.isCurrent())
            .releasedAt(versionData.getInitialRelease())
            .ossSupportEnd(versionData.getOssSupportEnd())
            .enterpriseSupportEnd(versionData.getEnterpriseSupportEnd())
            .referenceDocUrl(versionData.getReferenceDocUrl())
            .apiDocUrl(versionData.getApiDocUrl())
            .isEnterpriseOnly(versionData.isEnterpriseOnly())
            .build();
    }

    /**
     * Update an existing SpringBootVersion entity with new data
     */
    private void updateVersion(SpringBootVersion existing, VersionData versionData) {
        // Update fields that may have changed
        if (versionData.getReferenceDocUrl() != null) {
            existing.setReferenceDocUrl(versionData.getReferenceDocUrl());
        }
        if (versionData.getApiDocUrl() != null) {
            existing.setApiDocUrl(versionData.getApiDocUrl());
        }
        if (versionData.getInitialRelease() != null) {
            existing.setReleasedAt(versionData.getInitialRelease());
        }
        if (versionData.getOssSupportEnd() != null) {
            existing.setOssSupportEnd(versionData.getOssSupportEnd());
        }
        if (versionData.getEnterpriseSupportEnd() != null) {
            existing.setEnterpriseSupportEnd(versionData.getEnterpriseSupportEnd());
        }

        // Update current flag
        existing.setIsCurrent(versionData.isCurrent());

        // Update enterprise-only flag
        existing.setIsEnterpriseOnly(versionData.isEnterpriseOnly());

        // Update state based on Gatsby status
        VersionState state = determineVersionState(versionData.getGatsbyStatus(), versionData.getVersion());
        existing.setState(state);
    }

    /**
     * Determine VersionState from Gatsby status and version string
     */
    private VersionState determineVersionState(String gatsbyStatus, String version) {
        // Check version string first for explicit markers
        if (version.contains("SNAPSHOT")) {
            return VersionState.SNAPSHOT;
        } else if (version.contains("RC")) {
            return VersionState.RC;
        } else if (version.contains("M")) {
            return VersionState.MILESTONE;
        }

        // Fall back to Gatsby status
        if ("SNAPSHOT".equals(gatsbyStatus)) {
            return VersionState.SNAPSHOT;
        } else if ("PRERELEASE".equals(gatsbyStatus)) {
            // PRERELEASE could be RC or MILESTONE, default to RC
            return VersionState.RC;
        } else if ("GENERAL_AVAILABILITY".equals(gatsbyStatus)) {
            return VersionState.GA;
        }

        // Default to GA
        return VersionState.GA;
    }

    /**
     * Version data holder
     */
    @Data
    @lombok.Builder
    private static class VersionData {
        private String version;
        private String referenceDocUrl;
        private String apiDocUrl;
        private boolean isCurrent;
        private String gatsbyStatus;
        private LocalDate initialRelease;
        private LocalDate ossSupportEnd;
        private LocalDate enterpriseSupportEnd;
        private boolean isEnterpriseOnly;
    }

    /**
     * Sync result holder
     */
    @Data
    public static class SyncResult {
        private boolean success;
        private String errorMessage;
        private int versionsParsed;
        private int versionsCreated;
        private int versionsUpdated;
        private int errorsEncountered;

        public void incrementCreated() {
            this.versionsCreated++;
        }

        public void incrementUpdated() {
            this.versionsUpdated++;
        }

        public void incrementErrors() {
            this.errorsEncountered++;
        }
    }
}
