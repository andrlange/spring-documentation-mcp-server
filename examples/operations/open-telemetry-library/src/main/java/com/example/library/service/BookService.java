package com.example.library.service;

import com.example.library.data.DataInitializer;
import com.example.library.model.Book;
import io.micrometer.observation.annotation.Observed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Service for book operations with observability.
 */
@Service
public class BookService {

    private static final Logger log = LoggerFactory.getLogger(BookService.class);
    private final DataInitializer dataInitializer;

    public BookService(DataInitializer dataInitializer) {
        this.dataInitializer = dataInitializer;
    }

    @Observed(name = "library.book.findAll", contextualName = "find-all-books")
    public Collection<Book> findAll() {
        log.info("Finding all books");
        return dataInitializer.getBooks().values();
    }

    @Observed(name = "library.book.findByIsbn", contextualName = "find-book-by-isbn")
    public Optional<Book> findByIsbn(String isbn) {
        log.info("Finding book by ISBN: {}", isbn);
        return Optional.ofNullable(dataInitializer.getBooks().get(isbn));
    }

    @Observed(name = "library.book.findByCategory", contextualName = "find-books-by-category")
    public List<Book> findByCategory(Long categoryId) {
        log.info("Finding books by category: {}", categoryId);
        return dataInitializer.getBooks().values().stream()
            .filter(book -> book.categoryId().equals(categoryId))
            .toList();
    }

    @Observed(name = "library.book.search", contextualName = "search-books")
    public List<Book> search(String query) {
        log.info("Searching books with query: {}", query);
        String lowerQuery = query.toLowerCase();
        return dataInitializer.getBooks().values().stream()
            .filter(book ->
                book.title().toLowerCase().contains(lowerQuery) ||
                book.author().toLowerCase().contains(lowerQuery) ||
                book.isbn().contains(query))
            .toList();
    }

    @Observed(name = "library.book.findAvailable", contextualName = "find-available-books")
    public List<Book> findAvailable() {
        log.info("Finding available books");
        return dataInitializer.getBooks().values().stream()
            .filter(Book::isAvailable)
            .toList();
    }

    public void updateBook(Book book) {
        dataInitializer.getBooks().put(book.isbn(), book);
    }

    public long count() {
        return dataInitializer.getBooks().size();
    }

    public long countAvailable() {
        return dataInitializer.getBooks().values().stream()
            .mapToInt(Book::availableCopies)
            .sum();
    }

    public long countByCategory(Long categoryId) {
        return dataInitializer.getBooks().values().stream()
            .filter(book -> book.categoryId().equals(categoryId))
            .count();
    }
}
