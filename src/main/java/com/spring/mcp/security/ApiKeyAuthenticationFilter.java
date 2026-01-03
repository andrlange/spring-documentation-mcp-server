package com.spring.mcp.security;

import com.spring.mcp.model.entity.ApiKey;
import com.spring.mcp.service.ApiKeyService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.Optional;

/**
 * Authentication filter for API Key validation on MCP endpoints
 * <p>
 * Checks for API key in these locations (in order of priority):
 * 1. X-API-Key header
 * 2. Authorization: Bearer <key> header
 * 3. api_key query parameter (less secure, for testing only)
 * <p>
 * Only applies to /mcp/** endpoints
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {

    private final ApiKeyService apiKeyService;

    private static final String API_KEY_HEADER = "X-API-Key";
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String API_KEY_PARAM = "api_key";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String requestUri = request.getRequestURI();

        // Allow OPTIONS requests (CORS preflight) to pass through without API key
        // CORS preflight requests don't carry custom headers like X-API-Key
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            log.debug("Allowing OPTIONS preflight request for: {}", requestUri);
            filterChain.doFilter(request, response);
            return;
        }

        // Only process MCP protocol endpoints (both /mcp/** and default Spring AI /sse)
        // Exclude /mcp-tools (management UI) - uses session-based auth
        if (requestUri.startsWith("/mcp-tools")) {
            filterChain.doFilter(request, response);
            return;
        }

        if (!requestUri.startsWith("/mcp") && !requestUri.startsWith("/sse")) {
            filterChain.doFilter(request, response);
            return;
        }

        // Extract API key from request
        String apiKey = extractApiKey(request);

        if (apiKey == null || apiKey.isBlank()) {
            log.warn("Missing API key for MCP endpoint: {}", requestUri);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Missing API key. Provide via X-API-Key header or Authorization: Bearer header\"}");
            return;
        }

        // Validate API key
        Optional<ApiKey> validatedKey = apiKeyService.validateKey(apiKey);

        if (validatedKey.isEmpty()) {
            log.warn("Invalid API key attempt for MCP endpoint: {}", requestUri);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Invalid or inactive API key\"}");
            return;
        }

        // Authentication successful - set security context with custom token
        ApiKey key = validatedKey.get();
        ApiKeyAuthenticationToken authentication = new ApiKeyAuthenticationToken(
                key,
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_API_USER"))
        );

        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authentication);

        log.debug("API key authentication successful: keyName={}, endpoint={}", key.getName(), requestUri);

        // Continue filter chain
        filterChain.doFilter(request, response);
    }

    /**
     * Extract API key from request in order of priority:
     * 1. X-API-Key header
     * 2. Authorization: Bearer <key> header
     * 3. api_key query parameter
     */
    private String extractApiKey(HttpServletRequest request) {
        // 1. Check X-API-Key header (preferred method)
        String apiKey = request.getHeader(API_KEY_HEADER);
        if (apiKey != null && !apiKey.isBlank()) {
            return apiKey.trim();
        }

        // 2. Check Authorization: Bearer header
        String authHeader = request.getHeader(AUTHORIZATION_HEADER);
        if (authHeader != null && authHeader.startsWith(BEARER_PREFIX)) {
            return authHeader.substring(BEARER_PREFIX.length()).trim();
        }

        // 3. Check query parameter (less secure, for testing/debugging only)
        String queryParam = request.getParameter(API_KEY_PARAM);
        if (queryParam != null && !queryParam.isBlank()) {
            log.warn("API key provided via query parameter - this is less secure. Use headers instead.");
            return queryParam.trim();
        }

        return null;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // Only filter MCP protocol endpoints, not MCP Tools management UI
        String path = request.getRequestURI();

        // Exclude /mcp-tools (management UI) from API key filter
        // This UI uses session-based authentication
        if (path.startsWith("/mcp-tools")) {
            return true;
        }

        // Apply filter to /mcp/** (protocol endpoints)
        return !path.startsWith("/mcp");
    }
}
