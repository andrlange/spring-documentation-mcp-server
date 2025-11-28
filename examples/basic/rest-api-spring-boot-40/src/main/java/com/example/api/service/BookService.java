package com.example.api.service;

import com.example.api.model.Book;
import com.example.api.model.CreateBookRequest;
import com.example.api.model.UpdateBookRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service layer for Book operations.
 * Uses in-memory storage with ConcurrentHashMap for thread-safety.
 */
@Service
public class BookService {

    private final Map<String, Book> books = new ConcurrentHashMap<>();

    public BookService() {
        // Initialize with sample data
        initializeSampleData();
    }

    private void initializeSampleData() {
        var book1 = Book.create(
            "Spring in Action",
            "Craig Walls",
            "978-1617294945",
            LocalDate.of(2018, 10, 5),
            "Technology",
            520
        );
        var book2 = Book.create(
            "Clean Code",
            "Robert C. Martin",
            "978-0132350884",
            LocalDate.of(2008, 8, 1),
            "Technology",
            464
        );
        var book3 = Book.create(
            "Effective Java",
            "Joshua Bloch",
            "978-0134685991",
            LocalDate.of(2018, 1, 6),
            "Technology",
            416
        );

        books.put(book1.id(), book1);
        books.put(book2.id(), book2);
        books.put(book3.id(), book3);
    }

    /**
     * Get all books.
     */
    public List<Book> findAll() {
        return new ArrayList<>(books.values());
    }

    /**
     * Find book by ID.
     */
    public Optional<Book> findById(String id) {
        return Optional.ofNullable(books.get(id));
    }

    /**
     * Search books by title (case-insensitive partial match).
     */
    public List<Book> searchByTitle(String title) {
        String lowerTitle = title.toLowerCase();
        return books.values().stream()
            .filter(book -> book.title().toLowerCase().contains(lowerTitle))
            .toList();
    }

    /**
     * Search books by author (case-insensitive partial match).
     */
    public List<Book> searchByAuthor(String author) {
        String lowerAuthor = author.toLowerCase();
        return books.values().stream()
            .filter(book -> book.author().toLowerCase().contains(lowerAuthor))
            .toList();
    }

    /**
     * Create a new book.
     */
    public Book create(CreateBookRequest request) {
        Book book = request.toBook();
        books.put(book.id(), book);
        return book;
    }

    /**
     * Update an existing book.
     */
    public Optional<Book> update(String id, UpdateBookRequest request) {
        return findById(id).map(existing -> {
            Book updated = new Book(
                existing.id(),
                request.title() != null ? request.title() : existing.title(),
                request.author() != null ? request.author() : existing.author(),
                request.isbn() != null ? request.isbn() : existing.isbn(),
                request.publishedDate() != null ? request.publishedDate() : existing.publishedDate(),
                request.genre() != null ? request.genre() : existing.genre(),
                request.pages() != null ? request.pages() : existing.pages()
            );
            books.put(id, updated);
            return updated;
        });
    }

    /**
     * Delete a book by ID.
     */
    public boolean delete(String id) {
        return books.remove(id) != null;
    }

    /**
     * Get total count of books.
     */
    public int count() {
        return books.size();
    }
}
