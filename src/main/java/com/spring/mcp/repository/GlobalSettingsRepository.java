package com.spring.mcp.repository;

import com.spring.mcp.model.entity.GlobalSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for GlobalSettings entity.
 *
 * @author Spring MCP Server
 * @version 1.4.3
 * @since 2025-12-08
 */
@Repository
public interface GlobalSettingsRepository extends JpaRepository<GlobalSettings, Long> {

    /**
     * Find a setting by its unique key.
     *
     * @param settingKey the setting key
     * @return the setting if found
     */
    Optional<GlobalSettings> findBySettingKey(String settingKey);

    /**
     * Check if a setting exists.
     *
     * @param settingKey the setting key
     * @return true if exists
     */
    boolean existsBySettingKey(String settingKey);
}
