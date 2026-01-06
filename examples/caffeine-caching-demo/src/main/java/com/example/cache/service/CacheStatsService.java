package com.example.cache.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.stats.CacheStats;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Service for retrieving Caffeine cache statistics.
 * Provides real-time monitoring of cache performance.
 */
@Service
public class CacheStatsService {

    private final CacheManager cacheManager;

    public CacheStatsService(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    /**
     * Record containing cache statistics for display.
     */
    public record CacheStatistics(
        String cacheName,
        long hitCount,
        long missCount,
        double hitRate,
        long evictionCount,
        long requestCount,
        long estimatedSize
    ) {
        /**
         * Calculate hit rate percentage for display.
         */
        public String hitRatePercentage() {
            return String.format("%.1f%%", hitRate * 100);
        }
    }

    /**
     * Get statistics for all configured caches.
     *
     * @return Map of cache name to statistics
     */
    public Map<String, CacheStatistics> getAllCacheStats() {
        Map<String, CacheStatistics> stats = new HashMap<>();

        for (String cacheName : cacheManager.getCacheNames()) {
            var cache = cacheManager.getCache(cacheName);
            if (cache instanceof CaffeineCache caffeineCache) {
                Cache<Object, Object> nativeCache = caffeineCache.getNativeCache();
                CacheStats cacheStats = nativeCache.stats();

                stats.put(cacheName, new CacheStatistics(
                    cacheName,
                    cacheStats.hitCount(),
                    cacheStats.missCount(),
                    cacheStats.hitRate(),
                    cacheStats.evictionCount(),
                    cacheStats.requestCount(),
                    nativeCache.estimatedSize()
                ));
            }
        }
        return stats;
    }

    /**
     * Get aggregated statistics across all caches.
     * Useful for displaying overall cache health.
     *
     * @return Aggregated cache statistics
     */
    public CacheStatistics getAggregatedStats() {
        var allStats = getAllCacheStats();

        long totalHits = allStats.values().stream()
            .mapToLong(CacheStatistics::hitCount)
            .sum();
        long totalMisses = allStats.values().stream()
            .mapToLong(CacheStatistics::missCount)
            .sum();
        long totalEvictions = allStats.values().stream()
            .mapToLong(CacheStatistics::evictionCount)
            .sum();
        long totalRequests = allStats.values().stream()
            .mapToLong(CacheStatistics::requestCount)
            .sum();
        long totalSize = allStats.values().stream()
            .mapToLong(CacheStatistics::estimatedSize)
            .sum();

        double hitRate = totalRequests > 0
            ? (double) totalHits / totalRequests
            : 0.0;

        return new CacheStatistics(
            "AGGREGATED",
            totalHits,
            totalMisses,
            hitRate,
            totalEvictions,
            totalRequests,
            totalSize
        );
    }
}
