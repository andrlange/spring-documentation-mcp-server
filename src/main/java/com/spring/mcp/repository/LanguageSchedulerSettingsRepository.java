package com.spring.mcp.repository;

import com.spring.mcp.model.entity.LanguageSchedulerSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for LanguageSchedulerSettings entity.
 * Since there should only be one settings record, provides convenience method.
 *
 * @author Spring MCP Server
 * @version 1.2.0
 * @since 2025-11-29
 */
@Repository
public interface LanguageSchedulerSettingsRepository extends JpaRepository<LanguageSchedulerSettings, Long> {

    /**
     * Find the first (and should be only) language scheduler settings record
     *
     * @return Optional containing the settings, or empty if none exists
     */
    Optional<LanguageSchedulerSettings> findFirstByOrderByIdAsc();
}
