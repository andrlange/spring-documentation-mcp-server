package com.spring.mcp.model.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * Entity tracking Javadoc sync status per project.
 * Manages enabled/disabled state and failure tracking for automatic disable.
 */
@Entity
@Table(name = "javadoc_sync_status")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"project"})
@EqualsAndHashCode(of = {"id"})
public class JavadocSyncStatus {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false, unique = true)
    private SpringProject project;

    @Column(nullable = false)
    @Builder.Default
    private Boolean enabled = false;

    @Column(name = "last_sync_at")
    private LocalDateTime lastSyncAt;

    @Column(name = "failure_count", nullable = false)
    @Builder.Default
    private Integer failureCount = 0;

    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * Record a successful sync, resetting failure count.
     */
    public void recordSuccess() {
        this.lastSyncAt = LocalDateTime.now();
        this.failureCount = 0;
        this.lastError = null;
    }

    /**
     * Record a failed sync attempt.
     *
     * @param errorMessage the error message
     * @param maxFailures  max allowed failures before auto-disable
     */
    public void recordFailure(String errorMessage, int maxFailures) {
        this.failureCount++;
        this.lastError = errorMessage;
        if (this.failureCount >= maxFailures) {
            this.enabled = false;
        }
    }

    /**
     * Check if sync is currently failing (has recent errors).
     */
    public boolean isInFailureState() {
        return failureCount > 0;
    }

    /**
     * Check if sync was auto-disabled due to too many failures.
     */
    public boolean wasAutoDisabled(int maxFailures) {
        return !enabled && failureCount >= maxFailures;
    }
}
