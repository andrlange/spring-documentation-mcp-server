package com.example.cache.service;

import com.example.cache.config.CacheConfig;
import com.example.cache.model.Book;
import com.example.cache.repository.MockBookRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Service layer for book operations with caching enabled.
 *
 * Uses Spring's @Cacheable annotation to automatically cache
 * method results in Caffeine cache. Cache entries expire after
 * 60 seconds as configured in CacheConfig.
 */
@Service
public class BookService {

    private final MockBookRepository repository;

    public BookService(MockBookRepository repository) {
        this.repository = repository;
    }

    /**
     * Search books by query with caching.
     *
     * First call: Hits the repository (100ms latency simulated)
     * Subsequent calls (within 60s): Returns cached result instantly
     *
     * @param query Search query (title, author, genre, ISBN)
     * @return List of matching books
     */
    @Cacheable(value = CacheConfig.BOOK_SEARCH_CACHE, key = "#query")
    public List<Book> searchBooks(String query) {
        return repository.search(query);
    }

    /**
     * Find a book by ID with caching.
     *
     * First call: Hits the repository (100ms latency simulated)
     * Subsequent calls (within 60s): Returns cached result instantly
     *
     * @param id Book ID
     * @return Optional containing the book if found
     */
    @Cacheable(value = CacheConfig.BOOK_BY_ID_CACHE, key = "#id")
    public Optional<Book> findById(Long id) {
        return repository.findById(id);
    }

    /**
     * Get all books (not cached - returns full list).
     * Used for initial page load showing first 50 books.
     *
     * @return List of all 1000 books
     */
    public List<Book> findAll() {
        return repository.findAll();
    }

    /**
     * Get total book count.
     *
     * @return Number of books in the repository
     */
    public int getBookCount() {
        return repository.count();
    }

    /**
     * Clear all caches.
     * Use this to manually invalidate cached data.
     */
    @CacheEvict(value = {CacheConfig.BOOK_SEARCH_CACHE, CacheConfig.BOOK_BY_ID_CACHE}, allEntries = true)
    public void clearCache() {
        // Cache cleared via annotation
    }
}
