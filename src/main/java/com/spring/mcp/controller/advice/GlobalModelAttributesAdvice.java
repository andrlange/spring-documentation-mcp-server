package com.spring.mcp.controller.advice;

import com.spring.mcp.config.LanguageEvolutionFeatureConfig;
import com.spring.mcp.config.OpenRewriteFeatureConfig;
import com.spring.mcp.service.SettingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Global controller advice that adds common model attributes to all views.
 * This includes system-wide settings like enterprise subscription status
 * and feature toggles.
 *
 * @author Spring MCP Server
 * @version 1.1
 * @since 2025-01-10
 */
@ControllerAdvice
@RequiredArgsConstructor
public class GlobalModelAttributesAdvice {

    private final SettingsService settingsService;
    private final OpenRewriteFeatureConfig openRewriteFeatureConfig;
    private final LanguageEvolutionFeatureConfig languageEvolutionFeatureConfig;

    @Value("${info.app.name:Spring MCP Server}")
    private String appName;

    @Value("${info.app.version:1.0.0}")
    private String appVersion;

    @Value("${info.spring-boot.version:3.5.8}")
    private String springBootVersion;

    /**
     * Adds the enterprise subscription status to all models.
     * This allows all templates to access the current support level.
     *
     * @return true if enterprise subscription is enabled, false otherwise
     */
    @ModelAttribute("enterpriseSubscriptionEnabled")
    public boolean addEnterpriseSubscriptionEnabled() {
        return settingsService.isEnterpriseSubscriptionEnabled();
    }

    /**
     * Adds the OpenRewrite feature flag to all models.
     * This controls visibility of recipe-related navigation and features.
     *
     * @return true if OpenRewrite feature is enabled, false otherwise
     */
    @ModelAttribute("openRewriteEnabled")
    public boolean addOpenRewriteEnabled() {
        return openRewriteFeatureConfig.isEnabled();
    }

    /**
     * Adds the OpenRewrite attribution configuration to all models.
     * This provides license and source information for attribution display.
     *
     * @return the OpenRewrite attribution configuration
     */
    @ModelAttribute("openRewriteAttribution")
    public OpenRewriteFeatureConfig.AttributionConfig addOpenRewriteAttribution() {
        return openRewriteFeatureConfig.getAttribution();
    }

    /**
     * Adds the Language Evolution feature flag to all models.
     * This controls visibility of Java/Kotlin language navigation and features.
     *
     * @return true if Language Evolution feature is enabled, false otherwise
     */
    @ModelAttribute("languageEvolutionEnabled")
    public boolean addLanguageEvolutionEnabled() {
        return languageEvolutionFeatureConfig.isEnabled();
    }

    /**
     * Adds the Language Evolution attribution configuration to all models.
     * This provides license and source information for Java and Kotlin attribution display.
     *
     * @return the Language Evolution attribution configuration
     */
    @ModelAttribute("languageEvolutionAttribution")
    public LanguageEvolutionFeatureConfig.AttributionConfig addLanguageEvolutionAttribution() {
        return languageEvolutionFeatureConfig.getAttribution();
    }

    /**
     * Adds the application name to all models.
     *
     * @return the application name
     */
    @ModelAttribute("appName")
    public String addAppName() {
        return appName;
    }

    /**
     * Adds the application version to all models.
     *
     * @return the application version
     */
    @ModelAttribute("appVersion")
    public String addAppVersion() {
        return appVersion;
    }

    /**
     * Adds the Spring Boot version to all models.
     *
     * @return the Spring Boot version
     */
    @ModelAttribute("springBootVersion")
    public String addSpringBootVersion() {
        return springBootVersion;
    }
}
