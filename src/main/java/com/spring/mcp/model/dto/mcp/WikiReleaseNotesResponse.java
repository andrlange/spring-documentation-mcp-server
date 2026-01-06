package com.spring.mcp.model.dto.mcp;

import com.spring.mcp.model.entity.WikiReleaseNotes;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * MCP Tool response for wiki release notes.
 *
 * @author Spring MCP Server
 * @version 1.7.0
 * @since 2026-01-06
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WikiReleaseNotesResponse {

    private String version;
    private String title;
    private String contentMarkdown;
    private String sourceUrl;
    private boolean found;
    private String message;

    /**
     * Create response from entity.
     */
    public static WikiReleaseNotesResponse from(WikiReleaseNotes entity) {
        if (entity == null) {
            return notFound("Release notes not found");
        }

        return WikiReleaseNotesResponse.builder()
                .version(entity.getVersionString())
                .title(entity.getTitle())
                .contentMarkdown(entity.getContentMarkdown())
                .sourceUrl(entity.getSourceUrl())
                .found(true)
                .build();
    }

    /**
     * Create not found response.
     */
    public static WikiReleaseNotesResponse notFound(String message) {
        return WikiReleaseNotesResponse.builder()
                .found(false)
                .message(message)
                .build();
    }
}
