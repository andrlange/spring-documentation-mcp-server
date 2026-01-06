package com.spring.mcp.repository;

import com.spring.mcp.model.entity.WikiReleaseNotes;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for WikiReleaseNotes entity operations.
 *
 * @author Spring MCP Server
 * @version 1.7.0
 * @since 2026-01-06
 */
@Repository
public interface WikiReleaseNotesRepository extends JpaRepository<WikiReleaseNotes, Long> {

    /**
     * Find by version string (e.g., "3.5", "4.0").
     */
    Optional<WikiReleaseNotes> findByVersionString(String versionString);

    /**
     * Find by major and minor version.
     */
    Optional<WikiReleaseNotes> findByMajorVersionAndMinorVersion(int majorVersion, int minorVersion);

    /**
     * Find all ordered by version descending (newest first).
     */
    @Query("SELECT w FROM WikiReleaseNotes w ORDER BY w.majorVersion DESC, w.minorVersion DESC")
    List<WikiReleaseNotes> findAllOrderByVersionDesc();

    /**
     * Find all release notes for a major version.
     */
    List<WikiReleaseNotes> findByMajorVersionOrderByMinorVersionDesc(int majorVersion);

    /**
     * Full-text search using PostgreSQL tsvector.
     */
    @Query(value = """
        SELECT * FROM wiki_release_notes w
        WHERE w.search_vector @@ plainto_tsquery('english', :query)
        ORDER BY ts_rank(w.search_vector, plainto_tsquery('english', :query)) DESC
        LIMIT :limit
        """, nativeQuery = true)
    List<WikiReleaseNotes> searchByQuery(@Param("query") String query, @Param("limit") int limit);

    /**
     * Find release notes without embeddings (for embedding sync).
     */
    @Query("SELECT w FROM WikiReleaseNotes w WHERE w.embeddingModel IS NULL")
    Page<WikiReleaseNotes> findWithoutEmbedding(Pageable pageable);

    /**
     * Count release notes with embeddings.
     */
    @Query("SELECT COUNT(w) FROM WikiReleaseNotes w WHERE w.embeddingModel IS NOT NULL")
    long countWithEmbedding();

    /**
     * Check if version exists.
     */
    boolean existsByVersionString(String versionString);

    /**
     * Find by content hash (for change detection).
     */
    Optional<WikiReleaseNotes> findByContentHash(String contentHash);
}
