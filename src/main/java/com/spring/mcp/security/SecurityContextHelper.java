package com.spring.mcp.security;

import com.spring.mcp.model.entity.ApiKey;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Helper class to extract authentication information from the security context.
 * Provides convenient methods to get API key details for MCP tools.
 *
 * @author Spring MCP Server
 * @version 1.3.3
 * @since 2025-12-04
 */
@Component
public class SecurityContextHelper {

    /**
     * Get the current API key ID from the security context.
     * Returns null if not authenticated with an API key.
     *
     * @return The API key ID or null
     */
    public Long getCurrentApiKeyId() {
        return getCurrentApiKey()
                .map(ApiKey::getId)
                .orElse(null);
    }

    /**
     * Get the current API key from the security context.
     * Returns empty if not authenticated with an API key.
     *
     * @return Optional containing the API key, or empty
     */
    public Optional<ApiKey> getCurrentApiKey() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication instanceof ApiKeyAuthenticationToken apiKeyAuth) {
            return Optional.ofNullable(apiKeyAuth.getApiKey());
        }

        return Optional.empty();
    }

    /**
     * Get the current API key name from the security context.
     * Returns null if not authenticated with an API key.
     *
     * @return The API key name or null
     */
    public String getCurrentApiKeyName() {
        return getCurrentApiKey()
                .map(ApiKey::getName)
                .orElse(null);
    }

    /**
     * Check if the current request is authenticated with an API key.
     *
     * @return true if authenticated with API key
     */
    public boolean isApiKeyAuthenticated() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication instanceof ApiKeyAuthenticationToken;
    }
}
