package com.spring.mcp.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entity representing global application settings.
 * Used for settings that apply across the entire application.
 *
 * @author Spring MCP Server
 * @version 1.4.3
 * @since 2025-12-08
 */
@Entity
@Table(name = "global_settings")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GlobalSettings {

    public static final String TIME_FORMAT_KEY = "time_format";
    public static final String TIME_FORMAT_24H = "24h";
    public static final String TIME_FORMAT_12H = "12h";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Unique key identifying this setting
     */
    @Column(name = "setting_key", nullable = false, unique = true, length = 100)
    private String settingKey;

    /**
     * Value of the setting
     */
    @Column(name = "setting_value", nullable = false, length = 500)
    private String settingValue;

    /**
     * Description of what this setting controls
     */
    @Column(name = "description")
    private String description;

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
}
