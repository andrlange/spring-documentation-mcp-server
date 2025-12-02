package com.spring.mcp.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuration properties for GitHub-based documentation fetching.
 *
 * @author Spring MCP Server
 * @version 1.0.0
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "github")
public class GitHubProperties {

    /**
     * GitHub API configuration
     */
    private Api api = new Api();

    /**
     * Documentation configuration
     */
    private Documentation documentation = new Documentation();

    @Data
    public static class Api {
        /**
         * GitHub API base URL
         */
        private String baseUrl = "https://api.github.com";

        /**
         * GitHub raw content URL
         */
        private String rawUrl = "https://raw.githubusercontent.com";

        /**
         * Optional GitHub token for increased rate limit (5000 req/hour with token)
         */
        private String token = "";

        /**
         * GitHub organization for Spring projects
         */
        private String organization = "spring-projects";

        /**
         * Request timeout in milliseconds
         */
        private int timeout = 30000;

        /**
         * Maximum retries for failed requests
         */
        private int maxRetries = 3;

        /**
         * Delay between retries in milliseconds
         */
        private int retryDelay = 1000;
    }

    @Data
    public static class Documentation {
        /**
         * Default documentation paths for each project (for main/latest branch)
         */
        private Map<String, String> paths = new HashMap<>();

        /**
         * Version-specific path overrides (when structure changed between versions)
         */
        private Map<String, VersionPathConfig> versionPaths = new HashMap<>();

        /**
         * Tag prefixes per project (some use 'v', others don't)
         */
        private Map<String, String> tagPrefixes = new HashMap<>();

        /**
         * Enable GitHub-based documentation sync
         */
        private boolean enabled = true;

        /**
         * Sync schedule (cron expression)
         */
        private String schedule = "0 0 4 * * ?"; // Daily at 4 AM

        /**
         * Maximum number of files to fetch per sync
         */
        private int maxFilesPerSync = 500;

        /**
         * File extensions to include in documentation discovery
         */
        private String[] fileExtensions = {".adoc"};
    }

    @Data
    public static class VersionPathConfig {
        /**
         * Version threshold - versions >= this use new path
         */
        private String threshold;

        /**
         * Old path for versions < threshold
         */
        private String oldPath;

        /**
         * New path for versions >= threshold (or null to use default)
         */
        private String newPath;
    }
}
