package com.spring.mcp.service.sync;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.spring.mcp.model.entity.CodeExample;
import com.spring.mcp.model.entity.ProjectVersion;
import com.spring.mcp.model.entity.SpringProject;
import com.spring.mcp.repository.CodeExampleRepository;
import com.spring.mcp.repository.ProjectVersionRepository;
import com.spring.mcp.repository.SpringProjectRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.*;

/**
 * Service to fetch code example repositories from Spring GitHub organizations.
 *
 * Uses GitHub API to discover sample/example repositories for all Spring projects.
 * This provides comprehensive coverage of official Spring samples across all projects.
 *
 * @author Spring MCP Server
 * @version 1.0
 * @since 2025-01-12
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GitHubSamplesFetchService {

    private final CodeExampleRepository codeExampleRepository;
    private final ProjectVersionRepository projectVersionRepository;
    private final SpringProjectRepository springProjectRepository;
    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper objectMapper;

    private static final String GITHUB_API_BASE = "https://api.github.com";
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(30);

    /**
     * Spring GitHub organizations to search for sample repositories.
     */
    private static final List<String> SPRING_ORGS = Arrays.asList(
        "spring-projects",
        "spring-cloud",
        "spring-cloud-samples",
        "spring-guides"
    );

    /**
     * Sync code examples from GitHub sample repositories.
     *
     * @return number of code examples synchronized
     */
    public int syncGitHubSamples() {
        log.info("Starting GitHub samples sync from Spring organizations");
        int totalExamples = 0;

        for (String org : SPRING_ORGS) {
            try {
                int samplesFromOrg = syncOrganizationSamples(org);
                totalExamples += samplesFromOrg;
                log.debug("Synced {} samples from organization: {}", samplesFromOrg, org);
            } catch (Exception e) {
                log.error("Error syncing samples from organization: {} - {}", org, e.getMessage(), e);
            }
        }

        log.info("GitHub samples sync complete. Total examples: {}", totalExamples);
        return totalExamples;
    }

    /**
     * Sync sample repositories from a GitHub organization.
     *
     * @param orgName the GitHub organization name
     * @return number of samples synced
     */
    private int syncOrganizationSamples(String orgName) {
        log.debug("Fetching repositories from organization: {}", orgName);

        List<GitHubRepo> repos = fetchOrganizationRepos(orgName);
        log.debug("Found {} repositories in {}", repos.size(), orgName);

        int savedCount = 0;

        for (GitHubRepo repo : repos) {
            // Filter for sample/example repositories
            if (isSampleRepository(repo)) {
                try {
                    // Determine which Spring project this sample belongs to
                    Optional<String> projectSlugOpt = determineProjectSlug(repo.name, orgName);

                    if (projectSlugOpt.isEmpty()) {
                        log.debug("Could not determine project for repository: {}", repo.name);
                        continue;
                    }

                    String projectSlug = projectSlugOpt.get();

                    // Find or create the project version
                    Optional<ProjectVersion> versionOpt = findOrCreateProjectVersion(projectSlug);

                    if (versionOpt.isEmpty()) {
                        log.warn("No version found for project: {}", projectSlug);
                        continue;
                    }

                    ProjectVersion version = versionOpt.get();

                    // Check if example already exists
                    if (codeExampleRepository.existsByVersionAndSourceUrl(version, repo.htmlUrl)) {
                        log.debug("Sample already exists: {}", repo.name);
                        continue;
                    }

                    // Create code example
                    CodeExample example = CodeExample.builder()
                        .version(version)
                        .title(formatRepoName(repo.name))
                        .description(repo.description != null ? repo.description : formatRepoName(repo.name))
                        .sourceUrl(repo.htmlUrl)
                        .codeSnippet("GitHub repository: " + repo.htmlUrl)
                        .language("java")
                        .category("Sample Repository")
                        .tags(new String[]{"github", "sample", orgName, projectSlug})
                        .build();

                    codeExampleRepository.save(example);
                    savedCount++;
                    log.debug("Saved sample repository: {}", repo.name);

                } catch (Exception e) {
                    log.error("Error saving sample repository {}: {}", repo.name, e.getMessage());
                }
            }
        }

        return savedCount;
    }

    /**
     * Fetch all repositories from a GitHub organization.
     *
     * @param orgName the organization name
     * @return list of repositories
     */
    private List<GitHubRepo> fetchOrganizationRepos(String orgName) {
        String url = String.format("%s/orgs/%s/repos?type=public&per_page=100", GITHUB_API_BASE, orgName);

        WebClient webClient = webClientBuilder
            .defaultHeader("Accept", "application/vnd.github.v3+json")
            .defaultHeader("User-Agent", "Spring-MCP-Server/1.0.0")
            .build();

        try {
            String jsonResponse = webClient.get()
                .uri(url)
                .retrieve()
                .bodyToMono(String.class)
                .timeout(REQUEST_TIMEOUT)
                .onErrorResume(error -> {
                    log.error("Error fetching repos from {}: {}", orgName, error.getMessage());
                    return Mono.just("[]");
                })
                .block();

            if (jsonResponse == null || jsonResponse.equals("[]")) {
                return Collections.emptyList();
            }

            // Parse JSON response
            JsonNode reposNode = objectMapper.readTree(jsonResponse);
            List<GitHubRepo> repos = new ArrayList<>();

            for (JsonNode repoNode : reposNode) {
                String name = repoNode.get("name").asText();
                String htmlUrl = repoNode.get("html_url").asText();
                String description = repoNode.has("description") && !repoNode.get("description").isNull()
                    ? repoNode.get("description").asText()
                    : "";

                repos.add(new GitHubRepo(name, htmlUrl, description));
            }

            return repos;

        } catch (Exception e) {
            log.error("Error parsing GitHub API response: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Determine if a repository is a sample/example repository.
     *
     * @param repo the repository
     * @return true if it's a sample repository
     */
    private boolean isSampleRepository(GitHubRepo repo) {
        String nameLower = repo.name.toLowerCase();
        String descLower = repo.description != null ? repo.description.toLowerCase() : "";

        return nameLower.contains("sample") ||
               nameLower.contains("example") ||
               nameLower.contains("demo") ||
               descLower.contains("sample") ||
               descLower.contains("example") ||
               descLower.contains("demo");
    }

    /**
     * Determine which Spring project a repository belongs to based on its name.
     *
     * @param repoName the repository name
     * @param orgName the organization name
     * @return optional project slug
     */
    private Optional<String> determineProjectSlug(String repoName, String orgName) {
        String nameLower = repoName.toLowerCase();

        // Direct project name mapping
        if (nameLower.contains("spring-boot")) return Optional.of("spring-boot");
        if (nameLower.contains("spring-cloud")) return Optional.of("spring-cloud");
        if (nameLower.contains("spring-security")) return Optional.of("spring-security");
        if (nameLower.contains("spring-data")) return Optional.of("spring-data");
        if (nameLower.contains("spring-batch")) return Optional.of("spring-batch");
        if (nameLower.contains("spring-integration")) return Optional.of("spring-integration");
        if (nameLower.contains("spring-webflux")) return Optional.of("spring-framework");
        if (nameLower.contains("spring-mvc")) return Optional.of("spring-framework");
        if (nameLower.contains("spring-amqp")) return Optional.of("spring-amqp");
        if (nameLower.contains("spring-kafka")) return Optional.of("spring-kafka");

        // Organization-based mapping
        if (orgName.equals("spring-cloud") || orgName.equals("spring-cloud-samples")) {
            return Optional.of("spring-cloud");
        }

        // Default to spring-boot for generic samples
        if (nameLower.contains("spring")) {
            return Optional.of("spring-boot");
        }

        return Optional.empty();
    }

    /**
     * Find or create a project version for the given project slug.
     *
     * @param projectSlug the project slug
     * @return optional project version
     */
    private Optional<ProjectVersion> findOrCreateProjectVersion(String projectSlug) {
        Optional<SpringProject> projectOpt = springProjectRepository.findBySlug(projectSlug);

        if (projectOpt.isEmpty()) {
            log.warn("No project found with slug: {}", projectSlug);
            return Optional.empty();
        }

        SpringProject project = projectOpt.get();

        // Try to find the latest version
        // Use the safer method that handles multiple "latest" versions due to potential data issues
        Optional<ProjectVersion> latestVersion = projectVersionRepository.findTopByProjectAndIsLatestTrueOrderByCreatedAtDesc(project);

        // If no latest version, get the most recently created one
        if (latestVersion.isEmpty()) {
            latestVersion = projectVersionRepository.findFirstByProjectOrderByCreatedAtDesc(project);
        }

        return latestVersion;
    }

    /**
     * Format repository name into a human-readable title.
     *
     * @param repoName the repository name
     * @return formatted title
     */
    private String formatRepoName(String repoName) {
        // Replace hyphens and underscores with spaces
        String formatted = repoName.replace("-", " ").replace("_", " ");

        // Capitalize first letter of each word
        String[] words = formatted.split(" ");
        StringBuilder result = new StringBuilder();

        for (String word : words) {
            if (!word.isEmpty()) {
                result.append(Character.toUpperCase(word.charAt(0)))
                      .append(word.substring(1))
                      .append(" ");
            }
        }

        return result.toString().trim();
    }

    /**
     * DTO for GitHub repository information.
     */
    private static class GitHubRepo {
        String name;
        String htmlUrl;
        String description;

        GitHubRepo(String name, String htmlUrl, String description) {
            this.name = name;
            this.htmlUrl = htmlUrl;
            this.description = description;
        }
    }
}
