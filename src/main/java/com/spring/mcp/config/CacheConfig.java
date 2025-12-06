package com.spring.mcp.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

/**
 * Cache configuration using Caffeine high-performance caching library.
 *
 * <p>Caffeine provides near-optimal hit rates using the Window TinyLfu eviction policy,
 * making it ideal for caching frequently accessed data like documentation and Initializr metadata.</p>
 *
 * <h3>Cache Definitions:</h3>
 * <ul>
 *   <li><b>documentationSearch</b>: Basic search results (1 hour TTL, max 500 entries)</li>
 *   <li><b>documentationSearchPaged</b>: Paginated search results (30 min TTL, max 200 entries)</li>
 *   <li><b>documentationByVersion</b>: Documentation by version ID (2 hour TTL, max 100 entries)</li>
 *   <li><b>initializr-metadata</b>: Full Initializr API metadata (1 hour TTL, max 10 entries)</li>
 *   <li><b>initializr-dependencies</b>: Individual dependency lookups (30 min TTL, max 500 entries)</li>
 *   <li><b>initializr-categories</b>: Dependency categories (1 hour TTL, max 50 entries)</li>
 * </ul>
 *
 * @see <a href="https://github.com/ben-manes/caffeine">Caffeine GitHub</a>
 */
@Configuration
@EnableCaching
public class CacheConfig {

    // Cache names as constants for use in @Cacheable annotations
    public static final String CACHE_DOCUMENTATION_SEARCH = "documentationSearch";
    public static final String CACHE_DOCUMENTATION_SEARCH_PAGED = "documentationSearchPaged";
    public static final String CACHE_DOCUMENTATION_BY_VERSION = "documentationByVersion";
    public static final String CACHE_INITIALIZR_METADATA = "initializr-metadata";
    public static final String CACHE_INITIALIZR_DEPENDENCIES = "initializr-dependencies";
    public static final String CACHE_INITIALIZR_CATEGORIES = "initializr-categories";

    /**
     * Configure Caffeine cache manager with predefined caches and TTL settings.
     *
     * @return configured CacheManager with Caffeine-backed caches
     */
    @Bean
    public CacheManager cacheManager() {
        SimpleCacheManager cacheManager = new SimpleCacheManager();

        cacheManager.setCaches(Arrays.asList(
            // Documentation caches (existing)
            buildCache(CACHE_DOCUMENTATION_SEARCH, 60, TimeUnit.MINUTES, 500),
            buildCache(CACHE_DOCUMENTATION_SEARCH_PAGED, 30, TimeUnit.MINUTES, 200),
            buildCache(CACHE_DOCUMENTATION_BY_VERSION, 120, TimeUnit.MINUTES, 100),

            // Initializr caches (new for Phase 0)
            buildCache(CACHE_INITIALIZR_METADATA, 60, TimeUnit.MINUTES, 10),
            buildCache(CACHE_INITIALIZR_DEPENDENCIES, 30, TimeUnit.MINUTES, 500),
            buildCache(CACHE_INITIALIZR_CATEGORIES, 60, TimeUnit.MINUTES, 50)
        ));

        return cacheManager;
    }

    /**
     * Build a Caffeine-backed cache with specified TTL and max size.
     *
     * @param name cache name
     * @param ttl time-to-live value
     * @param timeUnit time unit for TTL
     * @param maxSize maximum number of entries
     * @return configured CaffeineCache
     */
    private CaffeineCache buildCache(String name, long ttl, TimeUnit timeUnit, int maxSize) {
        return new CaffeineCache(name,
            Caffeine.newBuilder()
                .expireAfterWrite(ttl, timeUnit)
                .maximumSize(maxSize)
                .recordStats()  // Enable statistics for monitoring
                .build()
        );
    }
}
