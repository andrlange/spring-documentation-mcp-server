package com.spring.mcp.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for Sync feature toggles.
 * Controls visibility and behavior of sync-related features in the admin UI.
 */
@Configuration
@ConfigurationProperties(prefix = "mcp.features.sync")
@Getter
@Setter
public class SyncFeatureConfig {

    /**
     * Configuration for the "Fix Documentation Versions" feature.
     * This is a one-time cleanup utility, disabled by default.
     */
    private FixVersionsConfig fixVersions = new FixVersionsConfig();

    @Getter
    @Setter
    public static class FixVersionsConfig {
        /**
         * Enable/disable the "Fix Documentation Versions" button in the sync UI.
         * Default: false (hidden) - only needed for one-time cleanup of placeholder versions.
         */
        private boolean enabled = false;
    }
}
