package com.spring.mcp.model.dto.mcp;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response DTO for getSpringBootLanguageRequirements tool
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SpringBootLanguageRequirementResponse {
    private String springBootVersion;
    private List<LanguageRequirement> requirements;
    private String recommendation;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class LanguageRequirement {
        private String language;
        private String minimumVersion;
        private String recommendedVersion;
        private String maximumVersion;
        private String versionRange;
        private String notes;
        private List<String> supportedFeatures;
    }
}
