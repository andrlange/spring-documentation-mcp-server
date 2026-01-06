package com.spring.mcp.model.dto.mcp;

import com.spring.mcp.model.entity.WikiMigrationGuide;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * MCP Tool response for wiki migration guide.
 *
 * @author Spring MCP Server
 * @version 1.7.0
 * @since 2026-01-06
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WikiMigrationGuideResponse {

    private String sourceVersion;
    private String targetVersion;
    private String migrationPath;
    private String title;
    private String contentMarkdown;
    private String sourceUrl;
    private boolean found;
    private String message;

    /**
     * Create response from entity.
     */
    public static WikiMigrationGuideResponse from(WikiMigrationGuide entity) {
        if (entity == null) {
            return notFound("Migration guide not found");
        }

        return WikiMigrationGuideResponse.builder()
                .sourceVersion(entity.getSourceVersionString())
                .targetVersion(entity.getTargetVersionString())
                .migrationPath(entity.getMigrationPath())
                .title(entity.getTitle())
                .contentMarkdown(entity.getContentMarkdown())
                .sourceUrl(entity.getSourceUrl())
                .found(true)
                .build();
    }

    /**
     * Create not found response.
     */
    public static WikiMigrationGuideResponse notFound(String message) {
        return WikiMigrationGuideResponse.builder()
                .found(false)
                .message(message)
                .build();
    }
}
