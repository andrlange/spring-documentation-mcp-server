package com.spring.mcp.model.dto.mcp;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response DTO for getModernPatterns tool
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CodePatternResponse {
    private String featureName;
    private String featureDescription;
    private String language;
    private String version;
    private List<PatternInfo> patterns;
    private Integer totalPatterns;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class PatternInfo {
        private Long id;
        private String patternLanguage;
        private String oldPattern;
        private String newPattern;
        private String explanation;
        private String minVersion;
    }
}
