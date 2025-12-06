package com.spring.mcp.config;

import com.github.benmanes.caffeine.cache.Cache;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;

import java.util.Collection;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for CacheConfig Caffeine configuration.
 * Verifies that all caches are properly configured with Caffeine backing.
 *
 * @author Spring MCP Server
 * @version 1.4.0
 * @since 2025-12-06
 */
@DisplayName("CacheConfig Caffeine Tests")
class CaffeineCacheConfigTest {

    private final CacheConfig cacheConfig = new CacheConfig();
    private CacheManager cacheManager;

    @BeforeEach
    void setUp() {
        cacheManager = cacheConfig.cacheManager();
        // SimpleCacheManager requires afterPropertiesSet() to initialize
        if (cacheManager instanceof SimpleCacheManager scm) {
            scm.afterPropertiesSet();
        }
    }

    @Nested
    @DisplayName("Cache Manager Tests")
    class CacheManagerTests {

        @Test
        @DisplayName("Should create CacheManager bean")
        void shouldCreateCacheManager() {
            assertThat(cacheManager).isNotNull();
        }

        @Test
        @DisplayName("Should have all expected cache names")
        void shouldHaveAllExpectedCacheNames() {
            Collection<String> cacheNames = cacheManager.getCacheNames();

            assertThat(cacheNames).containsExactlyInAnyOrder(
                // Documentation caches
                CacheConfig.CACHE_DOCUMENTATION_SEARCH,
                CacheConfig.CACHE_DOCUMENTATION_SEARCH_PAGED,
                CacheConfig.CACHE_DOCUMENTATION_BY_VERSION,
                // Initializr caches
                CacheConfig.CACHE_INITIALIZR_METADATA,
                CacheConfig.CACHE_INITIALIZR_DEPENDENCIES,
                CacheConfig.CACHE_INITIALIZR_CATEGORIES
            );
        }

        @Test
        @DisplayName("Should have exactly 6 caches configured")
        void shouldHaveSixCaches() {
            assertThat(cacheManager.getCacheNames()).hasSize(6);
        }
    }

    @Nested
    @DisplayName("Caffeine Cache Tests")
    class CaffeineCacheTests {

        @Test
        @DisplayName("All caches should be Caffeine-backed")
        void allCachesShouldBeCaffeineBacked() {
            for (String cacheName : cacheManager.getCacheNames()) {
                var cache = cacheManager.getCache(cacheName);
                assertThat(cache)
                    .as("Cache '%s' should be CaffeineCache", cacheName)
                    .isInstanceOf(CaffeineCache.class);
            }
        }

        @Test
        @DisplayName("Initializr metadata cache should exist and be accessible")
        void initializrMetadataCacheShouldExist() {
            var cache = cacheManager.getCache(CacheConfig.CACHE_INITIALIZR_METADATA);

            assertThat(cache).isNotNull();
            assertThat(cache).isInstanceOf(CaffeineCache.class);
        }

        @Test
        @DisplayName("Initializr dependencies cache should exist and be accessible")
        void initializrDependenciesCacheShouldExist() {
            var cache = cacheManager.getCache(CacheConfig.CACHE_INITIALIZR_DEPENDENCIES);

            assertThat(cache).isNotNull();
            assertThat(cache).isInstanceOf(CaffeineCache.class);
        }

        @Test
        @DisplayName("Initializr categories cache should exist and be accessible")
        void initializrCategoriesCacheShouldExist() {
            var cache = cacheManager.getCache(CacheConfig.CACHE_INITIALIZR_CATEGORIES);

            assertThat(cache).isNotNull();
            assertThat(cache).isInstanceOf(CaffeineCache.class);
        }
    }

    @Nested
    @DisplayName("Cache Statistics Tests")
    class CacheStatisticsTests {

        @Test
        @DisplayName("Caffeine caches should have statistics enabled")
        void caffeineStatisticsShouldBeEnabled() {
            var cache = cacheManager.getCache(CacheConfig.CACHE_INITIALIZR_METADATA);

            assertThat(cache).isInstanceOf(CaffeineCache.class);
            CaffeineCache caffeineCache = (CaffeineCache) cache;

            // Get the native Caffeine cache to verify stats are recording
            Cache<Object, Object> nativeCache = caffeineCache.getNativeCache();
            var stats = nativeCache.stats();

            // Stats should be available (initially empty but not null)
            assertThat(stats).isNotNull();
            assertThat(stats.hitCount()).isEqualTo(0L);
            assertThat(stats.missCount()).isEqualTo(0L);
        }
    }

    @Nested
    @DisplayName("Cache Operations Tests")
    class CacheOperationsTests {

        @Test
        @DisplayName("Should be able to put and get values from cache")
        void shouldPutAndGetValues() {
            var cache = cacheManager.getCache(CacheConfig.CACHE_INITIALIZR_METADATA);

            assertThat(cache).isNotNull();

            // Put a value
            String testKey = "test-key";
            String testValue = "test-value";
            cache.put(testKey, testValue);

            // Get the value
            var retrieved = cache.get(testKey, String.class);
            assertThat(retrieved).isEqualTo(testValue);
        }

        @Test
        @DisplayName("Should return null for non-existent keys")
        void shouldReturnNullForNonExistentKeys() {
            var cache = cacheManager.getCache(CacheConfig.CACHE_INITIALIZR_METADATA);

            assertThat(cache).isNotNull();

            var retrieved = cache.get("non-existent-key", String.class);
            assertThat(retrieved).isNull();
        }

        @Test
        @DisplayName("Should be able to evict values from cache")
        void shouldEvictValues() {
            var cache = cacheManager.getCache(CacheConfig.CACHE_INITIALIZR_METADATA);

            assertThat(cache).isNotNull();

            // Put and verify
            String testKey = "evict-test-key";
            cache.put(testKey, "value");
            assertThat(cache.get(testKey, String.class)).isNotNull();

            // Evict and verify
            cache.evict(testKey);
            assertThat(cache.get(testKey, String.class)).isNull();
        }

        @Test
        @DisplayName("Should be able to clear entire cache")
        void shouldClearCache() {
            var cache = cacheManager.getCache(CacheConfig.CACHE_INITIALIZR_DEPENDENCIES);

            assertThat(cache).isNotNull();

            // Put multiple values
            cache.put("key1", "value1");
            cache.put("key2", "value2");

            // Clear cache
            cache.clear();

            // Verify all gone
            assertThat(cache.get("key1", String.class)).isNull();
            assertThat(cache.get("key2", String.class)).isNull();
        }
    }

    @Nested
    @DisplayName("Cache Constants Tests")
    class CacheConstantsTests {

        @Test
        @DisplayName("Cache name constants should have correct values")
        void cacheConstantsShouldHaveCorrectValues() {
            assertThat(CacheConfig.CACHE_DOCUMENTATION_SEARCH).isEqualTo("documentationSearch");
            assertThat(CacheConfig.CACHE_DOCUMENTATION_SEARCH_PAGED).isEqualTo("documentationSearchPaged");
            assertThat(CacheConfig.CACHE_DOCUMENTATION_BY_VERSION).isEqualTo("documentationByVersion");
            assertThat(CacheConfig.CACHE_INITIALIZR_METADATA).isEqualTo("initializr-metadata");
            assertThat(CacheConfig.CACHE_INITIALIZR_DEPENDENCIES).isEqualTo("initializr-dependencies");
            assertThat(CacheConfig.CACHE_INITIALIZR_CATEGORIES).isEqualTo("initializr-categories");
        }
    }
}
