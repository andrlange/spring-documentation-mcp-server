package com.example.cache;

import com.example.cache.config.CacheConfig;
import com.example.cache.model.Book;
import com.example.cache.repository.MockBookRepository;
import com.example.cache.service.BookService;
import com.example.cache.service.CacheStatsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class CaffeineCachingDemoApplicationTests {

    @Autowired
    private BookService bookService;

    @Autowired
    private CacheStatsService cacheStatsService;

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private MockBookRepository mockBookRepository;

    @BeforeEach
    void setUp() {
        // Clear caches before each test
        bookService.clearCache();
    }

    @Test
    @DisplayName("Application context loads successfully")
    void contextLoads() {
        assertThat(bookService).isNotNull();
        assertThat(cacheStatsService).isNotNull();
        assertThat(cacheManager).isNotNull();
    }

    @Test
    @DisplayName("Cache manager is configured with correct caches")
    void cacheManagerConfigured() {
        assertThat(cacheManager.getCacheNames())
            .contains(CacheConfig.BOOK_SEARCH_CACHE, CacheConfig.BOOK_BY_ID_CACHE);
    }

    @Test
    @DisplayName("Mock repository contains 1000 books")
    void repositoryContains1000Books() {
        assertThat(mockBookRepository.count()).isEqualTo(1000);
    }

    @Test
    @DisplayName("Search results are cached")
    void searchResultsAreCached() {
        String query = "fantasy";

        // First call - cache miss
        long start1 = System.currentTimeMillis();
        List<Book> result1 = bookService.searchBooks(query);
        long duration1 = System.currentTimeMillis() - start1;

        // Second call - cache hit
        long start2 = System.currentTimeMillis();
        List<Book> result2 = bookService.searchBooks(query);
        long duration2 = System.currentTimeMillis() - start2;

        // Verify results are the same
        assertThat(result2).isEqualTo(result1);

        // Second call should be significantly faster (cached)
        assertThat(duration2).isLessThan(duration1);
    }

    @Test
    @DisplayName("Cache statistics are recorded")
    void cacheStatisticsRecorded() {
        // Perform some cache operations
        bookService.searchBooks("science");
        bookService.searchBooks("science"); // Hit
        bookService.searchBooks("fiction");

        // Get statistics
        var stats = cacheStatsService.getAggregatedStats();

        // Should have recorded requests
        assertThat(stats.requestCount()).isGreaterThan(0);
    }

    @Test
    @DisplayName("Cache clear evicts all entries")
    void cacheClearEvictsEntries() {
        // Populate cache
        bookService.searchBooks("mystery");
        bookService.searchBooks("thriller");

        // Clear cache
        bookService.clearCache();

        // Get statistics - size should be 0 after clear
        var stats = cacheStatsService.getAllCacheStats();
        var searchCacheStats = stats.get(CacheConfig.BOOK_SEARCH_CACHE);

        assertThat(searchCacheStats.estimatedSize()).isZero();
    }

    @Test
    @DisplayName("Find all returns all books")
    void findAllReturnsAllBooks() {
        List<Book> allBooks = bookService.findAll();
        assertThat(allBooks).hasSize(1000);
    }

    @Test
    @DisplayName("Book record has correct structure")
    void bookRecordStructure() {
        List<Book> books = bookService.findAll();
        Book book = books.getFirst();

        assertThat(book.id()).isNotNull();
        assertThat(book.title()).isNotBlank();
        assertThat(book.author()).isNotBlank();
        assertThat(book.isbn()).matches("978-0-\\d{6}-\\d");
        assertThat(book.genre()).isNotBlank();
        assertThat(book.publicationYear()).isBetween(1950, 2025);
        assertThat(book.description()).isNotBlank();
    }
}
