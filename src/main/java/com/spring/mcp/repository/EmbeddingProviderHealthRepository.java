package com.spring.mcp.repository;

import com.spring.mcp.model.entity.EmbeddingProviderHealth;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for EmbeddingProviderHealth entity.
 *
 * @author Spring MCP Server
 * @version 1.6.0
 * @since 2026-01-01
 */
@Repository
public interface EmbeddingProviderHealthRepository extends JpaRepository<EmbeddingProviderHealth, Long> {

    /**
     * Find health record by provider name.
     */
    Optional<EmbeddingProviderHealth> findByProvider(String provider);

    /**
     * Check if provider is available.
     */
    default boolean isProviderAvailable(String provider) {
        return findByProvider(provider)
                .map(EmbeddingProviderHealth::getIsAvailable)
                .orElse(false);
    }
}
