package com.example.cache.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * Caffeine cache configuration for the book search demo.
 *
 * Cache settings:
 * - Maximum 500 entries per cache
 * - 60-second TTL (time-to-live) after write
 * - Statistics recording enabled for monitoring
 */
@Configuration
@EnableCaching
public class CacheConfig {

    /**
     * Cache name for book search results.
     * Key: search query string
     * Value: List of matching books
     */
    public static final String BOOK_SEARCH_CACHE = "bookSearchCache";

    /**
     * Cache name for individual book lookups.
     * Key: book ID
     * Value: Book object
     */
    public static final String BOOK_BY_ID_CACHE = "bookByIdCache";

    /**
     * Configures the Caffeine cache builder with:
     * - Maximum 500 entries (prevents memory overflow)
     * - 60-second expiration after write (as per requirements)
     * - Statistics recording for cache monitoring
     */
    @Bean
    public Caffeine<Object, Object> caffeineConfig() {
        return Caffeine.newBuilder()
            .maximumSize(500)
            .expireAfterWrite(1, TimeUnit.MINUTES)
            .recordStats();
    }

    /**
     * Creates a CacheManager that manages both book caches.
     * Uses the Caffeine configuration defined above.
     */
    @Bean
    public CacheManager cacheManager(Caffeine<Object, Object> caffeine) {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager(
            BOOK_SEARCH_CACHE,
            BOOK_BY_ID_CACHE
        );
        cacheManager.setCaffeine(caffeine);
        return cacheManager;
    }
}
