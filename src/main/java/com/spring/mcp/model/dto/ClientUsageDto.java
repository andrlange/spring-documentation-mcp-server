package com.spring.mcp.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * DTO for MCP client usage statistics.
 *
 * @author Spring MCP Server
 * @version 1.4.4
 * @since 2025-12-16
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClientUsageDto {

    private String userAgent;
    private String clientName;
    private String clientVersion;
    private Long connectionCount;

    // Pattern to extract client name and version from User-Agent
    // Examples: "claude-code/2.0.70", "cursor/0.45.0", "vscode-mcp/1.0.0"
    private static final Pattern USER_AGENT_PATTERN = Pattern.compile("^([^/\\s]+)(?:/([\\d.]+))?.*$");

    /**
     * Create from raw query result.
     */
    public static ClientUsageDto fromQueryResult(Object[] row) {
        String userAgent = (String) row[0];
        Long count = ((Number) row[1]).longValue();

        String clientName = userAgent;
        String clientVersion = null;

        if (userAgent != null) {
            Matcher matcher = USER_AGENT_PATTERN.matcher(userAgent);
            if (matcher.matches()) {
                clientName = matcher.group(1);
                clientVersion = matcher.group(2);
            }
        }

        return ClientUsageDto.builder()
                .userAgent(userAgent)
                .clientName(clientName)
                .clientVersion(clientVersion)
                .connectionCount(count)
                .build();
    }

    /**
     * Get display name (client name with version if available).
     */
    public String getDisplayName() {
        if (clientVersion != null && !clientVersion.isEmpty()) {
            return clientName + " v" + clientVersion;
        }
        return clientName != null ? clientName : "Unknown";
    }

    /**
     * Get icon class based on client name.
     */
    public String getIconClass() {
        if (clientName == null) {
            return "bi-question-circle";
        }
        String lowerName = clientName.toLowerCase();
        if (lowerName.contains("claude")) {
            return "bi-chat-dots";
        } else if (lowerName.contains("cursor")) {
            return "bi-cursor";
        } else if (lowerName.contains("vscode") || lowerName.contains("code")) {
            return "bi-code-square";
        } else if (lowerName.contains("intellij") || lowerName.contains("idea")) {
            return "bi-braces";
        } else if (lowerName.contains("copilot")) {
            return "bi-robot";
        } else if (lowerName.contains("python") || lowerName.contains("sdk")) {
            return "bi-terminal";
        }
        return "bi-app";
    }

    /**
     * Get color class based on client name.
     */
    public String getColorClass() {
        if (clientName == null) {
            return "text-muted";
        }
        String lowerName = clientName.toLowerCase();
        if (lowerName.contains("claude")) {
            return "text-warning";
        } else if (lowerName.contains("cursor")) {
            return "text-info";
        } else if (lowerName.contains("vscode") || lowerName.contains("code")) {
            return "text-primary";
        } else if (lowerName.contains("intellij") || lowerName.contains("idea")) {
            return "text-danger";
        }
        return "text-success";
    }

    /**
     * Summary DTO containing top clients and others.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ClientUsageSummary {
        private List<ClientUsageDto> topClients;
        private List<ClientUsageDto> otherClients;
        private long totalClients;
        private long totalConnections;
    }
}
