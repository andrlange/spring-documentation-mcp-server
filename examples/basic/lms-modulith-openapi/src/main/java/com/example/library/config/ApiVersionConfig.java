package com.example.library.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ApiVersionConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * API Versioning Configuration using Spring Framework 7's first-class support.
 *
 * Supports:
 * - Header-based versioning via API-Version header
 * - Default version 1.0 when no version specified
 * - Versions: 1.0, 2.0
 */
@Configuration
public class ApiVersionConfig implements WebMvcConfigurer {

    @Override
    public void configureApiVersioning(ApiVersionConfigurer configurer) {
        configurer
            .useRequestHeader("API-Version")
            .addSupportedVersions("1.0", "2.0")
            .setDefaultVersion("1.0");
    }
}
