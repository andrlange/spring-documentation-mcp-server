package com.spring.mcp.service.settings;

import com.spring.mcp.model.entity.GlobalSettings;
import com.spring.mcp.repository.GlobalSettingsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for managing global application settings.
 * These settings apply across the entire application (e.g., time format).
 *
 * @author Spring MCP Server
 * @version 1.4.3
 * @since 2025-12-08
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GlobalSettingsService {

    private final GlobalSettingsRepository globalSettingsRepository;

    /**
     * Get the global time format setting (12h or 24h).
     *
     * @return the time format ("12h" or "24h"), defaults to "24h"
     */
    public String getTimeFormat() {
        return globalSettingsRepository.findBySettingKey(GlobalSettings.TIME_FORMAT_KEY)
            .map(GlobalSettings::getSettingValue)
            .orElse(GlobalSettings.TIME_FORMAT_24H);
    }

    /**
     * Update the global time format setting.
     *
     * @param timeFormat the new time format ("12h" or "24h")
     * @return the updated GlobalSettings entity
     */
    @Transactional
    public GlobalSettings updateTimeFormat(String timeFormat) {
        // Validate input
        if (!GlobalSettings.TIME_FORMAT_12H.equals(timeFormat) &&
            !GlobalSettings.TIME_FORMAT_24H.equals(timeFormat)) {
            throw new IllegalArgumentException("Invalid time format: " + timeFormat +
                ". Must be '12h' or '24h'");
        }

        GlobalSettings settings = globalSettingsRepository.findBySettingKey(GlobalSettings.TIME_FORMAT_KEY)
            .orElseGet(() -> createDefaultTimeFormatSetting());

        settings.setSettingValue(timeFormat);
        settings = globalSettingsRepository.save(settings);

        log.info("Global time format updated to: {}", timeFormat);
        return settings;
    }

    /**
     * Get a setting by key.
     *
     * @param key the setting key
     * @return the setting value, or null if not found
     */
    public String getSetting(String key) {
        return globalSettingsRepository.findBySettingKey(key)
            .map(GlobalSettings::getSettingValue)
            .orElse(null);
    }

    /**
     * Set a setting value.
     *
     * @param key the setting key
     * @param value the setting value
     * @param description optional description
     * @return the updated GlobalSettings entity
     */
    @Transactional
    public GlobalSettings setSetting(String key, String value, String description) {
        GlobalSettings settings = globalSettingsRepository.findBySettingKey(key)
            .orElseGet(() -> GlobalSettings.builder()
                .settingKey(key)
                .settingValue(value)
                .description(description)
                .build());

        settings.setSettingValue(value);
        if (description != null) {
            settings.setDescription(description);
        }

        return globalSettingsRepository.save(settings);
    }

    /**
     * Create default time format setting if it doesn't exist.
     */
    private GlobalSettings createDefaultTimeFormatSetting() {
        GlobalSettings settings = GlobalSettings.builder()
            .settingKey(GlobalSettings.TIME_FORMAT_KEY)
            .settingValue(GlobalSettings.TIME_FORMAT_24H)
            .description("Display format for time (12h or 24h)")
            .build();

        return globalSettingsRepository.save(settings);
    }
}
