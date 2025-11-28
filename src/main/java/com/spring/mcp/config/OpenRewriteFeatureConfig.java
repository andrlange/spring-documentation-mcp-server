package com.spring.mcp.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for OpenRewrite feature toggle.
 * Controls whether the migration knowledge feature is enabled.
 */
@Configuration
@ConfigurationProperties(prefix = "mcp.features.openrewrite")
@Getter
@Setter
public class OpenRewriteFeatureConfig {

    /**
     * Enable/disable the OpenRewrite feature (default: true)
     */
    private boolean enabled = true;

    /**
     * Sync configuration
     */
    private SyncConfig sync = new SyncConfig();

    /**
     * GitHub configuration for recipe source
     */
    private GitHubConfig github = new GitHubConfig();

    /**
     * Attribution configuration
     */
    private AttributionConfig attribution = new AttributionConfig();

    @Getter
    @Setter
    public static class SyncConfig {
        /**
         * Enable automatic sync (default: true)
         */
        private boolean enabled = true;

        /**
         * Cron schedule for sync (default: every Sunday at 3 AM)
         */
        private String schedule = "0 0 3 * * SUN";
    }

    @Getter
    @Setter
    public static class GitHubConfig {
        /**
         * Base URL for OpenRewrite recipe repository
         */
        private String baseUrl = "https://raw.githubusercontent.com/openrewrite/rewrite-spring/main";

        /**
         * Repository owner
         */
        private String owner = "openrewrite";

        /**
         * Repository name
         */
        private String repo = "rewrite-spring";
    }

    @Getter
    @Setter
    public static class AttributionConfig {
        /**
         * Name of the source project
         */
        private String name = "OpenRewrite";

        /**
         * License name
         */
        private String license = "Moderne Source Available License";

        /**
         * URL to license information
         */
        private String licenseUrl = "https://docs.openrewrite.org/licensing/openrewrite-licensing";

        /**
         * URL to source repository
         */
        private String repository = "https://github.com/openrewrite/rewrite-spring";

        /**
         * Attribution notice
         */
        private String notice = "Recipe data sourced from OpenRewrite project. Not for commercial redistribution.";
    }
}
