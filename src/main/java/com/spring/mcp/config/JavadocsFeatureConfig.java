package com.spring.mcp.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for Javadocs Downloader feature.
 * Controls Javadoc crawling, parsing, and storage settings.
 *
 * @author Spring MCP Server
 * @version 1.4.2
 * @since 2025-12-08
 */
@Configuration
@ConfigurationProperties(prefix = "mcp.features.javadocs")
@Getter
@Setter
public class JavadocsFeatureConfig {

    /**
     * Enable/disable the Javadocs feature (default: true)
     */
    private boolean enabled = true;

    /**
     * Sync configuration for scheduled Javadoc downloads
     */
    private SyncConfig sync = new SyncConfig();

    /**
     * Parser configuration for HTML parsing
     */
    private ParserConfig parser = new ParserConfig();

    /**
     * Cache configuration for fetched pages
     */
    private CacheConfig cache = new CacheConfig();

    /**
     * Search configuration
     */
    private SearchConfig search = new SearchConfig();

    /**
     * Attribution information for Javadocs
     */
    private AttributionConfig attribution = new AttributionConfig();

    @Getter
    @Setter
    public static class SyncConfig {
        /**
         * Enable/disable scheduled sync
         */
        private boolean enabled = true;

        /**
         * Maximum consecutive failures before auto-disabling sync for a project
         */
        private int maxFailures = 5;

        /**
         * Rate limit between HTTP requests in milliseconds
         */
        private long rateLimitMs = 500;

        /**
         * Batch size for processing classes
         */
        private int batchSize = 50;

        /**
         * Cron expression for scheduled sync (default: weekly on Sunday at 4 AM)
         */
        private String schedule = "0 0 4 * * SUN";

        /**
         * Enable sync on application startup
         */
        private boolean onStartup = false;
    }

    @Getter
    @Setter
    public static class ParserConfig {
        /**
         * HTTP connection timeout in milliseconds
         */
        private int connectionTimeoutMs = 10000;

        /**
         * HTTP read timeout in milliseconds
         */
        private int readTimeoutMs = 30000;

        /**
         * User agent for HTTP requests
         */
        private String userAgent = "Spring-MCP-Server/1.4.2 (Javadoc-Crawler)";

        /**
         * Maximum crawl depth (packages -> classes -> members)
         */
        private int maxDepth = 3;

        /**
         * Maximum classes to process per package
         */
        private int maxClassesPerPackage = 500;

        /**
         * Maximum methods to process per class
         */
        private int maxMethodsPerClass = 200;

        /**
         * Maximum retry attempts for failed requests
         */
        private int maxRetries = 3;

        /**
         * Initial retry delay in milliseconds
         */
        private long retryDelayMs = 1000;
    }

    @Getter
    @Setter
    public static class CacheConfig {
        /**
         * Enable in-memory caching of fetched pages
         */
        private boolean enabled = true;

        /**
         * Cache TTL in seconds
         */
        private int ttlSeconds = 3600;

        /**
         * Maximum cached pages
         */
        private int maxEntries = 1000;
    }

    @Getter
    @Setter
    public static class SearchConfig {
        /**
         * Default search limit
         */
        private int defaultLimit = 10;

        /**
         * Maximum search limit
         */
        private int maxLimit = 50;

        /**
         * Enable full-text search
         */
        private boolean fullTextSearch = true;

        /**
         * Include method documentation in search results
         */
        private boolean includeMethods = true;

        /**
         * Include field documentation in search results
         */
        private boolean includeFields = false;
    }

    @Getter
    @Setter
    public static class AttributionConfig {
        /**
         * Attribution notice for Javadocs
         */
        private String notice = "Javadoc content sourced from Spring projects. See individual project licenses.";

        /**
         * Base URL for Spring docs
         */
        private String baseUrl = "https://docs.spring.io";
    }
}
