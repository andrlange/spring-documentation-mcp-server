package com.example.apiversioning.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ApiVersionConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * API Versioning Configuration
 *
 * Demonstrates Spring Framework 7's native API versioning support.
 * Uses header-based versioning via the API-Version request header.
 *
 * @see <a href="https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-config/api-version.html">
 *      Spring Framework API Versioning Documentation</a>
 */
@Configuration
public class ApiVersionConfig implements WebMvcConfigurer {

    // Default version when none is specified
    public static final String DEFAULT_VERSION = "1.0";

    // Header name for version selection
    public static final String VERSION_HEADER = "API-Version";

    // Supported versions
    public static final String VERSION_1 = "1.0";
    public static final String VERSION_2 = "2.0";

    /**
     * Configure API versioning with Spring Framework 7's native support.
     *
     * This configures:
     * - Header-based version resolution using "API-Version" header
     * - Supported versions: 1.0 and 2.0
     * - Default version: 1.0 (when no header is provided)
     */
    @Override
    public void configureApiVersioning(ApiVersionConfigurer configurer) {
        configurer
            .useRequestHeader(VERSION_HEADER)    // Use API-Version header
            .addSupportedVersions(VERSION_1, VERSION_2)  // Supported versions
            .setDefaultVersion(DEFAULT_VERSION);  // Default when no version specified
    }

    // Check if a version is deprecated
    public static boolean isDeprecated(String version) {
        return VERSION_1.equals(version);
    }

    // Check if a version is supported
    public static boolean isSupported(String version) {
        return VERSION_1.equals(version) || VERSION_2.equals(version);
    }
}
