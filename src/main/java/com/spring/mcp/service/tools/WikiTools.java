package com.spring.mcp.service.tools;

import com.spring.mcp.model.dto.mcp.WikiMigrationGuideResponse;
import com.spring.mcp.model.dto.mcp.WikiReleaseNotesResponse;
import com.spring.mcp.model.entity.WikiMigrationGuide;
import com.spring.mcp.model.entity.WikiReleaseNotes;
import com.spring.mcp.service.wiki.WikiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springaicommunity.mcp.annotation.McpTool;
import org.springaicommunity.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * MCP Tools for Spring Boot Wiki content (Release Notes and Migration Guides).
 * These tools provide access to official Spring Boot documentation from the GitHub wiki.
 *
 * @author Spring MCP Server
 * @version 1.7.0
 * @since 2026-01-06
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WikiTools {

    private final WikiService wikiService;

    @McpTool(description = """
        Get Spring Boot release notes for a specific version from the official GitHub wiki.
        Release notes document new features, enhancements, bug fixes, and deprecations
        for each Spring Boot version.

        Use this to understand what changed in a specific Spring Boot version.
        Example versions: "4.0", "3.5", "3.4", "3.3", "3.2", "3.1", "3.0", "2.7"
        """)
    public WikiReleaseNotesResponse getWikiReleaseNotes(
            @McpToolParam(description = "Spring Boot version (e.g., '4.0', '3.5', '3.4')") String version
    ) {
        log.info("Tool: getWikiReleaseNotes - version={}", version);

        if (version == null || version.isBlank()) {
            return WikiReleaseNotesResponse.notFound("Version parameter is required");
        }

        // Clean up version string
        String cleanVersion = version.trim();

        // Handle versions with or without patch number
        // If they pass "3.5.0", convert to "3.5"
        if (cleanVersion.matches("\\d+\\.\\d+\\.\\d+")) {
            cleanVersion = cleanVersion.substring(0, cleanVersion.lastIndexOf('.'));
        }

        Optional<WikiReleaseNotes> releaseNotes = wikiService.findReleaseNotesByVersion(cleanVersion);

        if (releaseNotes.isEmpty()) {
            log.info("Release notes not found for version: {}", cleanVersion);
            return WikiReleaseNotesResponse.notFound(
                    "Release notes for Spring Boot " + cleanVersion + " not found. " +
                    "Available versions can be synced from the GitHub wiki."
            );
        }

        return WikiReleaseNotesResponse.from(releaseNotes.get());
    }

    @McpTool(description = """
        Get Spring Boot migration guide for upgrading between specific versions.
        Migration guides document breaking changes, required modifications, and
        upgrade instructions when migrating from one Spring Boot version to another.

        Use this when planning an upgrade between Spring Boot versions.
        Example: from "3.5" to "4.0", from "2.7" to "3.0"
        """)
    public WikiMigrationGuideResponse getWikiMigrationGuide(
            @McpToolParam(description = "Source Spring Boot version to upgrade from (e.g., '3.5', '2.7')") String fromVersion,
            @McpToolParam(description = "Target Spring Boot version to upgrade to (e.g., '4.0', '3.0')") String toVersion
    ) {
        log.info("Tool: getWikiMigrationGuide - from {} to {}", fromVersion, toVersion);

        if (fromVersion == null || fromVersion.isBlank()) {
            return WikiMigrationGuideResponse.notFound("fromVersion parameter is required");
        }
        if (toVersion == null || toVersion.isBlank()) {
            return WikiMigrationGuideResponse.notFound("toVersion parameter is required");
        }

        // Clean up version strings
        String cleanFrom = cleanVersion(fromVersion.trim());
        String cleanTo = cleanVersion(toVersion.trim());

        Optional<WikiMigrationGuide> guide = wikiService.findMigrationGuide(cleanFrom, cleanTo);

        if (guide.isEmpty()) {
            log.info("Migration guide not found: {} -> {}", cleanFrom, cleanTo);

            // Check if there are any guides for these versions
            var guidesFrom = wikiService.findMigrationGuidesFrom(cleanFrom);
            var guidesTo = wikiService.findMigrationGuidesTo(cleanTo);

            StringBuilder message = new StringBuilder();
            message.append("Migration guide for ").append(cleanFrom).append(" -> ")
                   .append(cleanTo).append(" not found.");

            if (!guidesFrom.isEmpty()) {
                message.append(" Available upgrade paths from ").append(cleanFrom).append(": ");
                message.append(guidesFrom.stream()
                        .map(g -> g.getTargetVersionString())
                        .reduce((a, b) -> a + ", " + b)
                        .orElse(""));
                message.append(".");
            }

            if (!guidesTo.isEmpty()) {
                message.append(" Available upgrade paths to ").append(cleanTo).append(" from: ");
                message.append(guidesTo.stream()
                        .map(g -> g.getSourceVersionString())
                        .reduce((a, b) -> a + ", " + b)
                        .orElse(""));
                message.append(".");
            }

            return WikiMigrationGuideResponse.notFound(message.toString());
        }

        return WikiMigrationGuideResponse.from(guide.get());
    }

    /**
     * Clean up version string by removing patch number if present.
     */
    private String cleanVersion(String version) {
        if (version.matches("\\d+\\.\\d+\\.\\d+")) {
            return version.substring(0, version.lastIndexOf('.'));
        }
        return version;
    }
}
