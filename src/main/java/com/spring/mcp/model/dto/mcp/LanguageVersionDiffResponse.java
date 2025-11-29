package com.spring.mcp.model.dto.mcp;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response DTO for getLanguageVersionDiff tool
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LanguageVersionDiffResponse {
    private String language;
    private String fromVersion;
    private String toVersion;
    private Integer totalChanges;
    private List<FeatureSummary> newFeatures;
    private List<FeatureSummary> deprecatedFeatures;
    private List<FeatureSummary> removedFeatures;
    private MigrationRecommendations recommendations;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class FeatureSummary {
        private String featureName;
        private String description;
        private String category;
        private String jepNumber;
        private String kepNumber;
        private String impactLevel;
        private String introducedIn;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class MigrationRecommendations {
        private List<String> mustDoItems;
        private List<String> shouldDoItems;
        private List<String> niceToHaveItems;
        private String overallComplexity;
    }
}
