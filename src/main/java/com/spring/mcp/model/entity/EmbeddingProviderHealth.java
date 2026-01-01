package com.spring.mcp.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entity for tracking embedding provider health status.
 *
 * @author Spring MCP Server
 * @version 1.6.0
 * @since 2026-01-01
 */
@Entity
@Table(name = "embedding_provider_health")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@EqualsAndHashCode(of = {"id"})
public class EmbeddingProviderHealth {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Provider name (ollama, openai, etc.)
     */
    @Column(nullable = false, unique = true, length = 50)
    private String provider;

    /**
     * Whether the provider is currently available.
     */
    @Column(name = "is_available", nullable = false)
    @Builder.Default
    private Boolean isAvailable = false;

    /**
     * Last health check timestamp.
     */
    @Column(name = "last_check_at")
    private LocalDateTime lastCheckAt;

    /**
     * Last successful health check timestamp.
     */
    @Column(name = "last_success_at")
    private LocalDateTime lastSuccessAt;

    /**
     * Last error message.
     */
    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError;

    /**
     * Number of consecutive failures.
     */
    @Column(name = "consecutive_failures", nullable = false)
    @Builder.Default
    private Integer consecutiveFailures = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

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
     * Record a successful health check.
     */
    public void recordSuccess() {
        this.isAvailable = true;
        this.lastCheckAt = LocalDateTime.now();
        this.lastSuccessAt = LocalDateTime.now();
        this.consecutiveFailures = 0;
        this.lastError = null;
    }

    /**
     * Record a failed health check.
     */
    public void recordFailure(String error) {
        this.isAvailable = false;
        this.lastCheckAt = LocalDateTime.now();
        this.consecutiveFailures++;
        this.lastError = error;
    }
}
