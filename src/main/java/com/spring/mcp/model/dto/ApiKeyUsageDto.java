package com.spring.mcp.model.dto;

import com.spring.mcp.model.entity.ApiKey;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * DTO for API key usage statistics displayed on the monitoring dashboard.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiKeyUsageDto {

    private Long id;
    private String name;
    private String description;
    private Long requestCount;
    private LocalDateTime lastUsedAt;
    private LocalDateTime createdAt;
    private String createdBy;
    private Boolean isActive;

    /**
     * Create DTO from ApiKey entity
     */
    public static ApiKeyUsageDto fromEntity(ApiKey apiKey) {
        return ApiKeyUsageDto.builder()
                .id(apiKey.getId())
                .name(apiKey.getName())
                .description(apiKey.getDescription())
                .requestCount(apiKey.getRequestCount() != null ? apiKey.getRequestCount() : 0L)
                .lastUsedAt(apiKey.getLastUsedAt())
                .createdAt(apiKey.getCreatedAt())
                .createdBy(apiKey.getCreatedBy())
                .isActive(apiKey.getIsActive())
                .build();
    }

    /**
     * Get formatted last used time
     */
    public String getLastUsedFormatted() {
        if (lastUsedAt == null) {
            return "Never";
        }
        return lastUsedAt.format(DateTimeFormatter.ofPattern("MMM d, HH:mm"));
    }

    /**
     * Get relative time since last used (e.g., "5 min ago", "2h ago")
     */
    public String getLastUsedRelative() {
        if (lastUsedAt == null) {
            return "Never used";
        }

        LocalDateTime now = LocalDateTime.now();
        long minutes = ChronoUnit.MINUTES.between(lastUsedAt, now);

        if (minutes < 1) {
            return "Just now";
        } else if (minutes < 60) {
            return minutes + " min ago";
        } else if (minutes < 1440) {
            long hours = minutes / 60;
            return hours + "h ago";
        } else {
            long days = minutes / 1440;
            return days + "d ago";
        }
    }

    /**
     * Get status class for styling (based on activity)
     */
    public String getStatusClass() {
        if (!Boolean.TRUE.equals(isActive)) {
            return "text-muted";
        }
        if (lastUsedAt == null) {
            return "text-secondary";
        }

        long minutesAgo = ChronoUnit.MINUTES.between(lastUsedAt, LocalDateTime.now());
        if (minutesAgo < 60) {
            return "text-success"; // Active in last hour
        } else if (minutesAgo < 1440) {
            return "text-warning"; // Active in last 24h
        } else {
            return "text-secondary"; // Older activity
        }
    }

    /**
     * Wrapper class for API key usage summary containing top keys and all keys
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ApiKeyUsageSummary {
        private List<ApiKeyUsageDto> topKeys;      // Top 5 keys by request count
        private List<ApiKeyUsageDto> otherKeys;    // Remaining keys
        private long totalKeys;
        private long totalRequests;
        private long activeKeys;
    }
}
