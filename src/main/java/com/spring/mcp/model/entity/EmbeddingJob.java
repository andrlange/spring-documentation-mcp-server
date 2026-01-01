package com.spring.mcp.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entity for tracking async embedding job state.
 * Embeddings are generated asynchronously after sync operations.
 *
 * @author Spring MCP Server
 * @version 1.6.0
 * @since 2026-01-01
 */
@Entity
@Table(name = "embedding_jobs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@EqualsAndHashCode(of = {"id"})
public class EmbeddingJob {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Type of entity to embed.
     */
    @Column(name = "entity_type", nullable = false, length = 100)
    private String entityType;

    /**
     * ID of the entity to embed.
     */
    @Column(name = "entity_id", nullable = false)
    private Long entityId;

    /**
     * Type of job.
     */
    @Column(name = "job_type", nullable = false, length = 50)
    @Builder.Default
    private String jobType = "SINGLE_ENTITY";

    /**
     * Current status of the job.
     */
    @Column(name = "status", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private JobStatus status = JobStatus.PENDING;

    /**
     * Priority (lower = higher priority).
     */
    @Column(nullable = false)
    @Builder.Default
    private Integer priority = 5;

    /**
     * Number of retry attempts.
     */
    @Column(name = "retry_count", nullable = false)
    @Builder.Default
    private Integer retryCount = 0;

    /**
     * Maximum number of retries allowed.
     */
    @Column(name = "max_retries", nullable = false)
    @Builder.Default
    private Integer maxRetries = 10;

    /**
     * Time when next retry should be attempted.
     */
    @Column(name = "next_retry_at")
    private LocalDateTime nextRetryAt;

    /**
     * Last error message if job failed.
     */
    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError;

    /**
     * Provider used for embedding.
     */
    @Column(length = 50)
    private String provider;

    /**
     * Model used for embedding.
     */
    @Column(length = 100)
    private String model;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * Mark this job as in progress.
     */
    public void markInProgress() {
        this.status = JobStatus.IN_PROGRESS;
        this.startedAt = LocalDateTime.now();
    }

    /**
     * Mark this job as completed.
     */
    public void markCompleted() {
        this.status = JobStatus.COMPLETED;
        this.completedAt = LocalDateTime.now();
    }

    /**
     * Mark this job as failed with retry.
     */
    public void markRetryPending(String error, LocalDateTime nextRetry) {
        this.status = JobStatus.RETRY_PENDING;
        this.lastError = error;
        this.retryCount++;
        this.nextRetryAt = nextRetry;
    }

    /**
     * Mark this job as permanently failed.
     */
    public void markFailed(String error) {
        this.status = JobStatus.FAILED;
        this.lastError = error;
    }

    /**
     * Check if this job can be retried.
     */
    public boolean canRetry() {
        return retryCount < maxRetries;
    }

    /**
     * Job status values.
     */
    public enum JobStatus {
        PENDING,
        IN_PROGRESS,
        COMPLETED,
        FAILED,
        RETRY_PENDING,
        CANCELLED
    }
}
