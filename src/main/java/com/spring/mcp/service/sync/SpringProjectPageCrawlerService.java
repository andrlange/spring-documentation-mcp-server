package com.spring.mcp.service.sync;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.spring.mcp.model.dto.crawler.VersionData;
import com.spring.mcp.model.entity.ProjectVersion;
import com.spring.mcp.model.entity.SpringBootVersion;
import com.spring.mcp.model.entity.SpringProject;
import com.spring.mcp.model.enums.VersionState;
import com.spring.mcp.repository.ProjectVersionRepository;
import com.spring.mcp.repository.SpringBootVersionRepository;
import com.spring.mcp.repository.SpringProjectRepository;
import com.spring.mcp.util.VersionParser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Service for crawling Spring project pages to extract version information,
 * documentation links, and support dates.
 *
 * @author Spring MCP Server
 * @version 1.0
 * @since 2025-01-08
 */
@Service
@Slf4j
public class SpringProjectPageCrawlerService {

    private final SpringProjectRepository springProjectRepository;
    private final ProjectVersionRepository projectVersionRepository;
    private final SpringBootVersionRepository springBootVersionRepository;
    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    public SpringProjectPageCrawlerService(SpringProjectRepository springProjectRepository,
                                           ProjectVersionRepository projectVersionRepository,
                                           SpringBootVersionRepository springBootVersionRepository,
                                           WebClient.Builder webClientBuilder,
                                           ObjectMapper objectMapper) {
        this.springProjectRepository = springProjectRepository;
        this.projectVersionRepository = projectVersionRepository;
        this.springBootVersionRepository = springBootVersionRepository;
        this.webClient = webClientBuilder.build();
        this.objectMapper = objectMapper;
    }

    private static final String SPRING_IO_BASE_URL = "https://spring.io";
    private static final String PAGE_DATA_URL_PATTERN = "%s/page-data/projects/%s/page-data.json";
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(30);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");

    /**
     * Crawl a Spring project page and update version information
     *
     * @param slug The project slug (e.g., "spring-boot", "spring-ai")
     * @return CrawlResult with statistics about the crawl operation
     */
    @Transactional
    public CrawlResult crawlProject(String slug) {
        log.info("Starting crawl for project: {}", slug);
        CrawlResult result = new CrawlResult();
        result.setProjectSlug(slug);

        try {
            // Verify project exists
            SpringProject project = springProjectRepository.findBySlug(slug)
                .orElseThrow(() -> new IllegalArgumentException("Project not found: " + slug));

            // Fetch JSON page data
            String jsonData = fetchPageData(slug);

            // Parse JSON to extract version and support data
            List<VersionData> versionDataList = parseProjectData(jsonData);
            log.debug("Parsed {} versions from JSON for {}", versionDataList.size(), slug);
            result.setVersionsParsed(versionDataList.size());

            // Update database
            int updated = updateProjectVersions(project, versionDataList);
            result.setVersionsUpdated(updated);

            // For spring-boot: also sync enterprise-only versions from spring_boot_versions
            // These versions are under extended enterprise support but not in spring.io documentation
            if ("spring-boot".equals(slug)) {
                int enterpriseVersions = syncEnterpriseOnlyVersions(project);
                if (enterpriseVersions > 0) {
                    log.info("Synced {} enterprise-only versions for spring-boot", enterpriseVersions);
                    result.setVersionsUpdated(result.getVersionsUpdated() + enterpriseVersions);
                }
            }

            result.setSuccess(true);
            log.info("Crawl completed for {}: {} versions parsed, {} updated",
                slug, result.getVersionsParsed(), result.getVersionsUpdated());

        } catch (Exception e) {
            log.error("Error crawling project: {}", slug, e);
            result.setSuccess(false);
            result.setErrorMessage(e.getMessage());
        }

        return result;
    }

    /**
     * Fetch Gatsby page data JSON for a project
     */
    private String fetchPageData(String slug) {
        String url = String.format(PAGE_DATA_URL_PATTERN, SPRING_IO_BASE_URL, slug);
        log.debug("Fetching JSON URL: {}", url);

        return webClient.get()
            .uri(url)
            .retrieve()
            .bodyToMono(String.class)
            .timeout(REQUEST_TIMEOUT)
            .onErrorResume(error -> {
                log.error("Error fetching {}: {}", url, error.getMessage());
                return Mono.just("{}");
            })
            .block();
    }

    /**
     * Parse Gatsby JSON page data to extract version and support information
     */
    private List<VersionData> parseProjectData(String jsonData) {
        List<VersionData> versionDataList = new ArrayList<>();

        if (jsonData == null || jsonData.isEmpty() || jsonData.equals("{}")) {
            log.warn("Empty JSON provided to parseProjectData");
            return versionDataList;
        }

        try {
            JsonNode root = objectMapper.readTree(jsonData);
            JsonNode fields = root.path("result").path("data").path("page").path("fields");

            // Parse documentation array for version info
            JsonNode documentation = fields.path("documentation");
            Map<String, VersionData.VersionDataBuilder> versionBuilders = new HashMap<>();

            if (documentation.isArray()) {
                for (JsonNode versionNode : documentation) {
                    String version = versionNode.path("version").asText(null);
                    if (version != null) {
                        VersionData.VersionDataBuilder builder = VersionData.builder()
                            .version(version)
                            .referenceDocUrl(versionNode.path("ref").asText(null))
                            .apiDocUrl(versionNode.path("api").asText(null));

                        // Map Gatsby status to our status format
                        String gatsbyStatus = versionNode.path("status").asText(null);
                        boolean isCurrent = versionNode.path("current").asBoolean(false);

                        if (gatsbyStatus != null) {
                            String status = mapGatsbyStatus(gatsbyStatus, isCurrent);
                            builder.status(status);
                        }

                        versionBuilders.put(version, builder);
                        log.trace("Parsed version data for: {}", version);
                    }
                }
            }

            // Parse support generations for support dates
            Map<String, SupportDates> supportDatesMap = new HashMap<>();
            JsonNode support = fields.path("support").path("generations");

            if (support.isArray()) {
                for (JsonNode generation : support) {
                    String gen = generation.path("generation").asText(null);
                    if (gen != null) {
                        SupportDates dates = SupportDates.builder()
                            .branch(gen)
                            .initialRelease(parseDate(generation.path("initialRelease").asText(null)))
                            .ossSupportEnd(parseDate(generation.path("ossSupportEnd").asText(null)))
                            .enterpriseSupportEnd(parseDate(generation.path("enterpriseSupportEnd").asText(null)))
                            .build();

                        supportDatesMap.put(gen, dates);
                        log.trace("Parsed support dates for generation: {}", gen);
                    }
                }
            }

            // Merge version data with support dates
            Set<String> branches = supportDatesMap.keySet();
            for (Map.Entry<String, VersionData.VersionDataBuilder> entry : versionBuilders.entrySet()) {
                String version = entry.getKey();
                VersionData.VersionDataBuilder builder = entry.getValue();

                // Match version to branch
                String matchedBranch = matchVersionToBranch(version, branches);
                if (matchedBranch != null) {
                    SupportDates supportDates = supportDatesMap.get(matchedBranch);
                    builder.branch(matchedBranch)
                        .initialRelease(supportDates.getInitialRelease())
                        .ossSupportEnd(supportDates.getOssSupportEnd())
                        .enterpriseSupportEnd(supportDates.getEnterpriseSupportEnd());
                }

                versionDataList.add(builder.build());
            }

        } catch (Exception e) {
            log.error("Error parsing project JSON data", e);
        }

        return versionDataList;
    }

    /**
     * Map Gatsby status to our database status format
     */
    private String mapGatsbyStatus(String gatsbyStatus, boolean isCurrent) {
        if ("GENERAL_AVAILABILITY".equals(gatsbyStatus)) {
            return isCurrent ? "CURRENT" : "GA";
        } else if ("SNAPSHOT".equals(gatsbyStatus)) {
            return "SNAPSHOT";
        } else if ("PRERELEASE".equals(gatsbyStatus)) {
            return "PRE";
        }
        return "GA"; // Default
    }

    /**
     * Parse date string in YYYY-MM format to LocalDate (first day of month)
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
     * Match a specific version (e.g., "3.5.7") to a support branch (e.g., "3.5.x")
     */
    private String matchVersionToBranch(String version, Set<String> branches) {
        VersionParser.ParsedVersion parsed = VersionParser.parse(version);

        for (String branch : branches) {
            if (branch.endsWith(".x")) {
                String branchPattern = branch.replace(".x", "");
                String versionPattern = parsed.getMajorVersion() + "." + parsed.getMinorVersion();

                if (versionPattern.equals(branchPattern)) {
                    return branch;
                }
            }
        }

        return null;
    }

    /**
     * Update project versions in the database with crawled data
     */
    private int updateProjectVersions(SpringProject project, List<VersionData> versionDataList) {
        int updatedCount = 0;

        for (VersionData versionData : versionDataList) {
            // Use the actual version number directly (e.g., "3.5.7")
            String version = versionData.getVersion();

            if (version == null || version.isEmpty()) {
                log.debug("Skipping null or empty version");
                continue;
            }

            log.debug("Processing version: {} for project: {}", version, project.getName());

            // Parse version to extract major, minor, patch
            VersionParser.ParsedVersion parsed = VersionParser.parse(version);

            // Determine the state of the incoming version
            VersionState incomingState = determineVersionState(version);

            // Find or create the version record
            Optional<ProjectVersion> existingVersion = projectVersionRepository
                .findByProjectAndVersion(project, version);

            ProjectVersion projectVersion;
            boolean isNew = false;

            if (existingVersion.isPresent()) {
                log.debug("Found existing version: {}, updating with crawled data", version);
                projectVersion = existingVersion.get();
            } else {
                // For GA versions, check if there's a corresponding SNAPSHOT version to replace
                if (incomingState == VersionState.GA) {
                    String snapshotVersion = version + "-SNAPSHOT";
                    Optional<ProjectVersion> snapshotVersionOpt = projectVersionRepository
                        .findByProjectAndVersion(project, snapshotVersion);

                    if (snapshotVersionOpt.isPresent()) {
                        // Update the SNAPSHOT version to become the GA version
                        log.info("Upgrading SNAPSHOT version {} to GA version {} for project: {}",
                            snapshotVersion, version, project.getName());
                        projectVersion = snapshotVersionOpt.get();
                        projectVersion.setVersion(version);
                        projectVersion.setState(VersionState.GA);
                        // Patch version might have changed (e.g., 2.0.1 vs 2.0.0)
                        projectVersion.setPatchVersion(parsed.getPatchVersion());
                        isNew = false;
                    } else {
                        log.debug("Version {} not found, creating new record", version);
                        projectVersion = new ProjectVersion();
                        projectVersion.setProject(project);
                        projectVersion.setVersion(version);
                        projectVersion.setMajorVersion(parsed.getMajorVersion());
                        projectVersion.setMinorVersion(parsed.getMinorVersion());
                        projectVersion.setPatchVersion(parsed.getPatchVersion());
                        projectVersion.setState(incomingState);
                        isNew = true;
                    }
                } else {
                    log.debug("Version {} not found, creating new record", version);
                    projectVersion = new ProjectVersion();
                    projectVersion.setProject(project);
                    projectVersion.setVersion(version);
                    projectVersion.setMajorVersion(parsed.getMajorVersion());
                    projectVersion.setMinorVersion(parsed.getMinorVersion());
                    projectVersion.setPatchVersion(parsed.getPatchVersion());
                    projectVersion.setState(incomingState);
                    isNew = true;
                }
            }

            boolean updated = false;

            // Update status
            if (versionData.getStatus() != null) {
                projectVersion.setStatus(versionData.getStatus());
                updated = true;
            }

            // Update reference doc URL (replace {version} placeholder with actual version)
            if (versionData.getReferenceDocUrl() != null) {
                String referenceUrl = versionData.getReferenceDocUrl().replace("{version}", version);
                projectVersion.setReferenceDocUrl(referenceUrl);
                updated = true;
            }

            // Update API doc URL (replace {version} placeholder with actual version)
            if (versionData.getApiDocUrl() != null) {
                String apiUrl = versionData.getApiDocUrl().replace("{version}", version);
                projectVersion.setApiDocUrl(apiUrl);
                updated = true;
            }

            // Update support dates
            if (versionData.getInitialRelease() != null) {
                projectVersion.setReleaseDate(versionData.getInitialRelease());
                updated = true;
            }

            if (versionData.getOssSupportEnd() != null) {
                projectVersion.setOssSupportEnd(versionData.getOssSupportEnd());
                updated = true;
            }

            if (versionData.getEnterpriseSupportEnd() != null) {
                projectVersion.setEnterpriseSupportEnd(versionData.getEnterpriseSupportEnd());
                updated = true;
            }

            // Track if this was a SNAPSHOT upgrade (for logging)
            boolean wasSnapshotUpgrade = !isNew && projectVersion.getState() == VersionState.GA
                && version.equals(projectVersion.getVersion());

            if (updated || isNew || wasSnapshotUpgrade) {
                projectVersionRepository.save(projectVersion);
                updatedCount++;
                if (isNew) {
                    log.info("Created new version: {} for project: {}", version, project.getName());
                } else if (wasSnapshotUpgrade) {
                    log.info("Upgraded version to GA: {} for project: {}", version, project.getName());
                } else {
                    log.debug("Updated version: {} for project: {}", version, project.getName());
                }
            } else {
                log.debug("No changes needed for version: {}", version);
            }
        }

        return updatedCount;
    }

    /**
     * Convert a specific version (e.g., "3.5.7") to a branch version (e.g., "3.5.x")
     */
    private String convertToBranchVersion(String specificVersion) {
        VersionParser.ParsedVersion parsed = VersionParser.parse(specificVersion);
        return parsed.getMajorVersion() + "." + parsed.getMinorVersion() + ".x";
    }

    /**
     * Determine VersionState based on version string
     * @param version Version string (e.g., "3.4.11-SNAPSHOT", "3.4.11-RC1", "3.4.11")
     * @return Appropriate VersionState enum value
     */
    private VersionState determineVersionState(String version) {
        if (version == null) {
            return VersionState.GA;
        }

        String versionUpper = version.toUpperCase();

        if (versionUpper.contains("-SNAPSHOT")) {
            return VersionState.SNAPSHOT;
        } else if (versionUpper.contains("-M") || versionUpper.contains("-MILESTONE")) {
            return VersionState.MILESTONE;
        } else if (versionUpper.contains("-RC")) {
            return VersionState.RC;
        } else {
            return VersionState.GA;  // Default for release versions
        }
    }

    /**
     * Sync enterprise-only Spring Boot versions from spring_boot_versions to project_versions.
     * These are versions that are under extended enterprise support but no longer in spring.io documentation.
     *
     * For example: Spring Boot 2.7.x has enterprise support until 2029-06 but is not in active documentation.
     *
     * @param springBootProject The spring-boot project entity
     * @return Number of enterprise versions synced
     */
    private int syncEnterpriseOnlyVersions(SpringProject springBootProject) {
        int syncedCount = 0;

        // Find all enterprise-only versions from spring_boot_versions
        List<SpringBootVersion> enterpriseVersions = springBootVersionRepository.findAll().stream()
            .filter(v -> Boolean.TRUE.equals(v.getIsEnterpriseOnly()))
            .toList();

        if (enterpriseVersions.isEmpty()) {
            log.debug("No enterprise-only Spring Boot versions found");
            return 0;
        }

        log.info("Found {} enterprise-only Spring Boot versions to sync", enterpriseVersions.size());

        for (SpringBootVersion enterpriseVersion : enterpriseVersions) {
            String version = enterpriseVersion.getVersion();

            // Check if this version already exists in project_versions
            Optional<ProjectVersion> existing = projectVersionRepository
                .findByProjectAndVersion(springBootProject, version);

            if (existing.isPresent()) {
                log.debug("Enterprise version {} already exists in project_versions", version);
                continue;
            }

            // Create new project version entry for the enterprise version
            ProjectVersion projectVersion = new ProjectVersion();
            projectVersion.setProject(springBootProject);
            projectVersion.setVersion(version);
            projectVersion.setMajorVersion(enterpriseVersion.getMajorVersion());
            projectVersion.setMinorVersion(enterpriseVersion.getMinorVersion());
            projectVersion.setPatchVersion(enterpriseVersion.getPatchVersion() != null ?
                enterpriseVersion.getPatchVersion() : 0);
            projectVersion.setState(enterpriseVersion.getState() != null ?
                enterpriseVersion.getState() : VersionState.GA);
            projectVersion.setStatus("ENTERPRISE");
            projectVersion.setReleaseDate(enterpriseVersion.getReleasedAt());
            projectVersion.setOssSupportEnd(enterpriseVersion.getOssSupportEnd());
            projectVersion.setEnterpriseSupportEnd(enterpriseVersion.getEnterpriseSupportEnd());
            projectVersion.setReferenceDocUrl(enterpriseVersion.getReferenceDocUrl());
            projectVersion.setApiDocUrl(enterpriseVersion.getApiDocUrl());

            projectVersionRepository.save(projectVersion);
            syncedCount++;

            log.info("Created enterprise project version: {} (enterprise support until {})",
                version, enterpriseVersion.getEnterpriseSupportEnd());
        }

        return syncedCount;
    }

    /**
     * Support dates for a version branch
     */
    @lombok.Data
    @lombok.Builder
    private static class SupportDates {
        private String branch;
        private LocalDate initialRelease;
        private LocalDate ossSupportEnd;
        private LocalDate enterpriseSupportEnd;
    }

    /**
     * Result of a crawl operation
     */
    @lombok.Data
    public static class CrawlResult {
        private boolean success;
        private String projectSlug;
        private int versionsParsed;
        private int versionsUpdated;
        private String errorMessage;
    }
}
