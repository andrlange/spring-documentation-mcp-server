package com.example.library.model;

/**
 * View model for displaying books with category name.
 */
public record BookView(
    String isbn,
    String title,
    String author,
    String category,
    int publishYear,
    int availableCopies,
    int totalCopies
) {
    /**
     * Create a BookView from a Book and category name.
     */
    public static BookView from(Book book, String categoryName) {
        return new BookView(
            book.isbn(),
            book.title(),
            book.author(),
            categoryName,
            book.publicationYear(),
            book.availableCopies(),
            book.totalCopies()
        );
    }
}
