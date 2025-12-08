package com.spring.mcp.repository;

import com.spring.mcp.model.entity.JavadocSyncStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for JavadocSyncStatus entities.
 * Manages sync status tracking per project.
 */
@Repository
public interface JavadocSyncStatusRepository extends JpaRepository<JavadocSyncStatus, Long> {

    /**
     * Find sync status by project ID.
     */
    Optional<JavadocSyncStatus> findByProjectId(Long projectId);

    /**
     * Find all projects with sync enabled.
     */
    List<JavadocSyncStatus> findByEnabledTrue();

    /**
     * Find all projects with sync disabled.
     */
    List<JavadocSyncStatus> findByEnabledFalse();

    /**
     * Find projects with failure count at or above threshold.
     * Useful for identifying auto-disabled projects.
     */
    List<JavadocSyncStatus> findByFailureCountGreaterThanEqual(Integer threshold);

    /**
     * Find projects that need attention (failed but still enabled).
     */
    @Query("SELECT js FROM JavadocSyncStatus js WHERE js.enabled = true AND js.failureCount > 0")
    List<JavadocSyncStatus> findEnabledWithFailures();

    /**
     * Find all sync statuses with their projects eagerly loaded.
     */
    @Query("SELECT js FROM JavadocSyncStatus js JOIN FETCH js.project ORDER BY js.project.name")
    List<JavadocSyncStatus> findAllWithProjects();

    /**
     * Count enabled projects.
     */
    long countByEnabledTrue();

    /**
     * Count projects with failures.
     */
    @Query("SELECT COUNT(js) FROM JavadocSyncStatus js WHERE js.failureCount > 0")
    long countWithFailures();

    /**
     * Check if sync status exists for a project.
     */
    boolean existsByProjectId(Long projectId);

    /**
     * Delete sync status by project ID.
     */
    void deleteByProjectId(Long projectId);

    /**
     * Find sync status by project slug (via join).
     */
    @Query("SELECT js FROM JavadocSyncStatus js WHERE js.project.slug = :slug")
    Optional<JavadocSyncStatus> findByProjectSlug(@Param("slug") String slug);
}
