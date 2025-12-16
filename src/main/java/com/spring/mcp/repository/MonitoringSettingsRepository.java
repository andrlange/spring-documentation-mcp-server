package com.spring.mcp.repository;

import com.spring.mcp.model.entity.MonitoringSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for MonitoringSettings entities.
 * Provides access to monitoring configuration settings.
 *
 * @author Spring MCP Server
 * @version 1.4.4
 * @since 2025-12-16
 */
@Repository
public interface MonitoringSettingsRepository extends JpaRepository<MonitoringSettings, Long> {

    /**
     * Find setting by key.
     */
    Optional<MonitoringSettings> findBySettingKey(String settingKey);

    /**
     * Check if setting exists.
     */
    boolean existsBySettingKey(String settingKey);

    /**
     * Update setting value.
     */
    @Modifying
    @Query("UPDATE MonitoringSettings s SET s.settingValue = :value, s.updatedBy = :updatedBy, s.updatedAt = CURRENT_TIMESTAMP WHERE s.settingKey = :key")
    int updateSetting(
            @Param("key") String key,
            @Param("value") String value,
            @Param("updatedBy") String updatedBy);

    /**
     * Get setting value by key (convenience method).
     */
    @Query("SELECT s.settingValue FROM MonitoringSettings s WHERE s.settingKey = :key")
    Optional<String> getValueByKey(@Param("key") String key);

    /**
     * Delete setting by key.
     */
    void deleteBySettingKey(String settingKey);
}
