package com.spring.mcp.service.javadoc;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.spring.mcp.config.JavadocsFeatureConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Service for fetching Javadoc HTML pages from remote servers.
 * Implements rate limiting, retries, and optional caching.
 */
@Service
@Slf4j
@ConditionalOnProperty(prefix = "mcp.features.javadocs", name = "enabled", havingValue = "true", matchIfMissing = true)
public class JavadocFetcherService {

    private final WebClient webClient;
    private final JavadocsFeatureConfig config;
    private final Cache<String, String> pageCache;

    public JavadocFetcherService(WebClient.Builder webClientBuilder, JavadocsFeatureConfig config) {
        this.config = config;

        // Build WebClient with configured timeouts
        this.webClient = webClientBuilder
                .defaultHeader(HttpHeaders.USER_AGENT, config.getParser().getUserAgent())
                .defaultHeader(HttpHeaders.ACCEPT, "text/html,application/xhtml+xml")
                .build();

        // Initialize cache if enabled
        if (config.getCache().isEnabled()) {
            this.pageCache = Caffeine.newBuilder()
                    .maximumSize(config.getCache().getMaxEntries())
                    .expireAfterWrite(config.getCache().getTtlSeconds(), TimeUnit.SECONDS)
                    .recordStats()
                    .build();
            log.info("Javadoc page cache initialized: maxEntries={}, ttl={}s",
                    config.getCache().getMaxEntries(), config.getCache().getTtlSeconds());
        } else {
            this.pageCache = null;
            log.info("Javadoc page cache disabled");
        }
    }

    /**
     * Fetch a page from the given URL.
     * Uses caching if enabled and applies rate limiting.
     *
     * @param url The URL to fetch
     * @return Mono containing the HTML content
     */
    public Mono<String> fetchPage(String url) {
        // Check cache first
        if (pageCache != null) {
            String cached = pageCache.getIfPresent(url);
            if (cached != null) {
                log.debug("Cache hit for: {}", url);
                return Mono.just(cached);
            }
        }

        log.debug("Fetching: {}", url);

        return Mono.delay(Duration.ofMillis(config.getSync().getRateLimitMs()))
                .then(webClient.get()
                        .uri(url)
                        .retrieve()
                        .onStatus(HttpStatusCode::is4xxClientError, response -> {
                            log.warn("Client error {} for URL: {}", response.statusCode(), url);
                            return Mono.error(new JavadocFetchException(
                                    "Client error: " + response.statusCode(), url));
                        })
                        .onStatus(HttpStatusCode::is5xxServerError, response -> {
                            log.warn("Server error {} for URL: {}", response.statusCode(), url);
                            return Mono.error(new JavadocFetchException(
                                    "Server error: " + response.statusCode(), url));
                        })
                        .bodyToMono(String.class)
                        .timeout(Duration.ofMillis(config.getParser().getReadTimeoutMs()))
                        .doOnSuccess(content -> {
                            if (pageCache != null && content != null) {
                                pageCache.put(url, content);
                            }
                        })
                        .retryWhen(Retry.backoff(
                                config.getParser().getMaxRetries(),
                                Duration.ofMillis(config.getParser().getRetryDelayMs())
                        ).filter(this::isRetryable))
                        .doOnError(e -> log.error("Failed to fetch {}: {}", url, e.getMessage())));
    }

    /**
     * Fetch a page synchronously (blocking).
     *
     * @param url The URL to fetch
     * @return Optional containing the HTML content, empty if fetch failed
     */
    public Optional<String> fetchPageBlocking(String url) {
        try {
            return Optional.ofNullable(fetchPage(url).block());
        } catch (Exception e) {
            log.error("Failed to fetch {} (blocking): {}", url, e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Check if a URL exists (returns 200).
     *
     * @param url The URL to check
     * @return true if the URL returns 200
     */
    public Mono<Boolean> exists(String url) {
        return webClient.head()
                .uri(url)
                .exchangeToMono(response -> Mono.just(response.statusCode().is2xxSuccessful()))
                .onErrorReturn(false);
    }

    /**
     * Fetch the package-list or element-list file.
     *
     * @param baseUrl Base Javadoc URL
     * @return Mono containing the package list content
     */
    public Mono<String> fetchPackageList(String baseUrl) {
        String normalizedUrl = baseUrl.endsWith("/") ? baseUrl : baseUrl + "/";

        // Try element-list first (Java 10+), then fall back to package-list
        return fetchPage(normalizedUrl + "element-list")
                .onErrorResume(e -> {
                    log.debug("element-list not found, trying package-list");
                    return fetchPage(normalizedUrl + "package-list");
                })
                .doOnSuccess(content -> log.debug("Fetched package list from {}", baseUrl));
    }

    /**
     * Clear the page cache.
     */
    public void clearCache() {
        if (pageCache != null) {
            pageCache.invalidateAll();
            log.info("Javadoc page cache cleared");
        }
    }

    /**
     * Get cache statistics.
     */
    public String getCacheStats() {
        if (pageCache != null) {
            var stats = pageCache.stats();
            return String.format("hits=%d, misses=%d, hitRate=%.2f%%, size=%d",
                    stats.hitCount(), stats.missCount(), stats.hitRate() * 100, pageCache.estimatedSize());
        }
        return "Cache disabled";
    }

    /**
     * Determine if an exception is retryable.
     */
    private boolean isRetryable(Throwable t) {
        if (t instanceof JavadocFetchException jfe) {
            // Don't retry 404s
            return !jfe.getMessage().contains("404");
        }
        return true;
    }

    /**
     * Exception for Javadoc fetch failures.
     */
    public static class JavadocFetchException extends RuntimeException {
        private final String url;

        public JavadocFetchException(String message, String url) {
            super(message + " - URL: " + url);
            this.url = url;
        }

        public String getUrl() {
            return url;
        }
    }
}
