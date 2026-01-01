package com.spring.mcp.service.embedding;

import com.spring.mcp.config.EmbeddingProperties;
import com.spring.mcp.model.entity.EmbeddingJob;
import com.spring.mcp.repository.EmbeddingJobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Scheduled processor for embedding jobs.
 * Automatically picks up pending jobs and processes them in the background.
 *
 * @author Spring MCP Server
 * @version 1.6.0
 * @since 2026-01-01
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "mcp.features.embeddings.enabled", havingValue = "true")
public class EmbeddingJobProcessor {

    private final EmbeddingJobRepository jobRepository;
    private final EmbeddingService embeddingService;
    private final EmbeddingSyncService syncService;
    private final EmbeddingProperties properties;
    private final TransactionTemplate transactionTemplate;

    private final AtomicBoolean processing = new AtomicBoolean(false);

    public EmbeddingJobProcessor(
            EmbeddingJobRepository jobRepository,
            EmbeddingService embeddingService,
            EmbeddingSyncService syncService,
            EmbeddingProperties properties,
            PlatformTransactionManager transactionManager) {
        this.jobRepository = jobRepository;
        this.embeddingService = embeddingService;
        this.syncService = syncService;
        this.properties = properties;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    /**
     * Check if embedding job processing is currently running.
     *
     * @return true if processing is in progress
     */
    public boolean isProcessing() {
        return processing.get();
    }

    /**
     * Process pending jobs on application startup.
     * Also resets any jobs stuck in IN_PROGRESS from previous shutdown.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        // Reset any jobs stuck in IN_PROGRESS from unclean shutdown
        transactionTemplate.executeWithoutResult(status -> {
            int resetCount = jobRepository.resetInProgressJobsToPending(LocalDateTime.now());
            if (resetCount > 0) {
                log.info("Reset {} stuck IN_PROGRESS embedding jobs to PENDING on startup", resetCount);
            }
        });

        long pendingCount = jobRepository.countPendingAndRetryJobs();
        if (pendingCount > 0) {
            log.info("Found {} pending embedding jobs on startup - starting automatic processing", pendingCount);
            // Trigger async processing
            processJobsAsync();
        }
    }

    /**
     * Scheduled job processor - runs every 30 seconds.
     * Checks for pending jobs and processes them if the provider is available.
     */
    @Scheduled(fixedDelayString = "${mcp.features.embeddings.job.poll-interval:30000}")
    public void scheduledJobProcessing() {
        if (!embeddingService.isAvailable()) {
            log.debug("Embedding provider not available, skipping job processing");
            return;
        }

        long pendingCount = jobRepository.countPendingAndRetryJobs();
        if (pendingCount > 0 && !processing.get()) {
            log.debug("Found {} pending embedding jobs, starting processing", pendingCount);
            processJobsAsync();
        }
    }

    /**
     * Process jobs asynchronously.
     */
    private void processJobsAsync() {
        if (processing.compareAndSet(false, true)) {
            try {
                processPendingJobs();
            } finally {
                processing.set(false);
            }
        }
    }

    /**
     * Process all pending and retry-ready jobs.
     * Each job is processed in its own transaction to ensure progress is saved
     * even if the server is stopped mid-processing.
     */
    public void processPendingJobs() {
        if (!embeddingService.isAvailable()) {
            log.warn("Cannot process embedding jobs: provider is not available");
            return;
        }

        int batchSize = properties.getBatchSize();
        int processed = 0;
        int succeeded = 0;
        int failed = 0;

        // Process pending jobs
        List<EmbeddingJob> jobs;
        do {
            // Fetch jobs in a read-only transaction
            jobs = transactionTemplate.execute(status ->
                    jobRepository.findPendingJobs(PageRequest.of(0, batchSize)));

            if (jobs == null) jobs = List.of();

            for (EmbeddingJob job : jobs) {
                // Each job processed in its own transaction - commits immediately
                boolean success = processJobInTransaction(job);
                if (success) {
                    succeeded++;
                } else {
                    failed++;
                }
                processed++;

                // Log progress every 50 jobs
                if (processed % 50 == 0) {
                    log.info("Embedding job progress: {} processed ({} succeeded, {} failed)",
                            processed, succeeded, failed);
                }
            }
        } while (!jobs.isEmpty());

        // Process retry-ready jobs
        List<EmbeddingJob> retryJobs = transactionTemplate.execute(status ->
                jobRepository.findJobsReadyForRetry(LocalDateTime.now(), PageRequest.of(0, batchSize)));

        if (retryJobs == null) retryJobs = List.of();

        for (EmbeddingJob job : retryJobs) {
            boolean success = processJobInTransaction(job);
            if (success) {
                succeeded++;
            } else {
                failed++;
            }
            processed++;
        }

        if (processed > 0) {
            log.info("Embedding job processing complete: {} processed ({} succeeded, {} failed)",
                    processed, succeeded, failed);
        }
    }

    /**
     * Process a single job in its own transaction.
     * This ensures each job's progress is committed immediately.
     * Success and failure are handled in separate transactions to ensure
     * failure state is always persisted.
     *
     * @param job the job to process
     * @return true if successful, false if failed
     */
    private boolean processJobInTransaction(EmbeddingJob job) {
        final Long jobId = job.getId();

        try {
            // Process job in its own transaction
            transactionTemplate.executeWithoutResult(status -> {
                // Re-fetch job within transaction to ensure fresh state
                EmbeddingJob freshJob = jobRepository.findById(jobId)
                        .orElseThrow(() -> new RuntimeException("Job not found: " + jobId));
                processJob(freshJob);
            });
            return true;

        } catch (Exception e) {
            // Handle failure in a SEPARATE transaction to ensure it's saved
            try {
                transactionTemplate.executeWithoutResult(status -> {
                    EmbeddingJob freshJob = jobRepository.findById(jobId)
                            .orElseThrow(() -> new RuntimeException("Job not found: " + jobId));
                    handleJobFailure(freshJob, e);
                });
            } catch (Exception failureEx) {
                log.error("Failed to save job failure state for job {}: {}", jobId, failureEx.getMessage());
            }
            return false;
        }
    }

    /**
     * Process a single embedding job.
     */
    private void processJob(EmbeddingJob job) {
        job.setStatus(EmbeddingJob.JobStatus.IN_PROGRESS);
        job.setUpdatedAt(LocalDateTime.now());
        jobRepository.save(job);

        try {
            // Get text for the entity
            String text = syncService.getTextForEntity(job.getEntityType(), job.getEntityId());

            if (text == null || text.isBlank()) {
                log.debug("Skipping job {} - empty text for {} #{}",
                        job.getId(), job.getEntityType(), job.getEntityId());
                markJobCompleted(job);
                return;
            }

            // Generate embedding (with automatic chunking for long texts)
            float[] embedding = embeddingService.embedWithChunking(text);

            // Store embedding
            syncService.updateEmbeddingForEntity(
                    job.getEntityType(),
                    job.getEntityId(),
                    embedding,
                    embeddingService.getModelName()
            );

            // Mark completed
            markJobCompleted(job);

            log.debug("Processed embedding job {} for {} #{}",
                    job.getId(), job.getEntityType(), job.getEntityId());

        } catch (Exception e) {
            throw new RuntimeException("Failed to process job " + job.getId(), e);
        }
    }

    private void markJobCompleted(EmbeddingJob job) {
        job.setStatus(EmbeddingJob.JobStatus.COMPLETED);
        job.setCompletedAt(LocalDateTime.now());
        job.setUpdatedAt(LocalDateTime.now());
        jobRepository.save(job);
    }

    private void handleJobFailure(EmbeddingJob job, Exception e) {
        log.warn("Embedding job {} failed: {}", job.getId(), e.getMessage());

        int retryCount = job.getRetryCount() + 1;
        int maxRetries = properties.getRetry().getMaxRetries();

        if (retryCount >= maxRetries) {
            // Mark as failed after max retries
            job.markFailed(e.getMessage());
            log.error("Embedding job {} failed permanently after {} retries", job.getId(), retryCount);
        } else {
            // Schedule for retry with exponential backoff
            long delayMs = properties.getRetry().getInitialDelayMs() * (long) Math.pow(2, retryCount - 1);
            LocalDateTime nextRetry = LocalDateTime.now().plusNanos(delayMs * 1_000_000);
            job.markRetryPending(e.getMessage(), nextRetry);
            job.setRetryCount(retryCount);
            log.info("Embedding job {} scheduled for retry {} at {}",
                    job.getId(), retryCount, job.getNextRetryAt());
        }

        jobRepository.save(job);
    }
}
