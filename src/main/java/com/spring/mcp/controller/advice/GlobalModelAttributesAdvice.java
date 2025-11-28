package com.spring.mcp.controller.advice;

import com.spring.mcp.config.OpenRewriteFeatureConfig;
import com.spring.mcp.service.SettingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Global controller advice that adds common model attributes to all views.
 * This includes system-wide settings like enterprise subscription status
 * and feature toggles.
 *
 * @author Spring MCP Server
 * @version 1.0
 * @since 2025-01-10
 */
@ControllerAdvice
@RequiredArgsConstructor
public class GlobalModelAttributesAdvice {

    private final SettingsService settingsService;
    private final OpenRewriteFeatureConfig openRewriteFeatureConfig;

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
}
