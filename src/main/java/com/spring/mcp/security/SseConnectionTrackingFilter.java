package com.spring.mcp.security;

import com.spring.mcp.model.enums.ConnectionEventType;
import com.spring.mcp.service.monitoring.McpMonitoringService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Filter to track SSE connections for monitoring purposes.
 * Records connection and disconnection events to the monitoring service.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Order(Ordered.HIGHEST_PRECEDENCE + 100) // After security filters but before MCP handling
public class SseConnectionTrackingFilter extends OncePerRequestFilter {

    private final McpMonitoringService monitoringService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String requestUri = request.getRequestURI();
        String accept = request.getHeader("Accept");

        // Check if this is an SSE connection request
        boolean isSseRequest = isSseEndpoint(requestUri) &&
                               accept != null &&
                               accept.contains("text/event-stream");

        if (!isSseRequest) {
            filterChain.doFilter(request, response);
            return;
        }

        // Generate or retrieve session ID
        String sessionId = getSessionId(request);

        // Build client info
        Map<String, Object> clientInfo = buildClientInfo(request);

        // Record connection event
        try {
            monitoringService.recordConnectionEvent(
                    sessionId,
                    ConnectionEventType.CONNECTED,
                    clientInfo,
                    request.getHeader("MCP-Protocol-Version")
            );
            log.info("SSE connection established: sessionId={}, client={}",
                    sessionId, request.getHeader("User-Agent"));
        } catch (Exception e) {
            log.warn("Failed to record connection event: {}", e.getMessage());
        }

        try {
            // Continue with the filter chain
            filterChain.doFilter(request, response);
        } finally {
            // Record disconnection when the request completes (SSE connection closed)
            try {
                monitoringService.recordConnectionEvent(
                        sessionId,
                        ConnectionEventType.DISCONNECTED,
                        clientInfo,
                        request.getHeader("MCP-Protocol-Version")
                );
                log.info("SSE connection closed: sessionId={}", sessionId);
            } catch (Exception e) {
                log.warn("Failed to record disconnection event: {}", e.getMessage());
            }
        }
    }

    /**
     * Check if the request URI is an SSE endpoint.
     */
    private boolean isSseEndpoint(String uri) {
        return uri.contains("/sse") ||
               uri.equals("/mcp/spring/sse") ||
               uri.startsWith("/mcp") && uri.endsWith("/sse");
    }

    /**
     * Get session ID from request (header, attribute, or query param).
     */
    private String getSessionId(HttpServletRequest request) {
        // Check header first
        String sessionId = request.getHeader("Mcp-Session-Id");
        if (sessionId != null && !sessionId.isBlank()) {
            return sessionId;
        }

        // Check request attribute (set by McpProtocolHeaderFilter)
        Object attrSessionId = request.getAttribute("mcp.session.id");
        if (attrSessionId != null) {
            return attrSessionId.toString();
        }

        // Check query parameter
        sessionId = request.getParameter("sessionId");
        if (sessionId != null && !sessionId.isBlank()) {
            return sessionId;
        }

        // Generate a new session ID if none provided
        return "sse-" + UUID.randomUUID().toString().substring(0, 8);
    }

    /**
     * Build client info map from request.
     */
    private Map<String, Object> buildClientInfo(HttpServletRequest request) {
        Map<String, Object> info = new HashMap<>();
        info.put("userAgent", request.getHeader("User-Agent"));
        info.put("remoteAddr", request.getRemoteAddr());
        info.put("remoteHost", request.getRemoteHost());
        info.put("protocol", request.getProtocol());

        String origin = request.getHeader("Origin");
        if (origin != null) {
            info.put("origin", origin);
        }

        String apiKeyName = getApiKeyName(request);
        if (apiKeyName != null) {
            info.put("apiKeyName", apiKeyName);
        }

        return info;
    }

    /**
     * Get API key name from authentication context.
     */
    private String getApiKeyName(HttpServletRequest request) {
        // Try to get from security context via ApiKeyAuthenticationToken
        var securityContext = org.springframework.security.core.context.SecurityContextHolder.getContext();
        var auth = securityContext.getAuthentication();
        if (auth instanceof ApiKeyAuthenticationToken apiKeyAuth) {
            return apiKeyAuth.getApiKey().getName();
        }
        return null;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // Only filter SSE endpoints
        String path = request.getRequestURI();
        return !isSseEndpoint(path);
    }
}
