package com.spring.mcp.model.dto.mcp;

import lombok.Builder;
import lombok.Data;

/**
 * Response DTO for the getLanguageFeatureExample MCP tool.
 * Returns code example for a specific language feature.
 *
 * @author Spring MCP Server
 * @version 1.5.2
 * @since 2025-12-17
 */
@Data
@Builder
public class LanguageFeatureExampleResponse {

    /**
     * Programming language (Java or Kotlin)
     */
    private String language;

    /**
     * Language version where the feature was introduced
     */
    private String version;

    /**
     * Name of the feature
     */
    private String featureName;

    /**
     * Description of the feature
     */
    private String description;

    /**
     * Code example demonstrating the feature
     */
    private String codeExample;

    /**
     * Source type: OFFICIAL (from specification) or SYNTHESIZED (manually curated)
     */
    private String exampleSourceType;

    /**
     * JEP number for Java features (may be null)
     */
    private String jepNumber;

    /**
     * KEP number for Kotlin features (may be null)
     */
    private String kepNumber;

    /**
     * URL to specification (JEP or KEP page)
     */
    private String specificationUrl;

    /**
     * Internal URL to JEP/KEP detail page (if available)
     */
    private String detailPageUrl;

    /**
     * Feature category (e.g., Pattern Matching, Concurrency)
     */
    private String category;

    /**
     * Feature status (NEW, DEPRECATED, REMOVED, PREVIEW, INCUBATING)
     */
    private String status;

    /**
     * Impact level for migration (LOW, MEDIUM, HIGH)
     */
    private String impactLevel;

    /**
     * Whether this feature has migration patterns available
     */
    private boolean hasPatterns;

    /**
     * Error message if feature was not found
     */
    private String error;

    /**
     * Create an error response
     */
    public static LanguageFeatureExampleResponse error(String message) {
        return LanguageFeatureExampleResponse.builder()
                .error(message)
                .build();
    }
}
