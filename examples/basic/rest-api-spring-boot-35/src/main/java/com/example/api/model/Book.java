package com.example.api.model;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Book entity using Java Record (modern Java 21 feature).
 * Records provide immutable data carriers with auto-generated
 * equals(), hashCode(), and toString() methods.
 */
public record Book(
    String id,
    String title,
    String author,
    String isbn,
    LocalDate publishedDate,
    String genre,
    int pages
) {
    /**
     * Factory method to create a new Book with auto-generated ID.
     */
    public static Book create(String title, String author, String isbn,
                              LocalDate publishedDate, String genre, int pages) {
        return new Book(UUID.randomUUID().toString(), title, author, isbn,
                       publishedDate, genre, pages);
    }

    /**
     * Create a copy with updated fields.
     */
    public Book withTitle(String newTitle) {
        return new Book(id, newTitle, author, isbn, publishedDate, genre, pages);
    }

    public Book withAuthor(String newAuthor) {
        return new Book(id, title, newAuthor, isbn, publishedDate, genre, pages);
    }
}
