package com.spring.mcp.model.dto.mcp;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response DTO for getLanguageVersions tool
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LanguageVersionsResponse {
    private String language;
    private List<VersionInfo> versions;
    private Integer count;
    private VersionInfo currentVersion;
    private List<VersionInfo> ltsVersions;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class VersionInfo {
        private String version;
        private String displayName;
        private Integer majorVersion;
        private Integer minorVersion;
        private Boolean isLts;
        private Boolean isCurrent;
        private String releaseDate;
        private String endOfLife;
        private String extendedSupportEnd;
        private Integer featureCount;
    }
}
