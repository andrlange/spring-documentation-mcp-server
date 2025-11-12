package com.spring.mcp.model.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * API Key entity for MCP endpoint authentication
 * Keys are stored as BCrypt hashes for security
 */
@Entity
@Table(name = "api_keys")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiKey {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Human-readable name for the API key (minimum 3 characters)
     */
    @NotBlank(message = "API key name is required")
    @Size(min = 3, max = 255, message = "API key name must be between 3 and 255 characters")
    @Column(nullable = false, unique = true)
    private String name;

    /**
     * BCrypt hashed API key
     * NEVER store plain text keys in database
     */
    @NotBlank
    @Column(name = "key_hash", nullable = false)
    private String keyHash;

    /**
     * Timestamp when the API key was created
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Username of the user who created this API key
     */
    @NotBlank
    @Column(name = "created_by", nullable = false, updatable = false)
    private String createdBy;

    /**
     * Timestamp of last successful authentication with this key
     */
    @Column(name = "last_used_at")
    private LocalDateTime lastUsedAt;

    /**
     * Whether this API key is currently active
     */
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    /**
     * Optional description of the API key's purpose
     */
    @Column(length = 1000)
    private String description;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (isActive == null) {
            isActive = true;
        }
    }

    /**
     * Update last used timestamp
     */
    public void updateLastUsed() {
        this.lastUsedAt = LocalDateTime.now();
    }
}
