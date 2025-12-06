package com.spring.mcp.controller.web;

import com.spring.mcp.config.InitializrProperties;
import com.spring.mcp.service.initializr.InitializrService;
import com.spring.mcp.service.initializr.dto.BootVersion;
import com.spring.mcp.service.initializr.dto.DependencyCategory;
import com.spring.mcp.service.initializr.dto.DependencyInfo;
import com.spring.mcp.service.initializr.dto.InitializrMetadata;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Web controller for Spring Boot Initializr UI integration.
 *
 * <p>Provides a web interface for browsing Spring Initializr metadata,
 * configuring new projects, and generating project archives.</p>
 *
 * <h3>Features:</h3>
 * <ul>
 *   <li>Browse available dependencies by category</li>
 *   <li>Configure project settings (name, group, version, etc.)</li>
 *   <li>Preview build files (Maven/Gradle)</li>
 *   <li>Generate and download project archives</li>
 *   <li>Search dependencies with live filtering</li>
 * </ul>
 *
 * <h3>Routes:</h3>
 * <ul>
 *   <li>GET /initializr - Main page with two-tab layout</li>
 *   <li>GET /initializr/dependencies/search - AJAX dependency search</li>
 *   <li>GET /initializr/preview - Preview build file content</li>
 *   <li>POST /initializr/generate - Generate and download project ZIP</li>
 * </ul>
 *
 * @author Spring MCP Server
 * @version 1.4.0
 * @since 2025-12-06
 */
@Controller
@RequestMapping("/initializr")
@Slf4j
@ConditionalOnProperty(prefix = "mcp.features.initializr", name = "enabled", havingValue = "true", matchIfMissing = true)
public class InitializrController {

    private final InitializrService initializrService;
    private final InitializrProperties properties;
    private final RestTemplate restTemplate;

    public InitializrController(
            InitializrService initializrService,
            InitializrProperties properties,
            @Qualifier("initializrRestTemplate") RestTemplate restTemplate) {
        this.initializrService = initializrService;
        this.properties = properties;
        this.restTemplate = restTemplate;
    }

    /**
     * Display the main Initializr page with two-tab layout.
     * Tab 1: Project Configuration (metadata)
     * Tab 2: Dependencies Selection
     *
     * @param model Spring MVC model to add attributes for the view
     * @return view name "initializr/index"
     */
    @GetMapping
    public String showInitializrPage(Model model) {
        log.debug("Showing Initializr page");

        // Set active page for sidebar navigation
        model.addAttribute("activePage", "initializr");
        model.addAttribute("pageTitle", "Spring Initializr");

        // Load metadata for dropdowns
        List<BootVersion> bootVersions = initializrService.getBootVersions();
        BootVersion defaultVersion = initializrService.getDefaultBootVersion();
        List<DependencyCategory> categories = initializrService.getDependencyCategories();
        List<InitializrMetadata.JavaVersion> javaVersions = initializrService.getJavaVersions();
        List<InitializrMetadata.LanguageOption> languages = initializrService.getLanguages();
        List<InitializrMetadata.TypeOption> projectTypes = initializrService.getProjectTypes();
        List<InitializrMetadata.PackagingOption> packagingTypes = initializrService.getPackagingTypes();

        // Add to model
        model.addAttribute("bootVersions", bootVersions);
        model.addAttribute("defaultBootVersion", defaultVersion != null ? defaultVersion.getId() : properties.getDefaults().getBootVersion());
        model.addAttribute("dependencyCategories", categories);
        model.addAttribute("javaVersions", javaVersions);
        model.addAttribute("languages", languages);
        model.addAttribute("projectTypes", projectTypes);
        model.addAttribute("packagingTypes", packagingTypes);

        // Add defaults from configuration
        model.addAttribute("defaults", properties.getDefaults());

        // Cache status for admin info
        model.addAttribute("cacheStatus", initializrService.getCacheStatus());

        // Calculate total dependencies
        int totalDependencies = categories.stream()
                .filter(cat -> cat.getContent() != null)
                .mapToInt(cat -> cat.getContent().size())
                .sum();
        model.addAttribute("totalDependencies", totalDependencies);

        return "initializr/index";
    }

    /**
     * Search dependencies with AJAX.
     * Returns matching dependencies as JSON for live filtering.
     *
     * @param query search query (matches name, description, or ID)
     * @param bootVersion optional Spring Boot version filter
     * @param category optional category filter
     * @param limit max results (default 20)
     * @return JSON response with matching dependencies
     */
    @GetMapping("/dependencies/search")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> searchDependencies(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String bootVersion,
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "20") int limit) {

        log.debug("Searching dependencies: query={}, bootVersion={}, category={}, limit={}",
                query, bootVersion, category, limit);

        try {
            if (query == null || query.isBlank()) {
                // Return all dependencies grouped by category if no query
                List<DependencyCategory> categories = initializrService.getDependencyCategories();
                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "categories", categories,
                        "totalCount", categories.stream()
                                .filter(cat -> cat.getContent() != null)
                                .mapToInt(cat -> cat.getContent().size())
                                .sum()
                ));
            }

            InitializrService.SearchResult result = initializrService.searchDependencies(query, bootVersion, category, limit);

            List<Map<String, Object>> dependencies = result.results().stream()
                    .map(this::dependencyToMap)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "query", query,
                    "count", result.totalCount(),
                    "dependencies", dependencies,
                    "byCategory", result.byCategory().entrySet().stream()
                            .collect(Collectors.toMap(
                                    Map.Entry::getKey,
                                    e -> e.getValue().stream()
                                            .map(this::dependencyToMap)
                                            .collect(Collectors.toList())
                            ))
            ));

        } catch (Exception e) {
            log.error("Error searching dependencies", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "success", false,
                            "error", "Failed to search dependencies: " + e.getMessage()
                    ));
        }
    }

    /**
     * Preview build file content (pom.xml or build.gradle).
     * Returns the generated build file as text.
     *
     * @param type project type (maven-project, gradle-project, gradle-project-kotlin)
     * @param bootVersion Spring Boot version
     * @param groupId project group ID
     * @param artifactId project artifact ID
     * @param name project name
     * @param description project description
     * @param packageName base package name
     * @param packaging packaging type (jar, war)
     * @param javaVersion Java version
     * @param language programming language (java, kotlin, groovy)
     * @param dependencies comma-separated dependency IDs
     * @return build file content
     */
    @GetMapping("/preview")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> previewBuildFile(
            @RequestParam String type,
            @RequestParam String bootVersion,
            @RequestParam(defaultValue = "com.example") String groupId,
            @RequestParam(defaultValue = "demo") String artifactId,
            @RequestParam(defaultValue = "demo") String name,
            @RequestParam(defaultValue = "Demo project for Spring Boot") String description,
            @RequestParam(required = false) String packageName,
            @RequestParam(defaultValue = "jar") String packaging,
            @RequestParam(defaultValue = "21") String javaVersion,
            @RequestParam(defaultValue = "java") String language,
            @RequestParam(required = false) String dependencies) {

        log.debug("Previewing build file: type={}, bootVersion={}, deps={}", type, bootVersion, dependencies);

        try {
            // Determine endpoint based on project type
            String endpoint = switch (type.toLowerCase()) {
                case "maven-project" -> "/pom.xml";
                case "gradle-project" -> "/build.gradle";
                case "gradle-project-kotlin" -> "/build.gradle.kts";
                default -> "/build.gradle";
            };

            // Build request URL - normalize bootVersion for API compatibility
            String normalizedVersion = normalizeBootVersion(bootVersion);
            UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(properties.getBaseUrl() + endpoint)
                    .queryParam("type", type)
                    .queryParam("bootVersion", normalizedVersion)
                    .queryParam("groupId", groupId)
                    .queryParam("artifactId", artifactId)
                    .queryParam("name", name)
                    .queryParam("description", description)
                    .queryParam("packaging", packaging)
                    .queryParam("javaVersion", javaVersion)
                    .queryParam("language", language);

            if (packageName != null && !packageName.isBlank()) {
                builder.queryParam("packageName", packageName);
            }

            if (dependencies != null && !dependencies.isBlank()) {
                builder.queryParam("dependencies", dependencies);
            }

            // Replace %20 with + for spaces - start.spring.io doesn't decode %20 properly in build files
            String url = builder.toUriString().replace("%20", "+");
            log.debug("Fetching build file from: {}", url);

            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", properties.getApi().getUserAgent());
            HttpEntity<Void> request = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, request, String.class);

            String content = response.getBody();
            String filename = switch (type.toLowerCase()) {
                case "maven-project" -> "pom.xml";
                case "gradle-project-kotlin" -> "build.gradle.kts";
                default -> "build.gradle";
            };

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "filename", filename,
                    "content", content != null ? content : "",
                    "contentType", type.contains("maven") ? "application/xml" : "text/x-groovy"
            ));

        } catch (Exception e) {
            log.error("Error generating build file preview", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "success", false,
                            "error", "Failed to generate preview: " + e.getMessage()
                    ));
        }
    }

    /**
     * Generate and download project archive.
     * Proxies request to Spring Initializr and returns the ZIP file.
     *
     * @param type project type
     * @param bootVersion Spring Boot version
     * @param groupId project group ID
     * @param artifactId project artifact ID
     * @param name project name
     * @param description project description
     * @param packageName base package name
     * @param packaging packaging type
     * @param javaVersion Java version
     * @param language programming language
     * @param dependencies comma-separated dependency IDs
     * @return ZIP file download response
     */
    @PostMapping("/generate")
    public ResponseEntity<byte[]> generateProject(
            @RequestParam String type,
            @RequestParam String bootVersion,
            @RequestParam(defaultValue = "com.example") String groupId,
            @RequestParam(defaultValue = "demo") String artifactId,
            @RequestParam(defaultValue = "demo") String name,
            @RequestParam(defaultValue = "Demo project for Spring Boot") String description,
            @RequestParam(required = false) String packageName,
            @RequestParam(defaultValue = "jar") String packaging,
            @RequestParam(defaultValue = "21") String javaVersion,
            @RequestParam(defaultValue = "java") String language,
            @RequestParam(required = false) String dependencies) {

        log.info("Generating project: artifact={}, bootVersion={}, type={}", artifactId, bootVersion, type);

        try {
            // Build request URL for starter.zip - normalize bootVersion for API compatibility
            String normalizedVersion = normalizeBootVersion(bootVersion);
            UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(properties.getStarterZipUrl())
                    .queryParam("type", type)
                    .queryParam("bootVersion", normalizedVersion)
                    .queryParam("groupId", groupId)
                    .queryParam("artifactId", artifactId)
                    .queryParam("name", name)
                    .queryParam("description", description)
                    .queryParam("packaging", packaging)
                    .queryParam("javaVersion", javaVersion)
                    .queryParam("language", language);

            if (packageName != null && !packageName.isBlank()) {
                builder.queryParam("packageName", packageName);
            }

            if (dependencies != null && !dependencies.isBlank()) {
                builder.queryParam("dependencies", dependencies);
            }

            // Replace %20 with + for spaces - start.spring.io doesn't decode %20 properly in build files
            String url = builder.toUriString().replace("%20", "+");
            log.debug("Generating project from: {}", url);

            HttpHeaders requestHeaders = new HttpHeaders();
            requestHeaders.set("User-Agent", properties.getApi().getUserAgent());
            requestHeaders.setAccept(List.of(MediaType.APPLICATION_OCTET_STREAM));
            HttpEntity<Void> request = new HttpEntity<>(requestHeaders);

            ResponseEntity<byte[]> response = restTemplate.exchange(url, HttpMethod.GET, request, byte[].class);

            if (response.getBody() == null || response.getBody().length == 0) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(null);
            }

            // Prepare response headers
            HttpHeaders responseHeaders = new HttpHeaders();
            responseHeaders.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            responseHeaders.setContentDisposition(ContentDisposition.attachment()
                    .filename(artifactId + ".zip")
                    .build());
            responseHeaders.setContentLength(response.getBody().length);

            log.info("Successfully generated project: {}.zip ({} bytes)", artifactId, response.getBody().length);

            return new ResponseEntity<>(response.getBody(), responseHeaders, HttpStatus.OK);

        } catch (Exception e) {
            log.error("Error generating project", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(null);
        }
    }

    /**
     * Get dependency details by ID.
     * Returns full dependency information including compatibility.
     *
     * @param id dependency ID
     * @param bootVersion optional Spring Boot version for compatibility check
     * @return dependency details as JSON
     */
    @GetMapping("/dependencies/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getDependency(
            @PathVariable String id,
            @RequestParam(required = false) String bootVersion) {

        log.debug("Getting dependency details: id={}, bootVersion={}", id, bootVersion);

        try {
            InitializrService.DependencyResult result = initializrService.getDependency(id, bootVersion, "gradle");

            if (!result.found()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of(
                                "success", false,
                                "error", "Dependency not found: " + id
                        ));
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("dependency", dependencyToMap(result.dependency()));
            response.put("compatible", result.compatible());
            response.put("message", result.message());

            if (result.compatibilityInfo() != null) {
                response.put("compatibilityInfo", Map.of(
                        "versionRange", result.compatibilityInfo().getVersionRange(),
                        "reason", result.compatibilityInfo().getReason(),
                        "suggestedVersion", result.compatibilityInfo().getSuggestedBootVersion()
                ));
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error getting dependency details", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "success", false,
                            "error", "Failed to get dependency: " + e.getMessage()
                    ));
        }
    }

    /**
     * Refresh the Initializr cache.
     * Admin-only endpoint to force cache refresh.
     *
     * @return refresh status
     */
    @PostMapping("/cache/refresh")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> refreshCache() {
        log.info("Refreshing Initializr cache");

        try {
            initializrService.refreshCache();
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Cache refreshed successfully",
                    "status", initializrService.getCacheStatus()
            ));
        } catch (Exception e) {
            log.error("Error refreshing cache", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "success", false,
                            "error", "Failed to refresh cache: " + e.getMessage()
                    ));
        }
    }

    // Private helper methods

    /**
     * Normalize the Spring Boot version for API requests.
     * The metadata API returns versions like "4.0.0.RELEASE" and "4.0.1.BUILD-SNAPSHOT",
     * but the project generation APIs expect versions like "4.0.0" and "4.0.1-SNAPSHOT".
     *
     * @param version the version from metadata API
     * @return normalized version for project generation
     */
    private String normalizeBootVersion(String version) {
        if (version == null) {
            return "3.5.8"; // default fallback
        }
        // Remove .RELEASE suffix (e.g., "4.0.0.RELEASE" -> "4.0.0")
        String normalized = version.replace(".RELEASE", "");
        // Convert .BUILD-SNAPSHOT to -SNAPSHOT (e.g., "4.0.1.BUILD-SNAPSHOT" -> "4.0.1-SNAPSHOT")
        normalized = normalized.replace(".BUILD-SNAPSHOT", "-SNAPSHOT");
        return normalized;
    }

    private Map<String, Object> dependencyToMap(DependencyInfo dep) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", dep.getId());
        map.put("name", dep.getName());
        map.put("description", dep.getDescription());
        map.put("groupId", dep.getGroupId());
        map.put("artifactId", dep.getArtifactId());
        map.put("versionRange", dep.getVersionRange());
        if (dep.getLinks() != null && !dep.getLinks().isEmpty()) {
            map.put("links", dep.getLinks());
        }
        return map;
    }
}
