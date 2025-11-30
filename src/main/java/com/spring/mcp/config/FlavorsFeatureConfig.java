package com.spring.mcp.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Configuration properties for Flavors feature toggle.
 * Controls company guidelines, architecture patterns, compliance rules,
 * agent configurations, and project initialization templates.
 *
 * @author Spring MCP Server
 * @version 1.2.0
 * @since 2025-11-30
 */
@Configuration
@ConfigurationProperties(prefix = "mcp.features.flavors")
@Getter
@Setter
public class FlavorsFeatureConfig {

    /**
     * Enable/disable the Flavors feature (default: true)
     */
    private boolean enabled = true;

    /**
     * List of enabled categories
     */
    private List<String> categories = List.of(
        "ARCHITECTURE",
        "COMPLIANCE",
        "AGENTS",
        "INITIALIZATION",
        "GENERAL"
    );

    /**
     * Search configuration
     */
    private SearchConfig search = new SearchConfig();

    /**
     * Editor configuration
     */
    private EditorConfig editor = new EditorConfig();

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
    }

    @Getter
    @Setter
    public static class EditorConfig {
        /**
         * Enable autosave
         */
        private boolean autosave = true;

        /**
         * Autosave delay in milliseconds
         */
        private int autosaveDelay = 5000;

        /**
         * Enable spell checker
         */
        private boolean spellChecker = true;

        /**
         * Minimum editor height
         */
        private String minHeight = "400px";

        /**
         * Maximum editor height
         */
        private String maxHeight = "600px";
    }
}
