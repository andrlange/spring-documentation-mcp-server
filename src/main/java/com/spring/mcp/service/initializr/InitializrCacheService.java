package com.spring.mcp.service.initializr;

import com.github.benmanes.caffeine.cache.Cache;
import com.spring.mcp.config.CacheConfig;
import com.spring.mcp.config.InitializrProperties;
import com.spring.mcp.service.initializr.dto.BootVersion;
import com.spring.mcp.service.initializr.dto.DependencyCategory;
import com.spring.mcp.service.initializr.dto.DependencyInfo;
import com.spring.mcp.service.initializr.dto.InitializrMetadata;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Service for managing Initializr metadata cache operations.
 *
 * <p>This service provides higher-level cache management on top of the
 * underlying Caffeine cache, including cache invalidation, status checks,
 * and statistics.</p>
 *
 * <p>Cache statistics are useful for monitoring and debugging:</p>
 * <ul>
 *   <li>Hit/miss ratio - indicates cache effectiveness</li>
 *   <li>Entry count - number of cached items</li>
 *   <li>Last refresh time - when data was last fetched from API</li>
 * </ul>
 *
 * @author Spring MCP Server
 * @version 1.4.0
 * @since 2025-12-06
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "mcp.features.initializr", name = "enabled", havingValue = "true", matchIfMissing = true)
public class InitializrCacheService {

    private final CacheManager cacheManager;
    private final InitializrMetadataService metadataService;
    private final InitializrProperties properties;

    /** Tracks when the cache was last refreshed */
    private final AtomicReference<Instant> lastRefreshTime = new AtomicReference<>();

    /**
     * Get cached metadata, or fetch fresh if not cached.
     *
     * @return the Initializr metadata
     */
    public InitializrMetadata getCachedMetadata() {
        if (!properties.getCache().isEnabled()) {
            log.debug("Cache disabled, fetching fresh metadata");
            return metadataService.fetchMetadata();
        }
        return metadataService.fetchMetadata(); // Uses @Cacheable
    }

    /**
     * Get cached boot versions.
     *
     * @return list of boot versions
     */
    public List<BootVersion> getCachedBootVersions() {
        return metadataService.getBootVersions(); // Uses @Cacheable
    }

    /**
     * Get cached dependency categories.
     *
     * @return list of dependency categories
     */
    public List<DependencyCategory> getCachedDependencyCategories() {
        return metadataService.getDependencyCategories(); // Uses @Cacheable
    }

    /**
     * Get cached dependencies.
     *
     * @return list of all dependencies
     */
    public List<DependencyInfo> getCachedDependencies() {
        return metadataService.getAllDependencies(); // Uses @Cacheable
    }

    /**
     * Invalidate all Initializr caches and refresh data.
     *
     * <p>This method clears all three Initializr caches and optionally
     * triggers a fresh fetch from the API.</p>
     *
     * @param refreshImmediately if true, immediately fetch fresh data
     */
    public void invalidateAndRefresh(boolean refreshImmediately) {
        log.info("Invalidating Initializr caches");

        evictCache(CacheConfig.CACHE_INITIALIZR_METADATA);
        evictCache(CacheConfig.CACHE_INITIALIZR_DEPENDENCIES);
        evictCache(CacheConfig.CACHE_INITIALIZR_CATEGORIES);

        if (refreshImmediately) {
            log.info("Triggering immediate cache refresh");
            try {
                metadataService.fetchMetadata();
                lastRefreshTime.set(Instant.now());
                log.info("Cache refresh completed successfully");
            } catch (Exception e) {
                log.error("Failed to refresh cache", e);
            }
        }
    }

    /**
     * Invalidate only the metadata cache.
     */
    public void invalidateMetadataCache() {
        evictCache(CacheConfig.CACHE_INITIALIZR_METADATA);
    }

    /**
     * Invalidate only the dependencies cache.
     */
    public void invalidateDependenciesCache() {
        evictCache(CacheConfig.CACHE_INITIALIZR_DEPENDENCIES);
    }

    /**
     * Invalidate only the categories cache.
     */
    public void invalidateCategoriesCache() {
        evictCache(CacheConfig.CACHE_INITIALIZR_CATEGORIES);
    }

    /**
     * Get the time of the last cache refresh.
     *
     * @return Optional containing the last refresh time, or empty if never refreshed
     */
    public Optional<Instant> getLastRefreshTime() {
        return Optional.ofNullable(lastRefreshTime.get());
    }

    /**
     * Check if caching is enabled.
     *
     * @return true if caching is enabled in configuration
     */
    public boolean isCachingEnabled() {
        return properties.getCache().isEnabled();
    }

    /**
     * Get cache statistics for all Initializr caches.
     *
     * @return map of cache name to statistics
     */
    public Map<String, CacheStats> getCacheStatistics() {
        Map<String, CacheStats> stats = new HashMap<>();

        stats.put(CacheConfig.CACHE_INITIALIZR_METADATA,
            getCacheStats(CacheConfig.CACHE_INITIALIZR_METADATA));
        stats.put(CacheConfig.CACHE_INITIALIZR_DEPENDENCIES,
            getCacheStats(CacheConfig.CACHE_INITIALIZR_DEPENDENCIES));
        stats.put(CacheConfig.CACHE_INITIALIZR_CATEGORIES,
            getCacheStats(CacheConfig.CACHE_INITIALIZR_CATEGORIES));

        return stats;
    }

    /**
     * Get combined statistics summary.
     *
     * @return summary of all cache statistics
     */
    public CacheSummary getCacheSummary() {
        Map<String, CacheStats> allStats = getCacheStatistics();

        long totalHits = allStats.values().stream()
            .mapToLong(CacheStats::hitCount).sum();
        long totalMisses = allStats.values().stream()
            .mapToLong(CacheStats::missCount).sum();
        long totalEntries = allStats.values().stream()
            .mapToLong(CacheStats::entryCount).sum();

        double hitRate = (totalHits + totalMisses) > 0
            ? (double) totalHits / (totalHits + totalMisses) * 100
            : 0.0;

        return new CacheSummary(
            totalHits,
            totalMisses,
            totalEntries,
            hitRate,
            lastRefreshTime.get(),
            properties.getCache().isEnabled()
        );
    }

    /**
     * Warm up the cache by pre-fetching metadata.
     *
     * <p>This can be called at application startup to ensure
     * the cache is populated before first use.</p>
     */
    public void warmUpCache() {
        log.info("Warming up Initializr cache");
        try {
            metadataService.fetchMetadata();
            metadataService.getDependencyCategories();
            metadataService.getBootVersions();
            lastRefreshTime.set(Instant.now());
            log.info("Cache warm-up completed");
        } catch (Exception e) {
            log.warn("Cache warm-up failed: {}", e.getMessage());
        }
    }

    // Private helper methods

    private void evictCache(String cacheName) {
        org.springframework.cache.Cache cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.clear();
            log.debug("Cleared cache: {}", cacheName);
        } else {
            log.warn("Cache not found: {}", cacheName);
        }
    }

    private CacheStats getCacheStats(String cacheName) {
        org.springframework.cache.Cache springCache = cacheManager.getCache(cacheName);

        if (springCache instanceof CaffeineCache caffeineCache) {
            Cache<Object, Object> nativeCache = caffeineCache.getNativeCache();
            var stats = nativeCache.stats();

            return new CacheStats(
                stats.hitCount(),
                stats.missCount(),
                nativeCache.estimatedSize(),
                stats.hitRate() * 100,
                stats.evictionCount()
            );
        }

        return CacheStats.empty();
    }

    /**
     * Cache statistics for a single cache.
     */
    public record CacheStats(
        long hitCount,
        long missCount,
        long entryCount,
        double hitRatePercent,
        long evictionCount
    ) {
        public static CacheStats empty() {
            return new CacheStats(0, 0, 0, 0.0, 0);
        }
    }

    /**
     * Summary of all cache statistics.
     */
    public record CacheSummary(
        long totalHits,
        long totalMisses,
        long totalEntries,
        double hitRatePercent,
        Instant lastRefreshTime,
        boolean cachingEnabled
    ) {}
}
