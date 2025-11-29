package com.spring.mcp.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for Language Evolution feature toggle.
 * Controls whether the Java/Kotlin language tracking feature is enabled.
 *
 * @author Spring MCP Server
 * @version 1.2.0
 * @since 2025-11-29
 */
@Configuration
@ConfigurationProperties(prefix = "mcp.features.language-evolution")
@Getter
@Setter
public class LanguageEvolutionFeatureConfig {

    /**
     * Enable/disable the Language Evolution feature (default: true)
     */
    private boolean enabled = true;

    /**
     * Sync configuration
     */
    private SyncConfig sync = new SyncConfig();

    /**
     * Source configuration for Java and Kotlin
     */
    private SourcesConfig sources = new SourcesConfig();

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
         * Default sync frequency (DAILY, WEEKLY, MONTHLY)
         */
        private String defaultFrequency = "DAILY";

        /**
         * Default sync time (HH:mm format)
         */
        private String defaultTime = "04:00";
    }

    @Getter
    @Setter
    public static class SourcesConfig {
        /**
         * Java source configuration
         */
        private JavaSourceConfig java = new JavaSourceConfig();

        /**
         * Kotlin source configuration
         */
        private KotlinSourceConfig kotlin = new KotlinSourceConfig();
    }

    @Getter
    @Setter
    public static class JavaSourceConfig {
        /**
         * Base URL for JEP documentation
         */
        private String jepBaseUrl = "https://openjdk.org/jeps";

        /**
         * Oracle Java documentation URL
         */
        private String oracleDocs = "https://docs.oracle.com/en/java";

        /**
         * Minimum supported Java version
         */
        private int minVersion = 8;
    }

    @Getter
    @Setter
    public static class KotlinSourceConfig {
        /**
         * Base URL for KEP (Kotlin Enhancement Proposals)
         */
        private String kepBaseUrl = "https://github.com/Kotlin/KEEP/tree/master/proposals";

        /**
         * Kotlin documentation URL
         */
        private String docs = "https://kotlinlang.org/docs";

        /**
         * Minimum supported Kotlin version
         */
        private String minVersion = "1.6";
    }

    @Getter
    @Setter
    public static class AttributionConfig {
        /**
         * Java attribution
         */
        private SourceAttribution java = new SourceAttribution(
                "OpenJDK",
                "GPL-2.0",
                "https://github.com/openjdk/jdk"
        );

        /**
         * Kotlin attribution
         */
        private SourceAttribution kotlin = new SourceAttribution(
                "Kotlin",
                "Apache-2.0",
                "https://github.com/JetBrains/kotlin"
        );
    }

    @Getter
    @Setter
    public static class SourceAttribution {
        private String name;
        private String license;
        private String repository;

        public SourceAttribution() {}

        public SourceAttribution(String name, String license, String repository) {
            this.name = name;
            this.license = license;
            this.repository = repository;
        }
    }
}
