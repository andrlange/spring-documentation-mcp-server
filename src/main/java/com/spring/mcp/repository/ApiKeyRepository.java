package com.spring.mcp.repository;

import com.spring.mcp.model.entity.ApiKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for API Key management
 */
@Repository
public interface ApiKeyRepository extends JpaRepository<ApiKey, Long> {

    /**
     * Find API key by name
     */
    Optional<ApiKey> findByName(String name);

    /**
     * Find all active API keys
     */
    List<ApiKey> findByIsActiveTrue();

    /**
     * Find all API keys ordered by created date (newest first)
     */
    List<ApiKey> findAllByOrderByCreatedAtDesc();

    /**
     * Find API keys created by a specific user
     */
    List<ApiKey> findByCreatedByOrderByCreatedAtDesc(String createdBy);

    /**
     * Count active API keys
     */
    long countByIsActiveTrue();

    /**
     * Check if an API key name already exists
     */
    boolean existsByName(String name);
}
