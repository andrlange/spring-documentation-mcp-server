package com.spring.mcp.service;

import com.spring.mcp.model.entity.ApiKey;
import com.spring.mcp.repository.ApiKeyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Service for API Key management
 * Handles secure key generation, validation, and lifecycle management
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ApiKeyService {

    private final ApiKeyRepository apiKeyRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder(12); // Strong cost factor
    private final SecureRandom secureRandom = new SecureRandom();

    // Key configuration
    private static final int KEY_LENGTH_BYTES = 32; // 256 bits
    private static final String KEY_PREFIX = "smcp_"; // Spring MCP prefix for identification

    /**
     * Generate a cryptographically secure random API key
     * Format: smcp_<base64-encoded-random-bytes>
     *
     * @return Secure random API key (plain text - show only once!)
     */
    public String generateSecureKey() {
        byte[] randomBytes = new byte[KEY_LENGTH_BYTES];
        secureRandom.nextBytes(randomBytes);

        String encodedKey = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
        return KEY_PREFIX + encodedKey;
    }

    /**
     * Create a new API key
     *
     * @param name Name for the API key (min 3 chars)
     * @param createdBy Username of creator
     * @param description Optional description
     * @return Map with the new ApiKey entity and the plain text key (for one-time display)
     */
    @Transactional
    public Map<String, Object> createApiKey(String name, String createdBy, String description) {
        log.info("Creating new API key: name={}, createdBy={}", name, createdBy);

        // Validate name
        if (name == null || name.trim().length() < 3) {
            throw new IllegalArgumentException("API key name must be at least 3 characters long");
        }

        // Check for duplicate name
        if (apiKeyRepository.existsByName(name.trim())) {
            throw new IllegalArgumentException("API key with name '" + name + "' already exists");
        }

        // Generate secure key
        String plainTextKey = generateSecureKey();

        // Hash the key for storage (NEVER store plain text!)
        String keyHash = passwordEncoder.encode(plainTextKey);

        // Create entity
        ApiKey apiKey = new ApiKey();
        apiKey.setName(name.trim());
        apiKey.setKeyHash(keyHash);
        apiKey.setCreatedBy(createdBy);
        apiKey.setDescription(description);
        apiKey.setIsActive(true);
        apiKey.setCreatedAt(LocalDateTime.now());

        // Save to database
        ApiKey saved = apiKeyRepository.save(apiKey);

        log.info("API key created successfully: id={}, name={}", saved.getId(), saved.getName());

        // Return both the entity and plain text key
        // IMPORTANT: Plain text key should be shown only once to the user!
        return Map.of(
            "apiKey", saved,
            "plainTextKey", plainTextKey
        );
    }

    /**
     * Validate an API key against stored hash
     * Updates last_used_at timestamp on successful validation
     *
     * @param plainTextKey The API key to validate
     * @return Optional containing the ApiKey if valid and active
     */
    @Transactional
    public Optional<ApiKey> validateKey(String plainTextKey) {
        if (plainTextKey == null || plainTextKey.isBlank()) {
            return Optional.empty();
        }

        // Get all active keys and check against each hash
        // Note: This is not the most efficient approach for large numbers of keys
        // For production with many keys, consider adding an index or caching strategy
        List<ApiKey> activeKeys = apiKeyRepository.findByIsActiveTrue();

        for (ApiKey apiKey : activeKeys) {
            try {
                if (passwordEncoder.matches(plainTextKey, apiKey.getKeyHash())) {
                    // Valid key found - update last used timestamp
                    apiKey.updateLastUsed();
                    apiKeyRepository.save(apiKey);

                    log.debug("API key validated successfully: name={}", apiKey.getName());
                    return Optional.of(apiKey);
                }
            } catch (Exception e) {
                log.warn("Error validating API key: {}", e.getMessage());
                continue;
            }
        }

        log.warn("Invalid API key attempt");
        return Optional.empty();
    }

    /**
     * Get all API keys (without plain text keys)
     */
    public List<ApiKey> getAllApiKeys() {
        return apiKeyRepository.findAllByOrderByCreatedAtDesc();
    }

    /**
     * Get active API keys only
     */
    public List<ApiKey> getActiveApiKeys() {
        return apiKeyRepository.findByIsActiveTrue();
    }

    /**
     * Get API key by ID
     */
    public Optional<ApiKey> getApiKeyById(Long id) {
        return apiKeyRepository.findById(id);
    }

    /**
     * Get API key by name
     */
    public Optional<ApiKey> getApiKeyByName(String name) {
        return apiKeyRepository.findByName(name);
    }

    /**
     * Deactivate an API key (soft delete)
     */
    @Transactional
    public void deactivateApiKey(Long id) {
        ApiKey apiKey = apiKeyRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("API key not found: " + id));

        apiKey.setIsActive(false);
        apiKeyRepository.save(apiKey);

        log.info("API key deactivated: id={}, name={}", id, apiKey.getName());
    }

    /**
     * Reactivate a deactivated API key
     */
    @Transactional
    public void reactivateApiKey(Long id) {
        ApiKey apiKey = apiKeyRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("API key not found: " + id));

        apiKey.setIsActive(true);
        apiKeyRepository.save(apiKey);

        log.info("API key reactivated: id={}, name={}", id, apiKey.getName());
    }

    /**
     * Permanently delete an API key
     */
    @Transactional
    public void deleteApiKey(Long id) {
        ApiKey apiKey = apiKeyRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("API key not found: " + id));

        apiKeyRepository.delete(apiKey);

        log.info("API key deleted permanently: id={}, name={}", id, apiKey.getName());
    }

    /**
     * Update API key description
     */
    @Transactional
    public ApiKey updateDescription(Long id, String description) {
        ApiKey apiKey = apiKeyRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("API key not found: " + id));

        apiKey.setDescription(description);
        ApiKey updated = apiKeyRepository.save(apiKey);

        log.info("API key description updated: id={}, name={}", id, apiKey.getName());
        return updated;
    }

    /**
     * Get statistics about API keys
     */
    public Map<String, Object> getStatistics() {
        long total = apiKeyRepository.count();
        long active = apiKeyRepository.countByIsActiveTrue();
        long inactive = total - active;

        return Map.of(
            "total", total,
            "active", active,
            "inactive", inactive
        );
    }
}
