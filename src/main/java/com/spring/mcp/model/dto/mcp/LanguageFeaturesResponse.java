package com.spring.mcp.model.dto.mcp;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response DTO for getLanguageFeatures tool
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LanguageFeaturesResponse {
    private String language;
    private String version;
    private List<FeatureInfo> features;
    private Integer totalCount;
    private FeatureStats stats;
    private List<String> categories;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class FeatureInfo {
        private Long id;
        private String featureName;
        private String description;
        private String status;
        private String statusBadgeClass;
        private String statusIconClass;
        private String category;
        private String jepNumber;
        private String kepNumber;
        private String proposalUrl;
        private String impactLevel;
        private String introducedVersion;
        private Boolean hasCodePatterns;
        private Integer codePatternCount;
        private String codeExample;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class FeatureStats {
        private Integer newCount;
        private Integer deprecatedCount;
        private Integer removedCount;
        private Integer previewCount;
        private Integer incubatingCount;
    }
}
