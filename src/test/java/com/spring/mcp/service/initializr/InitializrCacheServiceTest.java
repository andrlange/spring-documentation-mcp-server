package com.spring.mcp.service.initializr;

import com.spring.mcp.config.CacheConfig;
import com.spring.mcp.config.InitializrProperties;
import com.spring.mcp.service.initializr.dto.BootVersion;
import com.spring.mcp.service.initializr.dto.DependencyCategory;
import com.spring.mcp.service.initializr.dto.DependencyInfo;
import com.spring.mcp.service.initializr.dto.InitializrMetadata;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Unit tests for InitializrCacheService.
 *
 * @author Spring MCP Server
 * @version 1.4.0
 * @since 2025-12-06
 */
@DisplayName("Initializr Cache Service Tests")
@ExtendWith(MockitoExtension.class)
class InitializrCacheServiceTest {

    @Mock
    private InitializrMetadataService metadataService;

    private CacheManager cacheManager;
    private InitializrProperties properties;
    private InitializrCacheService cacheService;

    @BeforeEach
    void setUp() {
        // Create a real cache manager for testing
        CacheConfig cacheConfig = new CacheConfig();
        cacheManager = cacheConfig.cacheManager();
        ((SimpleCacheManager) cacheManager).afterPropertiesSet();

        properties = new InitializrProperties();
        cacheService = new InitializrCacheService(cacheManager, metadataService, properties);
    }

    @Nested
    @DisplayName("Cache Operations Tests")
    class CacheOperationsTests {

        @Test
        @DisplayName("Should get cached metadata")
        void shouldGetCachedMetadata() {
            // Given
            InitializrMetadata expectedMetadata = new InitializrMetadata();
            when(metadataService.fetchMetadata()).thenReturn(expectedMetadata);

            // When
            InitializrMetadata result = cacheService.getCachedMetadata();

            // Then
            assertThat(result).isEqualTo(expectedMetadata);
            verify(metadataService).fetchMetadata();
        }

        @Test
        @DisplayName("Should get cached boot versions")
        void shouldGetCachedBootVersions() {
            // Given
            List<BootVersion> expectedVersions = List.of(
                BootVersion.builder().id("3.5.8").name("3.5.8").defaultVersion(true).build(),
                BootVersion.builder().id("3.4.12").name("3.4.12").build()
            );
            when(metadataService.getBootVersions()).thenReturn(expectedVersions);

            // When
            List<BootVersion> result = cacheService.getCachedBootVersions();

            // Then
            assertThat(result).isEqualTo(expectedVersions);
            verify(metadataService).getBootVersions();
        }

        @Test
        @DisplayName("Should get cached dependency categories")
        void shouldGetCachedDependencyCategories() {
            // Given
            List<DependencyCategory> expectedCategories = List.of(
                DependencyCategory.builder().name("Web").build(),
                DependencyCategory.builder().name("SQL").build()
            );
            when(metadataService.getDependencyCategories()).thenReturn(expectedCategories);

            // When
            List<DependencyCategory> result = cacheService.getCachedDependencyCategories();

            // Then
            assertThat(result).isEqualTo(expectedCategories);
            verify(metadataService).getDependencyCategories();
        }

        @Test
        @DisplayName("Should get cached dependencies")
        void shouldGetCachedDependencies() {
            // Given
            List<DependencyInfo> expectedDeps = List.of(
                DependencyInfo.builder().id("web").name("Spring Web").build(),
                DependencyInfo.builder().id("data-jpa").name("Spring Data JPA").build()
            );
            when(metadataService.getAllDependencies()).thenReturn(expectedDeps);

            // When
            List<DependencyInfo> result = cacheService.getCachedDependencies();

            // Then
            assertThat(result).isEqualTo(expectedDeps);
            verify(metadataService).getAllDependencies();
        }
    }

    @Nested
    @DisplayName("Cache Invalidation Tests")
    class CacheInvalidationTests {

        @Test
        @DisplayName("Should invalidate all caches without refresh")
        void shouldInvalidateAllCachesWithoutRefresh() {
            // Given - populate caches first
            cacheManager.getCache(CacheConfig.CACHE_INITIALIZR_METADATA).put("test", "value");
            cacheManager.getCache(CacheConfig.CACHE_INITIALIZR_DEPENDENCIES).put("test", "value");
            cacheManager.getCache(CacheConfig.CACHE_INITIALIZR_CATEGORIES).put("test", "value");

            // When
            cacheService.invalidateAndRefresh(false);

            // Then
            assertThat(cacheManager.getCache(CacheConfig.CACHE_INITIALIZR_METADATA).get("test")).isNull();
            assertThat(cacheManager.getCache(CacheConfig.CACHE_INITIALIZR_DEPENDENCIES).get("test")).isNull();
            assertThat(cacheManager.getCache(CacheConfig.CACHE_INITIALIZR_CATEGORIES).get("test")).isNull();
            verify(metadataService, never()).fetchMetadata();
        }

        @Test
        @DisplayName("Should invalidate all caches with immediate refresh")
        void shouldInvalidateAllCachesWithImmediateRefresh() {
            // Given
            when(metadataService.fetchMetadata()).thenReturn(new InitializrMetadata());

            // When
            cacheService.invalidateAndRefresh(true);

            // Then
            verify(metadataService).fetchMetadata();
            assertThat(cacheService.getLastRefreshTime()).isPresent();
        }

        @Test
        @DisplayName("Should invalidate metadata cache only")
        void shouldInvalidateMetadataCacheOnly() {
            // Given
            cacheManager.getCache(CacheConfig.CACHE_INITIALIZR_METADATA).put("test", "value");
            cacheManager.getCache(CacheConfig.CACHE_INITIALIZR_DEPENDENCIES).put("test", "value");

            // When
            cacheService.invalidateMetadataCache();

            // Then
            assertThat(cacheManager.getCache(CacheConfig.CACHE_INITIALIZR_METADATA).get("test")).isNull();
            assertThat(cacheManager.getCache(CacheConfig.CACHE_INITIALIZR_DEPENDENCIES).get("test")).isNotNull();
        }

        @Test
        @DisplayName("Should invalidate dependencies cache only")
        void shouldInvalidateDependenciesCacheOnly() {
            // Given
            cacheManager.getCache(CacheConfig.CACHE_INITIALIZR_METADATA).put("test", "value");
            cacheManager.getCache(CacheConfig.CACHE_INITIALIZR_DEPENDENCIES).put("test", "value");

            // When
            cacheService.invalidateDependenciesCache();

            // Then
            assertThat(cacheManager.getCache(CacheConfig.CACHE_INITIALIZR_METADATA).get("test")).isNotNull();
            assertThat(cacheManager.getCache(CacheConfig.CACHE_INITIALIZR_DEPENDENCIES).get("test")).isNull();
        }
    }

    @Nested
    @DisplayName("Cache Statistics Tests")
    class CacheStatisticsTests {

        @Test
        @DisplayName("Should return caching enabled status")
        void shouldReturnCachingEnabledStatus() {
            // Default is enabled
            assertThat(cacheService.isCachingEnabled()).isTrue();

            // Disable and check
            properties.getCache().setEnabled(false);
            assertThat(cacheService.isCachingEnabled()).isFalse();
        }

        @Test
        @DisplayName("Should get cache statistics for all caches")
        void shouldGetCacheStatisticsForAllCaches() {
            // When
            Map<String, InitializrCacheService.CacheStats> stats = cacheService.getCacheStatistics();

            // Then
            assertThat(stats).containsKeys(
                CacheConfig.CACHE_INITIALIZR_METADATA,
                CacheConfig.CACHE_INITIALIZR_DEPENDENCIES,
                CacheConfig.CACHE_INITIALIZR_CATEGORIES
            );
        }

        @Test
        @DisplayName("Should get cache summary")
        void shouldGetCacheSummary() {
            // When
            InitializrCacheService.CacheSummary summary = cacheService.getCacheSummary();

            // Then
            assertThat(summary).isNotNull();
            assertThat(summary.cachingEnabled()).isTrue();
            assertThat(summary.totalHits()).isGreaterThanOrEqualTo(0);
            assertThat(summary.totalMisses()).isGreaterThanOrEqualTo(0);
        }

        @Test
        @DisplayName("Should track last refresh time")
        void shouldTrackLastRefreshTime() {
            // Initially empty
            assertThat(cacheService.getLastRefreshTime()).isEmpty();

            // After refresh
            when(metadataService.fetchMetadata()).thenReturn(new InitializrMetadata());
            cacheService.invalidateAndRefresh(true);

            assertThat(cacheService.getLastRefreshTime()).isPresent();
        }
    }

    @Nested
    @DisplayName("Cache Warm-up Tests")
    class CacheWarmUpTests {

        @Test
        @DisplayName("Should warm up cache successfully")
        void shouldWarmUpCacheSuccessfully() {
            // Given
            when(metadataService.fetchMetadata()).thenReturn(new InitializrMetadata());
            when(metadataService.getDependencyCategories()).thenReturn(List.of());
            when(metadataService.getBootVersions()).thenReturn(List.of());

            // When
            cacheService.warmUpCache();

            // Then
            verify(metadataService).fetchMetadata();
            verify(metadataService).getDependencyCategories();
            verify(metadataService).getBootVersions();
            assertThat(cacheService.getLastRefreshTime()).isPresent();
        }

        @Test
        @DisplayName("Should handle warm-up failure gracefully")
        void shouldHandleWarmUpFailureGracefully() {
            // Given
            when(metadataService.fetchMetadata())
                .thenThrow(new RuntimeException("API error"));

            // When - should not throw
            cacheService.warmUpCache();

            // Then - no refresh time set on failure
            assertThat(cacheService.getLastRefreshTime()).isEmpty();
        }
    }

    @Nested
    @DisplayName("Disabled Cache Tests")
    class DisabledCacheTests {

        @Test
        @DisplayName("Should fetch fresh data when cache disabled")
        void shouldFetchFreshDataWhenCacheDisabled() {
            // Given
            properties.getCache().setEnabled(false);
            when(metadataService.fetchMetadata()).thenReturn(new InitializrMetadata());

            // When
            cacheService.getCachedMetadata();

            // Then
            verify(metadataService).fetchMetadata();
        }
    }
}
