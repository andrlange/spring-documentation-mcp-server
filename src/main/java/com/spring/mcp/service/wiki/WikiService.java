package com.spring.mcp.service.wiki;

import com.spring.mcp.model.entity.WikiMigrationGuide;
import com.spring.mcp.model.entity.WikiReleaseNotes;

import java.util.List;
import java.util.Optional;

/**
 * Service interface for Wiki operations (Release Notes and Migration Guides).
 *
 * @author Spring MCP Server
 * @version 1.7.0
 * @since 2026-01-06
 */
public interface WikiService {

    // === Release Notes Operations ===

    /**
     * Find release notes by version string (e.g., "3.5", "4.0").
     */
    Optional<WikiReleaseNotes> findReleaseNotesByVersion(String versionString);

    /**
     * Find release notes by major and minor version.
     */
    Optional<WikiReleaseNotes> findReleaseNotesByVersion(int majorVersion, int minorVersion);

    /**
     * Get all release notes ordered by version descending.
     */
    List<WikiReleaseNotes> findAllReleaseNotes();

    /**
     * Get release notes for a major version.
     */
    List<WikiReleaseNotes> findReleaseNotesByMajorVersion(int majorVersion);

    /**
     * Search release notes using full-text search.
     */
    List<WikiReleaseNotes> searchReleaseNotes(String query, int limit);

    /**
     * Get total count of release notes.
     */
    long countReleaseNotes();

    /**
     * Get count of release notes with embeddings.
     */
    long countReleaseNotesWithEmbedding();

    // === Migration Guide Operations ===

    /**
     * Find migration guide between two versions.
     */
    Optional<WikiMigrationGuide> findMigrationGuide(String sourceVersion, String targetVersion);

    /**
     * Find all migration guides targeting a specific version.
     */
    List<WikiMigrationGuide> findMigrationGuidesTo(String targetVersion);

    /**
     * Find all migration guides from a specific version.
     */
    List<WikiMigrationGuide> findMigrationGuidesFrom(String sourceVersion);

    /**
     * Get all migration guides ordered by target version descending.
     */
    List<WikiMigrationGuide> findAllMigrationGuides();

    /**
     * Get migration guides for a target major version.
     */
    List<WikiMigrationGuide> findMigrationGuidesByTargetMajorVersion(int targetMajor);

    /**
     * Find direct upgrade paths (consecutive versions only).
     */
    List<WikiMigrationGuide> findDirectUpgradePaths();

    /**
     * Search migration guides using full-text search.
     */
    List<WikiMigrationGuide> searchMigrationGuides(String query, int limit);

    /**
     * Get total count of migration guides.
     */
    long countMigrationGuides();

    /**
     * Get count of migration guides with embeddings.
     */
    long countMigrationGuidesWithEmbedding();

    // === Statistics ===

    /**
     * Get wiki statistics.
     */
    WikiStats getStatistics();

    // === Maintenance Operations ===

    /**
     * Fix markdown artifacts in existing wiki content.
     * Processes all release notes and migration guides to fix:
     * - Table formatting issues from AsciiDoc conversion
     * - Internal document link artifacts like {#_test_code}
     *
     * @return the number of documents fixed
     */
    int fixExistingMarkdownArtifacts();

    /**
     * Wiki statistics record.
     */
    record WikiStats(
            long releaseNotesCount,
            long releaseNotesWithEmbedding,
            long migrationGuidesCount,
            long migrationGuidesWithEmbedding,
            String latestReleaseNotesVersion,
            String latestMigrationPath
    ) {}
}
