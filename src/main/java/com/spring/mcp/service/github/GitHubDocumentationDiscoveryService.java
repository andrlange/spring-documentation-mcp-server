package com.spring.mcp.service.github;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.spring.mcp.config.GitHubProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service for discovering AsciiDoc documentation files in Spring GitHub repositories.
 *
 * This service handles the discovery of documentation paths for different Spring projects
 * and versions, accounting for structure changes between versions.
 *
 * @author Spring MCP Server
 * @version 1.0.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GitHubDocumentationDiscoveryService {

    private final GitHubProperties gitHubProperties;
    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper objectMapper;

    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(30);

    /**
     * Default documentation paths for main/latest branch.
     * These are the current paths used by each Spring project.
     */
    private static final Map<String, String> DEFAULT_DOC_PATHS = Map.ofEntries(
        Map.entry("spring-boot", "documentation/spring-boot-docs/src/docs/antora/modules"),
        Map.entry("spring-security", "docs/modules/ROOT/pages"),
        Map.entry("spring-framework", "framework-docs/modules/ROOT/pages"),
        Map.entry("spring-data-commons", "src/main/antora/modules/ROOT/pages"),
        Map.entry("spring-data-jpa", "src/main/antora/modules/ROOT/pages"),
        Map.entry("spring-data-mongodb", "src/main/antora/modules/ROOT/pages"),
        Map.entry("spring-data-redis", "src/main/antora/modules/ROOT/pages"),
        Map.entry("spring-batch", "docs/modules/ROOT/pages"),
        Map.entry("spring-integration", "src/reference/antora/modules/ROOT/pages"),
        Map.entry("spring-kafka", "src/main/antora/modules/ROOT/pages"),
        Map.entry("spring-amqp", "src/reference/antora/modules/ROOT/pages"),
        Map.entry("spring-session", "docs/modules/ROOT/pages"),
        Map.entry("spring-graphql", "src/docs/antora/modules/ROOT/pages"),
        Map.entry("spring-ai", "docs/modules/ROOT/pages"),
        Map.entry("spring-shell", "docs/modules/ROOT/pages"),
        Map.entry("spring-vault", "docs/modules/ROOT/pages"),
        Map.entry("spring-ldap", "docs/modules/ROOT/pages"),
        Map.entry("spring-hateoas", "src/main/asciidoc"),
        Map.entry("spring-restdocs", "docs/modules/ROOT/pages"),
        Map.entry("spring-modulith", "src/docs/antora/modules/ROOT/pages"),
        Map.entry("spring-authorization-server", "docs/modules/ROOT/pages"),
        Map.entry("spring-statemachine", "docs/modules/ROOT/pages"),
        Map.entry("spring-ws", "docs/modules/ROOT/pages"),
        Map.entry("spring-pulsar", "docs/modules/ROOT/pages"),
        Map.entry("spring-retry", "docs/modules/ROOT/pages"),
        Map.entry("spring-credhub", "docs/modules/ROOT/pages"),
        Map.entry("spring-grpc", "docs/modules/ROOT/pages")
    );

    /**
     * Version-specific path overrides for projects where documentation structure changed.
     * Format: projectSlug -> VersionPathMapping(threshold, newPath, oldPath)
     */
    private static final Map<String, VersionPathMapping> VERSION_PATH_MAPPINGS = Map.of(
        "spring-boot", new VersionPathMapping(
            "4.0.0",
            "documentation/spring-boot-docs/src/docs/antora/modules",
            "spring-boot-project/spring-boot-docs/src/docs/antora/modules"
        )
    );

    /**
     * Tag prefixes by project (some use 'v' prefix, others don't).
     */
    private static final Map<String, String> TAG_PREFIXES = Map.ofEntries(
        Map.entry("spring-boot", "v"),
        Map.entry("spring-framework", "v"),
        Map.entry("spring-security", ""),
        Map.entry("spring-data-commons", ""),
        Map.entry("spring-data-jpa", ""),
        Map.entry("spring-data-mongodb", ""),
        Map.entry("spring-data-redis", ""),
        Map.entry("spring-batch", ""),
        Map.entry("spring-integration", "v"),
        Map.entry("spring-kafka", "v"),
        Map.entry("spring-amqp", "v"),
        Map.entry("spring-session", ""),
        Map.entry("spring-graphql", "v"),
        Map.entry("spring-ai", "v"),
        Map.entry("spring-shell", "v"),
        Map.entry("spring-vault", "v"),
        Map.entry("spring-ldap", ""),
        Map.entry("spring-hateoas", ""),
        Map.entry("spring-restdocs", "v"),
        Map.entry("spring-modulith", ""),
        Map.entry("spring-authorization-server", ""),
        Map.entry("spring-statemachine", ""),
        Map.entry("spring-ws", ""),
        Map.entry("spring-pulsar", ""),
        Map.entry("spring-retry", "v"),
        Map.entry("spring-credhub", ""),
        Map.entry("spring-grpc", "v")
    );

    /**
     * Code examples paths for projects that have example code.
     */
    private static final Map<String, String> CODE_EXAMPLE_PATHS = Map.ofEntries(
        Map.entry("spring-boot", "documentation/spring-boot-docs/src/main/java/org/springframework/boot/docs"),
        Map.entry("spring-security", "docs/src/main/java"),
        Map.entry("spring-framework", "framework-docs/src/main/java"),
        Map.entry("spring-data-commons", "src/main/java"),
        Map.entry("spring-batch", "spring-batch-docs/src/main/java")
    );

    /**
     * Get the correct documentation path for a specific project version.
     *
     * @param projectSlug the project slug (e.g., "spring-boot")
     * @param version the version string (e.g., "3.5.7")
     * @return the documentation path for this version
     */
    public String getDocumentationPath(String projectSlug, String version) {
        // First check configuration for override
        if (gitHubProperties.getDocumentation().getPaths().containsKey(projectSlug)) {
            String configPath = gitHubProperties.getDocumentation().getPaths().get(projectSlug);

            // Check if there's a version-specific override in config
            GitHubProperties.VersionPathConfig versionConfig =
                gitHubProperties.getDocumentation().getVersionPaths().get(projectSlug);

            if (versionConfig != null && versionConfig.getThreshold() != null) {
                if (compareVersions(version, versionConfig.getThreshold()) < 0) {
                    return versionConfig.getOldPath() != null ? versionConfig.getOldPath() : configPath;
                }
                return versionConfig.getNewPath() != null ? versionConfig.getNewPath() : configPath;
            }

            return configPath;
        }

        // Fall back to static mappings
        VersionPathMapping mapping = VERSION_PATH_MAPPINGS.get(projectSlug);
        if (mapping != null) {
            if (compareVersions(version, mapping.threshold()) < 0) {
                return mapping.oldPath();
            }
            return mapping.newPath();
        }

        return DEFAULT_DOC_PATHS.getOrDefault(projectSlug, "docs/modules/ROOT/pages");
    }

    /**
     * Get the Git tag for a specific version.
     *
     * @param projectSlug the project slug
     * @param version the version string
     * @return the Git tag (e.g., "v3.5.7" or "6.5.7")
     */
    public String getGitTag(String projectSlug, String version) {
        // Check configuration first
        String configPrefix = gitHubProperties.getDocumentation().getTagPrefixes().get(projectSlug);
        if (configPrefix != null) {
            return configPrefix + version;
        }

        // Fall back to static mapping
        String prefix = TAG_PREFIXES.getOrDefault(projectSlug, "v");
        return prefix + version;
    }

    /**
     * Get the code examples path for a project.
     *
     * @param projectSlug the project slug
     * @return the code examples path, or null if not available
     */
    public String getCodeExamplesPath(String projectSlug) {
        return CODE_EXAMPLE_PATHS.get(projectSlug);
    }

    /**
     * Discover all AsciiDoc documentation files for a project version.
     *
     * @param projectSlug the project slug
     * @param version the version string
     * @return list of discovered documentation files
     */
    public List<DocumentationFile> discoverDocumentation(String projectSlug, String version) {
        String tag = getGitTag(projectSlug, version);
        String basePath = getDocumentationPath(projectSlug, version);
        String repo = gitHubProperties.getApi().getOrganization() + "/" + projectSlug;

        log.info("Discovering documentation for {} version {} at tag {} in path {}",
                 projectSlug, version, tag, basePath);

        return discoverFilesAtRef(repo, tag, basePath, ".adoc");
    }

    /**
     * Discover all Java code example files for a project version.
     *
     * @param projectSlug the project slug
     * @param version the version string
     * @return list of discovered code example files
     */
    public List<DocumentationFile> discoverCodeExamples(String projectSlug, String version) {
        String codePath = getCodeExamplesPath(projectSlug);
        if (codePath == null) {
            log.debug("No code examples path configured for project: {}", projectSlug);
            return Collections.emptyList();
        }

        String tag = getGitTag(projectSlug, version);
        String repo = gitHubProperties.getApi().getOrganization() + "/" + projectSlug;

        log.info("Discovering code examples for {} version {} at tag {} in path {}",
                 projectSlug, version, tag, codePath);

        return discoverFilesAtRef(repo, tag, codePath, ".java");
    }

    /**
     * Discover files at a specific Git reference (branch/tag) matching the given extension.
     *
     * @param repo the repository (org/repo format)
     * @param ref the Git reference (tag or branch)
     * @param path the base path to search
     * @param extension the file extension to filter (e.g., ".adoc")
     * @return list of discovered files
     */
    private List<DocumentationFile> discoverFilesAtRef(String repo, String ref, String path, String extension) {
        // First try to use the Git Trees API for efficient discovery
        List<DocumentationFile> files = discoverViaTreesApi(repo, ref, path, extension);

        if (files.isEmpty()) {
            // Fallback to Contents API for smaller directories
            log.debug("Trees API returned no results, falling back to Contents API");
            files = discoverViaContentsApi(repo, ref, path, extension);
        }

        return files;
    }

    /**
     * Discover files using GitHub Git Trees API (more efficient for large repos).
     */
    private List<DocumentationFile> discoverViaTreesApi(String repo, String ref, String basePath, String extension) {
        String url = String.format("%s/repos/%s/git/trees/%s?recursive=1",
                                   gitHubProperties.getApi().getBaseUrl(), repo, ref);

        WebClient webClient = buildWebClient();

        try {
            String jsonResponse = webClient.get()
                .uri(url)
                .retrieve()
                .bodyToMono(String.class)
                .timeout(REQUEST_TIMEOUT)
                .onErrorResume(error -> {
                    log.error("Error fetching tree for {}: {}", repo, error.getMessage());
                    return Mono.just("{}");
                })
                .block();

            if (jsonResponse == null || jsonResponse.equals("{}")) {
                return Collections.emptyList();
            }

            JsonNode root = objectMapper.readTree(jsonResponse);
            JsonNode tree = root.get("tree");

            if (tree == null || !tree.isArray()) {
                return Collections.emptyList();
            }

            List<DocumentationFile> files = new ArrayList<>();

            for (JsonNode node : tree) {
                String nodePath = node.get("path").asText();
                String type = node.get("type").asText();

                // Filter by path prefix and extension
                if ("blob".equals(type) &&
                    nodePath.startsWith(basePath) &&
                    nodePath.endsWith(extension)) {

                    String relativePath = nodePath.substring(basePath.length());
                    if (relativePath.startsWith("/")) {
                        relativePath = relativePath.substring(1);
                    }

                    files.add(new DocumentationFile(
                        nodePath,
                        relativePath,
                        extractFileName(nodePath),
                        node.has("size") ? node.get("size").asLong() : 0
                    ));
                }
            }

            log.info("Discovered {} {} files in {}/{} at {}",
                     files.size(), extension, repo, basePath, ref);
            return files;

        } catch (Exception e) {
            log.error("Error discovering files via Trees API: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Discover files using GitHub Contents API (fallback for smaller directories).
     */
    private List<DocumentationFile> discoverViaContentsApi(String repo, String ref, String path, String extension) {
        String url = String.format("%s/repos/%s/contents/%s?ref=%s",
                                   gitHubProperties.getApi().getBaseUrl(), repo, path, ref);

        WebClient webClient = buildWebClient();

        try {
            String jsonResponse = webClient.get()
                .uri(url)
                .retrieve()
                .bodyToMono(String.class)
                .timeout(REQUEST_TIMEOUT)
                .onErrorResume(error -> {
                    log.error("Error fetching contents for {}: {}", path, error.getMessage());
                    return Mono.just("[]");
                })
                .block();

            if (jsonResponse == null || jsonResponse.equals("[]")) {
                return Collections.emptyList();
            }

            JsonNode contents = objectMapper.readTree(jsonResponse);
            List<DocumentationFile> files = new ArrayList<>();

            // Contents API may return array or single object
            if (contents.isArray()) {
                for (JsonNode item : contents) {
                    processContentsItem(item, files, repo, ref, path, extension);
                }
            }

            return files;

        } catch (Exception e) {
            log.error("Error discovering files via Contents API: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Process a single item from the Contents API response.
     */
    private void processContentsItem(JsonNode item, List<DocumentationFile> files,
                                     String repo, String ref, String basePath, String extension) {
        String type = item.get("type").asText();
        String itemPath = item.get("path").asText();
        String name = item.get("name").asText();

        if ("file".equals(type) && name.endsWith(extension)) {
            String relativePath = itemPath.substring(basePath.length());
            if (relativePath.startsWith("/")) {
                relativePath = relativePath.substring(1);
            }

            files.add(new DocumentationFile(
                itemPath,
                relativePath,
                name,
                item.has("size") ? item.get("size").asLong() : 0
            ));
        } else if ("dir".equals(type)) {
            // Recursively discover files in subdirectories
            List<DocumentationFile> subFiles = discoverViaContentsApi(repo, ref, itemPath, extension);
            files.addAll(subFiles);
        }
    }

    /**
     * Build raw content URL for a file.
     *
     * @param projectSlug the project slug
     * @param path the file path within the repository
     * @param ref the Git reference (tag or branch)
     * @return the raw content URL
     */
    public String buildRawContentUrl(String projectSlug, String path, String ref) {
        String org = gitHubProperties.getApi().getOrganization();
        return String.format("%s/%s/%s/%s/%s",
                            gitHubProperties.getApi().getRawUrl(), org, projectSlug, ref, path);
    }

    /**
     * Parse include-code:: directives from AsciiDoc content.
     *
     * @param adocContent the AsciiDoc content
     * @return list of referenced class names
     */
    public List<String> parseIncludeCodeReferences(String adocContent) {
        List<String> references = new ArrayList<>();
        Pattern pattern = Pattern.compile("include-code::([\\w.]+)\\[\\]");
        Matcher matcher = pattern.matcher(adocContent);

        while (matcher.find()) {
            references.add(matcher.group(1));
        }

        return references;
    }

    /**
     * Check if a project is supported for GitHub documentation discovery.
     *
     * @param projectSlug the project slug
     * @return true if the project is supported
     */
    public boolean isSupported(String projectSlug) {
        return DEFAULT_DOC_PATHS.containsKey(projectSlug) ||
               gitHubProperties.getDocumentation().getPaths().containsKey(projectSlug);
    }

    /**
     * Get list of supported projects.
     *
     * @return set of supported project slugs
     */
    public Set<String> getSupportedProjects() {
        Set<String> projects = new HashSet<>(DEFAULT_DOC_PATHS.keySet());
        projects.addAll(gitHubProperties.getDocumentation().getPaths().keySet());
        return projects;
    }

    /**
     * Build a WebClient with GitHub API headers.
     */
    private WebClient buildWebClient() {
        WebClient.Builder builder = webClientBuilder
            .defaultHeader("Accept", "application/vnd.github.v3+json")
            .defaultHeader("User-Agent", "Spring-MCP-Server/1.3.0");

        // Add authentication token if configured
        String token = gitHubProperties.getApi().getToken();
        if (token != null && !token.isBlank()) {
            builder.defaultHeader("Authorization", "Bearer " + token);
        }

        return builder.build();
    }

    /**
     * Extract filename from path.
     */
    private String extractFileName(String path) {
        int lastSlash = path.lastIndexOf('/');
        return lastSlash >= 0 ? path.substring(lastSlash + 1) : path;
    }

    /**
     * Compare two version strings.
     *
     * @param v1 first version
     * @param v2 second version
     * @return negative if v1 < v2, zero if equal, positive if v1 > v2
     */
    private int compareVersions(String v1, String v2) {
        // Remove common prefixes (v, V)
        v1 = v1.replaceFirst("^[vV]", "");
        v2 = v2.replaceFirst("^[vV]", "");

        // Split by dots and compare each part
        String[] parts1 = v1.split("[.\\-]");
        String[] parts2 = v2.split("[.\\-]");

        int maxLength = Math.max(parts1.length, parts2.length);

        for (int i = 0; i < maxLength; i++) {
            String part1 = i < parts1.length ? parts1[i] : "0";
            String part2 = i < parts2.length ? parts2[i] : "0";

            // Try numeric comparison first
            try {
                int num1 = Integer.parseInt(part1);
                int num2 = Integer.parseInt(part2);
                if (num1 != num2) {
                    return num1 - num2;
                }
            } catch (NumberFormatException e) {
                // Fall back to string comparison for qualifiers like RC, SNAPSHOT
                int cmp = part1.compareTo(part2);
                if (cmp != 0) {
                    return cmp;
                }
            }
        }

        return 0;
    }

    /**
     * Record to hold version path mapping information.
     */
    private record VersionPathMapping(String threshold, String newPath, String oldPath) {}

    /**
     * Record representing a discovered documentation file.
     */
    public record DocumentationFile(
        String fullPath,
        String relativePath,
        String fileName,
        long size
    ) {}
}
