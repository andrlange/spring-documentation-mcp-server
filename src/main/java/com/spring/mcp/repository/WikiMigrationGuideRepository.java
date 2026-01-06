package com.spring.mcp.repository;

import com.spring.mcp.model.entity.WikiMigrationGuide;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for WikiMigrationGuide entity operations.
 *
 * @author Spring MCP Server
 * @version 1.7.0
 * @since 2026-01-06
 */
@Repository
public interface WikiMigrationGuideRepository extends JpaRepository<WikiMigrationGuide, Long> {

    /**
     * Find by source and target version strings (e.g., "3.5" -> "4.0").
     */
    Optional<WikiMigrationGuide> findBySourceVersionStringAndTargetVersionString(
            String sourceVersionString, String targetVersionString);

    /**
     * Find all migration guides targeting a specific version.
     */
    List<WikiMigrationGuide> findByTargetVersionStringOrderBySourceMajorDescSourceMinorDesc(
            String targetVersionString);

    /**
     * Find all migration guides from a specific source version.
     */
    List<WikiMigrationGuide> findBySourceVersionStringOrderByTargetMajorDescTargetMinorDesc(
            String sourceVersionString);

    /**
     * Find all ordered by target version descending (newest first).
     */
    @Query("SELECT w FROM WikiMigrationGuide w ORDER BY w.targetMajor DESC, w.targetMinor DESC, w.sourceMajor DESC, w.sourceMinor DESC")
    List<WikiMigrationGuide> findAllOrderByVersionDesc();

    /**
     * Find migration guides for a target major version.
     */
    List<WikiMigrationGuide> findByTargetMajorOrderByTargetMinorDescSourceMajorDescSourceMinorDesc(int targetMajor);

    /**
     * Full-text search using PostgreSQL tsvector.
     */
    @Query(value = """
        SELECT * FROM wiki_migration_guides w
        WHERE w.search_vector @@ plainto_tsquery('english', :query)
        ORDER BY ts_rank(w.search_vector, plainto_tsquery('english', :query)) DESC
        LIMIT :limit
        """, nativeQuery = true)
    List<WikiMigrationGuide> searchByQuery(@Param("query") String query, @Param("limit") int limit);

    /**
     * Find migration guides without embeddings (for embedding sync).
     */
    @Query("SELECT w FROM WikiMigrationGuide w WHERE w.embeddingModel IS NULL")
    Page<WikiMigrationGuide> findWithoutEmbedding(Pageable pageable);

    /**
     * Count migration guides with embeddings.
     */
    @Query("SELECT COUNT(w) FROM WikiMigrationGuide w WHERE w.embeddingModel IS NOT NULL")
    long countWithEmbedding();

    /**
     * Check if migration path exists.
     */
    boolean existsBySourceVersionStringAndTargetVersionString(
            String sourceVersionString, String targetVersionString);

    /**
     * Find by content hash (for change detection).
     */
    Optional<WikiMigrationGuide> findByContentHash(String contentHash);

    /**
     * Find direct upgrade path (e.g., 3.4 -> 3.5, not 3.4 -> 4.0).
     */
    @Query("""
        SELECT w FROM WikiMigrationGuide w
        WHERE (w.sourceMajor = w.targetMajor AND w.targetMinor = w.sourceMinor + 1)
           OR (w.targetMajor = w.sourceMajor + 1 AND w.targetMinor = 0)
        ORDER BY w.targetMajor DESC, w.targetMinor DESC
        """)
    List<WikiMigrationGuide> findDirectUpgradePaths();
}
