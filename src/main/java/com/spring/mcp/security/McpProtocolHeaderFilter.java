package com.spring.mcp.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Set;

/**
 * Filter for MCP Transport 2025-06-18 protocol compliance.
 *
 * <p>This filter adds protocol compliance headers and validates security headers
 * for MCP endpoints according to the MCP Transport 2025-06-18 specification.</p>
 *
 * <h3>Features:</h3>
 * <ul>
 *   <li>Adds {@code MCP-Protocol-Version} header to all MCP responses</li>
 *   <li>Supports {@code Mcp-Session-Id} header for session management</li>
 *   <li>Validates {@code Origin} header for browser-based clients (optional)</li>
 *   <li>Maintains backwards compatibility with existing clients</li>
 * </ul>
 *
 * <h3>Header Support:</h3>
 * <ul>
 *   <li>{@code MCP-Protocol-Version: 2025-06-18} - Added to all MCP responses</li>
 *   <li>{@code Mcp-Session-Id} - Read from header (preferred) or query param (backwards compat)</li>
 *   <li>{@code Origin} - Validated against allowed origins list when present</li>
 * </ul>
 *
 * @author Spring MCP Server
 * @version 1.4.4
 * @since 2025-12-16
 * @see <a href="https://spec.modelcontextprotocol.io/2025-06-18/basic/transports/">MCP Transport Spec</a>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class McpProtocolHeaderFilter extends OncePerRequestFilter {

    /**
     * MCP Protocol version header name.
     */
    public static final String MCP_PROTOCOL_VERSION_HEADER = "MCP-Protocol-Version";

    /**
     * MCP Protocol version value (2025-06-18 spec).
     */
    public static final String MCP_PROTOCOL_VERSION = "2025-06-18";

    /**
     * Session ID header name (per MCP 2025-06-18 spec).
     */
    public static final String MCP_SESSION_ID_HEADER = "Mcp-Session-Id";

    /**
     * Legacy session ID query parameter (backwards compatibility).
     */
    public static final String SESSION_ID_PARAM = "sessionId";

    /**
     * Request attribute for storing resolved session ID.
     */
    public static final String SESSION_ID_ATTRIBUTE = "mcp.session.id";

    /**
     * Whether to validate Origin headers (security feature).
     * Enabled by default for production safety.
     */
    @Value("${mcp.security.validate-origin:true}")
    private boolean validateOrigin;

    /**
     * Allowed origins for MCP endpoints.
     * Defaults to allowing all origins (*) for CLI tools like Claude Code.
     */
    @Value("${mcp.security.allowed-origins:*}")
    private String allowedOrigins;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String requestUri = request.getRequestURI();

        // Only process MCP endpoints
        if (!isMcpEndpoint(requestUri)) {
            filterChain.doFilter(request, response);
            return;
        }

        // 1. Add MCP Protocol Version header to response
        response.setHeader(MCP_PROTOCOL_VERSION_HEADER, MCP_PROTOCOL_VERSION);
        log.trace("Added {} header to response for: {}", MCP_PROTOCOL_VERSION_HEADER, requestUri);

        // 2. Resolve session ID from header or query param
        String sessionId = resolveSessionId(request);
        if (sessionId != null) {
            request.setAttribute(SESSION_ID_ATTRIBUTE, sessionId);
            log.trace("Session ID resolved: {} for request: {}", sessionId, requestUri);

            // Echo session ID back in response header
            response.setHeader(MCP_SESSION_ID_HEADER, sessionId);
        }

        // 3. Validate Origin header if enabled and present
        if (validateOrigin) {
            String origin = request.getHeader("Origin");
            if (origin != null && !isAllowedOrigin(origin)) {
                log.warn("Rejected request from disallowed origin: {} for endpoint: {}", origin, requestUri);
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.setContentType("application/json");
                response.getWriter().write("{\"error\":\"Origin not allowed\",\"code\":\"ORIGIN_FORBIDDEN\"}");
                return;
            }
        }

        // Continue filter chain
        filterChain.doFilter(request, response);
    }

    /**
     * Check if the request URI is an MCP endpoint.
     */
    private boolean isMcpEndpoint(String uri) {
        return uri.startsWith("/mcp") ||
               uri.equals("/sse") ||
               uri.equals("/message");
    }

    /**
     * Resolve session ID from request.
     * Priority: Mcp-Session-Id header > sessionId query param
     *
     * @param request HTTP request
     * @return session ID or null if not provided
     */
    private String resolveSessionId(HttpServletRequest request) {
        // 1. Check Mcp-Session-Id header (preferred, per MCP 2025-06-18 spec)
        String sessionId = request.getHeader(MCP_SESSION_ID_HEADER);
        if (sessionId != null && !sessionId.isBlank()) {
            log.debug("Session ID from header: {}", sessionId);
            return sessionId.trim();
        }

        // 2. Check sessionId query parameter (backwards compatibility)
        sessionId = request.getParameter(SESSION_ID_PARAM);
        if (sessionId != null && !sessionId.isBlank()) {
            log.debug("Session ID from query param (legacy): {}", sessionId);
            return sessionId.trim();
        }

        return null;
    }

    /**
     * Check if the given origin is allowed.
     *
     * @param origin Origin header value
     * @return true if allowed
     */
    private boolean isAllowedOrigin(String origin) {
        if (allowedOrigins == null || allowedOrigins.isBlank()) {
            return false;
        }

        // Allow all origins if configured as "*"
        if ("*".equals(allowedOrigins.trim())) {
            return true;
        }

        // Check against comma-separated list of allowed origins
        Set<String> allowed = Set.of(allowedOrigins.split(","));
        for (String allowedOrigin : allowed) {
            String trimmed = allowedOrigin.trim().toLowerCase();
            if (trimmed.equals(origin.toLowerCase())) {
                return true;
            }
            // Support wildcard subdomains (e.g., *.example.com)
            if (trimmed.startsWith("*.") && origin.toLowerCase().endsWith(trimmed.substring(1))) {
                return true;
            }
        }

        return false;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // Only filter MCP endpoints
        String path = request.getRequestURI();
        return !isMcpEndpoint(path);
    }
}
