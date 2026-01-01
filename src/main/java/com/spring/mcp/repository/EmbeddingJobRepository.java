package com.spring.mcp.repository;

import com.spring.mcp.model.entity.EmbeddingJob;
import com.spring.mcp.model.entity.EmbeddingJob.JobStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for EmbeddingJob entity.
 * Manages async embedding job queue.
 *
 * @author Spring MCP Server
 * @version 1.6.0
 * @since 2026-01-01
 */
@Repository
public interface EmbeddingJobRepository extends JpaRepository<EmbeddingJob, Long> {

    /**
     * Find pending jobs ordered by priority and creation time.
     */
    @Query("SELECT j FROM EmbeddingJob j WHERE j.status = 'PENDING' ORDER BY j.priority ASC, j.createdAt ASC")
    List<EmbeddingJob> findPendingJobs(Pageable pageable);

    /**
     * Find jobs ready for retry.
     */
    @Query("SELECT j FROM EmbeddingJob j WHERE j.status = 'RETRY_PENDING' AND j.nextRetryAt <= :now ORDER BY j.priority ASC, j.createdAt ASC")
    List<EmbeddingJob> findJobsReadyForRetry(@Param("now") LocalDateTime now, Pageable pageable);

    /**
     * Find job for a specific entity.
     */
    Optional<EmbeddingJob> findByEntityTypeAndEntityId(String entityType, Long entityId);

    /**
     * Find job for a specific entity with given status.
     */
    Optional<EmbeddingJob> findByEntityTypeAndEntityIdAndStatus(String entityType, Long entityId, JobStatus status);

    /**
     * Find jobs by status.
     */
    List<EmbeddingJob> findByStatus(JobStatus status);

    /**
     * Count jobs by status.
     */
    long countByStatus(JobStatus status);

    /**
     * Count pending and retry jobs.
     */
    @Query("SELECT COUNT(j) FROM EmbeddingJob j WHERE j.status IN ('PENDING', 'RETRY_PENDING')")
    long countPendingAndRetryJobs();

    /**
     * Delete completed jobs older than given date.
     */
    @Modifying
    @Query("DELETE FROM EmbeddingJob j WHERE j.status = 'COMPLETED' AND j.completedAt < :before")
    int deleteCompletedJobsOlderThan(@Param("before") LocalDateTime before);

    /**
     * Delete failed jobs older than given date.
     */
    @Modifying
    @Query("DELETE FROM EmbeddingJob j WHERE j.status = 'FAILED' AND j.updatedAt < :before")
    int deleteFailedJobsOlderThan(@Param("before") LocalDateTime before);

    /**
     * Cancel all pending jobs for an entity type.
     */
    @Modifying
    @Query("UPDATE EmbeddingJob j SET j.status = 'CANCELLED', j.updatedAt = :now WHERE j.entityType = :entityType AND j.status IN ('PENDING', 'RETRY_PENDING')")
    int cancelPendingJobsByEntityType(@Param("entityType") String entityType, @Param("now") LocalDateTime now);

    /**
     * Find jobs by entity type with pagination.
     */
    Page<EmbeddingJob> findByEntityType(String entityType, Pageable pageable);

    /**
     * Check if a pending/in-progress job exists for an entity.
     */
    @Query("SELECT COUNT(j) > 0 FROM EmbeddingJob j WHERE j.entityType = :entityType AND j.entityId = :entityId AND j.status IN ('PENDING', 'IN_PROGRESS', 'RETRY_PENDING')")
    boolean existsPendingJobForEntity(@Param("entityType") String entityType, @Param("entityId") Long entityId);

    /**
     * Get job statistics by status.
     */
    @Query("SELECT j.status, COUNT(j) FROM EmbeddingJob j GROUP BY j.status")
    List<Object[]> getJobStatsByStatus();

    /**
     * Get job statistics by entity type.
     */
    @Query("SELECT j.entityType, j.status, COUNT(j) FROM EmbeddingJob j GROUP BY j.entityType, j.status")
    List<Object[]> getJobStatsByEntityType();

    /**
     * Reset stuck IN_PROGRESS jobs to PENDING on startup.
     * This handles jobs that were interrupted by server shutdown.
     */
    @Modifying
    @Query("UPDATE EmbeddingJob j SET j.status = 'PENDING', j.updatedAt = :now WHERE j.status = 'IN_PROGRESS'")
    int resetInProgressJobsToPending(@Param("now") LocalDateTime now);

    /**
     * Count jobs currently in progress.
     */
    @Query("SELECT COUNT(j) FROM EmbeddingJob j WHERE j.status = 'IN_PROGRESS'")
    long countInProgressJobs();
}
