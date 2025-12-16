package com.spring.mcp.model.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * Entity representing monitoring system settings.
 * Stores configuration options for the monitoring dashboard and services.
 *
 * @author Spring MCP Server
 * @version 1.4.4
 * @since 2025-12-16
 */
@Entity
@Table(name = "monitoring_settings")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@EqualsAndHashCode(of = {"id"})
public class MonitoringSettings {

    // Well-known setting keys
    public static final String KEY_RETENTION_HOURS = "retention_hours";
    public static final String KEY_AGGREGATION_ENABLED = "aggregation_enabled";
    public static final String KEY_HEARTBEAT_INTERVAL_MS = "heartbeat_interval_ms";
    public static final String KEY_AUTO_REFRESH_SECONDS = "auto_refresh_seconds";
    public static final String KEY_CLEANUP_INTERVAL_HOURS = "cleanup_interval_hours";

    // Default values
    public static final int DEFAULT_RETENTION_HOURS = 24;
    public static final boolean DEFAULT_AGGREGATION_ENABLED = true;
    public static final int DEFAULT_HEARTBEAT_INTERVAL_MS = 30000;
    public static final int DEFAULT_AUTO_REFRESH_SECONDS = 30;
    public static final int DEFAULT_CLEANUP_INTERVAL_HOURS = 6;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "setting_key", nullable = false, unique = true, length = 100)
    private String settingKey;

    @Column(name = "setting_value", length = 500)
    private String settingValue;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "updated_by", length = 100)
    private String updatedBy;

    /**
     * Get setting value as integer.
     */
    public Integer getIntValue() {
        if (settingValue == null || settingValue.isBlank()) {
            return null;
        }
        try {
            return Integer.parseInt(settingValue.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Get setting value as integer with default.
     */
    public int getIntValue(int defaultValue) {
        Integer value = getIntValue();
        return value != null ? value : defaultValue;
    }

    /**
     * Get setting value as long.
     */
    public Long getLongValue() {
        if (settingValue == null || settingValue.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(settingValue.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Get setting value as boolean.
     */
    public Boolean getBooleanValue() {
        if (settingValue == null || settingValue.isBlank()) {
            return null;
        }
        String value = settingValue.trim().toLowerCase();
        return "true".equals(value) || "1".equals(value) || "yes".equals(value);
    }

    /**
     * Get setting value as boolean with default.
     */
    public boolean getBooleanValue(boolean defaultValue) {
        Boolean value = getBooleanValue();
        return value != null ? value : defaultValue;
    }

    /**
     * Update the setting value.
     */
    public void updateValue(String newValue, String updatedBy) {
        this.settingValue = newValue;
        this.updatedAt = LocalDateTime.now();
        this.updatedBy = updatedBy;
    }

    /**
     * Create a new setting.
     */
    public static MonitoringSettings create(String key, String value, String description, String updatedBy) {
        return MonitoringSettings.builder()
                .settingKey(key)
                .settingValue(value)
                .description(description)
                .updatedAt(LocalDateTime.now())
                .updatedBy(updatedBy)
                .build();
    }

    @PreUpdate
    @PrePersist
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
