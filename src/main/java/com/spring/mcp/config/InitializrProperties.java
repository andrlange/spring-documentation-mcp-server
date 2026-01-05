package com.spring.mcp.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for Spring Boot Initializr integration.
 *
 * <p>Allows configuration of the Initializr API endpoint, caching behavior,
 * and default project settings for AI-assisted project generation.</p>
 *
 * <h3>Configuration Example:</h3>
 * <pre>{@code
 * mcp:
 *   features:
 *     initializr:
 *       enabled: true
 *       base-url: https://start.spring.io
 *       cache:
 *         enabled: true
 *       defaults:
 *         boot-version: "3.5.8"
 *         java-version: "21"
 * }</pre>
 *
 * @author Spring MCP Server
 * @version 1.4.0
 * @since 2025-12-06
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "mcp.features.initializr")
public class InitializrProperties {

    /**
     * Enable or disable Initializr integration.
     * Default: true
     */
    private boolean enabled = true;

    /**
     * Base URL for Spring Initializr API.
     * Default: https://start.spring.io
     * Can be changed to self-hosted instance.
     */
    private String baseUrl = "https://start.spring.io";

    /**
     * Cache configuration for Initializr metadata.
     */
    private Cache cache = new Cache();

    /**
     * Default values for project generation.
     */
    private Defaults defaults = new Defaults();

    /**
     * API request configuration.
     */
    private Api api = new Api();

    @Data
    public static class Cache {
        /**
         * Enable caching of Initializr metadata.
         * Default: true
         */
        private boolean enabled = true;

        // Note: TTL and max-size are configured in CacheConfig.java
        // for better performance with programmatic Caffeine configuration
    }

    @Data
    public static class Defaults {
        /**
         * Default Spring Boot version.
         * Default: "3.5.9" (current stable)
         */
        private String bootVersion = "3.5.9";

        /**
         * Default Java version.
         * Default: "25" (LTS)
         */
        private String javaVersion = "25";

        /**
         * Default language.
         * Options: java, kotlin, groovy
         * Default: java
         */
        private String language = "java";

        /**
         * Default packaging type.
         * Options: jar, war
         * Default: jar
         */
        private String packaging = "jar";

        /**
         * Default build type.
         * Options: gradle-project, gradle-project-kotlin, maven-project
         * Default: gradle-project
         */
        private String buildType = "gradle-project";

        /**
         * Default group ID for generated projects.
         * Default: com.example
         */
        private String groupId = "com.example";

        /**
         * Default artifact ID for generated projects.
         * Default: demo
         */
        private String artifactId = "demo";

        /**
         * Default project name.
         * Default: demo
         */
        private String name = "demo";

        /**
         * Default project description.
         * Default: Demo project for Spring Boot
         */
        private String description = "Demo project for Spring Boot";

        /**
         * Default package name (if not specified, derived from groupId + artifactId).
         * Default: empty (auto-derived)
         */
        private String packageName = "";
    }

    @Data
    public static class Api {
        /**
         * Request timeout in milliseconds.
         * Default: 30000 (30 seconds)
         */
        private int timeout = 30000;

        /**
         * Maximum retries for failed requests.
         * Default: 3
         */
        private int maxRetries = 3;

        /**
         * Delay between retries in milliseconds.
         * Default: 1000 (1 second)
         */
        private int retryDelay = 1000;

        /**
         * User-Agent header for API requests.
         * Default: Spring-MCP-Server/1.4.0
         */
        private String userAgent = "Spring-MCP-Server/1.4.0";
    }

    /**
     * Get the metadata endpoint URL.
     * @return full URL for /metadata/client endpoint
     */
    public String getMetadataUrl() {
        return baseUrl + "/metadata/client";
    }

    /**
     * Get the dependencies endpoint URL with optional version filter.
     * @param bootVersion optional Spring Boot version to filter dependencies
     * @return full URL for /dependencies endpoint
     */
    public String getDependenciesUrl(String bootVersion) {
        if (bootVersion != null && !bootVersion.isBlank()) {
            return baseUrl + "/dependencies?bootVersion=" + bootVersion;
        }
        return baseUrl + "/dependencies";
    }

    /**
     * Get the pom.xml generation endpoint URL.
     * @return full URL for /pom.xml endpoint
     */
    public String getPomUrl() {
        return baseUrl + "/pom.xml";
    }

    /**
     * Get the build.gradle generation endpoint URL.
     * @return full URL for /build.gradle endpoint
     */
    public String getBuildGradleUrl() {
        return baseUrl + "/build.gradle";
    }

    /**
     * Get the project ZIP generation endpoint URL.
     * @return full URL for /starter.zip endpoint
     */
    public String getStarterZipUrl() {
        return baseUrl + "/starter.zip";
    }
}
