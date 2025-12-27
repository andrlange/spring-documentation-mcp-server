package com.spring.mcp.model.dto.mcp;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response DTO for searchSpringDocs tool
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SearchDocsResponse {
    private String query;
    private SearchFilters filters;
    private Long totalResults;
    private Integer returnedResults;
    private Long executionTimeMs;
    private List<DocumentationResult> results;
    private PaginationInfo pagination;

    // Constructor without pagination (for backward compatibility)
    public SearchDocsResponse(String query, SearchFilters filters, Long totalResults,
                              Integer returnedResults, Long executionTimeMs,
                              List<DocumentationResult> results) {
        this.query = query;
        this.filters = filters;
        this.totalResults = totalResults;
        this.returnedResults = returnedResults;
        this.executionTimeMs = executionTimeMs;
        this.results = results;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class PaginationInfo {
        private Integer currentPage;
        private Integer pageSize;
        private Integer totalPages;
        private Boolean hasMore;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class SearchFilters {
        private String project;
        private String version;
        private String docType;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class DocumentationResult {
        private Long id;
        private String title;
        private String url;
        private String description;
        private String project;
        private String projectSlug;
        private String version;
        private String docType;
        private String contentType;
        private String snippet;
        private Double rank;
        private String relevance;
    }
}
