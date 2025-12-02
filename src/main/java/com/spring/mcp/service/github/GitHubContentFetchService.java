package com.spring.mcp.service.github;

import com.spring.mcp.config.GitHubProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for fetching raw content from GitHub repositories.
 *
 * This service handles fetching AsciiDoc documentation files and Java source code
 * from Spring GitHub repositories with rate limiting and retry support.
 *
 * @author Spring MCP Server
 * @version 1.0.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GitHubContentFetchService {

    private final GitHubProperties gitHubProperties;
    private final WebClient.Builder webClientBuilder;

    /**
     * Simple in-memory cache with TTL.
     */
    private final Map<String, CachedContent> cache = new ConcurrentHashMap<>();
    private static final Duration CACHE_TTL = Duration.ofMinutes(30);

    /**
     * Fetch raw content from GitHub.
     *
     * @param repo the repository in "org/repo" format
     * @param path the file path within the repository
     * @param ref the Git reference (branch or tag)
     * @return the file content, or empty if not found
     */
    public Optional<String> fetchRawContent(String repo, String path, String ref) {
        String url = buildRawUrl(repo, path, ref);
        String cacheKey = url;

        // Check cache first
        CachedContent cached = cache.get(cacheKey);
        if (cached != null && !cached.isExpired()) {
            log.debug("Cache hit for: {}", path);
            return Optional.of(cached.content());
        }

        log.debug("Fetching content from: {}", url);

        try {
            String content = buildRawWebClient()
                .get()
                .uri(url)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, response -> {
                    if (response.statusCode().value() == 404) {
                        return Mono.empty();
                    }
                    return response.createException();
                })
                .bodyToMono(String.class)
                .timeout(Duration.ofMillis(gitHubProperties.getApi().getTimeout()))
                .retryWhen(createRetrySpec())
                .block();

            if (content != null) {
                // Cache successful fetches
                cache.put(cacheKey, new CachedContent(content, System.currentTimeMillis()));
                return Optional.of(content);
            }

            return Optional.empty();

        } catch (WebClientResponseException.NotFound e) {
            log.debug("File not found: {}", path);
            return Optional.empty();
        } catch (Exception e) {
            log.error("Error fetching content from {}: {}", url, e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Fetch content for a specific project, version, and file path.
     *
     * @param projectSlug the project slug (e.g., "spring-boot")
     * @param version the version (e.g., "3.5.7")
     * @param filePath the file path within the documentation directory
     * @param discoveryService the discovery service for path resolution
     * @return the file content, or empty if not found
     */
    public Optional<String> fetchDocumentation(String projectSlug, String version, String filePath,
                                               GitHubDocumentationDiscoveryService discoveryService) {
        String tag = discoveryService.getGitTag(projectSlug, version);
        String repo = gitHubProperties.getApi().getOrganization() + "/" + projectSlug;

        return fetchRawContent(repo, filePath, tag);
    }

    /**
     * Fetch multiple files in parallel with rate limiting.
     *
     * @param requests list of fetch requests
     * @return list of fetch results
     */
    public List<FetchResult> fetchBatch(List<FetchRequest> requests) {
        if (requests.isEmpty()) {
            return List.of();
        }

        log.info("Batch fetching {} files", requests.size());

        return Flux.fromIterable(requests)
            .delayElements(Duration.ofMillis(100)) // Rate limiting: 10 requests per second
            .flatMap(request -> {
                return Mono.fromCallable(() -> {
                    Optional<String> content = fetchRawContent(request.repo(), request.path(), request.ref());
                    return new FetchResult(request, content.orElse(null), content.isPresent());
                })
                .subscribeOn(reactor.core.scheduler.Schedulers.boundedElastic()) // Allow blocking in this context
                .onErrorResume(e -> {
                    log.error("Error in batch fetch for {}: {}", request.path(), e.getMessage());
                    return Mono.just(new FetchResult(request, null, false));
                });
            }, 5) // Max 5 concurrent requests
            .collectList()
            .block(Duration.ofMinutes(10));
    }

    /**
     * Fetch all documentation files discovered for a project version.
     *
     * @param projectSlug the project slug
     * @param version the version
     * @param discoveryService the discovery service
     * @return map of file path to content
     */
    public Map<String, String> fetchAllDocumentation(String projectSlug, String version,
                                                      GitHubDocumentationDiscoveryService discoveryService) {
        List<GitHubDocumentationDiscoveryService.DocumentationFile> files =
            discoveryService.discoverDocumentation(projectSlug, version);

        if (files.isEmpty()) {
            log.warn("No documentation files discovered for {} version {}", projectSlug, version);
            return Map.of();
        }

        String tag = discoveryService.getGitTag(projectSlug, version);
        String repo = gitHubProperties.getApi().getOrganization() + "/" + projectSlug;

        List<FetchRequest> requests = files.stream()
            .map(file -> new FetchRequest(repo, file.fullPath(), tag))
            .toList();

        List<FetchResult> results = fetchBatch(requests);

        Map<String, String> contentMap = new ConcurrentHashMap<>();
        for (FetchResult result : results) {
            if (result.success() && result.content() != null) {
                contentMap.put(result.request().path(), result.content());
            }
        }

        log.info("Successfully fetched {}/{} documentation files for {} version {}",
                 contentMap.size(), files.size(), projectSlug, version);

        return contentMap;
    }

    /**
     * Fetch all code examples discovered for a project version.
     *
     * @param projectSlug the project slug
     * @param version the version
     * @param discoveryService the discovery service
     * @return map of file path to content
     */
    public Map<String, String> fetchAllCodeExamples(String projectSlug, String version,
                                                     GitHubDocumentationDiscoveryService discoveryService) {
        List<GitHubDocumentationDiscoveryService.DocumentationFile> files =
            discoveryService.discoverCodeExamples(projectSlug, version);

        if (files.isEmpty()) {
            log.debug("No code example files discovered for {} version {}", projectSlug, version);
            return Map.of();
        }

        String tag = discoveryService.getGitTag(projectSlug, version);
        String repo = gitHubProperties.getApi().getOrganization() + "/" + projectSlug;

        List<FetchRequest> requests = files.stream()
            .map(file -> new FetchRequest(repo, file.fullPath(), tag))
            .toList();

        List<FetchResult> results = fetchBatch(requests);

        Map<String, String> contentMap = new ConcurrentHashMap<>();
        for (FetchResult result : results) {
            if (result.success() && result.content() != null) {
                contentMap.put(result.request().path(), result.content());
            }
        }

        log.info("Successfully fetched {}/{} code example files for {} version {}",
                 contentMap.size(), files.size(), projectSlug, version);

        return contentMap;
    }

    /**
     * Clear the content cache.
     */
    public void clearCache() {
        cache.clear();
        log.info("Content cache cleared");
    }

    /**
     * Remove expired entries from the cache.
     */
    public void cleanupCache() {
        long now = System.currentTimeMillis();
        int beforeSize = cache.size();

        cache.entrySet().removeIf(entry -> entry.getValue().isExpired());

        int removed = beforeSize - cache.size();
        if (removed > 0) {
            log.debug("Removed {} expired cache entries", removed);
        }
    }

    /**
     * Get cache statistics.
     *
     * @return cache statistics
     */
    public CacheStats getCacheStats() {
        long now = System.currentTimeMillis();
        int total = cache.size();
        long valid = cache.values().stream()
            .filter(c -> !c.isExpired())
            .count();

        return new CacheStats(total, (int) valid, (int) (total - valid));
    }

    /**
     * Build raw content URL.
     */
    private String buildRawUrl(String repo, String path, String ref) {
        return String.format("%s/%s/%s/%s",
                            gitHubProperties.getApi().getRawUrl(), repo, ref, path);
    }

    /**
     * Build WebClient for raw content fetching.
     */
    private WebClient buildRawWebClient() {
        WebClient.Builder builder = webClientBuilder
            .defaultHeader("Accept", "text/plain")
            .defaultHeader("User-Agent", "Spring-MCP-Server/1.3.0");

        // Add authentication token if configured (increases rate limit)
        String token = gitHubProperties.getApi().getToken();
        if (token != null && !token.isBlank()) {
            builder.defaultHeader("Authorization", "Bearer " + token);
        }

        return builder.build();
    }

    /**
     * Create retry specification for failed requests.
     */
    private Retry createRetrySpec() {
        return Retry.backoff(
                gitHubProperties.getApi().getMaxRetries(),
                Duration.ofMillis(gitHubProperties.getApi().getRetryDelay())
            )
            .filter(throwable -> {
                // Retry on server errors and rate limiting
                if (throwable instanceof WebClientResponseException ex) {
                    return ex.getStatusCode().is5xxServerError() ||
                           ex.getStatusCode().value() == 429; // Too Many Requests
                }
                return false;
            })
            .doBeforeRetry(signal -> {
                log.debug("Retrying request, attempt {}", signal.totalRetries() + 1);
            });
    }

    /**
     * Record for fetch request.
     */
    public record FetchRequest(String repo, String path, String ref) {}

    /**
     * Record for fetch result.
     */
    public record FetchResult(FetchRequest request, String content, boolean success) {}

    /**
     * Record for cached content with timestamp.
     */
    private record CachedContent(String content, long timestamp) {
        boolean isExpired() {
            return System.currentTimeMillis() - timestamp > CACHE_TTL.toMillis();
        }
    }

    /**
     * Record for cache statistics.
     */
    public record CacheStats(int total, int valid, int expired) {}
}
