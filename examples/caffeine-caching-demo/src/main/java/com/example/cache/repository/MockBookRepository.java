package com.example.cache.repository;

import com.example.cache.model.Book;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

/**
 * Mock repository that generates 1000 books in-memory.
 * Simulates database latency to demonstrate caching benefits.
 */
@Repository
public class MockBookRepository {

    private final Map<Long, Book> books;

    private static final String[] GENRES = {
        "Fiction", "Science Fiction", "Fantasy", "Mystery",
        "Thriller", "Romance", "Horror", "Non-Fiction",
        "Biography", "History", "Science", "Technology",
        "Philosophy", "Psychology", "Self-Help", "Adventure"
    };

    private static final String[] AUTHORS = {
        "J.K. Rowling", "Stephen King", "George R.R. Martin",
        "Isaac Asimov", "Agatha Christie", "Dan Brown",
        "Neil Gaiman", "Terry Pratchett", "Brandon Sanderson",
        "Patrick Rothfuss", "Robert Jordan", "Jim Butcher",
        "Margaret Atwood", "Ursula K. Le Guin", "Philip K. Dick",
        "Arthur C. Clarke", "Frank Herbert", "Ray Bradbury",
        "H.P. Lovecraft", "Edgar Allan Poe"
    };

    private static final String[] TITLE_PREFIXES = {
        "The Chronicles of", "Adventures in", "Secrets of",
        "The Last", "Journey to", "Tales from",
        "The Art of", "Introduction to", "Mastering",
        "The Hidden", "Beyond the", "Inside"
    };

    private static final String[] TITLE_SUFFIXES = {
        "the Unknown", "Destiny", "Tomorrow",
        "the Stars", "Shadows", "Dreams",
        "Time", "Space", "Reality",
        "the Mind", "Creation", "Discovery"
    };

    public MockBookRepository() {
        this.books = LongStream.rangeClosed(1, 1000)
            .boxed()
            .collect(Collectors.toMap(
                id -> id,
                this::generateBook
            ));
    }

    private Book generateBook(Long id) {
        Random random = new Random(id);
        String title = generateTitle(id, random);
        String author = AUTHORS[random.nextInt(AUTHORS.length)];
        String genre = GENRES[random.nextInt(GENRES.length)];
        int year = 1950 + random.nextInt(75);

        return new Book(
            id,
            title,
            author,
            generateIsbn(id),
            genre,
            year,
            generateDescription(title, author, genre, year)
        );
    }

    private String generateTitle(Long id, Random random) {
        String prefix = TITLE_PREFIXES[random.nextInt(TITLE_PREFIXES.length)];
        String suffix = TITLE_SUFFIXES[random.nextInt(TITLE_SUFFIXES.length)];
        return prefix + " " + suffix + " (Vol. " + id + ")";
    }

    private String generateIsbn(Long id) {
        return String.format("978-0-%06d-%d", id, id % 10);
    }

    private String generateDescription(String title, String author, String genre, int year) {
        return String.format(
            "A captivating %s work by %s. Originally published in %d, '%s' " +
            "has become a beloved classic in its genre. This edition includes " +
            "a new foreword and comprehensive annotations.",
            genre.toLowerCase(), author, year, title
        );
    }

    /**
     * Find all books (returns all 1000 books).
     * No latency simulation for the full list.
     */
    public List<Book> findAll() {
        return new ArrayList<>(books.values());
    }

    /**
     * Find a book by ID with simulated database latency.
     */
    public Optional<Book> findById(Long id) {
        simulateLatency();
        return Optional.ofNullable(books.get(id));
    }

    /**
     * Search books by query with simulated database latency.
     * Searches across title, author, genre, and ISBN.
     */
    public List<Book> search(String query) {
        simulateLatency();
        String lowerQuery = query.toLowerCase();
        return books.values().stream()
            .filter(book ->
                book.title().toLowerCase().contains(lowerQuery) ||
                book.author().toLowerCase().contains(lowerQuery) ||
                book.genre().toLowerCase().contains(lowerQuery) ||
                book.isbn().contains(query)
            )
            .collect(Collectors.toList());
    }

    /**
     * Simulates 100ms database latency to demonstrate caching benefits.
     */
    private void simulateLatency() {
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Returns the total number of books in the repository.
     */
    public int count() {
        return books.size();
    }
}
