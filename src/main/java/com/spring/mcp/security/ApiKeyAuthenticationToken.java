package com.spring.mcp.security;

import com.spring.mcp.model.entity.ApiKey;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

/**
 * Custom authentication token for API key authentication.
 * Stores the full ApiKey entity so we can access the ID and other properties
 * in service methods that need to check group membership.
 *
 * @author Spring MCP Server
 * @version 1.3.3
 * @since 2025-12-04
 */
public class ApiKeyAuthenticationToken extends AbstractAuthenticationToken {

    private final ApiKey apiKey;

    public ApiKeyAuthenticationToken(ApiKey apiKey, Collection<? extends GrantedAuthority> authorities) {
        super(authorities);
        this.apiKey = apiKey;
        setAuthenticated(true);
    }

    @Override
    public Object getCredentials() {
        return null; // No credentials stored for API key auth
    }

    @Override
    public Object getPrincipal() {
        return apiKey.getName(); // Return name as principal for compatibility
    }

    /**
     * Get the full ApiKey entity.
     * @return The authenticated API key
     */
    public ApiKey getApiKey() {
        return apiKey;
    }

    /**
     * Get the API key ID.
     * @return The API key ID
     */
    public Long getApiKeyId() {
        return apiKey != null ? apiKey.getId() : null;
    }
}
